package com.kutoru.mikunotes.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import com.kutoru.mikunotes.ApiService
import com.kutoru.mikunotes.databinding.FragmentNotesBinding

class NotesFragment : Fragment() {

    private lateinit var binding: FragmentNotesBinding
    private lateinit var activity: FragmentActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNotesBinding.inflate(inflater, container, false)
        activity = requireActivity()

        binding.tvShelf.text = "This is the Notes menu"
        binding.fabAddNote.setOnClickListener {
            Snackbar.make(it, "*add a note*", Snackbar.LENGTH_LONG).show()
        }

        binding.btnDownload.setOnClickListener {
            val intent = Intent(activity, ApiService::class.java)
            val fileHash = "0249b1a3-7f06-4e39-88b7-1d6ab30400dd"
            intent.putExtra("FILE_HASH", fileHash)
            activity.startService(intent)
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
