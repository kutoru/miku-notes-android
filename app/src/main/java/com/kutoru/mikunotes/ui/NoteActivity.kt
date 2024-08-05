package com.kutoru.mikunotes.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityNoteBinding
import com.kutoru.mikunotes.logic.ApiService
import com.kutoru.mikunotes.models.File
import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NoteActivity : AppCompatActivity() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var binding: ActivityNoteBinding
    private lateinit var tagAdapter: TagListAdapter
    private lateinit var fileAdapter: FileListAdapter
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

        binding.rvNoteTags.adapter = tagAdapter

        binding.rvNoteFiles.adapter = fileAdapter
        binding.rvNoteFiles.layoutManager = GridLayoutManager(this, 3)

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

    private fun removeTag(position: Int) {
        println("removeTag: ${note.tags.getOrNull(position)}")
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
}
