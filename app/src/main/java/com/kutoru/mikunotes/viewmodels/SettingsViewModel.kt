package com.kutoru.mikunotes.viewmodels

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.getLogout

class SettingsViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    suspend fun getLogout() {
        requestManager.getLogout()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val requestManager = (this[APPLICATION_KEY] as MikuNotesApp).requestManager
                SettingsViewModel(requestManager)
            }
        }
    }
}
