package com.kutoru.mikunotes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.kutoru.mikunotes.databinding.FragmentNotesBinding

class NotesFragment : Fragment() {

    private lateinit var binding: FragmentNotesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNotesBinding.inflate(inflater, container, false)

        binding.tvShelf.text = "This is the Notes menu"
        binding.fabAddNote.setOnClickListener {
            Snackbar.make(it, "*add a note*", Snackbar.LENGTH_LONG).show()
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
