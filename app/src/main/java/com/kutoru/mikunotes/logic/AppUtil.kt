package com.kutoru.mikunotes.logic

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AppUtil {
    companion object {
        fun formatDate(timestampInSeconds: Long): String {
            return LocalDateTime
                .ofEpochSecond(timestampInSeconds, 0, ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yy/MM/dd"))
        }

        fun formatDateTime(timestampInSeconds: Long): String {
            return LocalDateTime
                .ofEpochSecond(timestampInSeconds, 0, ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yy/MM/dd HH:mm"))
        }

        fun getNowInMillis(): Long {
            return Calendar.getInstance().timeInMillis
        }
    }
}
