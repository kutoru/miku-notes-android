package com.kutoru.mikunotes.logic

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.deleteFile
import com.kutoru.mikunotes.logic.requests.getAccess
import com.kutoru.mikunotes.logic.requests.getFile
import com.kutoru.mikunotes.logic.requests.postFileToNote
import com.kutoru.mikunotes.logic.requests.postFileToShelf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class FileRequestService : JobService() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var requestManager: RequestManager

    @SuppressLint("NewApi")
    override fun onCreate() {
        requestManager = (application as MikuNotesApp).requestManager

        if (requestManager.notificationHelper.setServiceNotification == null) {
            requestManager.notificationHelper.setServiceNotification =
                { params, notificationId, notification ->
                    setNotification(
                        params!!,
                        notificationId,
                        notification,
                        JOB_END_NOTIFICATION_POLICY_DETACH,
                    )
                }
        }

        super.onCreate()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        if (params == null) {
            return false
        }

        if (params.extras.getBoolean(FILE_SERVICE_UPLOAD)) {
            val uris = params.extras.getStringArray(FILE_SERVICE_URIS)!!.map { Uri.parse(it) }
            val itemId = params.extras.getInt(FILE_SERVICE_ITEM_ID, -1).takeIf { it != -1 }!!
            val isShelf = params.extras.getInt(FILE_SERVICE_IS_SHELF, -1).takeIf { it == 0 || it == 1 }!! == 1

            val tasks = uris.map { uri ->
                scope.async {
                    uploadFile(uri, itemId, isShelf, params)
                }
            }

            scope.launch {
                tasks.awaitAll()
                jobFinished(params, false)
            }

            return uris.isNotEmpty()
        }

        if (params.extras.getBoolean(FILE_SERVICE_DOWNLOAD)) {
            val fileHash = params.extras.getString(FILE_SERVICE_FILE_HASH)!!
            scope.launch {
                downloadFile(fileHash, params)
                jobFinished(params, false)
            }

            return true
        }

        if (params.extras.getBoolean(FILE_SERVICE_DELETE)) {
            val fileId = params.extras.getInt(FILE_SERVICE_FILE_ID, -1).takeIf { it != -1 }!!
            scope.launch {
                deleteFile(fileId)
                jobFinished(params, false)
            }

            return true
        }

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        println("onStopJob: ${params?.extras}")
        return false
    }

    private suspend fun uploadFile(fileUri: Uri, itemId: Int, isShelf: Boolean, params: JobParameters?) {
        val notificationId = requestManager.notificationHelper.getNewNotificationIndex()

        val result = if (isShelf) {
            handleRequest { requestManager.postFileToShelf(
                contentResolver, fileUri, itemId, notificationId, params,
            ) }
        } else {
            handleRequest { requestManager.postFileToNote(
                contentResolver, fileUri, itemId, notificationId, params,
            ) }
        }

        if (result.isFailure) {
            if (result.exceptionOrNull() is RequestCancel) {
                requestManager.notificationHelper.cancelUpload(notificationId, params)
            } else {
                requestManager.notificationHelper.showUploadFailed(notificationId, params)
            }

            return
        }

        val file = result.getOrNull()!!
        if (isShelf) {
            file.attach_id = 0
        }

        val intent = Intent(FILE_CHANGE_BROADCAST)
        intent.setPackage(applicationContext.packageName)
        intent.putExtra(FILE_CHANGE_ADDED, file)
        val requestCode = FILE_CHANGE_INTENT_OFFSET + file.hashCode()

        PendingIntent.getBroadcast(
            applicationContext, requestCode, intent, PendingIntent.FLAG_IMMUTABLE,
        ).send()
    }

    private suspend fun downloadFile(fileHash: String, params: JobParameters?) {
        val notificationId = requestManager.notificationHelper.getNewNotificationIndex()

        val result = handleRequest { requestManager.getFile(
            fileHash, notificationId, params,
        ) }

        if (result.isFailure) {
            if (result.exceptionOrNull() is RequestCancel) {
                requestManager.notificationHelper.cancelDownload(notificationId, params)
            } else {
                requestManager.notificationHelper.showDownloadFailed(notificationId, params)
            }
        }
    }

    private suspend fun deleteFile(fileId: Int) {
        val result = handleRequest { requestManager.deleteFile(fileId) }
        if (result.isFailure) {
            Toast.makeText(applicationContext, "Could not delete the file", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(FILE_CHANGE_BROADCAST)
        intent.setPackage(applicationContext.packageName)
        intent.putExtra(FILE_CHANGE_DELETED, fileId)
        val requestCode = FILE_CHANGE_INTENT_OFFSET + fileId

        PendingIntent.getBroadcast(
            applicationContext, requestCode, intent, PendingIntent.FLAG_IMMUTABLE,
        ).send()
    }

    private suspend fun <T>handleRequest(requestFunction: suspend () -> T): Result<T> {
        return handleRequestErrors {
            try {
                requestFunction()
            } catch (e: Unauthorized) {
                requestManager.getAccess()
                requestFunction()
            }
        }
    }

    private suspend fun <T>handleRequestErrors(requestFunction: suspend () -> T): Result<T> {
        val result = runCatching {
            requestFunction()
        }

        if (result.isSuccess) {
            return result
        }

        val err = result.exceptionOrNull()
        if (err is Error) {
            println("Unhandleable error: $err")
            throw err
        }

        println("File request: $err")

        return result
    }
}
