package com.kutoru.mikunotes.ui

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.adapters.TagDialogAdapter
import com.kutoru.mikunotes.viewmodels.TagViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NoteTagDialog(
    context: Context,
    root: ConstraintLayout,
    private val scope: CoroutineScope,
    private val viewModel: TagViewModel,
    private val handleRequest: suspend (suspend () -> Any) -> Result<Any>,
    private val showToast: (String?) -> Unit,
    private val onTagAdd: (tag: Tag) -> Unit,
    private val onTagRemove: (tag: Tag) -> Unit,
    private val onTagChange: (tag: Tag) -> Unit,
) {

    private val adapter: TagDialogAdapter
    private val dialogView: View
    private val inputManager: InputMethodManager?

    init {
        adapter = TagDialogAdapter(
            context,
            listOf(),
            listOf(),
            ::addTag,
            ::removeTag,
            ::saveTag,
            ::deleteTag,
        )

        dialogView = View.inflate(context, R.layout.dialog_add_tag, null)
        val btnDialogTagBack = dialogView.findViewById<Button>(R.id.btnDialogTagBack)
        val rvDialogTags = dialogView.findViewById<RecyclerView>(R.id.rvDialogTagList)
        val btnDialogTagAdd = dialogView.findViewById<Button>(R.id.btnDialogTagAdd)

        btnDialogTagBack.setOnClickListener {
            hide()
        }

        rvDialogTags.adapter = adapter

        btnDialogTagAdd.setOnClickListener {
            dialogCreateNewTag()
        }

        dialogView.visibility = View.GONE
        root.addView(dialogView)

        val viewParams = dialogView.layoutParams
        viewParams.height = -1
        viewParams.width = -1
        dialogView.layoutParams = viewParams

        inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    }

    fun show(noteTags: List<Tag>) {
        scope.launch {
            val result = handleRequest { viewModel.getTags() }
            if (result.isFailure) {
                showToast("Could not get tags")
                return@launch
            }

            adapter.noteTagIds = noteTags.map { it.id }
            adapter.tags = viewModel.tags
            adapter.notifyDataSetChanged()

            if (viewModel.tags.isNotEmpty()) {
                dialogView
                    .findViewById<RecyclerView>(R.id.rvDialogTagList)
                    .scrollToPosition(0)
            }

            dialogView.visibility = View.VISIBLE
        }
    }

    fun hide() {
        dialogView.visibility = View.GONE
        inputManager?.hideSoftInputFromWindow(dialogView.windowToken, 0)
    }

    private fun addTag(position: Int) {
        println("dialogAddTag")
        onTagAdd(viewModel.tags[position])
    }

    private fun removeTag(position: Int) {
        println("dialogRemoveTag")
        onTagRemove(viewModel.tags[position])
    }

    private fun saveTag(position: Int) {
        println("dialogSaveTag")
        onTagChange(viewModel.tags[position])
    }

    private fun deleteTag(position: Int) {
        println("dialogDeleteTag")
        onTagRemove(viewModel.tags[position])
    }

    private fun dialogCreateNewTag() {
        println("createNewTag")
    }
}
