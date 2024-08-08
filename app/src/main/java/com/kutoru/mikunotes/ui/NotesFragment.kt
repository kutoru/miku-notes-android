package com.kutoru.mikunotes.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.kutoru.mikunotes.databinding.FragmentNotesBinding

class NotesFragment : CustomFragment() {

    private lateinit var binding: FragmentNotesBinding
    private lateinit var notificationPermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionActivityLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentNotesBinding.inflate(inflater, container, false)

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(requireActivity(), NoteActivity::class.java)
            requireActivity().startActivity(intent)
        }

        val permissionContract = ActivityResultContracts.RequestPermission()
        notificationPermissionActivityLauncher = registerForActivityResult(permissionContract) {
            println("notificationPermissionActivityLauncher $it")
        }

        storagePermissionActivityLauncher = registerForActivityResult(permissionContract) {
            println("storagePermissionActivityLauncher $it")
        }

        (requireActivity() as MainActivity).setNotesOptionsMenu()

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
