package com.kutoru.mikunotes.ui.notes

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.allViews
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.logic.AppUtil
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.TagViewModel

class NoteParamMenu(
    context: Context,
    viewLifecycleOwner: LifecycleOwner,
    parent: ConstraintLayout,
    private val fragmentManager: FragmentManager,
    private val tagViewModel: TagViewModel,
    private val queryViewModel: QueryViewModel,
) {

    private val view: View
    private val datePicker: MaterialDatePicker.Builder<Long>
    private val dayInSecs = 86400L

    private val etTitle: EditText
    private val cgTags: ChipGroup
    private val etDateStart: EditText
    private val etDateEnd: EditText
    private val etModifStart: EditText
    private val etModifEnd: EditText
    private val btgSortBy: MaterialButtonToggleGroup
    private val btgSortType: MaterialButtonToggleGroup

    init {
        view = View.inflate(context, R.layout.content_notes_param_menu, null)

        etTitle = view.findViewById(R.id.etNPMTitle)
        cgTags = view.findViewById(R.id.cgNPMTags)
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

        datePicker = MaterialDatePicker.Builder.datePicker()

        etTitle.addTextChangedListener {
            queryViewModel.setTitle(it.toString())
        }

        btnDateStart.setOnClickListener { removeDateStartLimit() }
        etDateStart.setOnClickListener { openDateStartPicker() }
        etDateEnd.setOnClickListener { openDateEndPicker() }
        btnDateEnd.setOnClickListener { removeDateEndLimit() }

        btnModifStart.setOnClickListener { removeModifStartLimit() }
        etModifStart.setOnClickListener { openModifStartPicker() }
        etModifEnd.setOnClickListener { openModifEndPicker() }
        btnModifEnd.setOnClickListener { removeModifEndLimit() }

        setupViewModelObservers(context, viewLifecycleOwner)

        parent.addView(view)

        val vp = view.layoutParams as MarginLayoutParams
        vp.width = -1
        vp.height = -1
        vp.topMargin = context.resources.getDimension(R.dimen.backdrop_header_height).toInt()
        view.layoutParams = vp
    }

    private fun setupViewModelObservers(context: Context, viewLifecycleOwner: LifecycleOwner) {
        queryViewModel.title.observe(viewLifecycleOwner) {
            if (it != etTitle.text.toString()) {
                etTitle.setText(it ?: "")
            }
        }

        tagViewModel.tags.observe(viewLifecycleOwner) { tags ->
            cgTags.removeAllViews()

            (tags + listOf(Tag(0, 0, "no tags", null, 0))).forEach { tag ->
                val chip = View.inflate(context, R.layout.chip_tag_choice, null) as Chip
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

                cgTags.addView(chip)
            }
        }

        queryViewModel.tags.observe(viewLifecycleOwner) { tagIds ->
            cgTags.allViews.forEach {
                val chip = it as? Chip ?: return@forEach
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
