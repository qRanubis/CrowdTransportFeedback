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
class Migration2To3Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-2-3-test"

    @After
    fun cleanUp() {
        context.deleteDatabase(name)
    }

    @Test
    fun legacyRowSurvivesWithNullStructuredAndIdentityFields() = runBlocking {
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL("CREATE TABLE feedback (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, feedbackId TEXT NOT NULL, score INTEGER NOT NULL, comment TEXT NOT NULL, latitude REAL, longitude REAL, line TEXT, createdAt INTEGER NOT NULL, syncState TEXT NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX index_feedback_feedbackId ON feedback(feedbackId)")
            db.execSQL("INSERT INTO feedback VALUES (7, 'stable-uuid', 5, 'legacy', 44.42, 26.1, 'M2', 1000, 'SYNCED')")
            db.version = 2
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()

        val row = database.feedbackDao().getByLocalIdOnce(7)!!
        assertEquals(7, row.localId)
        assertEquals("stable-uuid", row.feedbackId)
        assertEquals(5, row.score)
        assertEquals(SyncState.SYNCED, row.syncState)
        assertNull(row.transportType)
        assertNull(row.crowdingScore)
        assertNull(row.cleanlinessScore)
        assertNull(row.punctualityScore)
        assertNull(row.createdByUserId)
        assertNull(row.createdByUsername)
        assertNull(row.createdByAvatarKey)
        assertNull(row.rejectionReason)
        database.close()
    }
}
