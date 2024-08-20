package com.kutoru.mikunotes.ui.shelf

import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.drawable.TransitionDrawable
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
import com.kutoru.mikunotes.logic.ANIMATION_TRANSITION_TIME
import com.kutoru.mikunotes.logic.RequestCancel
import com.kutoru.mikunotes.ui.ApiReadyFragment
import com.kutoru.mikunotes.ui.main.MainActivity
import com.kutoru.mikunotes.ui.main.ShelfCallbacks
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ShelfFragment : ApiReadyFragment<ShelfViewModel>() {

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

        setupViewModelObservers()

        setInputOnFocusChange(
            binding.etShelfText,
            binding.dividerShelf1,
            binding.dividerShelf2,
        )

        binding.fdShelfFiles.setup<Any>(
            binding.root,
            ::showToast,
            { binding.etShelfText.height },
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
        binding.etShelfText.setText("")
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

    private fun setInputOnFocusChange(inputView: EditText, dividerTop: View, dividerBottom: View) {
        dividerTop.background = requireContext().getDrawable(R.drawable.input_transition)
        dividerBottom.background = requireContext().getDrawable(R.drawable.input_transition)

        inputView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            val transTop = dividerTop.background as TransitionDrawable
            val transBottom = dividerBottom.background as TransitionDrawable

            if (hasFocus) {
                transTop.startTransition(ANIMATION_TRANSITION_TIME)
                transBottom.startTransition(ANIMATION_TRANSITION_TIME)
            } else {
                transTop.reverseTransition(ANIMATION_TRANSITION_TIME)
                transBottom.reverseTransition(ANIMATION_TRANSITION_TIME)
            }
        }
    }

    private fun setupViewModelObservers() {
        viewModel.text.observe(viewLifecycleOwner) {
            val view = binding.etShelfText
            if (view.text.toString() != it) {
                view.setText(it)
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
                showToast("Upload cancelled")
            } else if (result.isFailure) {
                showToast("Could not upload the file")
            }
        }
    }

    private fun downloadFile(fileIndex: Int) {
        scope.launch {
            val result = handleRequest { viewModel.getFile(fileIndex) }

            if (result.isFailure && result.exceptionOrNull() is RequestCancel) {
                showToast("Download cancelled")
            } else if (result.isFailure) {
                showToast("Could not download the file")
            }
        }
    }

    private fun deleteFile(fileIndex: Int) {
        scope.launch {
            val result = handleRequest { viewModel.deleteFile(fileIndex) }
            if (result.isFailure) {
                showToast("Could not delete the file")
            } else {
                showToast("The file has been deleted")
            }
        }
    }

    private fun refreshShelf(silent: Boolean) {
        scope.launch {
            val result = handleRequest { viewModel.getShelf() }
            if (result.isFailure) {
                if (!silent) showToast("Could not refresh the shelf")
                return@launch
            }

            loadDialog.dismiss()

            if (!silent) showToast("The shelf has been refreshed")
        }
    }

    private fun saveShelf(silent: Boolean) {
        val newText = binding.etShelfText.text.toString()
        if (viewModel.text.value == newText) {
            if (!silent) showToast("The shelf hasn't changed since last save")
            return
        }

        scope.launch {
            val result = handleRequest { viewModel.patchShelf(newText) }
            if (!silent) {
                if (result.isFailure) {
                    showToast("Could not save the shelf")
                } else {
                    showToast("The shelf has been saved")
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
                        showToast("Could not clear the shelf")
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
                    showToast("Cannot convert with an empty title")
                    return@setPositiveButton
                }

                scope.launch {
                    val result = handleRequest { viewModel.postShelfToNote(title) }
                    if (result.isFailure) {
                        showToast("Could not convert the shelf")
                    }
                }
            }
            .show()
    }

    private fun copyShelfToClipboard() {
        val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
        val clipData = ClipData.newPlainText("shelf text", viewModel.text.value)
        clipboard.setPrimaryClip(clipData)
        showToast("Text has been copied")
    }

    fun handleSharedText(text: String) {
        onShare = {
            binding.etShelfText.setText(text)
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
