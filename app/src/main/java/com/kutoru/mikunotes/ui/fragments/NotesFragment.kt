package com.kutoru.mikunotes.ui.fragments

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
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
import com.kutoru.mikunotes.databinding.FragmentNotesBinding
import com.kutoru.mikunotes.logic.SELECTED_NOTE
import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.NoteQueryParameters
import com.kutoru.mikunotes.ui.NotesCallbacks
import com.kutoru.mikunotes.ui.activities.MainActivity
import com.kutoru.mikunotes.ui.activities.NoteActivity
import kotlinx.coroutines.launch

class NotesFragment : ServiceBoundFragment() {

    private lateinit var binding: FragmentNotesBinding
    private lateinit var notificationPermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionActivityLauncher: ActivityResultLauncher<String>

    private lateinit var loadDialog: ProgressDialog

    private var notes = mutableListOf<Note>()
    private var pageCount = 0u
    private var initialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentNotesBinding.inflate(inflater, container, false)

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(requireActivity(), NoteActivity::class.java)
            intent.putExtra(SELECTED_NOTE, notes[0])
            requireActivity().startActivity(intent)
        }

        val permissionContract = ActivityResultContracts.RequestPermission()
        notificationPermissionActivityLauncher = registerForActivityResult(permissionContract) {
            println("notificationPermissionActivityLauncher $it")
        }

        storagePermissionActivityLauncher = registerForActivityResult(permissionContract) {
            println("storagePermissionActivityLauncher $it")
        }

        (requireActivity() as MainActivity)
            .setNotesOptionsMenu(NotesCallbacks(
                refresh = { scope.launch { refreshNotes(false) } },
            ))

        loadDialog = ProgressDialog(requireContext())
        loadDialog.setMessage("Loading the notes...")
        loadDialog.setCancelable(false)

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        loadDialog.show()

        if (serviceIsBound) {
            scope.launch {
                refreshNotes(true)
            }
        } else {
            onServiceBound = {
                this.afterUrlPropertySave = {
                    scope.launch {
                        refreshNotes(true)
                    }
                }

                refreshNotes(true)
            }
        }
    }

    private suspend fun refreshNotes(silent: Boolean) {
        // gather the params from inputs or something
        val parameters = NoteQueryParameters(null, null, null, null, null, null, null, null)

        val (newNotes, newPageCount) = apiService.getNotes(
            if (!silent) "Could not refresh the shelf" else null,
            parameters,
        ) ?: return

        notes = newNotes
        pageCount = newPageCount
        updateCurrentNotes()

        initialized = true
        loadDialog.dismiss()

        if (!silent) {
            showToast("The notes have been refreshed")
        }
    }

    private fun updateCurrentNotes() {
        // update the ui stuff
        binding.tvNotesTest.text = "Notes: ${notes.size}; Pages: $pageCount;\n$notes"
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
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
