package com.kutoru.mikunotes.ui.adapters

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.Tag

class NoteParamTagAdapter (
    var tags: List<Tag>,
    private val tagOnClick: (position: Int, isChecked: Boolean) -> Unit,
    private val getCheckedState: (tagId: Int) -> Boolean,
) : RecyclerView.Adapter<NoteParamTagAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View.inflate(parent.context, R.layout.chip_tag_choice, null)
        return ViewHolder(view)
    }

    override fun getItemCount() = tags.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chip = itemView as Chip

        fun bind(position: Int) {
            val tag = tags[position]

            chip.id = tag.id
            chip.isChecked = getCheckedState(tag.id)

            if (tag.id == 0) {
                val span = SpannableString(tag.name)
                span.setSpan(StyleSpan(Typeface.ITALIC), 0, span.length, 0)
                chip.text = span
            } else {
                chip.text = tag.name
            }

            chip.setOnClickListener {
                tagOnClick(position, (it as Chip).isChecked)
            }
        }
    }
}
