package com.kutoru.mikunotes.ui.note

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityNoteBinding
import com.kutoru.mikunotes.logic.AppUtil
import com.kutoru.mikunotes.logic.RequestCancel
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.RequestReadyActivity
import com.kutoru.mikunotes.ui.TagViewModel
import com.kutoru.mikunotes.ui.adapters.ItemMarginDecorator
import com.kutoru.mikunotes.ui.adapters.TagListAdapter
import kotlinx.coroutines.launch

class NoteActivity : RequestReadyActivity<NoteViewModel>() {

    private lateinit var binding: ActivityNoteBinding
    private lateinit var tagAdapter: TagListAdapter
    private lateinit var tagDialog: NoteTagDialog
    private var actionMenu: Menu? = null

    override val viewModel: NoteViewModel by viewModels { NoteViewModel.Factory }
    private val tagViewModel: TagViewModel by viewModels { TagViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarNote)
        supportActionBar?.title = "Note"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val result = runCatching { viewModel.parseFromIntent(intent) }
        if (result.isFailure) {
            Toast.makeText(this, "Tried to open an invalid note", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.fdNoteFiles.setup<Any>(
            binding.root,
            ::showMessage,
            { binding.etlNoteText.height },
            ::uploadFile,
            ::downloadFile,
            ::deleteFile,
            ::registerForActivityResult,
        )

        binding.btnNoteAddTag.setOnClickListener {
            tagDialog.show(viewModel.tags.value!!)
        }

        tagAdapter = TagListAdapter(
            listOf(),
            ::removeTag,
        )

        binding.rvNoteTags.adapter = tagAdapter
        binding.rvNoteTags.addItemDecoration(ItemMarginDecorator.Tags(
            resources.getDimension(R.dimen.margin).toInt(),
        ))

        tagDialog = NoteTagDialog(
            this,
            this,
            binding.root,
            true,

            tagViewModel,
            ::handleRequest,
            ::showMessage,

            // if i really want the button to be animated: https://stackoverflow.com/a/73798434
            { binding.btnNoteAddTag.isEnabled = false },
            { binding.btnNoteAddTag.isEnabled = true },
            ::onTagDialogAdd,
            ::onTagDialogRemove,
        )

        AppUtil.setNavigationBarColor(window, binding.root)
    }

    override fun setupViewModelObservers() {
        viewModel.title.observe(this) {
            if (binding.etlNoteTitle.text != it) {
                binding.etlNoteTitle.text = it
            }
        }

        viewModel.text.observe(this) {
            if (binding.etlNoteText.text != it) {
                binding.etlNoteText.text = it
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
            binding.fdNoteFiles.updateFiles(it)
        }

        viewModel.isNewNote.observe(this) {
            initializeActionMenu(it)
            binding.fdNoteFiles.uploadEnabled = !it
            binding.btnNoteAddTag.isEnabled = !it
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
            refreshNote(true)
        }

        super.onResume()
    }

    override fun onPause() {
        if (viewModel.initialized && !viewModel.isNewNote.value!!) {
            saveNote(true)
        }

        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.note, menu)
        actionMenu = menu
        initializeActionMenu(viewModel.isNewNote.value!!)
        return true
    }

    override fun afterUrlDialogSave() {
        scope.launch {
            refreshNote(true)
        }
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
            R.id.actionNoteRefresh -> refreshNote(false)
            R.id.actionNoteSave -> saveNote(false)
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

    private fun uploadFile(fileUri: Uri) {
        scope.launch {
            val result = handleRequest { viewModel.postFile(
                contentResolver, fileUri,
            ) }

            if (result.isFailure && result.exceptionOrNull() is RequestCancel) {
                showMessage("Upload cancelled")
            } else if (result.isFailure) {
                showMessage("Could not upload the file")
            }
        }
    }

    private fun onTagDialogAdd(tag: Tag, updateMoveButton: () -> Unit) {
        scope.launch {
            val result = handleRequest { viewModel.postNotesTag(tag) }
            if (result.isFailure) {
                showMessage("Could not add the tag")
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
                showMessage("Could not remove the tag")
            } else {
                updateMoveButton?.invoke()
            }
        }
    }

    private fun refreshNote(silent: Boolean) {
        scope.launch {
            val result = handleRequest { viewModel.getNote() }
            if (!silent) {
                if (result.isFailure) {
                    showMessage("Could not refresh the note")
                } else {
                    showMessage("Note has been refreshed")
                }
            }
        }
    }

    private fun saveNote(silent: Boolean) {
        val text = binding.etlNoteText.text
        val title = binding.etlNoteTitle.text

        if (title.isBlank()) {
            if (!silent) showMessage("Can't save the note with an empty title")
            return
        }

        if (viewModel.text.value == text && viewModel.title.value == title) {
            if (!silent) showMessage("Note's text or title haven't changed since last save")
            return
        }

        scope.launch {
            val result = if (viewModel.isNewNote.value!!) {
                handleRequest { viewModel.postNote(text, title) }
            } else {
                handleRequest { viewModel.patchNote(text, title) }
            }

            if (result.isFailure) {
                showMessage("Could not save the note")
            } else if (!silent) {
                showMessage("Note has been saved")
            }
        }
    }

    private fun deleteNote() {
        AlertDialog
            .Builder(this)
            .setTitle("Delete this note?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                scope.launch {
                    val result = handleRequest { viewModel.deleteNote() }

                    if (result.isFailure) {
                        showMessage("Could not delete the note")
                    } else {
                        this@NoteActivity.finish()
                    }
                }
            }
            .show()
    }
}
