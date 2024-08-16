package com.kutoru.mikunotes.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.Tag

class TagListAdapter (
    private var itemMargin: Int,
    private val withCross: Boolean,
    var tags: List<Tag>,
    private val tagOnClick: (position: Int) -> Unit,
) : RecyclerView.Adapter<TagListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_tag, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.setMargins(itemMargin)
        return ViewHolder(view)
    }

    override fun getItemCount() = tags.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView = itemView.findViewById<CardView>(R.id.cardView)
        private val tvName = cardView.findViewById<TextView>(R.id.tvName)
        private val ivCross = cardView.findViewById<ImageView>(R.id.ivCross)

        fun bind(position: Int) {
            val tag = tags[position]

            cardView.setOnClickListener { tagOnClick(position) }
            tvName.text = tag.name
            ivCross.visibility = if (withCross) View.VISIBLE else View.GONE
        }
    }
}
