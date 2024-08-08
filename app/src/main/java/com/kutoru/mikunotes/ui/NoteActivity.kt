package com.kutoru.mikunotes.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityNoteBinding
import com.kutoru.mikunotes.logic.ApiService
import com.kutoru.mikunotes.models.File
import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NoteActivity : AppCompatActivity() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var binding: ActivityNoteBinding
    private lateinit var tagDialog: View
    private lateinit var tagAdapter: TagListAdapter
    private lateinit var fileAdapter: FileListAdapter
    private lateinit var tagDialogAdapter: TagDialogAdapter
    private var tags = mutableListOf<Tag>()
    private lateinit var note: Note
    private var initialized = false

    private lateinit var apiService: ApiService
    private var serviceIsBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ApiService.ServiceBinder
            apiService = binder.getService()
            serviceIsBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceIsBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarNote)
        supportActionBar?.title = "Note"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnNoteAddTag.setOnClickListener {
            openTagDialog()
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

        fileAdapter.setHeightCallback = { cardSideLength ->
            val rvParams = binding.rvNoteFiles.layoutParams

            val desiredHeight = if (note.files.isNotEmpty()) {
                (FileListAdapter.MARGIN_SIZE * 2 + cardSideLength * 1.5).toInt()
            } else {
                0
            }

            if (rvParams.height != desiredHeight) {
                rvParams.height = desiredHeight
                binding.rvNoteFiles.layoutParams = rvParams
            }
        }

        tagDialogAdapter = TagDialogAdapter(
            this,
            listOf(),
            ::dialogMoveTag,
            ::dialogSaveTag,
            ::dialogDeleteTag,
            ::dialogGetMoveButtonIcon,
        )

        binding.rvNoteTags.adapter = tagAdapter

        binding.rvNoteFiles.adapter = fileAdapter
        binding.rvNoteFiles.layoutManager = GridLayoutManager(this, 3)

        setupTagDialog()
        startApiService()
    }

    override fun onResume() {

        note = Note(
            1722833500,
            mutableListOf(
                File( null, 1722833500, "laksdjf", 3, "filename1.jpg", 2342342342, 2, ),
                File( null, 1722833500, "laksdjf", 3, "filename2.jpg", 2342342342, 2, ),
                File( null, 1722833500, "laksdjf", 3, "filename3.jpg", 2342342342, 2, ),
                File( null, 1722833500, "laksdjf", 3, "filename4.jpg", 2342342342, 2, ),
            ),
            2,
            1722833500,
            mutableListOf(
                Tag( 1722833500, 5, "tag name1", null, 2, ),
                Tag( 1000000000, 5, "tag name2", null, 2, ),
                Tag( 1000000000, 5, "tag name3", null, 2, ),
                Tag( 1000000000, 5, "tag name4", null, 2, ),
            ),
            "note text",
            58,
            "note title",
            2,
        )
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
        if (serviceIsBound) {
            unbindService(serviceConnection)
            serviceIsBound = false
        }

        job.cancel()
        super.onDestroy()
    }

    private fun startApiService() {
        val bindIntent = Intent(this, ApiService::class.java)
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
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

    private fun updateCurrentNote(updateTags: Boolean, updateFiles: Boolean) {
        val created = LocalDateTime
            .ofEpochSecond(note.created, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yy/MM/dd HH:mm"))

        val lastEdited = LocalDateTime
            .ofEpochSecond(note.last_edited, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yy/MM/dd HH:mm"))

        binding.etNoteTitle.setText(note.title)
        binding.etNoteText.setText(note.text)
        binding.tvNoteCreated.text = "C: $created"
        binding.tvNoteEdited.text = "E: $lastEdited (${note.times_edited})"

        if (updateTags) {
            tagAdapter.tags = note.tags
            tagAdapter.notifyDataSetChanged()
        }

        if (updateFiles) {
            fileAdapter.files = note.files
            fileAdapter.notifyDataSetChanged()
        }
    }

    private fun openTagDialog() {
        println("openTagDialog")
        scope.launch {
            tags = apiService.getTags()
            tags = mutableListOf(
                Tag(1723018115, 1, "tag name 1", null, 1), Tag(1723018115, 2, "tag name 2", null, 1),
                Tag(1723018115, 3, "tag name 3", null, 1), Tag(1723018115, 4, "tag name 4", null, 1),
                Tag(1723018115, 1, "tag name 5", null, 1), Tag(1723018115, 2, "tag name 6", null, 1),
                Tag(1723018115, 3, "tag name 7", null, 1), Tag(1723018115, 4, "tag name 8", null, 1),
                Tag(1723018115, 1, "tag name 9", null, 1), Tag(1723018115, 2, "tag name 10", null, 1),
                Tag(1723018115, 3, "tag name 11", null, 1), Tag(1723018115, 4, "tag name 12", null, 1),
                Tag(1723018115, 1, "tag name 13", null, 1), Tag(1723018115, 2, "tag name 14", null, 1),
                Tag(1723018115, 3, "tag name 15", null, 1), Tag(1723018115, 4, "tag name 16", null, 1),
            )
            println("tags: $tags")

            tagDialogAdapter.tags = tags
            tagDialogAdapter.notifyDataSetChanged()

            tagDialog.visibility = View.VISIBLE
        }
    }

    private fun setupTagDialog() {
        val view = View.inflate(this@NoteActivity, R.layout.dialog_add_tag, null)
        val btnDialogTagBack = view.findViewById<Button>(R.id.btnDialogTagBack)
        val rvDialogTags = view.findViewById<RecyclerView>(R.id.rvDialogTagList)
        val btnDialogTagAdd = view.findViewById<Button>(R.id.btnDialogTagAdd)

        btnDialogTagBack.setOnClickListener {
            tagDialog.visibility = View.GONE
            val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputManager?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }

        rvDialogTags.adapter = tagDialogAdapter
        tagDialogAdapter.tags = tags
        tagDialogAdapter.notifyDataSetChanged()

        btnDialogTagAdd.setOnClickListener {
            createNewTag()
        }

        view.visibility = View.GONE
        binding.root.addView(view)

        val viewParams = view.layoutParams
        viewParams.height = -1
        viewParams.width = -1
        view.layoutParams = viewParams

        tagDialog = view
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

    private fun dialogGetMoveButtonIcon(position: Int): Drawable {
        return if (note.tags.contains(tags[position])) {
            getDrawable(R.drawable.ic_cross)!!
        } else {
            getDrawable(R.drawable.ic_add)!!
        }
    }

    private fun dialogMoveTag(position: Int) {
        println("dialogMoveTag")
    }

    private fun dialogSaveTag(position: Int) {
        println("dialogSaveTag")
    }

    private fun dialogDeleteTag(position: Int) {
        println("dialogDeleteTag")
    }

    private fun createNewTag() {
        println("createNewTag")
    }
}
