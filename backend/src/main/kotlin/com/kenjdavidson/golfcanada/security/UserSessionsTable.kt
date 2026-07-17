package com.kenjdavidson.golfcanada.security

import org.jetbrains.exposed.dao.id.IntIdTable

private const val MAX_USERNAME_LENGTH = 255

object UserSessionsTable : IntIdTable("user_sessions") {
    val username = varchar("username", MAX_USERNAME_LENGTH).uniqueIndex()
    val accessToken = text("access_token")
    val refreshToken = text("refresh_token").nullable()
    val expiresAt = long("expires_at")
    val rememberMe = bool("remember_me").default(false)
}
