package com.mashiverse.data.db.daos

import com.mashiverse.data.db.PostgresManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class ReactionsDao {

    private fun getConnection(): Connection = PostgresManager.dataSource().connection
    private val queries by lazy {
        Queries()
    }

    suspend fun updateReactionsCount(userId: Long, amount: Int): Unit = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement(queries.upsertReactionsCount).use { stmt ->
                stmt.setLong(1, userId)
                stmt.setInt(2, amount)
                stmt.executeUpdate()
                conn.commit()
            }
        }
    }

    suspend fun getReactionsCount(userId: Long): Int = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement(queries.selectReactionsCount).use { stmt ->
                stmt.setLong(1, userId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt("reaction_count") else 0
                }
            }
        }
    }

    private class Queries {
        val upsertReactionsCount = """
            INSERT INTO users (id, reaction_count, wallet) 
            VALUES (?, GREATEST(0, ?), '')
            ON CONFLICT (id) 
            DO UPDATE SET reaction_count = GREATEST(0, users.reaction_count + EXCLUDED.reaction_count);
        """

        val selectReactionsCount = "SELECT reaction_count FROM users WHERE id = ?"
    }
}