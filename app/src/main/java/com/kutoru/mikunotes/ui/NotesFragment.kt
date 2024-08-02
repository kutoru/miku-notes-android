package com.kutoru.mikunotes.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import com.kutoru.mikunotes.databinding.FragmentNotesBinding
import com.kutoru.mikunotes.logic.NotificationHelper
import com.kutoru.mikunotes.models.LoginBody
import kotlinx.coroutines.launch

class NotesFragment : CustomFragment() {

    private lateinit var binding: FragmentNotesBinding
    private lateinit var notificationPermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionActivityLauncher: ActivityResultLauncher<String>

    private val fileHash = "f37e3f64-14b4-4c87-9f1a-f183182115c2"
    private val email = "kuromix@mail.ru"
    private val pass = "12345678"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentNotesBinding.inflate(inflater, container, false)

        binding.tvNote.text = "This is the Notes menu"
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
}
