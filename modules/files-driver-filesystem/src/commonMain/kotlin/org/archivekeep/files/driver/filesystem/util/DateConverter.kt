package org.archivekeep.files.driver.filesystem.util

import androidx.room.TypeConverter
import java.util.Date

object DateConverter {
    @TypeConverter
    fun toDate(dateLong: Long?): Date? = if (dateLong == null) null else Date(dateLong)

    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time
}
