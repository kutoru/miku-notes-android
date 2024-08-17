package com.kutoru.mikunotes.logic

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AppUtil {
    companion object {
        fun formatDate(timestamp: Long): String {
            return LocalDateTime
                .ofEpochSecond(timestamp, 0, ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yy/MM/dd HH:mm"))
        }
    }
}
