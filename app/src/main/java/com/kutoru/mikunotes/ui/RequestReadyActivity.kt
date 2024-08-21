package com.kutoru.mikunotes.ui

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class RequestReadyActivity<T: ApiViewModel> : AppCompatActivity(), RequestReadyComponent<T> {

    private val job = Job()
    protected val scope = CoroutineScope(Dispatchers.Main + job)

    override var urlDialog: UrlPropertyDialog? = null

    override fun acquireContext() = this

    override fun onStart() {
        if (urlDialog != null) {
            urlDialog = UrlPropertyDialog(this)
        }

        setupViewModelObservers()
        super.onStart()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}
