package com.kutoru.mikunotes.ui

import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.FragmentShelfBinding
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.NotificationHelper
import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.logic.Unauthorized
import com.kutoru.mikunotes.logic.UnknownError
import com.kutoru.mikunotes.logic.UrlPropertyDialog
import com.kutoru.mikunotes.models.Shelf
import com.kutoru.mikunotes.models.ShelfPatch
import com.kutoru.mikunotes.models.ShelfToNote
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ShelfFragment : CustomFragment() {

    private lateinit var binding: FragmentShelfBinding
    private lateinit var adapter: FileListAdapter
    private lateinit var filePickActivityLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var loadDialog: ProgressDialog
    private lateinit var shelf: Shelf

    private var fromShareCallback: (suspend () -> Unit)? = null
    private var initialized = false

    private val readFilesPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
    private val postNotificationPermission = android.Manifest.permission.POST_NOTIFICATIONS

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentShelfBinding.inflate(inflater, container, false)

        adapter = FileListAdapter(
            requireContext(),
            listOf(),
            ::deleteFile,
            ::downloadFile,
        )

        binding.rvShelfFiles.adapter = adapter
        binding.rvShelfFiles.layoutManager = GridLayoutManager(requireContext(), 3)

        binding.fabShelfFileUpload.setOnClickListener {
            if (!readFilePermissionGranted()) {
                storagePermissionActivityLauncher.launch(readFilesPermission)
            } else {
                filePickActivityLauncher.launch("*/*")
            }
        }

        val contentsContract = ActivityResultContracts.GetMultipleContents()
        filePickActivityLauncher = registerForActivityResult(contentsContract, ::uploadFiles)

        val storagePermissionContract = ActivityResultContracts.RequestPermission()
        storagePermissionActivityLauncher = registerForActivityResult(storagePermissionContract) {
            if (it) {
                filePickActivityLauncher.launch("*/*")
            } else {
                showToast("You need to provide access to your files to upload them")
            }
        }

        val notificationPermissionContract = ActivityResultContracts.RequestPermission()
        notificationPermissionActivityLauncher = registerForActivityResult(notificationPermissionContract) {
            if (!it) {
                showToast("You won't see download notifications without the notification permission")
            }
        }

        (requireActivity() as MainActivity).setShelfOptionsMenu(
            { scope.launch { saveShelf(false) } },
            ::clearShelf,
            { scope.launch { refreshShelf(false) } },
            ::convertShelfToNote,
            ::copyShelfToClipboard,
        )

        loadDialog = ProgressDialog(requireContext())
        loadDialog.setMessage("Loading the shelf...")
        loadDialog.setCancelable(false)

        return binding.root
    }

    override fun onResume() {
        loadDialog.show()

        if (serviceIsBound) {
            scope.launch {
                refreshShelf(true)
                if (initialized && fromShareCallback != null) {
                    loadDialog.show()
                    fromShareCallback?.invoke()
                    loadDialog.dismiss()
                }
            }
        } else {
            onServiceBoundListener = {
                refreshShelf(true)
                if (initialized && fromShareCallback != null) {
                    loadDialog.show()
                    fromShareCallback?.invoke()
                    loadDialog.dismiss()
                }
            }
        }

        super.onResume()
    }

    override fun onPause() {
        onServiceBoundListener = null

        scope.launch {
            if (initialized) {
                saveShelf(true)
            }
        }

        super.onPause()
    }

    private fun deleteFile(fileIndex: Int) {
        scope.launch {
            val fileId = shelf.files[fileIndex].id

            if (handleRequest { apiService.deleteFile(fileId) } != null) {
                showToast("The file got successfully deleted")

                shelf.files.removeAt(fileIndex)
                adapter.files = shelf.files
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun downloadFile(fileIndex: Int) {
        if (!NotificationHelper.permissionGranted(requireContext())) {
            notificationPermissionActivityLauncher.launch(postNotificationPermission)
        }

        scope.launch {
            val fileHash = shelf.files[fileIndex].hash
            handleRequest { apiService.getFile(fileHash) }
        }
    }

    private fun uploadFiles(fileUris: List<Uri>) {
        fileUris.forEach { uri ->
            scope.launch {
                val file = handleRequest { apiService.postFileToShelf(uri, shelf.id) }
                if (file == null) {
                    showToast("Could not upload the file")
                    return@launch
                }

                shelf.files.add(file)
                adapter.files = shelf.files
                adapter.notifyDataSetChanged()
            }
        }
    }

    private suspend fun <T>handleRequest(requestFn: (suspend () -> T)): T? {
        try {
            return requestFn()
        } catch(e: Exception) {
            when (e) {
                is InvalidUrl -> {
                    UrlPropertyDialog.launch(
                        requireContext(),
                        false,
                        "Could not connect to the backend, make sure that the url properties are correct",
                    ) {
                        scope.launch {
                            apiService.updateUrl()
                            refreshShelf(true)
                        }
                    }

                    return null
                }

                is Unauthorized -> {
                    val intent = Intent(requireActivity(), LoginActivity::class.java)
                    requireActivity().startActivity(intent)
                    return null
                }

                is UnknownError, is ServerError -> {
                    println("Unknown error when calling backend: $e")
                    Toast.makeText(requireContext(), "Unknown error when calling backend: $e", Toast.LENGTH_LONG).show()
                    return null
                }

                else -> throw e
            }
        }
    }

    private suspend fun refreshShelf(silent: Boolean) {
        shelf = handleRequest { apiService.getShelf() } ?: return
        updateCurrentShelf(true)
        initialized = true
        loadDialog.dismiss()
        if (!silent) {
            showToast("The shelf has been refreshed")
        }
    }

    private suspend fun saveShelf(silent: Boolean) {
        val newText = binding.etShelfText.text.toString()
        if (shelf.text == newText) {
            if (!silent) {
                showToast("The text hasn't changed")
            }
            return
        }

        shelf = handleRequest { apiService.patchShelf(ShelfPatch(newText)) } ?: return
        updateCurrentShelf(false)
        if (!silent) {
            showToast("The shelf has been saved")
        }
    }

    private fun clearShelf() {
        AlertDialog
            .Builder(requireContext())
            .setTitle("Clear the shelf?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok") { _, _ ->
                scope.launch {
                    shelf = handleRequest { apiService.deleteShelf() } ?: return@launch
                    updateCurrentShelf(true)
                }
            }
            .show()
    }

    private fun convertShelfToNote() {
        val view = View.inflate(requireContext(), R.layout.dialog_convert_shelf, null)
        val etNoteTitle = view.findViewById<EditText>(R.id.etNoteTitle)

        AlertDialog
            .Builder(requireContext())
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Convert") { _, _ ->
                val title = etNoteTitle.text.toString().trim()
                if (title.isEmpty()) {
                    showToast("Cannot convert with an empty title")
                    return@setPositiveButton
                }

                scope.launch {
                    shelf = handleRequest { apiService.postShelfToNote(ShelfToNote(title, shelf.text)) } ?: return@launch
                    updateCurrentShelf(true)
                }
            }
            .show()
    }

    private fun copyShelfToClipboard() {
        val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
        val clipData = ClipData.newPlainText("shelf text", shelf.text)
        clipboard.setPrimaryClip(clipData)
        showToast("The text has been copied to clipboard")
    }

    private fun updateCurrentShelf(updateFiles: Boolean) {
        val lastEdited = LocalDateTime
            .ofEpochSecond(shelf.last_edited, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        binding.tvShelfDate.text = "Edited: $lastEdited"
        binding.tvShelfCount.text = "Count: ${shelf.times_edited}"
        binding.etShelfText.setText(shelf.text)

        if (updateFiles) {
            adapter.files = shelf.files
            adapter.notifyDataSetChanged()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun readFilePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            requireContext(), readFilesPermission,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun handleSharedText(text: String) {
        println("handleSharedText $text")
        fromShareCallback = {
            scope.launch {
                binding.etShelfText.setText(text)
                saveShelf(true)
            }
        }
    }

    fun handleSharedFiles(fileUris: List<Uri>) {
        println("handleSharedFiles $fileUris")
        fromShareCallback = {
            scope.launch {
                uploadFiles(fileUris)
            }
        }
    }
}
