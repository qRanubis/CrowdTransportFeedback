package com.example.crowdtransportfeedback.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [FeedbackEntity::class],
    version = 4
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedbackDao(): FeedbackDao
}
