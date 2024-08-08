package com.kutoru.mikunotes.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.Tag

class TagDialogAdapter (
    private val context: Context,
    var noteTagIds: List<Int>,
    var tags: List<Tag>,
    private val onTagAdd: (position: Int) -> Unit,
    private val onTagRemove: (position: Int) -> Unit,
    private val onTagSave: (position: Int) -> Unit,
    private val onTagDelete: (position: Int) -> Unit,
) : RecyclerView.Adapter<TagDialogAdapter.ViewHolder>() {

    companion object {
        private const val MARGIN_SIZE = 16
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_dialog_tag, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.setMargins(MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount() = tags.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView = itemView.findViewById<CardView>(R.id.cardView)

        fun bind(position: Int) {
            val tag = tags[position]

            val btnMoveTag = cardView.findViewById<MaterialButton>(R.id.btnMove)
            val etName = cardView.findViewById<EditText>(R.id.etName)
            val btnSaveTag = cardView.findViewById<Button>(R.id.btnSave)
            val btnDeleteTag = cardView.findViewById<Button>(R.id.btnDelete)

            if (tagPresentInNote(position)) {
                btnMoveTag.icon = AppCompatResources.getDrawable(context, R.drawable.ic_cross)!!
                btnMoveTag.setOnClickListener { onTagRemove(position) }
            } else {
                btnMoveTag.icon = AppCompatResources.getDrawable(context, R.drawable.ic_add)!!
                btnMoveTag.setOnClickListener { onTagAdd(position) }
            }

            etName.setText(tag.name)
            btnSaveTag.setOnClickListener { onTagSave(position) }
            btnDeleteTag.setOnClickListener { onTagDelete(position) }
        }
    }

    private fun tagPresentInNote(position: Int): Boolean {
        return noteTagIds.contains(tags[position].id)
    }
}
