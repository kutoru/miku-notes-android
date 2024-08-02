package com.kutoru.mikunotes.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.kutoru.mikunotes.databinding.FragmentShelfBinding
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.Unauthorized
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
        binding.tvShelfTitle.text = "This is the Shelf menu"

        return binding.root
    }

    override fun onResume() {
        println("shelf onResume")
        if (serviceIsBound) {
            initializeShelf()
        } else {
            onServiceBoundListener = ::initializeShelf
        }

        super.onResume()
    }

    override fun onStop() {
        println("shelf onStop")
        super.onStop()
    }

    private fun initializeShelf() {
        println("initializeShelf")

        scope.launch {
            try {
                apiService.updateUrl()
                apiService.access()
            } catch(e: InvalidUrl) {
                println("InvalidUrl")

                UrlPropertyDialog.launch(
                    requireContext(),
                    false,
                    "Could not connect to the backend, make sure that the url properties are correct",
                    ::initializeShelf,
                )
                return@launch
            } catch(e: Unauthorized) {
                println("Unauthorized")
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                requireActivity().startActivity(intent)
                return@launch
            } catch (e: Throwable) {
                println("unknown error when getting access: $e")
                throw e
            }

            try {
                val shelf = apiService.getShelf()!!
                println("shelf: $shelf")

                val lastEdited = LocalDateTime
                    .ofEpochSecond(shelf.last_edited, 0, ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                binding.tvShelf.text =
                    "id: ${shelf.id}" +
                            "\ntext: ${shelf.text}" +
                            "\nfiles: ${shelf.files.map { it.name }}" +
                            "\nlast edited: $lastEdited"
            } catch (e: Throwable) {
                println("getShelf err: $e")
                Toast
                    .makeText(requireContext(), "Could not get the shelf", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

//    inner class CreateActivityContract : ActivityResultContract<BoardSize, String?>() {
//        override fun createIntent(context: Context, input: BoardSize): Intent {
//            val intent = Intent(context, CreateActivity::class.java)
//            intent.putExtra(EXTRA_BOARD_SIZE, input)
//            return intent
//        }
//
//        override fun parseResult(resultCode: Int, intent: Intent?): String? {
//            return intent?.getStringExtra(EXTRA_GAME_NAME)
//        }
//    }
}
