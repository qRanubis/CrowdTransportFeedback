package com.example.crowdtransportfeedback.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pendingQueryAndLocalIdLookupUseLocalRoomIdentity() = runBlocking {
        val localId = database.feedbackDao().insert(feedback(feedbackId = "distributed-id"))

        val pending = database.feedbackDao().getPending()
        val found = database.feedbackDao().getByLocalId(localId).first()

        assertEquals(listOf("distributed-id"), pending.map { it.feedbackId })
        assertEquals(localId, found?.localId)
        assertEquals("distributed-id", found?.feedbackId)
    }

    @Test
    fun remoteReconciliationDoesNotOverwritePendingLocalFeedback() = runBlocking {
        val localId = database.feedbackDao().insert(
            feedback(feedbackId = "shared-id", comment = "pending local value")
        )

        database.feedbackDao().reconcileRemote(
            listOf(
                feedback(
                    feedbackId = "shared-id",
                    comment = "remote value",
                    syncState = SyncState.SYNCED
                )
            )
        )

        val stored = database.feedbackDao().getByLocalId(localId).first()
        assertEquals("pending local value", stored?.comment)
        assertEquals(SyncState.PENDING_CREATE, stored?.syncState)
    }

    @Test
    fun reconciliationPreservesAndNormalListHidesPendingDelete() = runBlocking {
        val localId = database.feedbackDao().insert(
            feedback("delete-tombstone", syncState = SyncState.PENDING_DELETE)
        )

        database.feedbackDao().reconcileRemote(emptyList())

        assertEquals(SyncState.PENDING_DELETE, database.feedbackDao().getByLocalIdOnce(localId)?.syncState)
        assertFalse(database.feedbackDao().getAll().first().any { it.localId == localId })
    }

    private fun feedback(
        feedbackId: String,
        comment: String = "Comment",
        syncState: SyncState = SyncState.PENDING_CREATE
    ) = FeedbackEntity(
        feedbackId = feedbackId,
        score = 4,
        comment = comment,
        latitude = 44.4268,
        longitude = 26.1025,
        line = "41",
        createdAt = 1_700_000_000_000,
        syncState = syncState
    )
}

@RunWith(AndroidJUnit4::class)
class FeedbackMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "feedback-migration-test"

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFrom1To2PreservesFeedbackAndLegacySyncMeaning() = runBlocking {
        context.deleteDatabase(databaseName)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null).use { legacy ->
            legacy.execSQL(
                """
                CREATE TABLE feedback (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    score INTEGER NOT NULL,
                    comment TEXT NOT NULL,
                    latitude REAL,
                    longitude REAL,
                    line TEXT,
                    createdAt INTEGER NOT NULL,
                    synced INTEGER NOT NULL
                )
                """.trimIndent()
            )
            legacy.execSQL(
                """
                INSERT INTO feedback
                    (id, score, comment, latitude, longitude, line, createdAt, synced)
                VALUES
                    (7, 5, 'Synced legacy', 44.42, 26.10, 'M2', 1000, 1),
                    (8, 2, 'Pending legacy', NULL, NULL, NULL, 2000, 0)
                """.trimIndent()
            )
            legacy.version = 1
        }

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val rows = migrated.feedbackDao().getAll().first().sortedBy { it.localId }
        migrated.close()

        assertEquals(listOf(7L, 8L), rows.map { it.localId })
        assertEquals(listOf("Synced legacy", "Pending legacy"), rows.map { it.comment })
        assertEquals(listOf("M2", null), rows.map { it.line })
        assertEquals(SyncState.SYNCED, rows[0].syncState)
        assertEquals(SyncState.PENDING_CREATE, rows[1].syncState)
        assertNotNull(rows[0].feedbackId)
        assertFalse(rows[0].feedbackId.isBlank())
        assertNotEquals(rows[0].feedbackId, rows[1].feedbackId)
        assertEquals(44.42, rows[0].latitude!!, 0.001)
        assertEquals(26.10, rows[0].longitude!!, 0.001)
        assertEquals(1000L, rows[0].createdAt)
    }
}
