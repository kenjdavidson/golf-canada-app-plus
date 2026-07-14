package com.kenjdavidson.golfcanada.database

import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.flyway.FlywayMigrator
import jakarta.annotation.PostConstruct
import jakarta.inject.Named
import org.jetbrains.exposed.sql.Database
import java.sql.Connection
import javax.sql.DataSource

@Context
@Requires(beans = [FlywayMigrator::class])
class DatabaseInitializer(
    @Named("default") private val primaryDataSource: DataSource,
    @Named("sessions") private val sessionsDataSource: DataSource,
) {
    lateinit var primaryDb: Database
        private set

    lateinit var sessionsDb: Database
        private set

    @PostConstruct
    fun init() {
        primaryDb = connectSqlite(primaryDataSource)
        sessionsDb = connectSqlite(sessionsDataSource)
    }

    private fun connectSqlite(dataSource: DataSource): Database = Database.connect(
        getNewConnection = { dataSource.connection.enableForeignKeys() },
    )

    private fun Connection.enableForeignKeys(): Connection = apply {
        createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON;")
        }
    }
}
