package com.kutoru.mikunotes.ui

import androidx.lifecycle.ViewModel
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.getAccess

abstract class ApiViewModel(
    protected val requestManager: RequestManager,
) : ViewModel() {

    fun updateUrl() {
        requestManager.updateUrl()
    }

    suspend fun getAccess() {
        requestManager.getAccess()
    }

}
