package com.kutoru.mikunotes.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityMainBinding
import com.kutoru.mikunotes.logic.ApiService
import com.kutoru.mikunotes.logic.DOWNLOAD_NOTIFICATION_CHANNEL_ID


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var optionsMenu: Menu

    var initializeOptionsMenu: () -> Unit = ::setDefaultOptionsMenu
    private var actionShelfSave: (() -> Unit)? = null
    private var actionShelfClear: (() -> Unit)? = null
    private var actionShelfRefresh: (() -> Unit)? = null
    private var actionShelfConvert: (() -> Unit)? = null
    private var actionShelfCopy: (() -> Unit)? = null

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

//        val storage = PersistentStorage(this)
//        storage.accessCookie = null
//        storage.refreshCookie = null

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbarMain)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navShelf, R.id.navNotes, R.id.navSettings),
            drawerLayout,
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        startApiService()
        createNotificationChannels()
        handleSharedData(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        optionsMenu = menu
        menuInflater.inflate(R.menu.main, menu)
        initializeOptionsMenu()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionShelfSave -> actionShelfSave?.invoke()
            R.id.actionShelfClear -> actionShelfClear?.invoke()
            R.id.actionShelfRefresh -> actionShelfRefresh?.invoke()
            R.id.actionShelfConvert -> actionShelfConvert?.invoke()
            R.id.actionShelfCopy -> actionShelfCopy?.invoke()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onDestroy() {
        if (serviceIsBound) {
            unbindService(serviceConnection)
            serviceIsBound = false
        }

        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun handleSharedData(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text == null) {
                showToast("Could not get the shared text")
                return
            }

            val fragment = supportFragmentManager.fragments[0].childFragmentManager.fragments[0] as ShelfFragment
            fragment.handleSharedText(text)
        } else if (intent.action == Intent.ACTION_SEND) {
            val fileUri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
            if (fileUri == null) {
                showToast("Could not get the shared file")
                return
            }

            val fragment = supportFragmentManager.fragments[0].childFragmentManager.fragments[0] as ShelfFragment
            fragment.handleSharedFiles(listOf(fileUri))
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val fileUris = intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.map { it as? Uri }
            if (fileUris == null || fileUris.any { it == null }) {
                showToast("Could not get the shared files")
                return
            }

            val checkedFileUris = fileUris.map { it!! }
            val fragment = supportFragmentManager.fragments[0].childFragmentManager.fragments[0] as ShelfFragment
            fragment.handleSharedFiles(checkedFileUris)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun startApiService() {
        val intent = Intent(this, ApiService::class.java)
        startService(intent)

        val bindIntent = Intent(this, ApiService::class.java)
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            "Download Notification",
            NotificationManager.IMPORTANCE_DEFAULT,
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun setOptionVisibility(optionId: Int, visible: Boolean) {
        optionsMenu.findItem(optionId).isVisible = visible
    }

    private fun setDefaultOptionsMenu() {
        setOptionVisibility(R.id.actionShelfSave, false)
        setOptionVisibility(R.id.actionShelfClear, false)
        setOptionVisibility(R.id.actionShelfRefresh, false)
        setOptionVisibility(R.id.actionShelfConvert, false)
        setOptionVisibility(R.id.actionShelfCopy, false)
    }

    fun setShelfOptionsMenu(
        actionShelfSave: () -> Unit,
        actionShelfClear: () -> Unit,
        actionShelfRefresh: () -> Unit,
        actionShelfConvert: () -> Unit,
        actionShelfCopy: () -> Unit,
    ) {
        try {
            setDefaultOptionsMenu()

            this.actionShelfSave = actionShelfSave
            this.actionShelfClear = actionShelfClear
            this.actionShelfRefresh = actionShelfRefresh
            this.actionShelfConvert = actionShelfConvert
            this.actionShelfCopy = actionShelfCopy

            setOptionVisibility(R.id.actionShelfSave, true)
            setOptionVisibility(R.id.actionShelfClear, true)
            setOptionVisibility(R.id.actionShelfRefresh, true)
            setOptionVisibility(R.id.actionShelfConvert, true)
            setOptionVisibility(R.id.actionShelfCopy, true)
        } catch (e: UninitializedPropertyAccessException) {
            initializeOptionsMenu = { setShelfOptionsMenu(
                actionShelfSave,
                actionShelfClear,
                actionShelfRefresh,
                actionShelfConvert,
                actionShelfCopy,
            ) }
        }
    }

    fun setNotesOptionsMenu() {
        try {
            setDefaultOptionsMenu()
        } catch (e: UninitializedPropertyAccessException) {
            initializeOptionsMenu = ::setNotesOptionsMenu
        }
    }

    fun setSettingsOptionsMenu() {
        try {
            setDefaultOptionsMenu()
        } catch (e: UninitializedPropertyAccessException) {
            initializeOptionsMenu = ::setSettingsOptionsMenu
        }
    }
}
