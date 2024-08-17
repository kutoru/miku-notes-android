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
import androidx.core.util.Pair
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
    private val dateRangePicker: MaterialDatePicker.Builder<Pair<Long, Long>>
    private val dayInMs = 86_400_000L

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

        dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()

        etTitle.addTextChangedListener {
            println("addTextChangedListener $it")
            queryViewModel.setTitle(it.toString())
        }

        btnDateStart.setOnClickListener { removeDateStartLimit() }
        etDateStart.setOnClickListener { openDateCreatedPicker() }
        etDateEnd.setOnClickListener { openDateCreatedPicker() }
        btnDateEnd.setOnClickListener { removeDateEndLimit() }

        btnModifStart.setOnClickListener { removeModifStartLimit() }
        etModifStart.setOnClickListener { openDateModifiedPicker() }
        etModifEnd.setOnClickListener { openDateModifiedPicker() }
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
                etDateEnd.setText(AppUtil.formatDate(it.second - dayInMs / 1000))
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
                etModifEnd.setText(AppUtil.formatDate(it.second - dayInMs / 1000))
            }
        }
    }

    private fun removeDateStartLimit() {
        val second = queryViewModel.date.value?.second
        queryViewModel.setDate(0, second ?: 0)
    }

    private fun removeDateEndLimit() {
        val first = queryViewModel.date.value?.first
        queryViewModel.setDate(first ?: 0, 0)
    }

    private fun removeModifStartLimit() {
        val second = queryViewModel.dateModified.value?.second
        queryViewModel.setDateModified(0, second ?: 0)
    }

    private fun removeModifEndLimit() {
        val first = queryViewModel.dateModified.value?.first
        queryViewModel.setDateModified(first ?: 0, 0)
    }

    private fun openDateCreatedPicker() {
        openDatePicker(
            "Date created",
            queryViewModel.date.value,
        ) {
            queryViewModel.setDate(it.first / 1000, (it.second + dayInMs) / 1000)
        }
    }

    private fun openDateModifiedPicker() {
        openDatePicker(
            "Date modified",
            queryViewModel.dateModified.value,
        ) {
            queryViewModel.setDateModified(it.first / 1000, (it.second + dayInMs) / 1000)
        }
    }

    private fun openDatePicker(
        title: String,
        dates: kotlin.Pair<Long, Long>?,
        onPositiveButton: (Pair<Long, Long>) -> Unit,
    ) {
        dateRangePicker.setTitleText(title)
        val now = AppUtil.getNowInMillis()

        var firstDate = if (dates != null && dates.first != 0L) {
            dates.first * 1000
        } else {
            now
        }

        val secondDate = if (dates != null && dates.second != 0L) {
            val res = dates.second * 1000 - dayInMs
            if (firstDate > res) firstDate = res
            res
        } else {
            if (firstDate < now) firstDate else now
        }

        dateRangePicker.setSelection(Pair(
            firstDate,
            secondDate,
        ))

        val datePicker = dateRangePicker.build()
        datePicker.addOnPositiveButtonClickListener {
            onPositiveButton(it)
        }

        datePicker.show(fragmentManager, null)
    }
}
