package com.kutoru.mikunotes.ui.note

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityNoteBinding
import com.kutoru.mikunotes.logic.ANIMATION_TRANSITION_TIME
import com.kutoru.mikunotes.logic.AppUtil
import com.kutoru.mikunotes.logic.RECYCLER_VIEW_FILE_COLUMNS
import com.kutoru.mikunotes.logic.RequestCancel
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.ApiReadyActivity
import com.kutoru.mikunotes.ui.TagViewModel
import com.kutoru.mikunotes.ui.adapters.FileListAdapter
import com.kutoru.mikunotes.ui.adapters.ItemMarginDecorator
import com.kutoru.mikunotes.ui.adapters.TagListAdapter
import kotlinx.coroutines.launch

class NoteActivity : ApiReadyActivity<NoteViewModel>() {

    private lateinit var binding: ActivityNoteBinding
    private lateinit var tagAdapter: TagListAdapter
    private lateinit var fileAdapter: FileListAdapter
    private lateinit var tagDialog: NoteTagDialog
    private var actionMenu: Menu? = null

    private lateinit var filePickActivityLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionActivityLauncher: ActivityResultLauncher<String>

    private var fileContainerExpanded = false
    private var lastRootHeight = 0

    override val viewModel: NoteViewModel by viewModels { NoteViewModel.Factory }
    private val tagViewModel: TagViewModel by viewModels { TagViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModelObservers()

        setSupportActionBar(binding.toolbarNote)
        supportActionBar?.title = "Note"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val result = runCatching { viewModel.parseFromIntent(intent) }
        if (result.isFailure) {
            Toast.makeText(this, "Tried to open an invalid note", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setInputOnFocusChange(
            binding.etNoteTitle,
            binding.dividerNote1,
            binding.dividerNote2,
        )

        setInputOnFocusChange(
            binding.etNoteText,
            binding.dividerNote3,
            binding.dividerNote4,
        )

        binding.btnNoteAddTag.setOnClickListener {
            tagDialog.show(viewModel.tags.value!!)
        }

        binding.fabNoteMoveFiles.setOnClickListener {
            moveFileContainer(true)
        }

        binding.fabNoteUpload.setOnClickListener {
            if (viewModel.isNewNote.value!!) {
                showToast("Save the new note before uploading any files")
                return@setOnClickListener
            }

            if (!canSendNotifications()) {
                notificationPermissionActivityLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (!readFilePermissionGranted()) {
                storagePermissionActivityLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                filePickActivityLauncher.launch("*/*")
            }
        }

        val rvItemMargin = resources.getDimension(R.dimen.margin).toInt()

        tagAdapter = TagListAdapter(
            true,
            listOf(),
            ::removeTag,
        )

        fileAdapter = FileListAdapter(
            rvItemMargin,
            listOf(),
            ::deleteFile,
            ::downloadFile,
        )

        binding.rvNoteTags.adapter = tagAdapter
        binding.rvNoteTags.addItemDecoration(ItemMarginDecorator.Tags(rvItemMargin))

        val bottomFilesPadding = resources.getDimension(R.dimen.fab_size).toInt() + fileAdapter.itemMargin * 2
        binding.rvNoteFiles.updatePadding(bottom = bottomFilesPadding)

        binding.rvNoteFiles.adapter = fileAdapter
        binding.rvNoteFiles.layoutManager = GridLayoutManager(this, RECYCLER_VIEW_FILE_COLUMNS)
        binding.rvNoteFiles.addItemDecoration(ItemMarginDecorator.Files(rvItemMargin))

        tagDialog = NoteTagDialog(
            this,
            this,
            binding.root,
            true,

            tagViewModel,
            ::handleRequest,
            ::showToast,

            // if i really want the button to be animated: https://stackoverflow.com/a/73798434
            { binding.btnNoteAddTag.isEnabled = false },
            { binding.btnNoteAddTag.isEnabled = true },
            ::onTagDialogAdd,
            ::onTagDialogRemove,
        )

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

        setNavigationBarColor(binding.root)

        val contentsContract = ActivityResultContracts.GetMultipleContents()
        val permissionContract = ActivityResultContracts.RequestPermission()

        filePickActivityLauncher = registerForActivityResult(contentsContract, ::uploadFiles)
        storagePermissionActivityLauncher = registerForActivityResult(permissionContract) {
            if (it) {
                filePickActivityLauncher.launch("*/*")
            } else {
                showToast("You need to provide access to your files to upload them")
            }
        }

        notificationPermissionActivityLauncher = registerForActivityResult(permissionContract) {
            if (!it) {
                showToast("You won't see download or upload notifications without the notification permission")
            }
        }
    }

    private fun setupViewModelObservers() {
        viewModel.title.observe(this) {
            val view = binding.etNoteTitle
            if (view.text.toString() != it) {
                view.setText(it)
            }
        }

        viewModel.text.observe(this) {
            val view = binding.etNoteText
            if (view.text.toString() != it) {
                view.setText(it)
            }
        }

        viewModel.created.observe(this) {
            val created = if (it != null) AppUtil.formatDateTime(it) else "-"
            binding.tvNoteCreated.text = created
        }

        viewModel.lastEdited.observe(this) {
            val lastEdited = if (it != null) AppUtil.formatDateTime(it) else "-"
            binding.tvNoteEdited.text = lastEdited
        }

        viewModel.timesEdited.observe(this) {
            binding.tvNoteCount.text = it?.toString() ?: "-"
        }

        viewModel.tags.observe(this) {
            if (it.isEmpty()) {
                binding.rvNoteTags.visibility = View.INVISIBLE
                binding.tvNoteNoTags.visibility = View.VISIBLE
            } else {
                binding.tvNoteNoTags.visibility = View.INVISIBLE
                binding.rvNoteTags.visibility = View.VISIBLE
            }

            tagAdapter.tags = it
            tagAdapter.notifyDataSetChanged()
        }

        viewModel.files.observe(this) {
            if (it.isEmpty()) {
                binding.rvNoteFiles.visibility = View.INVISIBLE
                binding.tvNoteNoFiles.visibility = View.VISIBLE
            } else {
                binding.tvNoteNoFiles.visibility = View.INVISIBLE
                binding.rvNoteFiles.visibility = View.VISIBLE
            }

            fileAdapter.files = it
            fileAdapter.notifyDataSetChanged()
        }

        viewModel.isNewNote.observe(this) {
            initializeActionMenu(it)
        }

        tagViewModel.tags.observe(this) { allTags ->
            if (!tagViewModel.initialized) {
                return@observe
            }

            var i = -1
            while (++i < viewModel.tags.value!!.size) {
                val attachedTag = viewModel.tags.value!![i]
                val matchingTag = allTags.find { it.id == attachedTag.id }

                if (matchingTag == null) {
                    viewModel.tagRemoved(i--)
                } else if (matchingTag.name != attachedTag.name) {
                    viewModel.tagUpdated(i, matchingTag.name)
                }
            }
        }
    }

    override fun onResume() {
        if (!viewModel.isNewNote.value!!) {
            refreshNote()
        }

        super.onResume()
    }

    override fun onPause() {
        if (viewModel.initialized && !viewModel.isNewNote.value!!) {
            saveNote()
        }

        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.note, menu)
        actionMenu = menu
        initializeActionMenu(viewModel.isNewNote.value!!)
        return true
    }

    private fun initializeActionMenu(isNewNote: Boolean) {
        if (isNewNote) {
            supportActionBar?.title = "New note"
            actionMenu?.findItem(R.id.actionNoteRefresh)?.isVisible = false
            actionMenu?.findItem(R.id.actionNoteDelete)?.isVisible = false
            actionMenu?.findItem(R.id.actionNoteSave)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        } else {
            supportActionBar?.title = "Note"
            actionMenu?.findItem(R.id.actionNoteRefresh)?.isVisible = true
            actionMenu?.findItem(R.id.actionNoteDelete)?.isVisible = true
            actionMenu?.findItem(R.id.actionNoteSave)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.actionNoteRefresh -> refreshNote()
            R.id.actionNoteSave -> saveNote()
            R.id.actionNoteDelete -> deleteNote()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onBackPressed() {
        if (tagDialog.isShown) {
            tagDialog.hide()
        } else {
            super.onBackPressed()
        }
    }

    private fun moveFileContainer(swapState: Boolean) {
        if (!swapState) {
            // if the container isn't expanded, then there is no
            // need to change the height, since it is static
            if (!fileContainerExpanded) {
                return
            }

            // otherwise, pre-swapping the state so that in the end
            // it stays expanded
            fileContainerExpanded = false
        }

        val currHeight = binding.rvNoteFiles.height
        val maxHeight = (binding.etNoteText.height + currHeight) / 2
        val minHeight = (
                resources.getDimension(R.dimen.fab_size) + fileAdapter.itemMargin * 2
            ).toInt()

        val desiredHeight = if (fileContainerExpanded) {
            fileContainerExpanded = false
            binding.fabNoteMoveFiles.setImageResource(R.drawable.ic_up)
            minHeight
        } else {
            fileContainerExpanded = true
            binding.fabNoteMoveFiles.setImageResource(R.drawable.ic_down)
            maxHeight
        }

        val animator = ValueAnimator.ofInt(currHeight, desiredHeight)
        animator.addUpdateListener {
            val height = it.animatedValue as Int
            val layoutParams = binding.rvNoteFiles.layoutParams
            layoutParams.height = height
            binding.rvNoteFiles.layoutParams = layoutParams
        }

        animator.duration = if (swapState) ANIMATION_TRANSITION_TIME.toLong() else 0
        animator.start()
    }

    private fun setInputOnFocusChange(inputView: EditText, dividerTop: View, dividerBottom: View) {
        dividerTop.background = getDrawable(R.drawable.input_transition)
        dividerBottom.background = getDrawable(R.drawable.input_transition)

        inputView.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
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

    private fun downloadFile(fileIndex: Int) {
        if (!canSendNotifications()) {
            notificationPermissionActivityLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        scope.launch {
            val result = handleRequest { viewModel.getFile(fileIndex) }

            if (result.isFailure && result.exceptionOrNull() is RequestCancel) {
                showToast("Download cancelled")
            } else if (result.isFailure) {
                showToast("Could not download the file")
            }
        }
    }

    private fun uploadFiles(fileUris: List<Uri>) {
        fileUris.forEach { uri ->
            scope.launch {
                val result = handleRequest { viewModel.postFile(
                    contentResolver, uri,
                ) }

                if (result.isFailure && result.exceptionOrNull() is RequestCancel) {
                    showToast("Upload cancelled")
                } else if (result.isFailure) {
                    showToast("Could not upload the file")
                }
            }
        }
    }

    private fun onTagDialogAdd(tag: Tag, updateMoveButton: () -> Unit) {
        scope.launch {
            val result = handleRequest { viewModel.postNotesTag(tag) }
            if (result.isFailure) {
                showToast("Could not add the tag")
            } else {
                updateMoveButton()
            }
        }
    }

    private fun onTagDialogRemove(tag: Tag, updateMoveButton: () -> Unit) {
        val position = viewModel.tags.value!!.indexOfFirst { it.id == tag.id }
        removeTag(position, updateMoveButton)
    }

    private fun removeTag(position: Int, updateMoveButton: (() -> Unit)? = null) {
        scope.launch {
            val result = handleRequest { viewModel.deleteNotesTag(position) }
            if (result.isFailure) {
                showToast("Could not remove the tag")
            } else {
                updateMoveButton?.invoke()
            }
        }
    }

    private fun refreshNote() {
        println("refreshNote")
    }

    private fun saveNote() {
        println("saveNote")
    }

    private fun deleteNote() {
        println("deleteNote")
    }

    private fun readFilePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun canSendNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
