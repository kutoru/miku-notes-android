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
import com.kutoru.mikunotes.logic.requests.deleteNotes
import com.kutoru.mikunotes.logic.requests.deleteNotesTag
import com.kutoru.mikunotes.logic.requests.deleteShelf
import com.kutoru.mikunotes.logic.requests.deleteTags
import com.kutoru.mikunotes.logic.requests.getAccess
import com.kutoru.mikunotes.logic.requests.getFile
import com.kutoru.mikunotes.logic.requests.getLogout
import com.kutoru.mikunotes.logic.requests.getNotes
import com.kutoru.mikunotes.logic.requests.getShelf
import com.kutoru.mikunotes.logic.requests.getTags
import com.kutoru.mikunotes.logic.requests.patchNotes
import com.kutoru.mikunotes.logic.requests.patchShelf
import com.kutoru.mikunotes.logic.requests.patchTags
import com.kutoru.mikunotes.logic.requests.postFileToNote
import com.kutoru.mikunotes.logic.requests.postFileToShelf
import com.kutoru.mikunotes.logic.requests.postLogin
import com.kutoru.mikunotes.logic.requests.postNotes
import com.kutoru.mikunotes.logic.requests.postNotesTag
import com.kutoru.mikunotes.logic.requests.postRegister
import com.kutoru.mikunotes.logic.requests.postShelfToNote
import com.kutoru.mikunotes.logic.requests.postTags
import com.kutoru.mikunotes.models.LoginBody
import com.kutoru.mikunotes.models.NotePost
import com.kutoru.mikunotes.models.NoteQueryParameters
import com.kutoru.mikunotes.models.NoteTagPost
import com.kutoru.mikunotes.models.ShelfPatch
import com.kutoru.mikunotes.models.ShelfToNote
import com.kutoru.mikunotes.models.TagPost
import com.kutoru.mikunotes.ui.UrlPropertyDialog
import com.kutoru.mikunotes.ui.activities.LoginActivity

class ApiService : Service() {

    private val binder = ServiceBinder()
    private lateinit var requestManager: RequestManager
    private var urlDialog: UrlPropertyDialog? = null

    var afterUrlPropertySave: (() -> Unit)? = null
    var currentContext: Context? = null
        set(value) {
            field = value

            if (currentContext == null) {
                urlDialog?.dismiss()
                urlDialog = null
            } else {
                urlDialog?.dismiss()
                urlDialog = UrlPropertyDialog(currentContext!!)
            }
        }

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
    suspend fun getLogout(onFailMessage: String?) = runRequest(onFailMessage) { requestManager.getLogout() }

    suspend fun getNotes(onFailMessage: String?, queryParams: NoteQueryParameters) = runRequest(onFailMessage) { requestManager.getNotes(queryParams) }
    suspend fun postNotes(onFailMessage: String?, body: NotePost) = runRequest(onFailMessage) { requestManager.postNotes(body) }
    suspend fun deleteNotes(onFailMessage: String?, noteId: Int) = runRequest(onFailMessage) { requestManager.deleteNotes(noteId) }
    suspend fun patchNotes(onFailMessage: String?, noteId: Int, body: NotePost) = runRequest(onFailMessage) { requestManager.patchNotes(noteId, body) }
    suspend fun postNotesTag(onFailMessage: String?, noteId: Int, body: NoteTagPost) = runRequest(onFailMessage) { requestManager.postNotesTag(noteId, body) }
    suspend fun deleteNotesTag(onFailMessage: String?, noteId: Int, tagId: Int) = runRequest(onFailMessage) { requestManager.deleteNotesTag(noteId, tagId) }

    suspend fun getTags(onFailMessage: String?) = runRequest(onFailMessage) { requestManager.getTags() }
    suspend fun postTags(onFailMessage: String?, body: TagPost) = runRequest(onFailMessage) { requestManager.postTags(body) }
    suspend fun deleteTags(onFailMessage: String?, tagId: Int) = runRequest(onFailMessage) { requestManager.deleteTags(tagId) }
    suspend fun patchTags(onFailMessage: String?, body: TagPost) = runRequest(onFailMessage) { requestManager.patchTags(body) }

    suspend fun postFileToNote(onFailMessage: String?, fileUri: Uri, noteId: Int) = runRequest(onFailMessage) { requestManager.postFileToNote(fileUri, noteId) }
    suspend fun postFileToShelf(onFailMessage: String?, fileUri: Uri, shelfId: Int) = runRequest(onFailMessage) { requestManager.postFileToShelf(fileUri, shelfId) }
    suspend fun getFile(onFailMessage: String?, fileHash: String) = runRequest(onFailMessage) { requestManager.getFile(fileHash) }
    suspend fun deleteFile(onFailMessage: String?, fileId: Int) = runRequest(onFailMessage) { requestManager.deleteFile(fileId) }

    suspend fun getShelf(onFailMessage: String?) = runRequest(onFailMessage) { requestManager.getShelf() }
    suspend fun deleteShelf(onFailMessage: String?) = runRequest(onFailMessage) { requestManager.deleteShelf() }
    suspend fun patchShelf(onFailMessage: String?, body: ShelfPatch) = runRequest(onFailMessage) { requestManager.patchShelf(body) }
    suspend fun postShelfToNote(onFailMessage: String?, body: ShelfToNote) = runRequest(onFailMessage) { requestManager.postShelfToNote(body) }

    private suspend fun <T>runRequest(onFailMessage: String?, requestFunction: suspend () -> T): T? {
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
        val result = runCatching {
            requestFunction()
        }

        if (result.isSuccess) {
            return result.getOrThrow()
        }

        if (onFailMessage != null) {
            Toast.makeText(currentContext!!, onFailMessage, Toast.LENGTH_LONG).show()
        }

        val err = result.exceptionOrNull()

        var errorMessage = when (err) {
            is Error -> {
                println("Unhandleable error: $err")
                throw err
            }

            is Unauthorized -> {
                val intent = Intent(currentContext!!, LoginActivity::class.java)
                intent.putExtra(LAUNCHED_LOGIN_FROM_ERROR, true)
                currentContext!!.startActivity(intent)
                return null
            }

            is InvalidUrl -> "Could not connect to the server"
            is ServerError -> "Unexpected server error: $err"
            else -> "Unknown error: $err"
        }

        errorMessage += ".\nMake sure that these properties are correct and the server is running"

        urlDialog!!.show(false, errorMessage) {
            updateUrl()
            afterUrlPropertySave?.invoke()
        }

        return null
    }

    inner class ServiceBinder : Binder() {
        fun getService(): ApiService {
            return this@ApiService
        }
    }
}
