package com.example.crowdtransportfeedback.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration3To4Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-3-4-test"

    @After
    fun cleanUp() {
        context.deleteDatabase(name)
    }

    @Test
    fun version3RowSurvivesWithNullCreator() = runBlocking {
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL(
                """
                CREATE TABLE feedback (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    feedbackId TEXT NOT NULL,
                    score INTEGER NOT NULL,
                    comment TEXT NOT NULL,
                    latitude REAL,
                    longitude REAL,
                    line TEXT,
                    createdAt INTEGER NOT NULL,
                    syncState TEXT NOT NULL,
                    transportType TEXT,
                    crowdingScore INTEGER,
                    cleanlinessScore INTEGER,
                    punctualityScore INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX index_feedback_feedbackId ON feedback(feedbackId)")
            db.execSQL(
                """
                INSERT INTO feedback VALUES (
                    9, 'v3-stable-uuid', 4, 'existing', 44.4268, 26.1025, '41',
                    1700000000000, 'SYNCED', 'TRAM', 4, 5, 3
                )
                """.trimIndent()
            )
            db.version = 3
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        val row = database.feedbackDao().getByLocalIdOnce(9)!!
        assertEquals("v3-stable-uuid", row.feedbackId)
        assertEquals(4, row.score)
        assertEquals(SyncState.SYNCED, row.syncState)
        assertEquals("41", row.line)
        assertNull(row.createdByUserId)
        database.close()
    }
}
