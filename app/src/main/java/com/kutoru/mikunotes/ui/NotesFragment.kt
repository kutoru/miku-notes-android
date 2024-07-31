package com.kutoru.mikunotes.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.viewmodel.ApiService
import com.kutoru.mikunotes.databinding.FragmentNotesBinding
import java.io.File

class NotesFragment : Fragment() {

    private lateinit var binding: FragmentNotesBinding
    private lateinit var activity: FragmentActivity
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var permissionActivityLauncher: ActivityResultLauncher<String>

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
            if (!notificationPermissionGranted()) {
                permissionActivityLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            val intent = Intent(activity, ApiService::class.java)
            val fileHash = "0249b1a3-7f06-4e39-88b7-1d6ab30400dd"
            intent.putExtra("FILE_HASH", fileHash)
            activity.startService(intent)
        }

        notificationManager = NotificationManagerCompat.from(requireContext())

        val permissionContract = ActivityResultContracts.RequestPermission()
        permissionActivityLauncher = registerForActivityResult(permissionContract) {
            println("permission granted: $it")

            if (!it) {
                Toast.makeText(
                    requireContext(),
                    "You won't see download progress notifications without the notification permission",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

        return binding.root
    }

    private fun notificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
