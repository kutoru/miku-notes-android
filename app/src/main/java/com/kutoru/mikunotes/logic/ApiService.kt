package com.kutoru.mikunotes.logic

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.deleteFile
import com.kutoru.mikunotes.logic.requests.deleteShelf
import com.kutoru.mikunotes.logic.requests.deleteTags
import com.kutoru.mikunotes.logic.requests.getAccess
import com.kutoru.mikunotes.logic.requests.getFile
import com.kutoru.mikunotes.logic.requests.getLogout
import com.kutoru.mikunotes.logic.requests.getShelf
import com.kutoru.mikunotes.logic.requests.getTags
import com.kutoru.mikunotes.logic.requests.patchShelf
import com.kutoru.mikunotes.logic.requests.patchTags
import com.kutoru.mikunotes.logic.requests.postFileToNote
import com.kutoru.mikunotes.logic.requests.postFileToShelf
import com.kutoru.mikunotes.logic.requests.postLogin
import com.kutoru.mikunotes.logic.requests.postRegister
import com.kutoru.mikunotes.logic.requests.postShelfToNote
import com.kutoru.mikunotes.logic.requests.postTags
import com.kutoru.mikunotes.models.LoginBody
import com.kutoru.mikunotes.models.ShelfPatch
import com.kutoru.mikunotes.models.ShelfToNote
import com.kutoru.mikunotes.models.TagPost
import com.kutoru.mikunotes.ui.UrlPropertyDialog
import com.kutoru.mikunotes.ui.activities.LoginActivity

class ApiService : Service() {

    private val binder = ServiceBinder()
    private lateinit var requestManager: RequestManager

    var afterUrlPropertySave: (() -> Unit)? = null
    var currentContext: Context? = null

    override fun onCreate() {
        super.onCreate()
        requestManager = RequestManager(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    fun updateUrl() = requestManager.updateUrl()

    private suspend fun getAccess() = requestManager.getAccess()
    suspend fun postLogin(loginBody: LoginBody) = requestManager.postLogin(loginBody)
    suspend fun postRegister(loginBody: LoginBody) = requestManager.postRegister(loginBody)
    suspend fun getLogout(onFailMessage: String?) = makeRequest(onFailMessage) { requestManager.getLogout() }

    suspend fun postFileToNote(onFailMessage: String?, fileUri: Uri, noteId: Int) = makeRequest(onFailMessage) { requestManager.postFileToNote(fileUri, noteId) }
    suspend fun postFileToShelf(onFailMessage: String?, fileUri: Uri, shelfId: Int) = makeRequest(onFailMessage) { requestManager.postFileToShelf(fileUri, shelfId) }
    suspend fun getFile(onFailMessage: String?, fileHash: String) = makeRequest(onFailMessage) { requestManager.getFile(fileHash) }
    suspend fun deleteFile(onFailMessage: String?, fileId: Int) = makeRequest(onFailMessage) { requestManager.deleteFile(fileId) }

    suspend fun getTags(onFailMessage: String?) = makeRequest(onFailMessage) { requestManager.getTags() }
    suspend fun postTags(onFailMessage: String?, body: TagPost) = makeRequest(onFailMessage) { requestManager.postTags(body) }
    suspend fun deleteTags(onFailMessage: String?, tagId: Int) = makeRequest(onFailMessage) { requestManager.deleteTags(tagId) }
    suspend fun patchTags(onFailMessage: String?, body: TagPost) = makeRequest(onFailMessage) { requestManager.patchTags(body) }

    suspend fun getShelf(onFailMessage: String?) = makeRequest(onFailMessage) { requestManager.getShelf() }
    suspend fun deleteShelf(onFailMessage: String?) = makeRequest(onFailMessage) { requestManager.deleteShelf() }
    suspend fun patchShelf(onFailMessage: String?, body: ShelfPatch) = makeRequest(onFailMessage) { requestManager.patchShelf(body) }
    suspend fun postShelfToNote(onFailMessage: String?, body: ShelfToNote) = makeRequest(onFailMessage) { requestManager.postShelfToNote(body) }

    private suspend fun <T>makeRequest(onFailMessage: String?, requestFunction: suspend () -> T): T? {
        return handleRequestErrors(onFailMessage) {
            try {
                requestFunction()
            } catch (e: Unauthorized) {
                getAccess()
                requestFunction()
            }
        }
    }

    private suspend fun <T>handleRequestErrors(onFailMessage: String?, requestFunction: suspend () -> T): T? {
        try {
            return requestFunction()
        } catch(e: Exception) {
            if (onFailMessage != null) {
                Toast.makeText(currentContext!!, onFailMessage, Toast.LENGTH_LONG).show()
            }

            when (e) {
                is InvalidUrl -> {
                    UrlPropertyDialog.launch(
                        currentContext!!,
                        "Could not connect to the server. Make sure that the URL properties are correct and the server is running",
                        false,
                    ) {
                        updateUrl()
                        afterUrlPropertySave?.invoke()
                    }

                    return null
                }

                is Unauthorized -> {
                    val intent = Intent(currentContext!!, LoginActivity::class.java)
                    intent.putExtra(LAUNCHED_LOGIN_FROM_ERROR, true)
                    currentContext!!.startActivity(intent)
                    return null
                }

                is ServerError -> {
                    println("Unknown server error; Message: $onFailMessage; Error: $e;")
                    return null
                }

                else -> throw e
            }
        }
    }

    inner class ServiceBinder : Binder() {
        fun getService(): ApiService {
            return this@ApiService
        }
    }
}
