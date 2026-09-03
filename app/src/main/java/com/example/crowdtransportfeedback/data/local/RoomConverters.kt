package com.example.crowdtransportfeedback.data.local

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)
}
