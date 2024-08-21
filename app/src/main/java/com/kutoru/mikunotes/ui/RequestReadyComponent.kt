package com.kutoru.mikunotes.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.LAUNCHED_LOGIN_FROM_ERROR
import com.kutoru.mikunotes.logic.RequestCancel
import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.logic.Unauthorized
import com.kutoru.mikunotes.ui.login.LoginActivity

interface RequestReadyComponent<T: ApiViewModel> {

    var urlDialog: UrlPropertyDialog?
    val viewModel: T

    fun acquireContext(): Context
    fun setupViewModelObservers()
    fun afterUrlDialogSave()

    fun showMessage(message: String?) {
        if (message != null) {
            Toast.makeText(acquireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    suspend fun <T>handleRequest(requestFunction: suspend () -> T): Result<T> {
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
                val intent = Intent(acquireContext(), LoginActivity::class.java)
                intent.putExtra(LAUNCHED_LOGIN_FROM_ERROR, true)
                acquireContext().startActivity(intent)
                return result
            }

            is RequestCancel -> {
                return result
            }

            is InvalidUrl -> "Could not connect to the server"
            is ServerError -> "Unexpected server error: $err"
            else -> "Unknown error: $err"
        }

        errorMessage += ".\nMake sure that these properties are correct and the server is running"

        urlDialog!!.show(false, errorMessage) {
            viewModel.updateUrl()
            afterUrlDialogSave()
        }

        return result
    }
}
