package com.kutoru.mikunotes.ui

import android.Manifest
import android.animation.ValueAnimator
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
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
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.logic.ANIMATION_TRANSITION_TIME
import com.kutoru.mikunotes.logic.FILE_CHANGE_ADDED
import com.kutoru.mikunotes.logic.FILE_CHANGE_BROADCAST
import com.kutoru.mikunotes.logic.FILE_CHANGE_DELETED
import com.kutoru.mikunotes.logic.FILE_SERVICE_DELETE
import com.kutoru.mikunotes.logic.FILE_SERVICE_DOWNLOAD
import com.kutoru.mikunotes.logic.FILE_SERVICE_FILE_HASH
import com.kutoru.mikunotes.logic.FILE_SERVICE_FILE_ID
import com.kutoru.mikunotes.logic.FILE_SERVICE_IS_SHELF
import com.kutoru.mikunotes.logic.FILE_SERVICE_ITEM_ID
import com.kutoru.mikunotes.logic.FILE_SERVICE_UPLOAD
import com.kutoru.mikunotes.logic.FILE_SERVICE_URIS
import com.kutoru.mikunotes.logic.FileRequestService
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
    private val fileChangeBroadcastReceiver: FileChangeBroadcastReceiver

    private lateinit var filePickActivityLauncher: ActivityResultLauncher<String>
    private lateinit var readStoragePermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var writeStoragePermissionActivityLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionActivityLauncher: ActivityResultLauncher<String>

    private lateinit var fileViewModel: FileHoldingViewModel
    private lateinit var showMessage: (message: String?) -> Unit
    private var getExtraSpace: () -> Int

    var uploadEnabled
        get() = fabUpload.isEnabled
        set(value) { fabUpload.isEnabled = value }

    private var lastFileJobId = 0
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

        fileChangeBroadcastReceiver = FileChangeBroadcastReceiver()
        ContextCompat.registerReceiver(
            context,
            fileChangeBroadcastReceiver,
            IntentFilter(FILE_CHANGE_BROADCAST),
            ContextCompat.RECEIVER_NOT_EXPORTED,
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
                readStoragePermissionActivityLauncher.launch(
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

    // this ain't pretty, but its prettier than writing this code outside of the view
    fun <O>setup(
        root: ViewGroup,
        lifecycleOwner: LifecycleOwner,
        fileHoldingViewModel: FileHoldingViewModel,

        showMessage: (message: String?) -> Unit,
        getExtraSpace: () -> Int,

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

        fileViewModel = fileHoldingViewModel
        fileViewModel.files.observe(lifecycleOwner) {
            updateFiles(it)
        }

        this.showMessage = showMessage
        this.getExtraSpace = getExtraSpace

        val contentsContract = ActivityResultContracts.GetMultipleContents() as ActivityResultContract<String, O>
        val permissionContract = ActivityResultContracts.RequestPermission() as ActivityResultContract<String, O>

        filePickActivityLauncher = registerForActivityResult(contentsContract) {
            uploadFiles(it as List<Uri>)
        }

        readStoragePermissionActivityLauncher = registerForActivityResult(permissionContract) {
            if (it as Boolean) {
                filePickActivityLauncher.launch("*/*")
            } else {
                showMessage("You need to provide access to your files to upload them")
            }
        }

        writeStoragePermissionActivityLauncher = registerForActivityResult(permissionContract) {
            if (!(it as Boolean)) {
                showMessage("You need to provide access to your storage to download the files")
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

    private fun writeStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE,
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

    fun onDestroy() {
        context.unregisterReceiver(
            fileChangeBroadcastReceiver,
        )
    }

    // file stuff

    private fun updateFiles(files: List<File>) {
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

    fun uploadFiles(fileUris: List<Uri>) {
        if (!notificationPermissionGranted()) {
            notificationPermissionActivityLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }

        val bundle = PersistableBundle()
        bundle.putBoolean(FILE_SERVICE_UPLOAD, true)

        bundle.putStringArray(
            FILE_SERVICE_URIS,
            fileUris.map { it.toString() }.toTypedArray(),
        )
        bundle.putInt(
            FILE_SERVICE_ITEM_ID,
            fileViewModel.itemId,
        )
        bundle.putInt(
            FILE_SERVICE_IS_SHELF,
            if (fileViewModel.isAttachedToShelf) 1 else 0,
        )

        startFileService(bundle)
    }

    private fun downloadFile(fileIndex: Int) {
        if (!writeStoragePermissionGranted()) {
            writeStoragePermissionActivityLauncher.launch(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )

            return
        }

        if (!notificationPermissionGranted()) {
            notificationPermissionActivityLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }

        val fileHash = fileAdapter.files[fileIndex].hash

        val bundle = PersistableBundle()
        bundle.putBoolean(FILE_SERVICE_DOWNLOAD, true)
        bundle.putString(FILE_SERVICE_FILE_HASH, fileHash)

        startFileService(bundle)
    }

    private fun deleteFile(fileIndex: Int) {
        val fileId = fileAdapter.files[fileIndex].id

        val bundle = PersistableBundle()
        bundle.putBoolean(FILE_SERVICE_DELETE, true)
        bundle.putInt(FILE_SERVICE_FILE_ID, fileId)

        startFileService(bundle)
    }

    private fun startFileService(bundle: PersistableBundle) {
        val jobBuilder = JobInfo.Builder(++lastFileJobId, ComponentName(
            context, FileRequestService::class.java,
        ))
            .setExtras(bundle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NET_CAPABILITY_INTERNET)
                .addCapability(NET_CAPABILITY_NOT_METERED)
                .build()

            jobBuilder
                .setUserInitiated(true)
                .setRequiredNetwork(networkRequest)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            jobBuilder.setOverrideDeadline(0)
        }

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(jobBuilder.build())
    }

    inner class FileChangeBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            println("FileChangeBroadcastReceiver")

            if (intent != null) {
                if (intent.hasExtra(FILE_CHANGE_ADDED)) {
                    val file = intent.getSerializableExtra(FILE_CHANGE_ADDED) as File
                    println(file)
                    fileViewModel.fileUploaded(file)
                }

                if (intent.hasExtra(FILE_CHANGE_DELETED)) {
                    val fileId = intent.getIntExtra(FILE_CHANGE_DELETED, 0)
                    println(fileId)
                    if (fileId > 0) {
                        fileViewModel.fileDeleted(fileId)
                    }
                }
            }
        }
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
