package com.momento.data.realtime

import ai.nexconn.call.api.NCCallEngine
import ai.nexconn.call.api.handler.NCCallAPIResultHandler
import ai.nexconn.call.api.handler.NCCallEventHandler
import ai.nexconn.call.api.handler.event.NCCallConnectedEvent
import ai.nexconn.call.api.handler.event.NCCallEndedEvent
import ai.nexconn.call.api.handler.event.NCCallLocalCallLogReceivedEvent
import ai.nexconn.call.api.handler.event.NCCallMediaTypeChangeRequestReceivedEvent
import ai.nexconn.call.api.handler.event.NCCallMediaTypeChangeResultReceivedEvent
import ai.nexconn.call.api.handler.event.NCCallReceivedEvent
import ai.nexconn.call.api.handler.event.NCCallRemoteCameraStateChangedEvent
import ai.nexconn.call.api.handler.event.NCCallRemoteMicrophoneStateChangedEvent
import ai.nexconn.call.api.handler.event.NCCallRemoteUserInvitedEvent
import ai.nexconn.call.api.handler.event.NCCallRemoteUserStateChangedEvent
import ai.nexconn.call.api.handler.event.NCCallServerCallLogReceivedEvent
import ai.nexconn.call.api.handler.event.NCCallStartTimeReceivedEvent
import ai.nexconn.call.api.handler.event.NCCallTypeChangedEvent
import ai.nexconn.call.api.handler.result.NCCallAcceptCallResult
import ai.nexconn.call.api.handler.result.NCCallEndCallResult
import ai.nexconn.call.api.handler.result.NCCallJoinCallResult
import ai.nexconn.call.api.handler.result.NCCallStartCallResult
import ai.nexconn.call.api.model.NCCallCode
import ai.nexconn.call.api.model.NCCallMediaType
import ai.nexconn.call.api.model.NCCallSession
import ai.nexconn.call.api.model.NCCallType
import ai.nexconn.call.api.params.NCCallAcceptCallParams
import ai.nexconn.call.api.params.NCCallEndCallParams
import ai.nexconn.call.api.params.NCCallInitParams
import ai.nexconn.call.api.params.NCCallJoinCallParams
import ai.nexconn.call.api.params.NCCallStartCallParams
import ai.nexconn.chat.NCEngine
import ai.nexconn.chat.channel.DirectChannel
import ai.nexconn.chat.error.NCError
import ai.nexconn.chat.handler.ConnectHandler
import ai.nexconn.chat.handler.MessageHandler
import ai.nexconn.chat.handler.SendMessageHandler
import ai.nexconn.chat.message.Message
import ai.nexconn.chat.message.TextMessage
import ai.nexconn.chat.message.model.MessageReceivedEvent
import ai.nexconn.chat.params.AreaCode
import ai.nexconn.chat.params.ConnectParams
import ai.nexconn.chat.params.InitParams
import ai.nexconn.chat.params.LogLevel
import ai.nexconn.chat.params.SendMessageParams
import com.momento.core.common.AppError
import com.momento.core.common.AppResult
import com.momento.core.common.map
import com.momento.data.remote.MomentoApi
import com.momento.data.remote.NexconnTokenDto
import com.momento.domain.realtime.ChatMessage
import com.momento.domain.realtime.RealtimeCall
import com.momento.domain.realtime.RealtimeConnectionState
import com.momento.domain.realtime.RealtimeSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import android.content.Context

/**
 * Nexconn-backed [RealtimeSession].
 *
 * Chat and Call are initialized lazily on the first successful call to
 * [connect], using the short-lived access token minted by our backend
 * (`POST /api/v1/communications/nexconn/token`). The App Secret never reaches
 * this process — only the public App Key and the user token do.
 *
 * Threading: SDK entry points are invoked on the main thread, as the SDK
 * expects. Blocking network work happens on the IO dispatcher.
 */
@Singleton
class NexconnRealtime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MomentoApi
) : RealtimeSession {

    private val connectMutex = Mutex()

    private val _connectionState = MutableStateFlow(RealtimeConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<RealtimeConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    override val incomingMessages: SharedFlow<ChatMessage> = _incomingMessages.asSharedFlow()

    private val _incomingCalls = MutableSharedFlow<RealtimeCall>(extraBufferCapacity = 8)
    override val incomingCalls: SharedFlow<RealtimeCall> = _incomingCalls.asSharedFlow()

    private val _activeCall = MutableStateFlow<RealtimeCall?>(null)
    override val activeCall: StateFlow<RealtimeCall?> = _activeCall.asStateFlow()

    private var chatInitialized = false
    private var chatHandlersRegistered = false
    private var callInstalled = false
    private var callInitialized = false
    private var currentUserId: String? = null

    private var connectDeferred: CompletableDeferred<Unit>? = null
    private var startCallDeferred: CompletableDeferred<String>? = null
    private var acceptCallDeferred: CompletableDeferred<String>? = null
    private var joinCallDeferred: CompletableDeferred<String>? = null
    private var endCallDeferred: CompletableDeferred<Unit>? = null

    private val messageHandler = object : MessageHandler {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            val message = event.message
            val text = (message.content as? TextMessage)?.text ?: return
            val peerId = message.channelIdentifier.channelId
            val sender = message.senderUserId.orEmpty()
            _incomingMessages.tryEmit(
                ChatMessage(
                    id = message.messageId.orEmpty(),
                    peerId = peerId,
                    senderId = sender,
                    text = text,
                    sentAtMillis = message.sentTime,
                    outgoing = sender == currentUserId
                )
            )
        }
    }

    private val callEventHandler = object : NCCallEventHandler {
        override fun onCallReceived(event: NCCallReceivedEvent) {
            val session = event.callSession ?: return
            _incomingCalls.tryEmit(session.toRealtimeCall(incoming = true))
        }

        override fun onCallConnected(event: NCCallConnectedEvent) {
            event.callSession?.let { _activeCall.value = it.toRealtimeCall(incoming = false) }
        }

        override fun onCallEnded(event: NCCallEndedEvent) {
            _activeCall.value = null
            endCallDeferred?.complete(Unit)
            endCallDeferred = null
        }

        override fun onRemoteUserInvited(event: NCCallRemoteUserInvitedEvent) = Unit

        override fun onRemoteUserStateChanged(event: NCCallRemoteUserStateChangedEvent) = Unit

        override fun onServerCallLogReceived(event: NCCallServerCallLogReceivedEvent) = Unit

        override fun onRemoteMicrophoneStateChanged(event: NCCallRemoteMicrophoneStateChangedEvent) = Unit

        override fun onRemoteCameraStateChanged(event: NCCallRemoteCameraStateChangedEvent) = Unit

        override fun onMediaTypeChangeRequestReceived(event: NCCallMediaTypeChangeRequestReceivedEvent) = Unit

        override fun onMediaTypeChangeResultReceived(event: NCCallMediaTypeChangeResultReceivedEvent) = Unit

        override fun onCallTypeChanged(event: NCCallTypeChangedEvent) = Unit

        override fun onLocalCallLogReceived(event: NCCallLocalCallLogReceivedEvent) = Unit

        override fun onStartTimeReceived(event: NCCallStartTimeReceivedEvent) = Unit
    }

    private val callResultHandler = object : NCCallAPIResultHandler {
        override fun onStartCall(result: NCCallStartCallResult) {
            val callId = result.callId
            if (result.code == NCCallCode.SUCCESS && callId != null) {
                startCallDeferred?.complete(callId)
            } else {
                startCallDeferred?.completeExceptionally(callError("startCall", result.code))
            }
            startCallDeferred = null
        }

        override fun onAcceptCall(result: NCCallAcceptCallResult) {
            val callId = result.callId
            if (result.code == NCCallCode.SUCCESS && callId != null) {
                acceptCallDeferred?.complete(callId)
            } else {
                acceptCallDeferred?.completeExceptionally(callError("acceptCall", result.code))
            }
            acceptCallDeferred = null
        }

        override fun onJoinCall(result: NCCallJoinCallResult) {
            val callId = result.callId
            if (result.code == NCCallCode.SUCCESS && callId != null) {
                joinCallDeferred?.complete(callId)
            } else {
                joinCallDeferred?.completeExceptionally(callError("joinCall", result.code))
            }
            joinCallDeferred = null
        }

        override fun onEndCall(result: NCCallEndCallResult) {
            if (result.code == NCCallCode.SUCCESS) {
                _activeCall.value = null
                endCallDeferred?.complete(Unit)
            } else {
                endCallDeferred?.completeExceptionally(callError("endCall", result.code))
            }
            endCallDeferred = null
        }
    }

    override suspend fun connect(): AppResult<Unit> = connectMutex.withLock {
        if (_connectionState.value == RealtimeConnectionState.CONNECTED) {
            return@withLock AppResult.Success(Unit)
        }
        _connectionState.value = RealtimeConnectionState.CONNECTING

        val outcome = runCatching {
            val token = withContext(Dispatchers.IO) { api.nexconnToken() }
            val appKey = token.appKey?.takeIf { it.isNotBlank() }
                ?: throw NexconnNotConfiguredException()
            currentUserId = token.userId
            withContext(Dispatchers.Main.immediate) { awaitConnected(appKey, token) }
        }

        outcome.fold(
            onSuccess = {
                _connectionState.value = RealtimeConnectionState.CONNECTED
                AppResult.Success(Unit)
            },
            onFailure = { error ->
                _connectionState.value = when {
                    error is NexconnNotConfiguredException -> RealtimeConnectionState.NO_CREDENTIALS
                    error is HttpException && error.code() in setOf(503, 501) ->
                        RealtimeConnectionState.NO_CREDENTIALS
                    else -> RealtimeConnectionState.ERROR
                }
                AppResult.Failure(
                    AppError(
                        message = error.message ?: "Could not connect to the real-time service",
                        cause = error,
                        isNetworkError = error is java.io.IOException
                    )
                )
            }
        )
    }

    private suspend fun awaitConnected(appKey: String, token: NexconnTokenDto) {
        installEngines(appKey, token.areaCode)

        val deferred = CompletableDeferred<Unit>()
        connectDeferred = deferred
        NCEngine.connect(
            ConnectParams(token.accessToken),
            object : ConnectHandler {
                override fun onResult(userId: String?, error: NCError?) {
                    when {
                        error != null -> deferred.completeExceptionally(error)
                        userId != null -> deferred.complete(Unit)
                        else -> deferred.completeExceptionally(
                            IllegalStateException("Nexconn connection ended without a user id")
                        )
                    }
                }
            }
        )

        val connected = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { deferred.await() }
        connectDeferred = null
        if (connected == null) {
            throw IllegalStateException("Timed out connecting to the real-time service")
        }

        // Call SDK must be initialized only after Chat is connected.
        if (!callInitialized) {
            runCatching {
                NCCallEngine.initialize(NCCallInitParams(context.applicationContext))
                NCCallEngine.getInstance().setCallEventHandler(callEventHandler)
                NCCallEngine.getInstance().setAPIResultHandler(callResultHandler)
                callInitialized = true
            }
        }
    }

    private fun installEngines(appKey: String, areaCode: String?) {
        if (!callInstalled) {
            // Call install must precede Chat initialization.
            NCCallEngine.install()
            callInstalled = true
        }
        if (!chatInitialized) {
            val params = InitParams(context.applicationContext, appKey).apply {
                this.areaCode = areaCode(areaCode)
                logLevel = if (isDebuggable()) LogLevel.INFO else LogLevel.WARN
            }
            NCEngine.initialize(params)
            chatInitialized = true
        }
        if (!chatHandlersRegistered) {
            NCEngine.addMessageHandler(MESSAGE_HANDLER_ID, messageHandler)
            chatHandlersRegistered = true
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.Main.immediate) {
            runCatching { NCEngine.disconnect(false) }
        }
        _connectionState.value = RealtimeConnectionState.DISCONNECTED
        _activeCall.value = null
        currentUserId = null
    }

    override suspend fun sendText(peerId: String, text: String): AppResult<ChatMessage> {
        val body = text.trim()
        if (body.isEmpty()) return AppResult.Failure(AppError("Message cannot be empty"))
        if (_connectionState.value != RealtimeConnectionState.CONNECTED) {
            return AppResult.Failure(AppError("Not connected to messaging"))
        }

        return withContext(Dispatchers.Main.immediate) {
            val result = CompletableDeferred<ChatMessage>()
            DirectChannel(peerId).sendMessage(
                SendMessageParams(TextMessage(body)),
                object : SendMessageHandler {
                    override fun onAttached(message: Message) = Unit

                    override fun onResult(message: Message?, error: NCError?) {
                        if (error != null || message == null) {
                            result.completeExceptionally(
                                error ?: IllegalStateException("Message was not accepted")
                            )
                        } else {
                            result.complete(
                                ChatMessage(
                                    id = message.messageId.orEmpty(),
                                    peerId = peerId,
                                    senderId = currentUserId ?: message.senderUserId.orEmpty(),
                                    text = body,
                                    sentAtMillis = message.sentTime,
                                    outgoing = true
                                )
                            )
                        }
                    }
                }
            )
            settle(result, SEND_TIMEOUT_MS, "Message send timed out")
        }
    }

    override suspend fun startCall(peerId: String, video: Boolean): AppResult<String> {
        if (!callReady()) return notConnected()
        return withContext(Dispatchers.Main.immediate) {
            val deferred = CompletableDeferred<String>()
            startCallDeferred = deferred
            val params = NCCallStartCallParams(
                listOf(peerId),
                NCCallType.SINGLE,
                if (video) NCCallMediaType.AUDIO_VIDEO else NCCallMediaType.AUDIO
            )
            NCCallEngine.getInstance().startCall(params)
            settle(deferred, CALL_TIMEOUT_MS, "Call did not start").onSuccess { callId ->
                _activeCall.value = RealtimeCall(callId, peerId, video, group = false, incoming = false)
            }
        }
    }

    override suspend fun startAudioRoom(roomId: String, participants: List<String>): AppResult<String> {
        if (!callReady()) return notConnected()
        val callees = participants.filter { it.isNotBlank() }.distinct()
        if (callees.isEmpty()) return AppResult.Failure(AppError("An audio room needs at least one host"))
        return withContext(Dispatchers.Main.immediate) {
            val deferred = CompletableDeferred<String>()
            startCallDeferred = deferred
            val params = NCCallStartCallParams(
                callees,
                NCCallType.MULTI,
                NCCallMediaType.AUDIO
            ).apply { targetId = roomId }
            NCCallEngine.getInstance().startCall(params)
            settle(deferred, CALL_TIMEOUT_MS, "Audio room did not start").onSuccess { callId ->
                _activeCall.value = RealtimeCall(
                    callId = callId,
                    peerId = roomId,
                    video = false,
                    group = true,
                    incoming = false
                )
            }
        }
    }

    override suspend fun joinCall(callId: String): AppResult<Unit> {
        if (!callReady()) return notConnected()
        return withContext(Dispatchers.Main.immediate) {
            val deferred = CompletableDeferred<String>()
            joinCallDeferred = deferred
            NCCallEngine.getInstance().joinCall(NCCallJoinCallParams(callId))
            settle(deferred, CALL_TIMEOUT_MS, "Could not join the room").onSuccess {
                _activeCall.value = RealtimeCall(callId, callId, video = false, group = true, incoming = false)
            }.map { }
        }
    }

    override suspend fun acceptCall(callId: String): AppResult<Unit> {
        if (!callReady()) return notConnected()
        return withContext(Dispatchers.Main.immediate) {
            val deferred = CompletableDeferred<String>()
            acceptCallDeferred = deferred
            NCCallEngine.getInstance().acceptCall(NCCallAcceptCallParams(callId))
            settle(deferred, CALL_TIMEOUT_MS, "Could not accept the call").onSuccess {
                _activeCall.value = _activeCall.value ?: RealtimeCall(callId, "", video = false, group = false, incoming = true)
            }.map { }
        }
    }

    override suspend fun endCall(callId: String): AppResult<Unit> {
        if (!callReady()) return notConnected()
        return withContext(Dispatchers.Main.immediate) {
            val deferred = CompletableDeferred<Unit>()
            endCallDeferred = deferred
            NCCallEngine.getInstance().endCall(NCCallEndCallParams(callId))
            settle(deferred, CALL_TIMEOUT_MS, "Could not end the call")
        }
    }

    override fun setMicrophoneEnabled(enabled: Boolean) {
        if (callInitialized) runCatching { NCCallEngine.getInstance().enableMicrophone(enabled) }
    }

    override fun setSpeakerEnabled(enabled: Boolean) {
        if (callInitialized) runCatching { NCCallEngine.getInstance().enableSpeaker(enabled) }
    }

    override fun switchCamera() {
        if (callInitialized) runCatching { NCCallEngine.getInstance().switchCamera() }
    }

    private fun callReady(): Boolean =
        callInitialized && _connectionState.value == RealtimeConnectionState.CONNECTED

    private fun notConnected(): AppResult.Failure =
        AppResult.Failure(AppError("Voice and video need an active connection"))

    private fun NCCallSession.toRealtimeCall(incoming: Boolean): RealtimeCall = RealtimeCall(
        callId = callId,
        peerId = if (callType == NCCallType.MULTI) {
            inviterUserId?.takeIf { it.isNotBlank() } ?: callId
        } else {
            callerUserId.orEmpty()
        },
        video = mediaType == NCCallMediaType.AUDIO_VIDEO,
        group = callType == NCCallType.MULTI,
        incoming = incoming
    )

    private suspend fun <T> settle(
        deferred: CompletableDeferred<T>,
        timeoutMs: Long,
        timeoutMessage: String
    ): AppResult<T> {
        val value = withTimeoutOrNull(timeoutMs) { deferred.await() }
            ?: return AppResult.Failure(AppError(timeoutMessage))
        return AppResult.Success(value)
    }

    private fun isDebuggable(): Boolean =
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun areaCode(value: String?): AreaCode = when (value?.uppercase()) {
        "BJ" -> AreaCode.BJ
        "NA" -> AreaCode.NA
        "SA" -> AreaCode.SA
        "OM" -> AreaCode.OM
        else -> AreaCode.SG
    }

    private fun callError(action: String, code: NCCallCode?): Throwable =
        IllegalStateException("Nexconn $action failed: ${code?.name ?: "UNKNOWN"}")

    private class NexconnNotConfiguredException :
        IllegalStateException("Nexconn is not configured: set NEXCONN_APP_ID on the backend")

    private companion object {
        const val MESSAGE_HANDLER_ID = "momento-messages"
        const val CONNECT_TIMEOUT_MS = 20_000L
        const val SEND_TIMEOUT_MS = 15_000L
        const val CALL_TIMEOUT_MS = 30_000L
    }
}

private fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) block(data)
    return this
}
