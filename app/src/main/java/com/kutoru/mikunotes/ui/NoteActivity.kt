package com.kutoru.mikunotes.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.kutoru.mikunotes.databinding.ActivityNoteBinding
import com.kutoru.mikunotes.logic.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class NoteActivity : AppCompatActivity() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var binding: ActivityNoteBinding

    private lateinit var apiService: ApiService
    private var serviceIsBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ApiService.ServiceBinder
            apiService = binder.getService()
            serviceIsBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceIsBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarNote)
        supportActionBar?.title = "Note"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        startApiService()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onDestroy() {
        if (serviceIsBound) {
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
