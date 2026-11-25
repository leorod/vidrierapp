package com.vidrierapp.server.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

object DatabaseFactory {
    fun createDataSource(): DataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = System.getenv("JDBC_DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/vidrierapp"
        username = System.getenv("POSTGRES_USER") ?: "vidrierapp"
        password = System.getenv("POSTGRES_PASSWORD") ?: "vidrierapp_secret"
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 5
        minimumIdle = 1
        transactionIsolation = "TRANSACTION_READ_COMMITTED"
    })
}
