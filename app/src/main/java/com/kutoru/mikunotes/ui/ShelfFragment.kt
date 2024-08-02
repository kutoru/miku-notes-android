package com.kutoru.mikunotes.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.kutoru.mikunotes.databinding.FragmentShelfBinding
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.logic.Unauthorized
import com.kutoru.mikunotes.logic.UnknownError
import com.kutoru.mikunotes.logic.UrlPropertyDialog
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ShelfFragment : CustomFragment() {

    private lateinit var binding: FragmentShelfBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentShelfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        println("shelf onResume")
        if (serviceIsBound) {
            scope.launch {
                initializeShelf()
            }
        } else {
            onServiceBoundListener = ::initializeShelf
        }

        super.onResume()
    }

    override fun onPause() {
        onServiceBoundListener = null
        super.onPause()
    }

    private suspend fun initializeShelf() {
        println("initializeShelf")

        val shelf = try {
            apiService.getShelf()
        } catch(e: Exception) {
            when (e) {
                is InvalidUrl -> {
                    UrlPropertyDialog.launch(
                        requireContext(),
                        false,
                        "Could not connect to the backend, make sure that the url properties are correct",
                    ) {
                        scope.launch {
                            apiService.updateUrl()
                            initializeShelf()
                        }
                    }
                }

                is Unauthorized -> {
                    val intent = Intent(requireActivity(), LoginActivity::class.java)
                    requireActivity().startActivity(intent)
                }

                is UnknownError, is ServerError -> {
                    println("Unknown error when getting shelf: $e")
                    Toast.makeText(requireContext(), "Unknown error when getting shelf: $e", Toast.LENGTH_LONG).show()
                }

                else -> throw e
            }

            return
        }

        println("shelf: $shelf")

        val lastEdited = LocalDateTime
            .ofEpochSecond(shelf.last_edited, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        binding.tvShelfDate.text = "Edited: $lastEdited"
        binding.tvShelfCount.text = "Count: ${shelf.times_edited}"
        binding.etShelfText.setText(shelf.text)

        binding.rvShelfFiles.adapter = FileListAdapter(
            requireContext(),
            shelf.files,
//            { fileId -> scope.launch { apiService.deleteFile(fileId) } },
//            { fileHash -> scope.launch { apiService.getFile(fileHash) } },
            { fileId -> scope.launch {println("delete file: $fileId")} },
            { fileHash -> scope.launch {println("download file: $fileHash")} },
        )

        binding.rvShelfFiles.layoutManager = GridLayoutManager(requireContext(), 3)
    }
}
