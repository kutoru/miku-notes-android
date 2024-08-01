package com.kutoru.mikunotes.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import com.kutoru.mikunotes.databinding.FragmentNotesBinding
import com.kutoru.mikunotes.logic.ApiService
import com.kutoru.mikunotes.logic.NotificationHelper
import com.kutoru.mikunotes.models.LoginBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var binding: FragmentNotesBinding
    private lateinit var activity: FragmentActivity
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationPermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var apiService: ApiService
    private var serviceIsBound = false

    private val fileHash = "f37e3f64-14b4-4c87-9f1a-f183182115c2"
    private val email = "kuromix@mail.ru"
    private val pass = "12345678"

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            println("onServiceConnected")

            val binder = service as ApiService.ServiceBinder
            apiService = binder.getService()
            serviceIsBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            println("onServiceDisconnected")
            serviceIsBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNotesBinding.inflate(inflater, container, false)
        activity = requireActivity()

        binding.tvShelf.text = "This is the Notes menu"
        binding.fabAddNote.setOnClickListener {
            Snackbar.make(it, "*add a note*", Snackbar.LENGTH_LONG).show()
        }

        binding.btnDownload.setOnClickListener {
            if (!NotificationHelper.permissionGranted(requireContext())) {
                notificationPermissionActivityLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (!storagePermissionGranted()) {
                storagePermissionActivityLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return@setOnClickListener
            }

            scope.launch {
                apiService.getFile(fileHash)
            }
        }

        notificationManager = NotificationManagerCompat.from(requireContext())

        val permissionContract = ActivityResultContracts.RequestPermission()
        notificationPermissionActivityLauncher = registerForActivityResult(permissionContract) {
            println("notification permission granted: $it")

            if (!it) {
                Toast.makeText(
                    requireContext(),
                    "You won't see download notifications without the notification permission",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

        storagePermissionActivityLauncher = registerForActivityResult(permissionContract) {
            println("storage permission granted: $it")

            if (!it) {
                Toast.makeText(
                    requireContext(),
                    "You won't be able to download files without the storage permission",
                    Toast.LENGTH_LONG,
                ).show()
            } else {
                scope.launch {
                    apiService.getFile(fileHash)
                }
            }
        }

        binding.btnGetShelf.setOnClickListener {
            scope.launch {
                val shelf = apiService.getShelf()
                println("shelf: $shelf")
            }
        }

        binding.btnLogin.setOnClickListener {
            scope.launch {
                apiService.login(LoginBody(email, pass))
            }
        }

        startApiService()

        return binding.root
    }

    override fun onDestroy() {
        if (serviceIsBound) {
            activity.unbindService(serviceConnection)
            serviceIsBound = false
        }

        job.cancel()
        super.onDestroy()
    }

    private fun startApiService() {
        val intent = Intent(activity, ApiService::class.java)
        activity.startService(intent)

        val bindIntent = Intent(activity, ApiService::class.java)
        activity.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun storagePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
