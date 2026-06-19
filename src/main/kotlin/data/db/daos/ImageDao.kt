package com.mashiverse.data.db.daos

import com.mashiverse.data.db.PostgresManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class ImageDao {

    private fun getConnection(): Connection = PostgresManager.dataSource().connection
    private val queries by lazy {
        Queries()
    }

    init {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(queries.createTable)
            }
            conn.commit()
        }
    }

    suspend fun addImage(url: String, byteData: ByteArray): Unit = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement(queries.upsertData).use { stmt ->
                stmt.setString(1, url)
                stmt.setBytes(2, byteData)
                stmt.executeUpdate()
                conn.commit()
            }
        }
    }

    suspend fun addWebpImage(url: String, webpData: ByteArray): Unit = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement(queries.upsertWebp).use { stmt ->
                stmt.setString(1, url)
                stmt.setBytes(2, webpData)
                stmt.executeUpdate()
                conn.commit()
            }
        }
    }

    suspend fun addSvgImage(url: String, svgData: ByteArray): Unit = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement(queries.upsertSvg).use { stmt ->
                stmt.setString(1, url)
                stmt.setBytes(2, svgData)
                stmt.executeUpdate()
                conn.commit()
            }
        }
    }

    suspend fun getImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement(queries.selectData).use { stmt ->
                stmt.setString(1, url)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getBytes("data") else null
                }
            }
        }
    }

    suspend fun getWebpImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement(queries.selectWebp).use { stmt ->
                stmt.setString(1, url)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getBytes("webp_data") else null
                }
            }
        }
    }

    suspend fun getSvgImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement(queries.selectSvg).use { stmt ->
                stmt.setString(1, url)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getBytes("svg_data") else null
                }
            }
        }
    }

    private class Queries {
        val createTable = """
            CREATE TABLE IF NOT EXISTS images (
                url VARCHAR(1000) PRIMARY KEY,
                data BYTEA,
                webp_data BYTEA,
                svg_data BYTEA
            );
        """

        val upsertData = """
            INSERT INTO images (url, data) VALUES (?, ?)
            ON CONFLICT (url) DO UPDATE SET data = EXCLUDED.data;
        """

        val upsertWebp = """
            INSERT INTO images (url, webp_data) VALUES (?, ?)
            ON CONFLICT (url) DO UPDATE SET webp_data = EXCLUDED.webp_data;
        """

        val upsertSvg = """
            INSERT INTO images (url, svg_data) VALUES (?, ?)
            ON CONFLICT (url) DO UPDATE SET svg_data = EXCLUDED.svg_data;
        """

        val selectData = "SELECT data FROM images WHERE url = ?"
        val selectWebp = "SELECT webp_data FROM images WHERE url = ?"
        val selectSvg = "SELECT svg_data FROM images WHERE url = ?"
    }
}