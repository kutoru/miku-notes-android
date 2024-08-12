package com.kutoru.mikunotes.ui.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.kutoru.mikunotes.databinding.FragmentNotesBinding
import com.kutoru.mikunotes.logic.SELECTED_NOTE
import com.kutoru.mikunotes.models.NoteQueryParameters
import com.kutoru.mikunotes.ui.NotesCallbacks
import com.kutoru.mikunotes.ui.activities.MainActivity
import com.kutoru.mikunotes.ui.activities.NoteActivity
import com.kutoru.mikunotes.viewmodels.NotesViewModel
import kotlinx.coroutines.launch

class NotesFragment : ApiReadyFragment<NotesViewModel>() {

    private lateinit var binding: FragmentNotesBinding
    private lateinit var loadDialog: ProgressDialog

    override val viewModel: NotesViewModel by viewModels { NotesViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentNotesBinding.inflate(inflater, container, false)

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(requireActivity(), NoteActivity::class.java)
            intent.putExtra(SELECTED_NOTE, viewModel.notes[0])
            requireActivity().startActivity(intent)
        }

        (requireActivity() as MainActivity)
            .setNotesOptionsMenu(NotesCallbacks(
                refresh = { scope.launch { refreshNotes(false) } },
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
            refreshNotes(true)
        }
    }

    override fun afterUrlDialogSave() {
        scope.launch {
            viewModel.updateUrl()
            refreshNotes(true)
        }
    }

    private suspend fun refreshNotes(silent: Boolean) {
        // gather the params from inputs or something
        val parameters = NoteQueryParameters(null, null, null, null, null, null, null, null)

        val result = handleRequest { viewModel.getNotes(parameters) }
        if (result.isFailure) {
            if (!silent) showToast("Could not refresh the shelf")
            return
        }

        updateCurrentNotes()
        loadDialog.dismiss()

        if (!silent) showToast("The notes have been refreshed")
    }

    private fun updateCurrentNotes() {
        // update the ui stuff
        binding.tvNotesTest.text =
            "Notes: ${viewModel.notes.size}; Pages: ${viewModel.pageCount};\n${viewModel.notes}"
    }
}
