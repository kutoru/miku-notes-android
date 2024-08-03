package com.kutoru.mikunotes.logic

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.kutoru.mikunotes.R
import java.io.File

class NotificationHelper {
    companion object {
        fun permissionGranted(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return true
            }

            return ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun getDownloadInProgress(context: Context, fileName: String): NotificationCompat.Builder {
            return NotificationCompat
                .Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("Download")
                .setContentText(fileName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, false)
                .setAutoCancel(false)
        }

        fun getDownloadFinished(context: Context, file: File): NotificationCompat.Builder {
            val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE,
            )

            return NotificationCompat
                .Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("Download")
                .setContentText("${file.name} has been downloaded")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        }

        fun getUploadInProgress(context: Context, fileName: String): NotificationCompat.Builder {
            return NotificationCompat
                .Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle("Upload")
                .setContentText(fileName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, true)
                .setAutoCancel(false)
        }

        fun getUploadFinished(context: Context, fileName: String): NotificationCompat.Builder {
            return NotificationCompat
                .Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle("Upload")
                .setContentText("$fileName has been uploaded")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
        }
    }
}
