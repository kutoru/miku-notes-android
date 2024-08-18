package com.kutoru.mikunotes.ui.main

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.widget.addTextChangedListener
import com.kutoru.mikunotes.R

class MainActionBarManager(
    private val menu: Menu,
    private val actionNotesSearch: EditText,
    private val actionNotesSearchReset: ImageButton,
    private val inputManager: InputMethodManager,
) {

    private var shelfCallbacks: ShelfCallbacks? = null
    private var notesCallbacks: NotesCallbacks? = null
    private var currentFragment: FragmentType? = null

    private var actionNotesSearchIsClear = false

    init {
        actionNotesSearch.addTextChangedListener {
            if (it.isNullOrEmpty()) {
                actionNotesSearchReset.visibility = View.GONE
            } else {
                actionNotesSearchReset.visibility = View.VISIBLE
            }

            notesCallbacks?.onInputChange?.invoke(
                it?.toString() ?: "",
                actionNotesSearchIsClear,
            )

            actionNotesSearchIsClear = false
        }

        actionNotesSearchReset.setOnClickListener {
            actionNotesSearchIsClear = true

            actionNotesSearch.setText("")
            inputManager.hideSoftInputFromWindow(actionNotesSearch.windowToken, 0)
            actionNotesSearch.clearFocus()
        }

        actionNotesSearch.setOnEditorActionListener { _, actionId, _ ->
            val submitAction = 6
            if (actionId != submitAction) {
                return@setOnEditorActionListener false
            }

            inputManager.hideSoftInputFromWindow(actionNotesSearch.windowToken, 0)
            actionNotesSearch.clearFocus()
            notesCallbacks?.onInputSubmit?.invoke()
            return@setOnEditorActionListener true
        }
    }

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
        actionNotesSearch.visibility = View.VISIBLE
        if (!actionNotesSearch.text.isNullOrEmpty()) {
            actionNotesSearchReset.visibility = View.VISIBLE
        } else {
            actionNotesSearchReset.visibility = View.GONE
        }

        notesCallbacks?.getSetText?.invoke {
            if (it != actionNotesSearch.text.toString()) {
                actionNotesSearch.setText(it)
            }
        }

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
        actionNotesSearch.visibility = View.GONE
        actionNotesSearchReset.visibility = View.GONE
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
    val onInputChange: (newInput: String, fromClear: Boolean) -> Unit,
    val onInputSubmit: () -> Unit,
    val getSetText: ((text: String) -> Unit) -> Unit,
)

enum class FragmentType {
    Shelf,
    Notes,
    Settings,
}
