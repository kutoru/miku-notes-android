package com.kutoru.mikunotes

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

// https://ktor.io/docs/client-serialization.html
// https://ktor.io/docs/client-cookies.html

class ApiService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val client = HttpClient()
    private val apiUrl = "http://192.168.1.12:3030"
    private lateinit var ctx: Context
    private val cookieValue = "at=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MjIxMjg2MzQsImlhdCI6MTcyMjEyMTQzNCwic3ViIjoiMSJ9.Dn3B-H5IgIpmbcTRJnpEI3huGzInrESvlI3mIqH-R2M"

    override fun onCreate() {
        ctx = applicationContext
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val fileHash = intent!!.getStringExtra("FILE_HASH")!!

        scope.launch {
            try {
                getShelf()
                getFile(fileHash)
            } catch (e: Throwable) {
                Log.i("abc", "Got an err: $e")
            }
        }

        return START_STICKY
    }

    private suspend fun getShelf() {
        Log.i("abc", "$apiUrl/shelf")

//        val res = client.get("$apiUrl/files/dl/$fileHash") {
        val res = client.get("$apiUrl/shelf") {
            this.headers.append("Cookie", cookieValue)
        }

        if (!handleHttpStatus(res.status)) {
            return
        }

        val body: String = res.body()
        Log.i("abc", body)
    }

    private suspend fun getFile(fileHash: String) {
        val url = "$apiUrl/files/dl/$fileHash"
        Log.i("abc", url)

//        val headReq = client.prepareHead("$apiUrl/files/dl/$fileHash"){
//            this.headers.append("Cookie", cookieValue)
//        }
//
//        val res = headReq.execute()
//        if (!handleHttpStatus(res.status)) {
//            return
//        }
//
//        val fileName = res.headers["content-disposition"]!!.split('=')[1].trim('"')

//        val dlManager = applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
//
//        val uri = Uri.parse(url)
//        val req = DownloadManager.Request(uri)
//
//        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
//        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//        req.setTitle(fileName)
//        req.addRequestHeader("cookie", cookieValue)
//
//        Log.i("abc", "starting a download")
//        val dlref = dlManager.enqueue(req)
//        Log.i("abc", "dlref: $dlref")

//        val file = File.createTempFile("files", "index")

        val req = client.prepareGet("$apiUrl/files/dl/$fileHash"){
            this.headers.append("Cookie", cookieValue)
        }

        val res = req.execute()
        if (!handleHttpStatus(res.status)) {
            return
        }

        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        var fileName = res.headers["content-disposition"]!!.split('=')[1].trim('"')
        var file = File("$downloadDir/$fileName")
        Log.i("abc", file.path)

        var fileIdx = 0
        val (filePrefix, fileExt) = fileName.split('.')

        while (!file.createNewFile()) {
            fileIdx++
            fileName = "$filePrefix ($fileIdx).$fileExt"
            file = File("$downloadDir/$fileName")
            Log.i("abc", file.path)
        }

        val channel: ByteReadChannel = res.body()

        while (!channel.isClosedForRead) {
            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())

            while (!packet.isEmpty) {
                val bytes = packet.readBytes()
                file.appendBytes(bytes)
                println("Received bytes ${file.length()}/${res.contentLength()}")
            }
        }

        println("A file saved to ${file.path}")
    }

    private fun handleHttpStatus(status: HttpStatusCode): Boolean {
        Log.i("abc", "Got a response with $status")
        return when (status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> true
            else -> false
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i("abc", "service onbind")
        return null
    }
}
