package com.kutoru.mikunotes.ui.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityMainBinding
import com.kutoru.mikunotes.logic.AppUtil
import com.kutoru.mikunotes.logic.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import com.kutoru.mikunotes.ui.notes.NotesFragment
import com.kutoru.mikunotes.ui.shelf.ShelfFragment

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var actionBarManager: MainActionBarManager
    private var initializeOptionsMenu: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbarMain)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navShelf, R.id.navNotes, R.id.navSettings),
            binding.drawerLayout,
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        AppUtil.setNavigationBarColor(window, binding.root)

        createNotificationChannels()
        handleSharedData(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        actionBarManager = MainActionBarManager(
            menu,
            binding.appBarMain.actionNotesSearch,
            binding.appBarMain.actionNotesSearchReset,
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager,
        )

        initializeOptionsMenu?.invoke()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return actionBarManager.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        val goBack = ((supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment)
            ?.childFragmentManager
            ?.fragments
            ?.get(0) as? NotesFragment)
            ?.onBackPressed()

        if (goBack == null || goBack == true) {
            super.onBackPressed()
        }
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

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            "Download Notification",
            NotificationManager.IMPORTANCE_LOW,
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun setShelfOptionsMenu(callbacks: ShelfCallbacks?) {
        try {
            actionBarManager.setShelfFragment(callbacks)
        } catch (e: UninitializedPropertyAccessException) {
            initializeOptionsMenu = {
                actionBarManager.setShelfFragment(callbacks)
            }
        }
    }

    fun setNotesOptionsMenu(callbacks: NotesCallbacks?) {
        try {
            actionBarManager.setNotesFragment(callbacks)
        } catch (e: UninitializedPropertyAccessException) {
            initializeOptionsMenu = {
                actionBarManager.setNotesFragment(callbacks)
            }
        }
    }

    fun setSettingsOptionsMenu() {
        try {
            actionBarManager.setSettingsFragment()
        } catch (e: UninitializedPropertyAccessException) {
            initializeOptionsMenu = {
                actionBarManager.setSettingsFragment()
            }
        }
    }
}
