package com.kutoru.mikunotes.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.LAUNCHED_LOGIN_FROM_ERROR
import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.logic.Unauthorized
import com.kutoru.mikunotes.ui.LoginActivity.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class ApiReadyActivity<T: ApiViewModel> : AppCompatActivity() {

    private val job = Job()
    protected val scope = CoroutineScope(Dispatchers.Main + job)

    protected lateinit var urlDialog: UrlPropertyDialog
        private set

    protected abstract val viewModel: T

    override fun onCreate(savedInstanceState: Bundle?) {
        urlDialog = UrlPropertyDialog(this)
        return super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    protected suspend fun <T>handleRequest(requestFunction: suspend () -> T): Result<T> {
        return handleRequestErrors {
            try {
                requestFunction()
            } catch (e: Unauthorized) {
                viewModel.getAccess()
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

        var errorMessage = when (err) {
            is Error -> {
                println("Unhandleable error: $err")
                throw err
            }

            is Unauthorized -> {
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra(LAUNCHED_LOGIN_FROM_ERROR, true)
                this.startActivity(intent)
                return result
            }

            is InvalidUrl -> "Could not connect to the server"
            is ServerError -> "Unexpected server error: $err"
            else -> "Unknown error: $err"
        }

        println(errorMessage)
        errorMessage += ".\nMake sure that these properties are correct and the server is running"

        urlDialog.show(false, errorMessage) {
            viewModel.updateUrl()
            afterUrlDialogSave()
        }

        return result
    }

    protected fun showToast(message: String?) {
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    protected open fun afterUrlDialogSave() {}
}
