package com.kenjdavidson.golfcanada.security

import com.kenjdavidson.golfcanada.database.DatabaseInitializer
import jakarta.inject.Singleton
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.time.Instant

private const val MAX_USERNAME_LENGTH = 255

object UserSessionsTable : IntIdTable("user_sessions") {
    val username = varchar("username", MAX_USERNAME_LENGTH).uniqueIndex()
    val accessToken = text("access_token")
    val refreshToken = text("refresh_token").nullable()
    val expiresAt = text("expires_at")
    val rememberMe = bool("remember_me").default(false)
}

@Singleton
class DatabaseTokenStorage(
    private val databaseInitializer: DatabaseInitializer,
    private val encryption: GolfCanadaTokenEncryption,
) : GolfCanadaTokenStorage {
    override fun saveSession(user: GolfCanadaAuthenticatedUser) {
        transaction(databaseInitializer.sessionsDb) {
            UserSessionsTable.upsert(UserSessionsTable.username) { row ->
                row[username] = user.username
                row[accessToken] = encryption.encrypt(user.accessToken)
                row[refreshToken] = user.refreshToken?.let(encryption::encrypt)
                row[expiresAt] = user.expiresAt.toString()
                row[rememberMe] = user.rememberMe
            }
        }
    }

    override fun getSession(username: String): GolfCanadaUserSession? = transaction(databaseInitializer.sessionsDb) {
        UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.username eq username }
            .firstOrNull()
            ?.let { row ->
                GolfCanadaUserSession(
                    username = row[UserSessionsTable.username],
                    accessToken = encryption.decrypt(row[UserSessionsTable.accessToken]),
                    refreshToken = row[UserSessionsTable.refreshToken]?.let(encryption::decrypt),
                    expiresAt = Instant.parse(row[UserSessionsTable.expiresAt]),
                    rememberMe = row[UserSessionsTable.rememberMe],
                )
            }
    }

    override fun updateSession(
        username: String,
        newAccessToken: String,
        newRefreshToken: String?,
        expiresInSeconds: Long,
    ) {
        transaction(databaseInitializer.sessionsDb) {
            val newExpiresAt = Instant.now().plusSeconds(expiresInSeconds).toString()
            UserSessionsTable.update({ UserSessionsTable.username eq username }) { row ->
                row[accessToken] = encryption.encrypt(newAccessToken)
                row[refreshToken] = newRefreshToken?.let(encryption::encrypt)
                row[expiresAt] = newExpiresAt
            }
        }
    }

    override fun clearSession(username: String) {
        transaction(databaseInitializer.sessionsDb) {
            UserSessionsTable.deleteWhere { UserSessionsTable.username eq username }
        }
    }
}
