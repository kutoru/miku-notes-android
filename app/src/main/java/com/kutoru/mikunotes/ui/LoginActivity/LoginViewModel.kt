package com.kutoru.mikunotes.ui.LoginActivity

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.postLogin
import com.kutoru.mikunotes.logic.requests.postRegister
import com.kutoru.mikunotes.models.LoginBody
import com.kutoru.mikunotes.ui.ApiViewModel

class LoginViewModel (requestManager: RequestManager) : ApiViewModel(requestManager) {

    suspend fun postLogin(email: String, password: String) {
        requestManager.postLogin(LoginBody(email, password))
    }

    suspend fun postRegister(email: String, password: String) {
        requestManager.postRegister(LoginBody(email, password))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val requestManager = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MikuNotesApp).requestManager
                LoginViewModel(requestManager)
            }
        }
    }
}
