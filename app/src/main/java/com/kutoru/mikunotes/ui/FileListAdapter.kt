package com.kutoru.mikunotes.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class FileListAdapter(
    private val context: Context,
    var files: List<File>,
    private val deleteFile: (position: Int) -> Unit,
    private val downloadFile: (position: Int) -> Unit,
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    companion object {
        private const val MARGIN_SIZE = 16
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListAdapter.ViewHolder {
        val cardSideLength = parent.width / 3 - (2 * MARGIN_SIZE)

        val view = LayoutInflater.from(context).inflate(R.layout.file_card, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE)

        return ViewHolder(view)
    }

    override fun getItemCount() = files.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView = itemView.findViewById<CardView>(R.id.cardView)

        fun bind(position: Int) {
            val file = files[position]

            val tvSize = cardView.findViewById<TextView>(R.id.tvSize)
            val btnDelete = cardView.findViewById<Button>(R.id.btnDelete)
            val btnDownload = cardView.findViewById<Button>(R.id.btnDownload)
            val tvTitle = cardView.findViewById<TextView>(R.id.tvTitle)
            val tvDate = cardView.findViewById<TextView>(R.id.tvDate)

            val created = LocalDateTime
                .ofEpochSecond(file.created, 0, ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss"))

            tvSize.text = "${file.size}"
            btnDelete.setOnClickListener { deleteFile(position) }
            btnDownload.setOnClickListener { downloadFile(position) }
            tvTitle.text = file.name
            tvDate.text = created
        }
    }
}
