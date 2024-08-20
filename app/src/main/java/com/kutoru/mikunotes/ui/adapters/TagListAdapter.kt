package com.kutoru.mikunotes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.Tag

class TagListAdapter (
    var tags: List<Tag>,
    private val tagOnClick: (position: Int) -> Unit,
) : RecyclerView.Adapter<TagListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chip_tag_entry, parent, false)
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

            chip.text = tag.name

            chip.setOnClickListener {
                tagOnClick(position)
            }

            chip.setOnCloseIconClickListener {
                tagOnClick(position)
            }
        }
    }
}
