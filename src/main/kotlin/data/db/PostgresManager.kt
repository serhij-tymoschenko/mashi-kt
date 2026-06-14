package com.mashiverse.data.db

import com.mashiverse.configs.POSTGRES_PASSWORD
import com.mashiverse.configs.POSTGRES_URI
import com.mashiverse.configs.POSTGRES_USER
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.DriverManager

object PostgresManager {
    private var dataSource: HikariDataSource? = null

    private fun init(): HikariDataSource {
        return dataSource ?: synchronized(this) {
            dataSource ?: let {
                check()

                val config = HikariConfig().apply {
                    jdbcUrl = POSTGRES_URI
                    driverClassName = "org.postgresql.Driver"
                    username = POSTGRES_USER
                    password = POSTGRES_PASSWORD

                    maximumPoolSize = 10
                    isAutoCommit = false
                    transactionIsolation = "TRANSACTION_READ_COMMITTED"
                }

                val initializedSource = HikariDataSource(config)
                dataSource = initializedSource
                initializedSource
            }
        }
    }

    fun dataSource(): HikariDataSource {
        return dataSource ?: init()
    }

    private fun check() {
        val baseUri = POSTGRES_URI.substringBeforeLast("/")
        val dbName = POSTGRES_URI.substringAfterLast("/")
        val maintenanceUrl = "$baseUri/postgres"

        Class.forName("org.postgresql.Driver")

        DriverManager.getConnection(maintenanceUrl, POSTGRES_USER, POSTGRES_PASSWORD).use { conn ->
            conn.createStatement().use { statement ->
                val rs = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname = '$dbName'")
                if (!rs.next()) {
                    println("Database '$dbName' not found. Creating it...")
                    statement.executeUpdate("CREATE DATABASE \"$dbName\"")
                }
            }
        }
    }
}