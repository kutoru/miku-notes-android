package com.kutoru.mikunotes.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.cardview.widget.CardView
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.Tag

class TagDialogAdapter (
    private val context: Context,
    var tags: List<Tag>,
    private val onTagMove: (position: Int) -> Unit,
    private val onTagSave: (position: Int) -> Unit,
    private val onTagDelete: (position: Int) -> Unit,
    private val getMoveButtonIcon: (position: Int) -> Drawable,
) : RecyclerView.Adapter<TagDialogAdapter.ViewHolder>() {

    var setHeightCallback: (() -> Unit)? = null

    companion object {
        private const val MARGIN_SIZE = 16
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagDialogAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.tag_dialog_card, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.setMargins(MARGIN_SIZE)

        setHeightCallback?.invoke()

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

            btnMoveTag.icon = getMoveButtonIcon(position)

            btnMoveTag.setOnClickListener { onTagMove(position) }
            etName.setText(tag.name)
            btnSaveTag.setOnClickListener { onTagSave(position) }
            btnDeleteTag.setOnClickListener { onTagDelete(position) }
        }
    }
}
