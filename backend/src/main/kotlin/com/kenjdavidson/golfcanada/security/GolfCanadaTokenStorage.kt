package com.kenjdavidson.golfcanada.security

import io.micronaut.context.annotation.Value
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant

/**
 * Represents a persisted user session loaded from the database.
 */
data class GolfCanadaUserSession(
    val username: String,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant,
    val rememberMe: Boolean,
)

/**
 * SQLite-backed storage for Golf Canada user sessions.  Access and refresh tokens are encrypted
 * at rest using [GolfCanadaTokenEncryption] (AES-256-GCM) before being written to disk.
 *
 * All public methods are thread-safe via [synchronized] on the shared [Connection].
 */
@Singleton
class GolfCanadaTokenStorage(
    @Value("\${golf-canada-app.security.session-db-path}") private val dbPath: String,
    private val encryption: GolfCanadaTokenEncryption,
) {
    private val log = LoggerFactory.getLogger(GolfCanadaTokenStorage::class.java)
    private lateinit var connection: Connection

    @PostConstruct
    fun initialize() {
        val safePath = resolveSafeDbPath(dbPath)
        log.info("Opening Golf Canada session database at {}", safePath)
        connection = DriverManager.getConnection("jdbc:sqlite:$safePath")
        connection.createStatement().use { stmt ->
            stmt.execute("PRAGMA journal_mode=WAL")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS user_sessions (
                    username                 TEXT    PRIMARY KEY,
                    encrypted_access_token  TEXT    NOT NULL,
                    encrypted_refresh_token TEXT,
                    expires_at_epoch        INTEGER NOT NULL,
                    remember_me             INTEGER NOT NULL DEFAULT 0,
                    created_at_epoch        INTEGER NOT NULL,
                    updated_at_epoch        INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
        log.info("Golf Canada session database ready")
    }

    @PreDestroy
    fun close() {
        if (::connection.isInitialized && !connection.isClosed) {
            connection.close()
        }
    }

    /**
     * Resolves [rawPath] to a safe, normalised file path, rejecting obvious path-traversal
     * attempts (e.g. paths containing `..` segments after normalisation).
     */
    private fun resolveSafeDbPath(rawPath: String): Path {
        try {
            val normalised = Path.of(rawPath).normalize()
            if (normalised.any { it.toString() == ".." }) {
                throw IllegalArgumentException(
                    "GOLF_CANADA_SESSION_DB_PATH contains illegal path traversal: $rawPath",
                )
            }
            return normalised
        } catch (e: InvalidPathException) {
            throw IllegalArgumentException("GOLF_CANADA_SESSION_DB_PATH is not a valid path: $rawPath", e)
        }
    }

    /**
     * Persists a new session, or replaces an existing one, for the given user.
     */
    fun saveSession(user: GolfCanadaAuthenticatedUser) {
        val now = Instant.now().epochSecond
        val encryptedAccess = encryption.encrypt(user.accessToken)
        val encryptedRefresh = user.refreshToken?.let { encryption.encrypt(it) }

        synchronized(connection) {
            connection.prepareStatement(
                """
                INSERT INTO user_sessions
                    (username, encrypted_access_token, encrypted_refresh_token,
                     expires_at_epoch, remember_me, created_at_epoch, updated_at_epoch)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(username) DO UPDATE SET
                    encrypted_access_token  = excluded.encrypted_access_token,
                    encrypted_refresh_token = excluded.encrypted_refresh_token,
                    expires_at_epoch        = excluded.expires_at_epoch,
                    remember_me             = excluded.remember_me,
                    updated_at_epoch        = excluded.updated_at_epoch
                """.trimIndent(),
            ).use { ps ->
                ps.setString(1, user.username)
                ps.setString(2, encryptedAccess)
                ps.setString(3, encryptedRefresh)
                ps.setLong(4, user.expiresAt.epochSecond)
                ps.setInt(5, if (user.rememberMe) 1 else 0)
                ps.setLong(6, now)
                ps.setLong(7, now)
                ps.executeUpdate()
            }
        }
    }

    /**
     * Returns the stored session for [username], or `null` if no session exists.
     */
    fun getSession(username: String): GolfCanadaUserSession? {
        synchronized(connection) {
            connection.prepareStatement(
                """
                SELECT encrypted_access_token, encrypted_refresh_token,
                       expires_at_epoch, remember_me
                FROM user_sessions
                WHERE username = ?
                """.trimIndent(),
            ).use { ps ->
                ps.setString(1, username)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return GolfCanadaUserSession(
                        username = username,
                        accessToken = encryption.decrypt(rs.getString("encrypted_access_token")),
                        refreshToken = rs.getString("encrypted_refresh_token")?.let { encryption.decrypt(it) },
                        expiresAt = Instant.ofEpochSecond(rs.getLong("expires_at_epoch")),
                        rememberMe = rs.getInt("remember_me") == 1,
                    )
                }
            }
        }
    }

    /**
     * Updates the access and refresh tokens for an existing session after a successful Golf Canada
     * token refresh.
     */
    fun updateSession(
        username: String,
        newAccessToken: String,
        newRefreshToken: String?,
        expiresInSeconds: Long,
    ) {
        val now = Instant.now()
        val encryptedAccess = encryption.encrypt(newAccessToken)
        val encryptedRefresh = newRefreshToken?.let { encryption.encrypt(it) }

        synchronized(connection) {
            connection.prepareStatement(
                """
                UPDATE user_sessions
                SET encrypted_access_token  = ?,
                    encrypted_refresh_token = ?,
                    expires_at_epoch        = ?,
                    updated_at_epoch        = ?
                WHERE username = ?
                """.trimIndent(),
            ).use { ps ->
                ps.setString(1, encryptedAccess)
                ps.setString(2, encryptedRefresh)
                ps.setLong(3, now.plusSeconds(expiresInSeconds).epochSecond)
                ps.setLong(4, now.epochSecond)
                ps.setString(5, username)
                ps.executeUpdate()
            }
        }
    }

    /**
     * Removes the session for [username].  Called when a refresh token is invalidated so the user
     * must log in again.
     */
    fun clearSession(username: String) {
        synchronized(connection) {
            connection.prepareStatement("DELETE FROM user_sessions WHERE username = ?").use { ps ->
                ps.setString(1, username)
                ps.executeUpdate()
            }
        }
    }
}
