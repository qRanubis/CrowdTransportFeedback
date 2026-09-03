package com.example.crowdtransportfeedback.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `feedback_new` (
                `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `feedbackId` TEXT NOT NULL,
                `score` INTEGER NOT NULL,
                `comment` TEXT NOT NULL,
                `latitude` REAL,
                `longitude` REAL,
                `line` TEXT,
                `createdAt` INTEGER NOT NULL,
                `syncState` TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.query(
            "SELECT id, score, comment, latitude, longitude, line, createdAt, synced FROM feedback"
        ).use { cursor ->
            val insertSql = """
                INSERT INTO feedback_new (
                    localId, feedbackId, score, comment, latitude, longitude, line, createdAt, syncState
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            while (cursor.moveToNext()) {
                val latitude = if (cursor.isNull(3)) null else cursor.getDouble(3)
                val longitude = if (cursor.isNull(4)) null else cursor.getDouble(4)
                val line = if (cursor.isNull(5)) null else cursor.getString(5)
                val syncState = if (cursor.getInt(7) == 1) {
                    SyncState.SYNCED.name
                } else {
                    SyncState.PENDING_CREATE.name
                }

                db.execSQL(
                    insertSql,
                    arrayOf(
                        cursor.getLong(0),
                        UUID.randomUUID().toString(),
                        cursor.getInt(1),
                        cursor.getString(2),
                        latitude,
                        longitude,
                        line,
                        cursor.getLong(6),
                        syncState
                    )
                )
            }
        }

        db.execSQL("DROP TABLE feedback")
        db.execSQL("ALTER TABLE feedback_new RENAME TO feedback")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_feedback_feedbackId` ON `feedback` (`feedbackId`)"
        )
    }
}
