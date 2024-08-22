package com.kutoru.mikunotes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.logic.AppUtil
import com.kutoru.mikunotes.models.File
import kotlin.math.ceil
import kotlin.math.round

class FileListAdapter(
    private val itemMargin: Int,
    var files: List<File>,
    private val deleteFile: (position: Int) -> Unit,
    private val downloadFile: (position: Int) -> Unit,
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val columnCount = 2

        val cardWidth = ceil(
            parent.width.toFloat() / columnCount -
            itemMargin * (1f + 100f / columnCount / 100f)
        ).toInt()
        val cardHeight = (cardWidth / 1.5).toInt()

        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_file, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = cardWidth
        layoutParams.height = cardHeight

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
            val tvTitle = cardView.findViewById<TextView>(R.id.tvConvertTitle)
            val tvDate = cardView.findViewById<TextView>(R.id.tvDate)

            val mul = 1000f

            val (fileSize, postFix) = when {
                file.size < mul -> Pair(file.size.toFloat(), "b")
                file.size < mul * mul -> Pair(file.size / mul, "KB")
                file.size < mul * mul * mul -> Pair(file.size / (mul * mul), "MB")
                else -> Pair(file.size / (mul * mul * mul), "GB")
            }

            tvSize.text = "${round(fileSize * 100) / 100}$postFix"
            btnDelete.setOnClickListener { deleteFile(position) }
            btnDownload.setOnClickListener { downloadFile(position) }
            tvTitle.text = file.name
            tvDate.text = AppUtil.formatDateTime(file.created)
        }
    }
}
