package com.kutoru.mikunotes.ui.main

import android.view.Menu
import android.view.MenuItem
import com.kutoru.mikunotes.R

class MainActionBarManager(
    private val menu: Menu,
) {

    private var shelfCallbacks: ShelfCallbacks? = null
    private var notesCallbacks: NotesCallbacks? = null
    private var currentFragment: FragmentType? = null

    fun setShelfFragment(shelfCallbacks: ShelfCallbacks?) {
        resetOptionsMenu()

        setOptionVisibility(R.id.actionShelfRefresh, true)
        setOptionVisibility(R.id.actionShelfCopy, true)
        setOptionVisibility(R.id.actionShelfSave, true)
        setOptionVisibility(R.id.actionShelfClear, true)
        setOptionVisibility(R.id.actionShelfConvert, true)

        this.shelfCallbacks = shelfCallbacks
        currentFragment = FragmentType.Shelf
    }

    fun setNotesFragment(notesCallbacks: NotesCallbacks?) {
        resetOptionsMenu()

        setOptionVisibility(R.id.actionNotesRefresh, true)
        setOptionVisibility(R.id.actionNotesFilter, true)
        setOptionVisibility(R.id.actionNotesSort, true)

        this.notesCallbacks = notesCallbacks
        currentFragment = FragmentType.Notes
    }

    fun setSettingsFragment() {
        resetOptionsMenu()
        currentFragment = FragmentType.Settings
    }

    private fun resetOptionsMenu() {
        setOptionVisibility(R.id.actionShelfRefresh, false)
        setOptionVisibility(R.id.actionShelfCopy, false)
        setOptionVisibility(R.id.actionShelfSave, false)
        setOptionVisibility(R.id.actionShelfClear, false)
        setOptionVisibility(R.id.actionShelfConvert, false)
        setOptionVisibility(R.id.actionNotesRefresh, false)
        setOptionVisibility(R.id.actionNotesFilter, false)
        setOptionVisibility(R.id.actionNotesSort, false)
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (currentFragment) {
            FragmentType.Shelf -> handleShelfCallbacks(item.itemId)
            FragmentType.Notes -> handleNotesCallbacks(item.itemId)
            else -> {}
        }

        return false
    }

    private fun setOptionVisibility(optionId: Int, visible: Boolean) {
        menu.findItem(optionId).isVisible = visible
    }

    private fun handleShelfCallbacks(itemId: Int): Boolean {
        when (itemId) {
            R.id.actionShelfRefresh -> shelfCallbacks?.refresh?.invoke()
            R.id.actionShelfCopy -> shelfCallbacks?.copy?.invoke()
            R.id.actionShelfSave -> shelfCallbacks?.save?.invoke()
            R.id.actionShelfClear -> shelfCallbacks?.clear?.invoke()
            R.id.actionShelfConvert -> shelfCallbacks?.convert?.invoke()
            else -> return false
        }

        return true
    }

    private fun handleNotesCallbacks(itemId: Int): Boolean {
        when (itemId) {
            R.id.actionNotesRefresh -> notesCallbacks?.refresh?.invoke()
            R.id.actionNotesFilter -> notesCallbacks?.filter?.invoke()
            R.id.actionNotesSort -> notesCallbacks?.sort?.invoke()
            else -> return false
        }

        return true
    }
}

data class ShelfCallbacks(
    val refresh: () -> Unit,
    val copy: () -> Unit,
    val save: () -> Unit,
    val clear: () -> Unit,
    val convert: () -> Unit,
)

data class NotesCallbacks(
    val refresh: () -> Unit,
    val filter: () -> Unit,
    val sort: () -> Unit,
)

enum class FragmentType {
    Shelf,
    Notes,
    Settings,
}
