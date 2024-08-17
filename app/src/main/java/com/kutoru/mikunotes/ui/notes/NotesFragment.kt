package com.kutoru.mikunotes.ui.notes

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.FragmentNotesBinding
import com.kutoru.mikunotes.logic.SELECTED_NOTE
import com.kutoru.mikunotes.models.NoteQueryParameters
import com.kutoru.mikunotes.ui.ApiReadyFragment
import com.kutoru.mikunotes.ui.TagViewModel
import com.kutoru.mikunotes.ui.adapters.ItemMarginDecorator
import com.kutoru.mikunotes.ui.adapters.TagListAdapter
import com.kutoru.mikunotes.ui.main.MainActivity
import com.kutoru.mikunotes.ui.main.NotesCallbacks
import com.kutoru.mikunotes.ui.note.NoteActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class NotesFragment : ApiReadyFragment<NotesViewModel>() {

    private lateinit var binding: FragmentNotesBinding
    private lateinit var loadDialog: ProgressDialog
    private lateinit var tagAdapter: TagListAdapter
    private lateinit var noteAdapter: NoteListAdapter

    override val viewModel: NotesViewModel by viewModels { NotesViewModel.Factory }
    private val tagViewModel: TagViewModel by viewModels { TagViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentNotesBinding.inflate(inflater, container, false)

        setupViewModelObservers()

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(requireActivity(), NoteActivity::class.java)
            intent.putExtra(SELECTED_NOTE, viewModel.notes.value!![0])
            requireActivity().startActivity(intent)
        }

        tagAdapter = TagListAdapter(
            false,
            listOf(),
            { pos -> println("notes on tag press $pos") },
        )

        noteAdapter = NoteListAdapter(
            listOf(),
            { pos -> println("notes on note press $pos") },
        )

        binding.rvNotesTags.adapter = tagAdapter
        binding.rvNotesTags.addItemDecoration(
            ItemMarginDecorator.Tags(
            resources.getDimension(R.dimen.margin).toInt(),
        ))

        binding.rvNotesNotes.adapter = noteAdapter
        binding.rvNotesNotes.addItemDecoration(
            ItemMarginDecorator.Notes(
            resources.getDimension(R.dimen.margin).toInt(),
        ))

        (requireActivity() as MainActivity)
            .setNotesOptionsMenu(NotesCallbacks(
                refresh = { scope.launch { refreshNotes(false) } },
                filter = { println("open filter menu") },
                sort = { println("open sort menu") },
            ))

        loadDialog = ProgressDialog(requireContext())
        loadDialog.setMessage("Loading the notes...")
        loadDialog.setCancelable(false)

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        loadDialog.show()

        scope.launch {
            listOf(
                async { refreshNotes(true) },
                async { refreshTags(true) },
            ).awaitAll()
        }
    }

    override fun afterUrlDialogSave() {
        scope.launch {
            viewModel.updateUrl()
            listOf(
                async { refreshNotes(true) },
                async { refreshTags(true) },
            ).awaitAll()
        }
    }

    private fun setupViewModelObservers() {
        tagViewModel.tags.observe(viewLifecycleOwner) {
            tagAdapter.tags = it
            tagAdapter.notifyDataSetChanged()
        }

        viewModel.notes.observe(viewLifecycleOwner) {
            noteAdapter.notes = it
            noteAdapter.notifyDataSetChanged()
        }
    }

    private suspend fun refreshTags(silent: Boolean) {
        val result = handleRequest { tagViewModel.getTags() }
        if (!silent && result.isFailure) {
            showToast("Could not refresh the tags")
        }
    }

    private suspend fun refreshNotes(silent: Boolean) {
        // gather the params from inputs or something
        val parameters = NoteQueryParameters(null, 100u, null, null, null, null, null, null)

        val result = handleRequest { viewModel.getNotes(parameters) }
        if (result.isFailure) {
            if (!silent) showToast("Could not refresh the shelf")
            return
        }

        loadDialog.dismiss()

        if (!silent) showToast("The notes have been refreshed")
    }
}
