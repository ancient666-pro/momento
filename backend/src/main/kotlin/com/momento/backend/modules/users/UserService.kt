package com.momento.backend.modules.users

import com.momento.backend.common.UserDto
import com.momento.backend.database.Follows
import com.momento.backend.database.UserProfiles
import com.momento.backend.database.Users
import java.util.UUID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll

/**
 * Reads a user plus profile and follower counters and maps it to the shared DTO.
 * Used by every module so user payloads stay consistent across the API.
 */
object UserService {

    fun dtoById(userId: UUID): UserDto? =
        org.jetbrains.exposed.sql.transactions.transaction {
            latestUserDto(listOf(userId)).firstOrNull()
        }

    /** Batch-loads DTOs, avoiding N+1 queries for feeds. */
    fun dtosByIds(userIds: Collection<UUID>): Map<UUID, UserDto> {
        val ids = userIds.distinct()
        if (ids.isEmpty()) return emptyMap()
        return org.jetbrains.exposed.sql.transactions.transaction {
            latestUserDto(ids).associateBy { UUID.fromString(it.id) }
        }
    }

    private fun latestUserDto(ids: List<UUID>): List<UserDto> {
        val rows = Users
            .join(UserProfiles, JoinType.LEFT, Users.id, UserProfiles.userId)
            .selectAll()
            .where { (Users.id inList ids) and Users.deletedAt.isNull() }
            .toList()

        val followerCounts = counts(Follows.followeeId, ids)
        val followingCounts = counts(Follows.followerId, ids)

        return rows.map { row -> row.toDto(followerCounts, followingCounts) }
    }

    private fun counts(column: org.jetbrains.exposed.sql.Column<UUID>, ids: List<UUID>): Map<UUID, Long> =
        Follows.select(column, column.count())
            .where { column inList ids }
            .groupBy(column)
            .associate { it[column] to it[column.count()] }

    fun ResultRow.toDto(
        followerCounts: Map<UUID, Long> = emptyMap(),
        followingCounts: Map<UUID, Long> = emptyMap()
    ): UserDto {
        val id = this[Users.id]
        return UserDto(
            id = id.toString(),
            displayName = this[Users.displayName],
            username = this[Users.username],
            avatarUrl = this[Users.avatarUrl] ?: "",
            verified = this[Users.verified],
            followerCount = followerCounts[id] ?: 0,
            followingCount = followingCounts[id] ?: 0,
            bio = runCatching { this[UserProfiles.bio] }.getOrDefault(""),
            category = runCatching { this[UserProfiles.category] }.getOrNull(),
            location = runCatching { this[UserProfiles.location] }.getOrNull(),
            online = false
        )
    }

    fun followingIds(userId: UUID): Set<UUID> =
        Follows.select(Follows.followeeId)
            .where { Follows.followerId eq userId }
            .map { it[Follows.followeeId] }
            .toSet()

    fun isBlocked(blockerId: UUID, blockedId: UUID): Boolean =
        com.momento.backend.database.Blocks.selectAll()
            .where { (com.momento.backend.database.Blocks.blockerId eq blockerId) and (com.momento.backend.database.Blocks.blockedId eq blockedId) }
            .count() > 0

    fun followerIds(userId: UUID): List<UUID> =
        Follows.select(Follows.followerId)
            .where { Follows.followeeId eq userId }
            .map { it[Follows.followerId] }
}
