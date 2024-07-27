package com.kutoru.mikunotes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kutoru.mikunotes.databinding.FragmentShelfBinding

class ShelfFragment : Fragment() {

    private lateinit var binding: FragmentShelfBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentShelfBinding.inflate(inflater, container, false)
        binding.tvShelf.text = "This is the Shelf menu"
        return binding.root
    }
}
