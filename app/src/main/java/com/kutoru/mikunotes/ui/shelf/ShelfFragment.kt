package com.kutoru.mikunotes.ui.shelf

import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.FragmentShelfBinding
import com.kutoru.mikunotes.logic.RequestCancel
import com.kutoru.mikunotes.ui.RequestReadyFragment
import com.kutoru.mikunotes.ui.main.MainActivity
import com.kutoru.mikunotes.ui.main.ShelfCallbacks
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ShelfFragment : RequestReadyFragment<ShelfViewModel>() {

    private lateinit var binding: FragmentShelfBinding

    private lateinit var loadDialog: ProgressDialog
    private var onShare: (suspend () -> Unit)? = null

    override val viewModel: ShelfViewModel by viewModels { ShelfViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentShelfBinding.inflate(inflater, container, false)

        binding.fdShelfFiles.setup<Any>(
            binding.root,
            ::showMessage,
            { binding.etlShelfText.height },
            ::uploadFile,
            ::downloadFile,
            ::deleteFile,
            ::registerForActivityResult,
        )

        (requireActivity() as MainActivity)
            .setShelfOptionsMenu(ShelfCallbacks(
                refresh = { refreshShelf(false) },
                copy = ::copyShelfToClipboard,
                save = { saveShelf(false) },
                clear = ::clearShelf,
                convert = ::convertShelfToNote,
            ))

        loadDialog = ProgressDialog(requireContext())
        loadDialog.setMessage("Loading the shelf...")
        loadDialog.setCancelable(false)

        return binding.root
    }

    override fun onStart() {
        binding.etlShelfText.text = ""
        super.onStart()
    }

    override fun onResume() {
        super.onResume()

        loadDialog.show()

        scope.launch {
            refreshShelf(true)
            if (viewModel.initialized && onShare != null) {
                loadDialog.show()
                onShare?.invoke()
                onShare = null
                loadDialog.dismiss()
            }
        }
    }

    override fun afterUrlDialogSave() {
        scope.launch {
            refreshShelf(true)
        }
    }

    override fun onPause() {
        if (viewModel.initialized) {
            scope.launch {
                saveShelf(true)
            }
        }

        super.onPause()
    }

    override fun setupViewModelObservers() {
        viewModel.text.observe(viewLifecycleOwner) {
            if (binding.etlShelfText.text != it) {
                binding.etlShelfText.text = it
            }
        }

        viewModel.lastEdited.observe(viewLifecycleOwner) {
            val lastEdited = LocalDateTime
                .ofEpochSecond(it, 0, ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))

            binding.tvShelfDate.text = lastEdited
        }

        viewModel.timesEdited.observe(viewLifecycleOwner) {
            binding.tvShelfCount.text = it.toString()
        }

        viewModel.files.observe(viewLifecycleOwner) {
            binding.fdShelfFiles.updateFiles(it)
        }
    }

    private fun uploadFile(fileUri: Uri) {
        scope.launch {
            val result = handleRequest { viewModel.postFile(
                requireContext().contentResolver, fileUri,
            ) }

            if (result.isFailure && result.exceptionOrNull() is RequestCancel) {
                showMessage("Upload cancelled")
            } else if (result.isFailure) {
                showMessage("Could not upload the file")
            }
        }
    }

    private fun downloadFile(fileIndex: Int) {
        scope.launch {
            val result = handleRequest { viewModel.getFile(fileIndex) }

            if (result.isFailure && result.exceptionOrNull() is RequestCancel) {
                showMessage("Download cancelled")
            } else if (result.isFailure) {
                showMessage("Could not download the file")
            }
        }
    }

    private fun deleteFile(fileIndex: Int) {
        scope.launch {
            val result = handleRequest { viewModel.deleteFile(fileIndex) }
            if (result.isFailure) {
                showMessage("Could not delete the file")
            } else {
                showMessage("The file has been deleted")
            }
        }
    }

    private fun refreshShelf(silent: Boolean) {
        scope.launch {
            val result = handleRequest { viewModel.getShelf() }
            if (result.isFailure) {
                if (!silent) showMessage("Could not refresh the shelf")
                return@launch
            }

            loadDialog.dismiss()

            if (!silent) showMessage("The shelf has been refreshed")
        }
    }

    private fun saveShelf(silent: Boolean) {
        val newText = binding.etlShelfText.text
        if (viewModel.text.value == newText) {
            if (!silent) showMessage("The shelf hasn't changed since last save")
            return
        }

        scope.launch {
            val result = handleRequest { viewModel.patchShelf(newText) }
            if (!silent) {
                if (result.isFailure) {
                    showMessage("Could not save the shelf")
                } else {
                    showMessage("The shelf has been saved")
                }
            }
        }
    }

    private fun clearShelf() {
        AlertDialog
            .Builder(requireContext())
            .setTitle("Clear the shelf?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear") { _, _ ->
                scope.launch {
                    val result = handleRequest { viewModel.deleteShelf() }
                    if (result.isFailure) {
                        showMessage("Could not clear the shelf")
                    }
                }
            }
            .show()
    }

    private fun convertShelfToNote() {
        val view = View.inflate(requireContext(), R.layout.dialog_convert_shelf, null)
        val etNoteTitle = view.findViewById<EditText>(R.id.etConvertNoteTitle)

        AlertDialog
            .Builder(requireContext())
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Convert") { _, _ ->
                val title = etNoteTitle.text.toString().trim()
                if (title.isEmpty()) {
                    showMessage("Cannot convert with an empty title")
                    return@setPositiveButton
                }

                scope.launch {
                    val result = handleRequest { viewModel.postShelfToNote(title) }
                    if (result.isFailure) {
                        showMessage("Could not convert the shelf")
                    }
                }
            }
            .show()
    }

    private fun copyShelfToClipboard() {
        val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
        val clipData = ClipData.newPlainText("shelf text", viewModel.text.value)
        clipboard.setPrimaryClip(clipData)
        showMessage("Text has been copied")
    }

    fun handleSharedText(text: String) {
        onShare = {
            binding.etlShelfText.text = text
            saveShelf(false)
        }
    }

    fun handleSharedFiles(fileUris: List<Uri>) {
        onShare = {
            fileUris.forEach {
                uploadFile(it)
            }
        }
    }
}
