package com.kutoru.mikunotes.logic

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.deleteShelf
import com.kutoru.mikunotes.logic.requests.getAccess
import com.kutoru.mikunotes.logic.requests.getFile
import com.kutoru.mikunotes.logic.requests.getLogin
import com.kutoru.mikunotes.logic.requests.getShelf
import com.kutoru.mikunotes.logic.requests.patchShelf
import com.kutoru.mikunotes.logic.requests.postShelfToNote
import com.kutoru.mikunotes.models.LoginBody
import com.kutoru.mikunotes.models.ShelfPatch
import com.kutoru.mikunotes.models.ShelfToNote

// https://ktor.io/docs/client-requests.html#upload_file

class ApiService : Service() {

    private val binder = ServiceBinder()
    private lateinit var requestManager: RequestManager

    override fun onCreate() {
        super.onCreate()
        requestManager = RequestManager(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    fun updateUrl() = requestManager.updateUrl()
    private fun updateCookies() = requestManager.updateCookies()

    private suspend fun getAccess() = requestManager.getAccess()
    suspend fun getLogin(loginBody: LoginBody) = requestManager.getLogin(loginBody)

    suspend fun getFile(fileHash: String) = makeRequest { requestManager.getFile(fileHash) }

    suspend fun getShelf() = makeRequest { requestManager.getShelf() }
    suspend fun deleteShelf() = makeRequest { requestManager.deleteShelf() }
    suspend fun patchShelf(body: ShelfPatch) = makeRequest { requestManager.patchShelf(body) }
    suspend fun postShelfToNote(body: ShelfToNote) = makeRequest { requestManager.postShelfToNote(body) }

    private suspend fun <T>makeRequest(fnToCall: suspend () -> T): T {
        return try {
            fnToCall()
        } catch (e: Unauthorized) {
            getAccess()
            fnToCall()
        }
    }

    inner class ServiceBinder : Binder() {
        fun getService(): ApiService {
            return this@ApiService
        }
    }
}
