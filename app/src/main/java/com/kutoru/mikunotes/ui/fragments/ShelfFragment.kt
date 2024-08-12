package com.kutoru.mikunotes.ui.fragments

import android.animation.ValueAnimator
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.FragmentShelfBinding
import com.kutoru.mikunotes.logic.ANIMATION_TRANSITION_TIME
import com.kutoru.mikunotes.logic.RECYCLER_VIEW_FILE_COLUMNS
import com.kutoru.mikunotes.ui.ShelfCallbacks
import com.kutoru.mikunotes.ui.activities.MainActivity
import com.kutoru.mikunotes.ui.adapters.FileListAdapter
import com.kutoru.mikunotes.viewmodels.ShelfViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ShelfFragment : ApiReadyFragment<ShelfViewModel>() {

    private lateinit var binding: FragmentShelfBinding
    private lateinit var adapter: FileListAdapter

    private lateinit var filePickActivityLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionActivityLauncher: ActivityResultLauncher<String>

    private lateinit var loadDialog: ProgressDialog
    private var onShare: (suspend () -> Unit)? = null
    private var lastRootHeight = 0
    private var fileContainerExpanded = true

    override val viewModel: ShelfViewModel by viewModels { ShelfViewModel.Factory }

    private val readFilesPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
    private val postNotificationPermission = android.Manifest.permission.POST_NOTIFICATIONS

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentShelfBinding.inflate(inflater, container, false)

        setInputOnFocusChange(
            binding.etShelfText,
            binding.dividerShelf1,
            binding.dividerShelf2,
        )

        adapter = FileListAdapter(
            requireContext(),
            listOf(),
            { pos -> scope.launch { deleteFile(pos) } },
            { pos -> scope.launch { downloadFile(pos) } },
        )

        binding.rvShelfFiles.adapter = adapter
        binding.rvShelfFiles.layoutManager = GridLayoutManager(requireContext(), RECYCLER_VIEW_FILE_COLUMNS)

        binding.fabShelfMoveFiles.setOnClickListener {
            moveFileContainer(true)
        }

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

        (requireActivity() as MainActivity)
            .setShelfOptionsMenu(ShelfCallbacks(
                refresh = { scope.launch { refreshShelf(false) } },
                copy = ::copyShelfToClipboard,
                save = { scope.launch { saveShelf(false) } },
                clear = ::clearShelf,
                convert = ::convertShelfToNote,
            ))

        loadDialog = ProgressDialog(requireContext())
        loadDialog.setMessage("Loading the shelf...")
        loadDialog.setCancelable(false)

        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val currHeight = Rect().let {
                binding.root.getWindowVisibleDisplayFrame(it)
                it.height()
            }

            if (currHeight != lastRootHeight) {
                lastRootHeight = currHeight
                moveFileContainer(false)
            }
        }

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

    private fun moveFileContainer(swapState: Boolean) {
        if (!swapState) {
            if (!fileContainerExpanded) {
                return
            }

            fileContainerExpanded = false
        }

        val currHeight = binding.rvShelfFiles.height
        val maxHeight = (binding.etShelfText.height + currHeight) / 2
        val minHeight = resources.getDimension(R.dimen.fab_size).toInt()

        val desiredHeight = if (fileContainerExpanded) {
            fileContainerExpanded = false
            binding.fabShelfMoveFiles.setImageResource(R.drawable.ic_up)
            minHeight
        } else {
            fileContainerExpanded = true
            binding.fabShelfMoveFiles.setImageResource(R.drawable.ic_down)
            maxHeight
        }

        val animator = ValueAnimator.ofInt(currHeight, desiredHeight)
        animator.addUpdateListener {
            val height = it.animatedValue as Int
            val layoutParams = binding.rvShelfFiles.layoutParams
            layoutParams.height = height
            binding.rvShelfFiles.layoutParams = layoutParams
        }

        animator.duration = if (swapState) ANIMATION_TRANSITION_TIME.toLong() else 0
        animator.start()
    }

    private suspend fun deleteFile(fileIndex: Int) {
        val result = handleRequest { viewModel.deleteFile(fileIndex) }
        if (result.isFailure) {
            showToast("Could not delete the file")
            return
        }

        updateCurrentFiles()

        showToast("The file has been deleted")
    }

    private suspend fun downloadFile(fileIndex: Int) {
        if (!canSendNotifications()) {
            notificationPermissionActivityLauncher.launch(postNotificationPermission)
        }

        val result = handleRequest { viewModel.getFile(fileIndex) }
        if (result.isFailure) {
            showToast("Could not download the file")
        }
    }

    private fun uploadFiles(fileUris: List<Uri>) {
        fileUris.forEach { uri ->
            scope.launch {
                val result = handleRequest { viewModel.postFile(
                    requireContext().contentResolver, uri,
                ) }
                if (result.isFailure) {
                    showToast("Could not upload the file")
                    return@launch
                }

                updateCurrentFiles()
            }
        }
    }

    private suspend fun refreshShelf(silent: Boolean) {
        val result = handleRequest { viewModel.getShelf() }
        if (result.isFailure) {
            if (!silent) showToast("Could not refresh the shelf")
            return
        }

        updateCurrentShelf(true)
        loadDialog.dismiss()

        if (!silent) showToast("The shelf has been refreshed")
    }

    private suspend fun saveShelf(silent: Boolean) {
        val newText = binding.etShelfText.text.toString()
        if (viewModel.shelf.text == newText) {
            if (!silent) showToast("The shelf hasn't changed since last save")
            return
        }

        val result = handleRequest { viewModel.patchShelf(newText) }
        if (result.isFailure) {
            if (!silent) showToast("Could not save the shelf")
            return
        }

        updateCurrentShelf(false)

        if (!silent) showToast("The shelf has been saved")
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
                        return@launch
                    }

                    updateCurrentShelf(true)
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
                        return@launch
                    }

                    updateCurrentShelf(true)
                }
            }
            .show()
    }

    private fun updateCurrentShelf(updateFiles: Boolean) {
        val lastEdited = LocalDateTime
            .ofEpochSecond(viewModel.shelf.last_edited, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))

        binding.tvShelfDate.text = lastEdited
        binding.tvShelfCount.text = viewModel.shelf.times_edited.toString()
        binding.etShelfText.setText(viewModel.shelf.text)

        if (updateFiles) {
            updateCurrentFiles()
        }
    }

    private fun updateCurrentFiles() {
        adapter.files = viewModel.shelf.files
        adapter.notifyDataSetChanged()
    }

    private fun copyShelfToClipboard() {
        val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
        val clipData = ClipData.newPlainText("shelf text", viewModel.shelf.text)
        clipboard.setPrimaryClip(clipData)
        showToast("The text has been copied to clipboard")
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
        onShare = {
            scope.launch {
                binding.etShelfText.setText(text)
                saveShelf(true)
            }
        }
    }

    fun handleSharedFiles(fileUris: List<Uri>) {
        println("handleSharedFiles $fileUris")
        onShare = {
            scope.launch {
                uploadFiles(fileUris)
            }
        }
    }

    private fun canSendNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
