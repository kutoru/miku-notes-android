package com.kutoru.mikunotes.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.Tag

class TagListAdapter (
    private val context: Context,
    var tags: List<Tag>,
    private val tagOnClick: (position: Int) -> Unit,
) : RecyclerView.Adapter<TagListAdapter.ViewHolder>() {

    companion object {
        private const val MARGIN_SIZE = 16
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_tag, parent, false)
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
            val tvName = cardView.findViewById<TextView>(R.id.tvName)

            tvName.text = tag.name
            cardView.setOnClickListener { tagOnClick(position) }
        }
    }
}
