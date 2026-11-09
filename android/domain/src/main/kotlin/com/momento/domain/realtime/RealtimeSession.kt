package com.momento.domain.realtime

import com.momento.core.common.AppResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transport-agnostic contract for everything real-time in the app.
 *
 * The production implementation is backed by the Nexconn Chat and Call SDKs;
 * feature modules depend on this interface only, so the SDK never leaks into UI.
 */
interface RealtimeSession {

    val connectionState: StateFlow<RealtimeConnectionState>

    /** Incoming 1:1 messages, already de-duplicated by the SDK. */
    val incomingMessages: SharedFlow<ChatMessage>

    /** Calls ringing for this user (1:1 and group audio rooms). */
    val incomingCalls: SharedFlow<RealtimeCall>

    /** The call this device is currently in, or null. */
    val activeCall: StateFlow<RealtimeCall?>

    /**
     * Exchanges the app's JWT session for a Nexconn access token and connects
     * both SDKs. Safe to call repeatedly; a connected session is a no-op.
     */
    suspend fun connect(): AppResult<Unit>

    suspend fun disconnect()

    suspend fun sendText(peerId: String, text: String): AppResult<ChatMessage>

    /** Starts a 1:1 call. Returns the Nexconn call id. */
    suspend fun startCall(peerId: String, video: Boolean): AppResult<String>

    /**
     * Starts (or reuses) a multi-party audio room. [roomId] is the app's pod or
     * audio-room id, reused as the group target so every participant joins the
     * same Nexconn call.
     */
    suspend fun startAudioRoom(roomId: String, participants: List<String>): AppResult<String>

    suspend fun joinCall(callId: String): AppResult<Unit>

    suspend fun acceptCall(callId: String): AppResult<Unit>

    suspend fun endCall(callId: String): AppResult<Unit>

    fun setMicrophoneEnabled(enabled: Boolean)

    fun setSpeakerEnabled(enabled: Boolean)

    fun switchCamera()
}

enum class RealtimeConnectionState { DISCONNECTED, CONNECTING, CONNECTED, NO_CREDENTIALS, ERROR }

data class ChatMessage(
    val id: String,
    val peerId: String,
    val senderId: String,
    val text: String,
    val sentAtMillis: Long,
    val outgoing: Boolean
)

data class RealtimeCall(
    val callId: String,
    val peerId: String,
    val video: Boolean,
    val group: Boolean,
    val incoming: Boolean
)
