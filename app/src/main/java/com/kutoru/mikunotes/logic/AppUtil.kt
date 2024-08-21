package com.kutoru.mikunotes.logic

import android.graphics.Color
import android.view.View
import android.view.Window
import com.kutoru.mikunotes.R
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

        fun setNavigationBarColor(window: Window, root: View) {
            val color = window.context.getColor(R.color.nav_bar_bg)
            val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            val isLight = darkness < 0.5

            window.navigationBarColor = color

            var flags = root.systemUiVisibility
            if (isLight) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }

            root.systemUiVisibility = flags
        }
    }
}
