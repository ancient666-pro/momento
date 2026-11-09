package com.momento.backend.modules.media

import com.momento.backend.common.ApiException
import com.momento.backend.common.MediaDto
import com.momento.backend.common.UploadIntentDto
import com.momento.backend.common.badRequest
import com.momento.backend.common.forbidden
import com.momento.backend.common.notFound
import com.momento.backend.common.pathUuid
import com.momento.backend.common.requireUserId
import com.momento.backend.config.AppConfig
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.MediaAssets
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.minio.BucketExistsArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import io.minio.http.Method
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

@Serializable
data class UploadIntentRequest(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)

@Serializable
data class CompleteUploadRequest(val mediaId: String)

private val allowedMimeTypes = mapOf(
    "image/jpeg" to 15L * 1024 * 1024,
    "image/png" to 15L * 1024 * 1024,
    "image/webp" to 15L * 1024 * 1024,
    "video/mp4" to 200L * 1024 * 1024,
    "video/quicktime" to 200L * 1024 * 1024,
    "audio/mpeg" to 50L * 1024 * 1024,
    "audio/mp4" to 50L * 1024 * 1024
)

private val minio: MinioClient? by lazy {
    runCatching {
        MinioClient.builder()
            .endpoint(AppConfig.s3Endpoint)
            .credentials(AppConfig.s3AccessKey, AppConfig.s3SecretKey)
            .build()
    }.getOrNull()
}

private fun requireMinio(): MinioClient = minio ?: throw ApiException(
    HttpStatusCode.ServiceUnavailable,
    "storage_unavailable",
    "Object storage is not reachable. Configure S3_ENDPOINT/S3_ACCESS_KEY/S3_SECRET_KEY."
)

fun Route.mediaModule() {
    route("/media") {
        post("/upload-intent") {
            val userId = call.requireUserId()
            val body = call.receive<UploadIntentRequest>()
            val maxSize = allowedMimeTypes[body.mimeType.lowercase()]
                ?: throw badRequest("Unsupported media type", "unsupported_mime")
            if (body.sizeBytes <= 0 || body.sizeBytes > maxSize) {
                throw badRequest("File is too large for ${body.mimeType}", "file_too_large")
            }
            val client = requireMinio()
            ensureBucket(client)
            val mediaId = UUID.randomUUID()
            val safeName = body.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
            val objectKey = "uploads/$userId/$mediaId/$safeName"

            DatabaseFactory.query {
                MediaAssets.insert {
                    it[MediaAssets.id] = mediaId
                    it[MediaAssets.ownerId] = userId
                    it[MediaAssets.bucket] = AppConfig.s3Bucket
                    it[MediaAssets.objectKey] = objectKey
                    it[MediaAssets.mimeType] = body.mimeType.lowercase()
                    it[MediaAssets.sizeBytes] = body.sizeBytes
                    it[MediaAssets.status] = "pending"
                }
            }

            val uploadUrl = client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(AppConfig.s3Bucket)
                    .`object`(objectKey)
                    .expiry(15, TimeUnit.MINUTES)
                    .build()
            )
            call.respond(
                HttpStatusCode.Created,
                UploadIntentDto(
                    mediaId = mediaId.toString(),
                    uploadUrl = uploadUrl,
                    publicUrl = publicUrl(objectKey),
                    expiresInSeconds = 900
                )
            )
        }

        post("/complete") {
            val userId = call.requireUserId()
            val body = call.receive<CompleteUploadRequest>()
            val mediaId = runCatching { UUID.fromString(body.mediaId) }
                .getOrElse { throw badRequest("Invalid media id") }
            val dto = DatabaseFactory.query {
                val row = MediaAssets.selectAll()
                    .where { (MediaAssets.id eq mediaId) and (MediaAssets.ownerId eq userId) }
                    .firstOrNull() ?: throw notFound("Media not found")
                val url = publicUrl(row[MediaAssets.objectKey])
                MediaAssets.update({ MediaAssets.id eq mediaId }) {
                    it[MediaAssets.status] = "ready"
                    it[MediaAssets.url] = url
                }
                MediaDto(
                    id = mediaId.toString(),
                    url = url,
                    mimeType = row[MediaAssets.mimeType],
                    status = "ready",
                    sizeBytes = row[MediaAssets.sizeBytes]
                )
            }
            call.respond(dto)
        }

        get("/{mediaId}") {
            val mediaId = call.pathUuid("mediaId")
            val dto = DatabaseFactory.query {
                val row = MediaAssets.selectAll().where { MediaAssets.id eq mediaId }.firstOrNull()
                    ?: throw notFound("Media not found")
                MediaDto(
                    id = mediaId.toString(),
                    url = row[MediaAssets.url] ?: publicUrl(row[MediaAssets.objectKey]),
                    mimeType = row[MediaAssets.mimeType],
                    status = row[MediaAssets.status],
                    sizeBytes = row[MediaAssets.sizeBytes]
                )
            }
            call.respond(dto)
        }

        delete("/{mediaId}") {
            val userId = call.requireUserId()
            val mediaId = call.pathUuid("mediaId")
            val objectKey = DatabaseFactory.query {
                val row = MediaAssets.selectAll().where { MediaAssets.id eq mediaId }.firstOrNull()
                    ?: throw notFound("Media not found")
                if (row[MediaAssets.ownerId] != userId) throw forbidden()
                val key = row[MediaAssets.objectKey]
                MediaAssets.deleteWhere { MediaAssets.id eq mediaId }
                key
            }
            runCatching {
                requireMinio().removeObject(
                    RemoveObjectArgs.builder().bucket(AppConfig.s3Bucket).`object`(objectKey).build()
                )
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun ensureBucket(client: MinioClient) {
    if (!client.bucketExists(BucketExistsArgs.builder().bucket(AppConfig.s3Bucket).build())) {
        client.makeBucket(MakeBucketArgs.builder().bucket(AppConfig.s3Bucket).build())
    }
}

private fun publicUrl(objectKey: String): String {
    val base = AppConfig.s3PublicBaseUrl.trimEnd('/')
    return URI("$base/${AppConfig.s3Bucket}/$objectKey").toASCIIString()
}
