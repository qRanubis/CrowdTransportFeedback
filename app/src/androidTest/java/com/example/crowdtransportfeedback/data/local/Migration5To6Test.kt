package com.example.crowdtransportfeedback.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration5To6Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-5-6-test"

    @After
    fun cleanUp() {
        context.deleteDatabase(name)
    }

    @Test fun version5RowSurvivesAndNewFieldsAreNullable() = runBlocking {
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL("""CREATE TABLE feedback (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, feedbackId TEXT NOT NULL, score INTEGER NOT NULL, comment TEXT NOT NULL, latitude REAL, longitude REAL, line TEXT, createdAt INTEGER NOT NULL, syncState TEXT NOT NULL, transportType TEXT, crowdingScore INTEGER, cleanlinessScore INTEGER, punctualityScore INTEGER, createdByUserId TEXT, createdByUsername TEXT)""")
            db.execSQL("CREATE UNIQUE INDEX index_feedback_feedbackId ON feedback(feedbackId)")
            db.execSQL("INSERT INTO feedback VALUES (12,'m5-id',4,'kept',44.4,26.1,'M5',1700000000000,'PENDING_DELETE','METRO',3,4,5,'owner-id','owner')")
            db.version = 5
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_5_6).allowMainThreadQueries().build()
        val row = database.feedbackDao().getByLocalIdOnce(12)!!
        assertEquals("m5-id", row.feedbackId); assertEquals("kept", row.comment)
        assertEquals("M5", row.line); assertEquals(SyncState.PENDING_DELETE, row.syncState)
        assertEquals("owner-id", row.createdByUserId); assertEquals("owner", row.createdByUsername)
        assertNull(row.createdByAvatarKey); assertNull(row.rejectionReason)
        database.close()
    }
}
