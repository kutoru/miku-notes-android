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
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.allViews
import androidx.core.view.get
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.FragmentNotesBinding
import com.kutoru.mikunotes.logic.CREATE_NEW_NOTE
import com.kutoru.mikunotes.logic.SELECTED_NOTE
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.ApiReadyFragment
import com.kutoru.mikunotes.ui.TagViewModel
import com.kutoru.mikunotes.ui.adapters.ItemMarginDecorator
import com.kutoru.mikunotes.ui.adapters.NoteListAdapter
import com.kutoru.mikunotes.ui.main.MainActivity
import com.kutoru.mikunotes.ui.main.NotesCallbacks
import com.kutoru.mikunotes.ui.note.NoteActivity
import com.kutoru.mikunotes.ui.note.NoteTagDialog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class NotesFragment : ApiReadyFragment<NotesViewModel>() {

    private lateinit var binding: FragmentNotesBinding
    private lateinit var loadDialog: ProgressDialog
    private lateinit var noteAdapter: NoteListAdapter
    private lateinit var paramMenuSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var noteParamMenu: NoteParamMenu
    private lateinit var noteTagDialog: NoteTagDialog

    private var paramMenuLastOffset = 0f
    private var paramMenuIsExpanded = false
    private var inputManager: InputMethodManager? = null
    private var setSearchBarText: ((text: String) -> Unit)? = null
    private var rvNotesPadding: Int = 0

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

        val fabParams = binding.fabAddNote.layoutParams as MarginLayoutParams
        fabParams.bottomMargin =
            (resources.getDimension(R.dimen.backdrop_header_height) +
            resources.getDimension(R.dimen.margin)).toInt()
        binding.fabAddNote.layoutParams = fabParams

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(requireActivity(), NoteActivity::class.java)
            intent.putExtra(CREATE_NEW_NOTE, true)
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
        ) {
            val intent = Intent(requireActivity(), NoteActivity::class.java)
            intent.putExtra(SELECTED_NOTE, viewModel.notes.value!![it])
            requireActivity().startActivity(intent)
        }

        rvNotesPadding =
            (resources.getDimension(R.dimen.backdrop_header_height) +
            resources.getDimension(R.dimen.fab_size) +
            resources.getDimension(R.dimen.margin) * 2).toInt()
        binding.rvNotesNotes.updatePadding(bottom = rvNotesPadding)

        binding.rvNotesNotes.layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }

        binding.rvNotesNotes.adapter = noteAdapter
        binding.rvNotesNotes.addItemDecoration(
            ItemMarginDecorator.Notes(
            resources.getDimension(R.dimen.margin).toInt(),
        ))

        (requireActivity() as MainActivity)
            .setNotesOptionsMenu(NotesCallbacks(
                refresh = { refreshFragment(false) },
                onInputChange = ::onSearchInputChange,
                onInputSubmit = ::onSearchInputSubmit,
                getSetText = { setSearchBarText = it },
            ))

        noteTagDialog = NoteTagDialog(
            requireContext(),
            viewLifecycleOwner,
            requireActivity().findViewById<CoordinatorLayout>(R.id.app_bar_main),
            false,
            tagViewModel,
            ::handleRequest,
            ::showToast,
        )

        noteParamMenu = NoteParamMenu(
            requireContext(),
            viewLifecycleOwner,
            binding.clNotesParamMenu,
            ::hideParamMenu,
            parentFragmentManager,
            tagViewModel,
            queryViewModel,
            noteTagDialog,
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
        noteTagDialog.hide()
        viewModel.updateUrl()
        refreshFragment(true)
    }

    fun onBackPressed(): Boolean {
        if (noteTagDialog.isShown) {
            noteTagDialog.hide()
            return false
        } else if (paramMenuIsExpanded) {
            hideParamMenu()
            return false
        } else {
            return true
        }
    }

    private fun onSearchInputChange(text: String, fromClear: Boolean) {
        queryViewModel.setTitle(text)
        if (fromClear) {
            onSearchInputSubmit()
        }
    }

    private fun onSearchInputSubmit() {
        scope.launch {
            refreshNotes(true)
        }
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
            if (!it.isNullOrEmpty()) {
                noteAdapter.notes = it
                noteAdapter.notifyDataSetChanged()
            }

            binding.tvNotesNoNotes.postDelayed({
                if (it == null) {
                    binding.rvNotesNotes.visibility = View.INVISIBLE
                    binding.tvNotesNoNotes.visibility = View.INVISIBLE
                    binding.pbNotesNotes.visibility = View.VISIBLE
                    changeContentHeight(false)
                } else if (it.isEmpty()) {
                    binding.rvNotesNotes.visibility = View.INVISIBLE
                    binding.tvNotesNoNotes.visibility = View.VISIBLE
                    binding.pbNotesNotes.visibility = View.INVISIBLE
                    changeContentHeight(false)
                } else {
                    binding.rvNotesNotes.visibility = View.VISIBLE
                    binding.tvNotesNoNotes.visibility = View.INVISIBLE
                    binding.pbNotesNotes.visibility = View.INVISIBLE
                    changeContentHeight(true)
                }
            }, 10)
        }

        queryViewModel.title.observe(viewLifecycleOwner) {
            setSearchBarText?.invoke(it)
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

    private fun changeContentHeight(notesVisible: Boolean) {
        val params = binding.clNotesNotes.layoutParams

        if (notesVisible) {
            val container = binding.rvNotesNotes
            val margin = resources.getDimension(R.dimen.margin).toInt()
            var totalHeight = rvNotesPadding

            for (i in 0..<container.childCount) {
                totalHeight += container.getChildAt(i).height + margin
            }

            params.height = totalHeight
        } else {
            params.height = binding.svNotesContent.height - binding.clNotesTags.height
        }

        binding.clNotesNotes.layoutParams = params
    }

    private suspend fun refreshTags(silent: Boolean) {
        val result = handleRequest { tagViewModel.getTags() }
        if (!silent && result.isFailure) {
            showToast("Could not refresh the tags")
        }
    }

    private suspend fun refreshNotes(silent: Boolean) {
        hideParamMenu()
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
