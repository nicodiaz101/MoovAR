package com.transportar.android.core.database

import androidx.room.TypeConverter
import com.transportar.android.core.database.entity.AlertSeverity
import com.transportar.android.core.database.entity.LineStatus
import com.transportar.android.core.database.entity.NetworkType

class RoomTypeConverters {
    @TypeConverter fun fromNetworkType(v: NetworkType): String = v.name
    @TypeConverter fun toNetworkType(v: String): NetworkType = NetworkType.valueOf(v)

    @TypeConverter fun fromLineStatus(v: LineStatus): String = v.name
    @TypeConverter fun toLineStatus(v: String): LineStatus = LineStatus.valueOf(v)

    @TypeConverter fun fromAlertSeverity(v: AlertSeverity): String = v.name
    @TypeConverter fun toAlertSeverity(v: String): AlertSeverity = AlertSeverity.valueOf(v)
}
