package com.kutoru.mikunotes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.logic.AppUtil
import com.kutoru.mikunotes.models.Note

class NoteListAdapter (
    var notes: List<Note>,
    private val noteOnClick: (position: Int) -> Unit,
) : RecyclerView.Adapter<NoteListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_note, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = notes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView = itemView.findViewById<CardView>(R.id.cardView)
        private val tvTitle = cardView.findViewById<TextView>(R.id.tvCardNoteTitle)
        private val tvCreated = cardView.findViewById<TextView>(R.id.tvCardNoteCreated)
        private val tvModified = cardView.findViewById<TextView>(R.id.tvCardNoteModified)
        private val tvFileCount = cardView.findViewById<TextView>(R.id.tvCardNoteFileCount)
        private val tvTagCount = cardView.findViewById<TextView>(R.id.tvCardNoteTagCount)
        private val divider = cardView.findViewById<View>(R.id.divider3)
        private val tvText = cardView.findViewById<TextView>(R.id.tvCardNoteText)

        fun bind(position: Int) {
            val note = notes[position]

            cardView.setOnClickListener { noteOnClick(position) }

            tvTitle.text = note.title.trim()
            tvCreated.text = AppUtil.formatDateTime(note.created)
            tvModified.text = AppUtil.formatDateTime(note.last_edited)
            tvFileCount.text = note.files.size.toString()
            tvTagCount.text = note.tags.size.toString()

            val text = note.text.trim()
            if (text.isBlank()) {
                tvText.visibility = View.GONE
                divider.visibility = View.GONE
            } else {
                tvText.text = text
                tvText.visibility = View.VISIBLE
                divider.visibility = View.VISIBLE
            }
        }
    }
}
