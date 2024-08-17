package com.kutoru.mikunotes.ui.notes

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.allViews
import androidx.core.view.get
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.FragmentNotesBinding
import com.kutoru.mikunotes.logic.SELECTED_NOTE
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.ApiReadyFragment
import com.kutoru.mikunotes.ui.TagViewModel
import com.kutoru.mikunotes.ui.adapters.ItemMarginDecorator
import com.kutoru.mikunotes.ui.adapters.NoteListAdapter
import com.kutoru.mikunotes.ui.main.MainActivity
import com.kutoru.mikunotes.ui.main.NotesCallbacks
import com.kutoru.mikunotes.ui.note.NoteActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class NotesFragment : ApiReadyFragment<NotesViewModel>() {

    private lateinit var binding: FragmentNotesBinding
    private lateinit var loadDialog: ProgressDialog
    private lateinit var noteAdapter: NoteListAdapter
    private lateinit var paramMenuSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var noteParamMenu: NoteParamMenu

    private var paramMenuLastOffset = 0f
    private var paramMenuIsExpanded = false
    private var inputManager: InputMethodManager? = null

    override val viewModel: NotesViewModel by viewModels { NotesViewModel.Factory }
    private val tagViewModel: TagViewModel by viewModels { TagViewModel.Factory }
    private val queryViewModel: QueryViewModel by viewModels { QueryViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentNotesBinding.inflate(inflater, container, false)

        val chip = binding.cgNotesTags[0] as Chip
        val span = SpannableString("no tags")
        span.setSpan(StyleSpan(Typeface.ITALIC), 0, span.length, 0)
        chip.text = span

        setupViewModelObservers()

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(requireActivity(), NoteActivity::class.java)
            intent.putExtra(SELECTED_NOTE, viewModel.notes.value!![0])
            requireActivity().startActivity(intent)
        }

        val filterMenu = binding.clNotesParamMenu

        paramMenuSheet = BottomSheetBehavior.from(filterMenu)
        paramMenuSheet.isFitToContents = true
        paramMenuSheet.isHideable = false
        paramMenuSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        paramMenuSheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val prevState = paramMenuIsExpanded
                paramMenuIsExpanded = newState == BottomSheetBehavior.STATE_EXPANDED

                if (prevState && !paramMenuIsExpanded) {
                    onParamMenuCollapse()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset - paramMenuLastOffset < 0) {
                    inputManager?.hideSoftInputFromWindow(binding.root.windowToken, 0)
                    hideParamMenu()
                }

                paramMenuLastOffset = slideOffset
            }
        })

        binding.ivNotesParamMenuIcon.setOnClickListener {
            toggleParamMenu()
        }

        noteAdapter = NoteListAdapter(
            listOf(),
            { pos -> println("notes on note press $pos") },
        )

        binding.rvNotesNotes.adapter = noteAdapter
        binding.rvNotesNotes.addItemDecoration(
            ItemMarginDecorator.Notes(
            resources.getDimension(R.dimen.margin).toInt(),
        ))

        (requireActivity() as MainActivity)
            .setNotesOptionsMenu(NotesCallbacks(
                refresh = { refreshFragment(false) },
                filter = { println("open filter menu") },
                sort = { println("open sort menu") },
            ))

        noteParamMenu = NoteParamMenu(
            requireContext(),
            viewLifecycleOwner,
            binding.clNotesParamMenu,
            parentFragmentManager,
            tagViewModel,
            queryViewModel,
        )

        loadDialog = ProgressDialog(requireContext())
        loadDialog.setMessage("Loading the notes...")
        loadDialog.setCancelable(false)

        inputManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        loadDialog.show()

        refreshFragment(true)
    }

    override fun afterUrlDialogSave() {
        viewModel.updateUrl()
        refreshFragment(true)
    }

    private fun refreshFragment(silentNotes: Boolean) {
        scope.launch {
            listOf(
                async { refreshNotes(silentNotes) },
                async { refreshTags(true) },
            ).awaitAll()
        }
    }

    private fun setupViewModelObservers() {
        tagViewModel.tags.observe(viewLifecycleOwner) { tags ->
            binding.cgNotesTags.removeAllViews()

            (tags + listOf(Tag(0, 0, "no tags", null, 0))).forEach { tag ->
                val chip = View.inflate(requireContext(), R.layout.chip_tag_choice, null) as Chip
                chip.id = tag.id

                if (tag.id == 0) {
                    val span = SpannableString(tag.name)
                    span.setSpan(StyleSpan(Typeface.ITALIC), 0, span.length, 0)
                    chip.text = span
                } else {
                    chip.text = tag.name
                }

                chip.setOnClickListener {
                    if ((it as Chip).isChecked) {
                        queryViewModel.addTag(tag.id)
                    } else {
                        queryViewModel.removeTag(tag.id)
                    }
                }

                binding.cgNotesTags.addView(chip)
            }
        }

        viewModel.notes.observe(viewLifecycleOwner) {
            if (it == null) {
                binding.rvNotesNotes.visibility = View.INVISIBLE
                binding.tvNotesNoNotes.visibility = View.INVISIBLE
                binding.pbNotesNotes.visibility = View.VISIBLE
            } else if (it.isEmpty()) {
                binding.rvNotesNotes.visibility = View.INVISIBLE
                binding.tvNotesNoNotes.visibility = View.VISIBLE
                binding.pbNotesNotes.visibility = View.INVISIBLE
            } else {
                binding.rvNotesNotes.visibility = View.VISIBLE
                binding.tvNotesNoNotes.visibility = View.INVISIBLE
                binding.pbNotesNotes.visibility = View.INVISIBLE

                noteAdapter.notes = it
                noteAdapter.notifyDataSetChanged()
            }
        }

        queryViewModel.tags.observe(viewLifecycleOwner) { tagIds ->
            binding.cgNotesTags.allViews.forEach {
                val chip = it as? Chip ?: return@forEach
                val chipState = tagIds != null && tagIds.contains(chip.id)
                if (chip.isChecked != chipState) {
                    chip.isChecked = chipState
                }
            }

            if (paramMenuSheet.state == BottomSheetBehavior.STATE_COLLAPSED) {
                scope.launch {
                    refreshNotes(true)
                }
            }
        }
    }

    private suspend fun refreshTags(silent: Boolean) {
        val result = handleRequest { tagViewModel.getTags() }
        if (!silent && result.isFailure) {
            showToast("Could not refresh the tags")
        }
    }

    private suspend fun refreshNotes(silent: Boolean) {
        println("refreshNotes ${queryViewModel.title}")
        paramMenuSheet.state = BottomSheetBehavior.STATE_COLLAPSED

        val parameters = queryViewModel.queryParameters

        val result = handleRequest { viewModel.getNotes(parameters) }
        if (result.isFailure) {
            if (!silent) showToast("Could not refresh the shelf")
            return
        }

        loadDialog.dismiss()

        if (!silent) showToast("Notes have been refreshed")
    }

    private fun hideParamMenu() {
        paramMenuSheet.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun showParamMenu() {
        paramMenuSheet.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun toggleParamMenu() {
        if (paramMenuIsExpanded) {
            hideParamMenu()
        } else {
            showParamMenu()
        }
    }

    private fun onParamMenuCollapse() {
        scope.launch {
            refreshNotes(true)
        }
    }
}
