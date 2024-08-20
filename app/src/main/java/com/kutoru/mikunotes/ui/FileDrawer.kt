package com.kutoru.mikunotes.ui

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.logic.ANIMATION_TRANSITION_TIME
import com.kutoru.mikunotes.models.File
import com.kutoru.mikunotes.ui.adapters.FileListAdapter
import com.kutoru.mikunotes.ui.adapters.ItemMarginDecorator

class FileDrawer(
    context: Context,
    attrs: AttributeSet,
) : FrameLayout(context, attrs) {

    private val rvFiles: RecyclerView
    private val tvNoFiles: TextView
    private val fabMoveFiles: FloatingActionButton
    private val fabUpload: FloatingActionButton

    private val defaultDuration = ANIMATION_TRANSITION_TIME.toLong()
    private val fileAdapter: FileListAdapter

    private lateinit var filePickActivityLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionActivityLauncher: ActivityResultLauncher<String>

    private lateinit var extUploadFile: (fileUri: Uri) -> Unit
    private lateinit var extDownloadFile: (position: Int) -> Unit
    private lateinit var extDeleteFile: (position: Int) -> Unit
    private lateinit var showMessage: (message: String?) -> Unit
    private var getExtraSpace: () -> Int

    var uploadEnabled = true
        get() = fabUpload.isEnabled
        set(value) {
            field = value
            fabUpload.isEnabled = value
        }

    private var lastRootHeight = 0
    private val minHeight: Int
    var expanded = false
        private set

    init {
        inflate(context, R.layout.content_file_drawer, this)

        val margin = resources.getDimension(R.dimen.margin).toInt()

        minHeight = resources.getDimension(R.dimen.fab_size).toInt() + margin * 2
        getExtraSpace = { minHeight }

        fileAdapter = FileListAdapter(
            margin,
            listOf(),
            ::deleteFile,
            ::downloadFile,
        )

        // ui elements

        rvFiles = findViewById(R.id.rvFileListFiles)
        tvNoFiles = findViewById(R.id.tvFileListNoFiles)
        fabMoveFiles = findViewById(R.id.fabFileListMoveFiles)
        fabUpload = findViewById(R.id.fabFileListUpload)

        rvFiles.updatePadding(bottom = minHeight)

        rvFiles.adapter = fileAdapter
        rvFiles.layoutManager = GridLayoutManager(context, 2)
        rvFiles.addItemDecoration(ItemMarginDecorator.Files(margin))

        fabMoveFiles.setOnClickListener {
            toggleExpand()
        }

        fabUpload.setOnClickListener {
            if (!notificationPermissionGranted()) {
                notificationPermissionActivityLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }

            if (!readStoragePermissionGranted()) {
                storagePermissionActivityLauncher.launch(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            } else {
                filePickActivityLauncher.launch("*/*")
            }
        }

        // initial expand

        val typedAttrs = context.theme.obtainStyledAttributes(
            attrs, R.styleable.FileDrawer, 0, 0,
        )

        val initExpanded = typedAttrs.getBoolean(R.styleable.FileDrawer_initExpanded, false)
        typedAttrs.recycle()

        if (initExpanded) {
            expand(0)
        }
    }

    // other setup

    // this ain't pretty, but i don't want to write this code outside of the view
    fun <O>setup(
        root: ViewGroup,

        showMessage: (message: String?) -> Unit,
        getExtraSpace: () -> Int,
        extUploadFile: (fileUri: Uri) -> Unit,
        extDownloadFile: (position: Int) -> Unit,
        extDeleteFile: (position: Int) -> Unit,

        registerForActivityResult: (
            contract: ActivityResultContract<String, O>,
            callback: ActivityResultCallback<O>,
        ) -> ActivityResultLauncher<String>,
    ) {
        root.viewTreeObserver.addOnGlobalLayoutListener {
            val newHeight = Rect().let {
                root.getWindowVisibleDisplayFrame(it)
                it.height()
            }

            if (newHeight != lastRootHeight) {
                lastRootHeight = newHeight
                if (expanded) {
                    expand(0)
                }
            }
        }

        this.showMessage = showMessage
        this.getExtraSpace = getExtraSpace
        this.extUploadFile = extUploadFile
        this.extDownloadFile = extDownloadFile
        this.extDeleteFile = extDeleteFile

        val contentsContract = ActivityResultContracts.GetMultipleContents() as ActivityResultContract<String, O>
        val permissionContract = ActivityResultContracts.RequestPermission() as ActivityResultContract<String, O>

        filePickActivityLauncher = registerForActivityResult(contentsContract) {
            uploadFiles(it as List<Uri>)
        }

        storagePermissionActivityLauncher = registerForActivityResult(permissionContract) {
            if (it as Boolean) {
                filePickActivityLauncher.launch("*/*")
            } else {
                showMessage("You need to provide access to your files to upload them")
            }
        }

        notificationPermissionActivityLauncher = registerForActivityResult(permissionContract) {
            if (!(it as Boolean)) {
                showMessage("You won't see download notifications without the notification permission")
            }
        }
    }

    private fun readStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    // file stuff

    fun updateFiles(files: List<File>) {
        if (files.isEmpty()) {
            rvFiles.visibility = View.INVISIBLE
            tvNoFiles.visibility = View.VISIBLE
        } else {
            fileAdapter.files = files
            fileAdapter.notifyDataSetChanged()

            tvNoFiles.visibility = View.INVISIBLE
            rvFiles.visibility = View.VISIBLE
        }
    }

    private fun uploadFiles(fileUris: List<Uri>) {
        fileUris.forEach { uri ->
            extUploadFile.invoke(uri)
        }
    }

    private fun downloadFile(fileIndex: Int) {
        if (!notificationPermissionGranted()) {
            notificationPermissionActivityLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }

        extDownloadFile.invoke(fileIndex)
    }

    private fun deleteFile(fileIndex: Int) {
        extDeleteFile.invoke(fileIndex)
    }

    // expansion stuff

    private fun toggleExpand() {
        if (expanded) {
            collapse()
        } else {
            expand()
        }
    }

    private fun expand(animationDuration: Long = defaultDuration) {
        expanded = true
        fabMoveFiles.setImageResource(R.drawable.ic_down)
        val currHeight = rvFiles.height
        val maxHeight = (getExtraSpace() + currHeight) / 2
        moveContainer(currHeight, maxHeight, animationDuration)
    }

    private fun collapse(animationDuration: Long = defaultDuration) {
        expanded = false
        fabMoveFiles.setImageResource(R.drawable.ic_up)
        moveContainer(rvFiles.height, minHeight, animationDuration)
    }

    private fun moveContainer(
        currHeight: Int,
        desiredHeight: Int,
        animationDuration: Long,
    ) {
        val animator = ValueAnimator.ofInt(currHeight, desiredHeight)
        animator.addUpdateListener {
            val height = it.animatedValue as Int
            val layoutParams = rvFiles.layoutParams
            layoutParams.height = height
            rvFiles.layoutParams = layoutParams
        }

        animator.duration = animationDuration
        animator.start()
    }
}
