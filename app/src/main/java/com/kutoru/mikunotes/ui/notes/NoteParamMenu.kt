package com.kutoru.mikunotes.ui.notes

import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.logic.AppUtil
import com.kutoru.mikunotes.models.QuerySortBy
import com.kutoru.mikunotes.models.QuerySortType
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.TagViewModel
import com.kutoru.mikunotes.ui.adapters.ItemMarginDecorator
import com.kutoru.mikunotes.ui.adapters.NoteParamTagAdapter
import com.kutoru.mikunotes.ui.note.NoteTagDialog


class NoteParamMenu(
    context: Context,
    viewLifecycleOwner: LifecycleOwner,
    parent: ConstraintLayout,
    onSubmit: () -> Unit,

    private val fragmentManager: FragmentManager,
    private val tagViewModel: TagViewModel,
    private val queryViewModel: QueryViewModel,
    private val tagDialog: NoteTagDialog,
) {

    private val view: View
    private val datePicker: MaterialDatePicker.Builder<Long>
    private val dayInSecs = 86400L
    private val tagAdapter: NoteParamTagAdapter
    private var lastTagListRowCount = 3

    private val rvTags: RecyclerView
    private val etDateStart: EditText
    private val etDateEnd: EditText
    private val etModifStart: EditText
    private val etModifEnd: EditText
    private val btgSortBy: MaterialButtonToggleGroup
    private val btgSortType: MaterialButtonToggleGroup

    init {
        view = View.inflate(context, R.layout.content_notes_param_menu, null)

        rvTags = view.findViewById(R.id.rvNPMTags)
        val btnEditTags: Button = view.findViewById(R.id.btnNPMEditTags)
        val btnDateStart: Button = view.findViewById(R.id.btnNPMDateStart)
        etDateStart = view.findViewById(R.id.etNPMDateStart)
        etDateEnd = view.findViewById(R.id.etNPMDateEnd)
        val btnDateEnd: Button = view.findViewById(R.id.btnNPMDateEnd)
        val btnModifStart: Button = view.findViewById(R.id.btnNPMModifStart)
        etModifStart = view.findViewById(R.id.etNPMModifStart)
        etModifEnd = view.findViewById(R.id.etNPMModifEnd)
        val btnModifEnd: Button = view.findViewById(R.id.btnNPMModifEnd)
        btgSortBy = view.findViewById(R.id.btgNPMSortBy)
        btgSortType = view.findViewById(R.id.btgNPMSortType)
        val btnReset: Button = view.findViewById(R.id.btnNPMReset)
        val btnSubmit: Button = view.findViewById(R.id.btnNPMSubmit)

        tagAdapter = NoteParamTagAdapter(
            listOf(Tag(0, 0, "no tags", null, 0)),
            ::onTagClick,
            ::getTagCheckedState,
        )

        rvTags.addItemDecoration(ItemMarginDecorator.TagsInParamMenu(
            context.resources.getDimension(R.dimen.margin).toInt(),
        ))
        rvTags.adapter = tagAdapter
        rvTags.layoutManager = StaggeredGridLayoutManager(
            lastTagListRowCount,
            LinearLayoutManager.HORIZONTAL,
        )

        datePicker = MaterialDatePicker.Builder.datePicker()

        btnDateStart.setOnClickListener { removeDateStartLimit() }
        etDateStart.setOnClickListener { openDateStartPicker() }
        etDateEnd.setOnClickListener { openDateEndPicker() }
        btnDateEnd.setOnClickListener { removeDateEndLimit() }

        btnModifStart.setOnClickListener { removeModifStartLimit() }
        etModifStart.setOnClickListener { openModifStartPicker() }
        etModifEnd.setOnClickListener { openModifEndPicker() }
        btnModifEnd.setOnClickListener { removeModifEndLimit() }

        btgSortBy.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }

            when (checkedId) {
                R.id.btnNPMSortByTitle -> queryViewModel.setSortBy(QuerySortBy.Title)
                R.id.btnNPMSortByDate -> queryViewModel.setSortBy(QuerySortBy.Date)
                R.id.btnNPMSortByModif -> queryViewModel.setSortBy(QuerySortBy.DateModified)
            }
        }

        btgSortType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }

            when (checkedId) {
                R.id.btnNPMSortTypeAsc -> queryViewModel.setSortType(QuerySortType.Ascending)
                R.id.btnNPMSortTypeDesc -> queryViewModel.setSortType(QuerySortType.Descending)
            }
        }

        btnReset.setOnClickListener { queryViewModel.clearQuery() }
        btnSubmit.setOnClickListener { onSubmit() }

        tagDialog.onShow = { btnEditTags.isEnabled = false }
        tagDialog.onHide = { btnEditTags.isEnabled = true }

        btnEditTags.setOnClickListener {
            tagDialog.show()
        }

        setupViewModelObservers(viewLifecycleOwner)

        parent.addView(view)

        val viewParams = view.layoutParams as MarginLayoutParams
        viewParams.width = -1
        viewParams.height = -1
        viewParams.topMargin = context.resources.getDimension(R.dimen.backdrop_header_height).toInt()
        view.layoutParams = viewParams
    }

    private fun setupViewModelObservers(viewLifecycleOwner: LifecycleOwner) {
        tagViewModel.tags.observe(viewLifecycleOwner) { tags ->
            setTagListRows(tags.size)
            tagAdapter.tags = tags + Tag(0, 0, "no tags", null, 0)
            tagAdapter.notifyDataSetChanged()
        }

        queryViewModel.tags.observe(viewLifecycleOwner) { tagIds ->
            for (pos in 0..<tagAdapter.itemCount) {
                val chip = rvTags
                    .findViewHolderForAdapterPosition(pos)
                    ?.itemView as? Chip
                    ?: continue

                val chipState = tagIds != null && tagIds.contains(chip.id)
                if (chip.isChecked != chipState) {
                    chip.isChecked = chipState
                }
            }
        }

        queryViewModel.date.observe(viewLifecycleOwner) {
            if (it == null || it.first == 0L) {
                etDateStart.setText("Not specified")
            } else {
                etDateStart.setText(AppUtil.formatDate(it.first))
            }

            if (it == null || it.second == 0L) {
                etDateEnd.setText("Not specified")
            } else {
                etDateEnd.setText(AppUtil.formatDate(it.second - dayInSecs))
            }
        }

        queryViewModel.dateModified.observe(viewLifecycleOwner) {
            if (it == null || it.first == 0L) {
                etModifStart.setText("Not specified")
            } else {
                etModifStart.setText(AppUtil.formatDate(it.first))
            }

            if (it == null || it.second == 0L) {
                etModifEnd.setText("Not specified")
            } else {
                etModifEnd.setText(AppUtil.formatDate(it.second - dayInSecs))
            }
        }

        queryViewModel.sortBy.observe(viewLifecycleOwner) {
            when (it) {
                QuerySortBy.Title -> btgSortBy.check(R.id.btnNPMSortByTitle)
                QuerySortBy.Date -> btgSortBy.check(R.id.btnNPMSortByDate)
                QuerySortBy.DateModified -> btgSortBy.check(R.id.btnNPMSortByModif)
            }
        }

        queryViewModel.sortType.observe(viewLifecycleOwner) {
            when (it) {
                QuerySortType.Ascending -> btgSortType.check(R.id.btnNPMSortTypeAsc)
                QuerySortType.Descending -> btgSortType.check(R.id.btnNPMSortTypeDesc)
            }
        }
    }

    private fun setTagListRows(tagCount: Int) {
        val rowCount = when {
            tagCount < 5 -> 1
            tagCount < 10 -> 2
            else -> 3
        }

        if (rowCount != lastTagListRowCount) {
            lastTagListRowCount = rowCount
            rvTags.layoutManager = StaggeredGridLayoutManager(
                rowCount,
                LinearLayoutManager.HORIZONTAL,
            )
        }
    }

    private fun onTagClick(position: Int, isChecked: Boolean) {
        val tags = tagViewModel.tags.value

        val tagId = if (
            (tags.isNullOrEmpty() && position == 0) ||
            (position == tags?.size)
        ) {
            0
        } else {
            tags!![position].id
        }

        if (isChecked) {
            queryViewModel.addTag(tagId)
        } else {
            queryViewModel.removeTag(tagId)
        }
    }

    private fun getTagCheckedState(tagId: Int): Boolean {
        return queryViewModel.tags.value?.contains(tagId) ?: false
    }

    private fun openDateStartPicker() {
        openDatePickerNew(
            "Date created start",
            queryViewModel.date.value!!.first,
        ) { queryViewModel.setDateStart(it) }
    }

    private fun openDateEndPicker() {
        openDatePickerNew(
            "Date created end",
            queryViewModel.date.value!!.second - dayInSecs,
        ) { queryViewModel.setDateEnd(it + dayInSecs) }  // adding a day to make the end inclusive
    }

    private fun openModifStartPicker() {
        openDatePickerNew(
            "Date modified start",
            queryViewModel.dateModified.value!!.first,
        ) { queryViewModel.setDateModifiedStart(it) }
    }

    private fun openModifEndPicker() {
        openDatePickerNew(
            "Date modified end",
            queryViewModel.dateModified.value!!.second - dayInSecs,
        ) { queryViewModel.setDateModifiedEnd(it + dayInSecs) }  // same thing as above
    }

    private fun removeDateStartLimit() {
        queryViewModel.setDateStart(0)
    }

    private fun removeDateEndLimit() {
        queryViewModel.setDateEnd(0)
    }

    private fun removeModifStartLimit() {
        queryViewModel.setDateModifiedStart(0)
    }

    private fun removeModifEndLimit() {
        queryViewModel.setDateModifiedEnd(0)
    }

    private fun openDatePickerNew(
        title: String,
        selection: Long,
        onPositiveButton: (timestampInSeconds: Long) -> Unit,
    ) {
        datePicker.setTitleText(title)
        datePicker.setSelection(
            if (selection > 0L) selection * 1000 else AppUtil.getNowInMillis(),
        )

        val picker = datePicker.build()
        picker.addOnPositiveButtonClickListener {
            onPositiveButton(it / 1000)
        }

        picker.show(fragmentManager, null)
    }
}
