package com.kutoru.mikunotes.logic

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.net.Uri
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.deleteFile
import com.kutoru.mikunotes.logic.requests.getAccess
import com.kutoru.mikunotes.logic.requests.getFile
import com.kutoru.mikunotes.logic.requests.postFileToNote
import com.kutoru.mikunotes.logic.requests.postFileToShelf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FileRequestService : JobService() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var requestManager: RequestManager

    @SuppressLint("NewApi")
    override fun onCreate() {
        requestManager = (application as MikuNotesApp).requestManager

//        notificationHelper = NotificationHelper(
//            applicationContext,
//        ) { params, notificationId, notification ->
//            setNotification(
//                params!!,
//                notificationId,
//                notification,
//                JOB_END_NOTIFICATION_POLICY_DETACH,
//            )
//        }

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

            uris.forEach { uri ->
                scope.launch {
                    uploadFile(uri, itemId, isShelf)
                }
            }

            return true
        }

        if (params.extras.getBoolean(FILE_SERVICE_DOWNLOAD)) {
            val fileHash = params.extras.getString(FILE_SERVICE_FILE_HASH)!!
            scope.launch {
                downloadFile(fileHash)
            }

            return true
        }

        if (params.extras.getBoolean(FILE_SERVICE_DELETE)) {
            val fileId = params.extras.getInt(FILE_SERVICE_FILE_ID, -1).takeIf { it != -1 }!!
            scope.launch {
                deleteFile(fileId)
            }

            return true
        }

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        println("onStopJob: ${params?.extras}")
        return false
    }

    private suspend fun uploadFile(fileUri: Uri, itemId: Int, isShelf: Boolean) {
        println("uploadFile $fileUri, $itemId, $isShelf")

//        val notificationIndex = notificationHelper.showUploadInProgress(null, fileName, 0)

        val result = if (isShelf) {
            handleRequest { requestManager.postFileToShelf(
                contentResolver, fileUri, itemId,
            ) }
        } else {
            handleRequest { requestManager.postFileToNote(
                contentResolver, fileUri, itemId,
            ) }
        }

        if (result.isFailure && result.exceptionOrNull() !is RequestCancel) {
//            showMessage("Could not upload the file")
            return
        }

        val file = result.getOrNull()!!
        if (isShelf) {
            file.attach_id = 0
        }

        val intent = Intent(FILE_CHANGE_BROADCAST)
        intent.putExtra(FILE_CHANGE_ADDED, file)
        val requestCode = FILE_CHANGE_INTENT_OFFSET + file.hashCode()

        PendingIntent.getBroadcast(
            applicationContext, requestCode, intent, PendingIntent.FLAG_IMMUTABLE,
        ).send()
    }

    private suspend fun downloadFile(fileHash: String) {
        println("downloadFile $fileHash")

        val result = handleRequest { requestManager.getFile(fileHash) }
        if (result.isFailure && result.exceptionOrNull() !is RequestCancel) {
//            showMessage("Could not download the file")
        }
    }

    private suspend fun deleteFile(fileId: Int) {
        println("deleteFile $fileId")

        val result = handleRequest { requestManager.deleteFile(fileId) }
        if (result.isFailure) {
//            showMessage("Could not delete the file")
            return
        }

        val intent = Intent(FILE_CHANGE_BROADCAST)
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
