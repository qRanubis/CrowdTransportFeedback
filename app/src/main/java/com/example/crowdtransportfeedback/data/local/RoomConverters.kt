package com.example.crowdtransportfeedback.data.local

import androidx.room.TypeConverter
import com.example.crowdtransportfeedback.domain.TransportType

class RoomConverters {
    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)

    @TypeConverter fun fromTransportType(value: TransportType?): String? = value?.name
    @TypeConverter fun toTransportType(value: String?): TransportType? = value?.let(TransportType::valueOf)
}
