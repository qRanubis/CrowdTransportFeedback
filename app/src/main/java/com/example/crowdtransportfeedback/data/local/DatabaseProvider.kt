package com.example.crowdtransportfeedback.data.local

import android.content.Context
import androidx.room.Room

// Simple singleton provider
object DatabaseProvider {

    private var db: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        if (db == null) {
            db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "crowd_feedback_db"
            ).build()
        }
        return db!!
    }
}
