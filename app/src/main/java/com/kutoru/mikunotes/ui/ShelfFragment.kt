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
import com.kutoru.mikunotes.models.Shelf
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ShelfFragment : CustomFragment() {

    private lateinit var binding: FragmentShelfBinding
    private lateinit var adapter: FileListAdapter
    private var currShelf: Shelf? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentShelfBinding.inflate(inflater, container, false)

        adapter = FileListAdapter(
            requireContext(),
            listOf(),
            { fileIndex -> scope.launch {
                val shelf = currShelf!!
                val fileId = shelf.files[fileIndex].id

                if (handleRequest { apiService.deleteFile(fileId) } != null) {
                    showToast("The file got successfully deleted")

                    shelf.files.removeAt(fileIndex)
                    adapter.files = shelf.files
                    adapter.notifyItemRemoved(fileIndex)
                }
            } },
            { fileIndex -> scope.launch {
                val fileHash = currShelf!!.files[fileIndex].hash
                handleRequest { apiService.getFile(fileHash) }
            } },
        )

        binding.rvShelfFiles.adapter = adapter
        binding.rvShelfFiles.layoutManager = GridLayoutManager(requireContext(), 3)

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

    private suspend fun <T>handleRequest(requestFn: (suspend () -> T)): T? {
        try {
            return requestFn()
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

                    return null
                }

                is Unauthorized -> {
                    val intent = Intent(requireActivity(), LoginActivity::class.java)
                    requireActivity().startActivity(intent)
                    return null
                }

                is UnknownError, is ServerError -> {
                    println("Unknown error when calling backend: $e")
                    Toast.makeText(requireContext(), "Unknown error when calling backend: $e", Toast.LENGTH_LONG).show()
                    return null
                }

                else -> throw e
            }
        }
    }

    private suspend fun initializeShelf() {
        println("initializeShelf")

        val shelf = handleRequest { apiService.getShelf() } ?: return
        currShelf = shelf

        val lastEdited = LocalDateTime
            .ofEpochSecond(shelf.last_edited, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        binding.tvShelfDate.text = "Edited: $lastEdited"
        binding.tvShelfCount.text = "Count: ${shelf.times_edited}"
        binding.etShelfText.setText(shelf.text)

        adapter.files = shelf.files
        adapter.notifyDataSetChanged()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}
