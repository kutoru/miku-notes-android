package com.kutoru.mikunotes.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.kutoru.mikunotes.logic.ApiService
import com.kutoru.mikunotes.databinding.FragmentNotesBinding
import com.kutoru.mikunotes.logic.NotificationHelper

class NotesFragment : Fragment() {

    private lateinit var binding: FragmentNotesBinding
    private lateinit var activity: FragmentActivity
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationPermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionActivityLauncher: ActivityResultLauncher<String>

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

            downloadFile()
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
                downloadFile()
            }
        }

        return binding.root
    }

    private fun storagePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun downloadFile() {
        val intent = Intent(activity, ApiService::class.java)
        val fileHash = "f37e3f64-14b4-4c87-9f1a-f183182115c2"
        intent.putExtra("FILE_HASH", fileHash)
        activity.startService(intent)
    }
}
