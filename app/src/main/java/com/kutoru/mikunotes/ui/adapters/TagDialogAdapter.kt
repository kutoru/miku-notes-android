package com.kutoru.mikunotes.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.view.allViews
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.Tag

class TagDialogAdapter (
    var noteTagIds: List<Int>,
    var tags: List<Tag>,
    private val withAdd: Boolean,
    private val inputManager: InputMethodManager?,

    private val onTagAdd: ((position: Int, updateMoveButton: () -> Unit) -> Unit)?,
    private val onTagRemove: ((position: Int, updateMoveButton: () -> Unit) -> Unit)?,
    private val onTagSave: (position: Int, tagName: String) -> Unit,
    private val onTagDelete: (position: Int) -> Unit,
) : RecyclerView.Adapter<TagDialogAdapter.ViewHolder>() {

    private var newTagInput: EditText? = null
    private var shouldFocusNewTag = false
    private var viewGroup: ViewGroup? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewGroup == null) {
            viewGroup = parent
        }

        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_dialog_tag, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = tags.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView = itemView.findViewById<CardView>(R.id.cardView)
        private val btnMoveTag = cardView.findViewById<MaterialButton>(R.id.btnMove)
        private val etName = cardView.findViewById<EditText>(R.id.etName)
        private val btnSaveTag = cardView.findViewById<Button>(R.id.btnSave)
        private val btnDeleteTag = cardView.findViewById<Button>(R.id.btnDelete)

        fun bind(position: Int) {
            if (tags[position].id > -1) {
                bindExistingTag(position)
            } else {
                bindNewTag(position)
            }
        }

        private fun bindExistingTag(position: Int) {
            val tag = tags[position]

            btnMoveTag.isEnabled = true
            if (withAdd && tagPresentInNote(position)) {
                setMoveToRemove(position)
            } else if (withAdd) {
                setMoveToAdd(position)
            } else {
                btnMoveTag.visibility = View.GONE
            }

            etName.setText(tag.name)
            btnSaveTag.setOnClickListener {
                clearFocus()
                onTagSave(position, etName.text?.toString() ?: "")
            }
            btnDeleteTag.setOnClickListener {
                clearFocus()
                onTagDelete(position)
            }
        }

        private fun bindNewTag(position: Int) {
            val tag = tags[position]

            if (withAdd) {
                btnMoveTag.isEnabled = false
            } else {
                btnMoveTag.visibility = View.GONE
            }

            etName.setText(tag.name)
            btnSaveTag.setOnClickListener {
                clearFocus()
                onTagSave(position, etName.text?.toString() ?: "")
            }
            btnDeleteTag.setOnClickListener {
                clearFocus()
                onTagDelete(position)
            }

            if (shouldFocusNewTag) {
                newTagInput = etName
                focusNewTag()
            }
        }

        private fun setMoveToAdd(position: Int) {
            btnMoveTag.icon = AppCompatResources.getDrawable(cardView.context, R.drawable.ic_add)
            btnMoveTag.backgroundTintList = ColorStateList.valueOf(cardView.context.getColor(R.color.primary))
            btnMoveTag.setOnClickListener {
                onTagAdd?.invoke(position) { setMoveToRemove(position) }
            }
        }

        private fun setMoveToRemove(position: Int) {
            btnMoveTag.icon = AppCompatResources.getDrawable(cardView.context, R.drawable.ic_cross)
            btnMoveTag.backgroundTintList = ColorStateList.valueOf(cardView.context.getColor(R.color.secondary))
            btnMoveTag.setOnClickListener {
                onTagRemove?.invoke(position) { setMoveToAdd(position) }
            }
        }

        private fun clearFocus() {
            (itemView.parent as View)
                .allViews
                .mapNotNull {
                    if (!it.hasFocus()) null else it as? EditText
                }
                .forEach {
                    it.clearFocus()
                }

            inputManager?.hideSoftInputFromWindow(itemView.windowToken, 0)
        }
    }

    private fun tagPresentInNote(position: Int): Boolean {
        return noteTagIds.contains(tags[position].id)
    }

    fun focusExistingNewTag() {
        newTagInput = viewGroup!!.allViews.mapNotNull { it as? EditText }.last()
        focusNewTag()
    }

    fun focusNewTag() {
        shouldFocusNewTag = false

        if (tags.isEmpty() || tags.last().id != -1 || newTagInput == null) {
            shouldFocusNewTag = true
            return
        }

        newTagInput!!.requestFocus()
        newTagInput!!.postDelayed({
            inputManager?.showSoftInput(newTagInput!!, 0)
            newTagInput = null
        }, 100)
    }
}
