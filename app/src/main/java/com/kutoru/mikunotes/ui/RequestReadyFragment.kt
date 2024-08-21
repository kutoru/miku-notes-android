package com.kutoru.mikunotes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class RequestReadyFragment<T: ApiViewModel> : Fragment(), RequestReadyComponent<T> {

    private val job = Job()
    protected val scope = CoroutineScope(Dispatchers.Main + job)

    override var urlDialog: UrlPropertyDialog? = null

    override fun acquireContext() = requireContext()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        urlDialog = UrlPropertyDialog(requireContext())
        setupViewModelObservers()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}
