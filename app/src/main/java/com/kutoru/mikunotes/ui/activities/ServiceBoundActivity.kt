package com.kutoru.mikunotes.ui.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.kutoru.mikunotes.logic.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

open class ServiceBoundActivity : AppCompatActivity() {

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

            apiService.currentContext = this@ServiceBoundActivity
            onServiceBound?.invoke()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceIsBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        startApiService()
        return super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        if (serviceIsBound && apiService.currentContext == null) {
            apiService.currentContext = this
        }

        super.onResume()
    }

    override fun onDestroy() {
        if (serviceIsBound) {
            apiService.currentContext = null
            unbindService(serviceConnection)
            serviceIsBound = false
        }

        job.cancel()
        super.onDestroy()
    }

    private fun startApiService() {
        val bindIntent = Intent(this, ApiService::class.java)
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
}
