package com.kutoru.mikunotes.logic

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.kutoru.mikunotes.R
import java.io.File

class NotificationHelper(
    private val context: Context,
) {

    private var maxNotificationIndex = 1
    private val notificationManager = NotificationManagerCompat.from(context)

    @SuppressLint("MissingPermission")
    fun showDownloadInProgress(notificationIndex: Int?, fileName: String, progress: Int): Int {
        val intent = Intent(FILE_NOTIFICATION_BROADCAST)
        var requestCode = 0

        if (notificationIndex != null) {
            intent.putExtra(FILE_NOTIFICATION_IDENTIFIER, notificationIndex)
            requestCode = 100 + notificationIndex
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat
            .Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Download")
            .setContentText(fileName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_cross, "Cancel", pendingIntent)
            .build()

        return showNotification(notificationIndex, notification)
    }

    @SuppressLint("MissingPermission")
    fun showDownloadFinished(notificationIndex: Int?, file: File) {
        val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat
            .Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Download")
            .setContentText("${file.name} has been downloaded")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        showNotification(notificationIndex, notification)
    }

    fun showUploadInProgress(notificationIndex: Int?, fileName: String, progress: Int): Int {
        val intent = Intent(FILE_NOTIFICATION_BROADCAST)
        var requestCode = 0

        if (notificationIndex != null) {
            intent.putExtra(FILE_NOTIFICATION_IDENTIFIER, notificationIndex)
            requestCode = 100 + notificationIndex
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat
            .Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_upload)
            .setContentTitle("Upload")
            .setContentText(fileName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_cross, "Cancel", pendingIntent)
            .build()

        return showNotification(notificationIndex, notification)
    }

    fun showUploadFinished(notificationIndex: Int?, fileName: String) {
        val notification = NotificationCompat
            .Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_upload)
            .setContentTitle("Upload")
            .setContentText("$fileName has been uploaded")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()

        showNotification(notificationIndex, notification)
    }

    fun hide(notificationIndex: Int?) {
        if (notificationIndex != null) {
            notificationManager.cancel(notificationIndex)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(notificationIndex: Int?, notification: Notification): Int {
        val currentIndex = notificationIndex ?: maxNotificationIndex++
        if (!canSend()) {
            return currentIndex
        }

        notificationManager.notify(currentIndex, notification)
        return currentIndex
    }

    private fun canSend(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
