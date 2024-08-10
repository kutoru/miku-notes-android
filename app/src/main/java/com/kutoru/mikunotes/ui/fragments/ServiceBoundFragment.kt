package com.kutoru.mikunotes.ui.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kutoru.mikunotes.logic.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

open class ServiceBoundFragment : Fragment() {

    private val job = Job()
    protected val scope = CoroutineScope(Dispatchers.Main + job)

    protected lateinit var apiService: ApiService
    protected var serviceIsBound = false
    protected var onServiceBound: (() -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ApiService.ServiceBinder
            apiService = binder.getService()
            serviceIsBound = true

            apiService.currentContext = requireContext()
            onServiceBound?.invoke()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceIsBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        startApiService()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onResume() {
        if (serviceIsBound && apiService.currentContext == null) {
            apiService.currentContext = requireContext()
        }

        super.onResume()
    }

    override fun onDestroy() {
        if (serviceIsBound) {
            apiService.currentContext = null
            requireActivity().unbindService(serviceConnection)
            serviceIsBound = false
        }

        job.cancel()
        super.onDestroy()
    }

    private fun startApiService() {
        val bindIntent = Intent(requireActivity(), ApiService::class.java)
        requireActivity().bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
}
