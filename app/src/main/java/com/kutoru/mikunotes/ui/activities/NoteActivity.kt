package com.kutoru.mikunotes.ui.activities

import android.animation.ValueAnimator
import android.graphics.Rect
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityNoteBinding
import com.kutoru.mikunotes.logic.ANIMATION_TRANSITION_TIME
import com.kutoru.mikunotes.logic.RECYCLER_VIEW_FILE_COLUMNS
import com.kutoru.mikunotes.logic.RECYCLER_VIEW_ITEM_MARGIN
import com.kutoru.mikunotes.logic.SELECTED_NOTE
import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.NoteTagDialog
import com.kutoru.mikunotes.ui.adapters.FileListAdapter
import com.kutoru.mikunotes.ui.adapters.TagListAdapter
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NoteActivity : ServiceBoundActivity() {

    private lateinit var binding: ActivityNoteBinding
    private lateinit var tagAdapter: TagListAdapter
    private lateinit var fileAdapter: FileListAdapter
    private lateinit var tagDialog: NoteTagDialog

    private var fileContainerExpanded = false
    private var lastRootHeight = 0

    private lateinit var note: Note
    private var initialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarNote)
        supportActionBar?.title = "Note"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val extractedNote = intent.getSerializableExtra(SELECTED_NOTE) as? Note
        if (extractedNote == null) {
            Toast.makeText(this, "Tried to open an invalid note", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        note = extractedNote

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
            tagDialog.show(note.tags)
        }

        binding.fabNoteMoveFiles.setOnClickListener {
            moveFileContainer(true)
        }

        binding.fabNoteUpload.setOnClickListener {
            uploadFile()
        }

        tagAdapter = TagListAdapter(
            this,
            listOf(),
            ::removeTag,
        )

        fileAdapter = FileListAdapter(
            this,
            listOf(),
            ::deleteFile,
            ::downloadFile,
        )

        binding.rvNoteTags.adapter = tagAdapter

        val bottomFilesPadding = (resources.getDimension(R.dimen.fab_size) + RECYCLER_VIEW_ITEM_MARGIN).toInt()
        binding.rvNoteFiles.setPadding(0, 0, 0, bottomFilesPadding)

        binding.rvNoteFiles.adapter = fileAdapter
        binding.rvNoteFiles.layoutManager = GridLayoutManager(this, RECYCLER_VIEW_FILE_COLUMNS)

        onServiceBound = {
            tagDialog = NoteTagDialog(
                this,
                binding.root,
                apiService,
                ::onTagDialogAdd,
                ::onTagDialogRemove,
                ::onTagDialogChange,
            )
        }

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
    }

    override fun onResume() {

        refreshNote()
        initialized = true

        updateCurrentNote(true, true)
        super.onResume()
    }

    override fun onPause() {
        if (initialized) {
            saveNote()
        }

        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.note, menu)
        return true
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

    override fun onDestroy() {
        tagDialog.cancelJob()
        super.onDestroy()
    }

    private fun moveFileContainer(swapState: Boolean) {
        println("moveFileContainer")

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
        val minHeight = resources.getDimension(R.dimen.fab_size).toInt()

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

    private fun deleteFile(position: Int) {
        println("deleteFile: ${note.files.getOrNull(position)}")
    }

    private fun downloadFile(position: Int) {
        println("downloadFile: ${note.files.getOrNull(position)}")
    }

    private fun uploadFile() {
        println("uploadFile")
    }

    private fun removeTag(position: Int) {
        println("removeTag: ${note.tags.getOrNull(position)}")
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

    private fun onTagDialogAdd(tag: Tag) {
        println("onTagDialogAdd: $tag")
    }

    private fun onTagDialogRemove(tag: Tag) {
        println("onTagDialogRemove: $tag")
    }

    private fun onTagDialogChange(tag: Tag) {
        println("onTagDialogChange: $tag")
    }

    private fun updateCurrentNote(updateTags: Boolean, updateFiles: Boolean) {
        val created = LocalDateTime
            .ofEpochSecond(note.created, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yy/MM/dd HH:mm"))

        val lastEdited = LocalDateTime
            .ofEpochSecond(note.last_edited, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yy/MM/dd HH:mm"))

        binding.etNoteTitle.setText(note.title)
        binding.etNoteText.setText(note.text)
        binding.tvNoteCreated.text = created
        binding.tvNoteEdited.text = lastEdited
        binding.tvNoteCount.text = note.times_edited.toString()

        if (updateTags) {
            tagAdapter.tags = note.tags
            tagAdapter.notifyDataSetChanged()
        }

        if (updateFiles) {
            fileAdapter.files = note.files
            fileAdapter.notifyDataSetChanged()
        }
    }
}
