package com.kutoru.mikunotes.ui.NoteActivity

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NoteTagDialog(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    root: ConstraintLayout,

    private val scope: CoroutineScope,
    private val viewModel: TagViewModel,
    private val handleRequest: suspend (suspend () -> Any) -> Result<Any>,
    private val showToast: (String?) -> Unit,

    private val onShow: () -> Unit,
    private val onHide: () -> Unit,
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

        viewModel.tags.observe(lifecycleOwner) {
            adapter.tags = it
            adapter.notifyDataSetChanged()
        }

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
        adapter.noteTagIds = noteTags.map { it.id }
        if (dialogView.visibility == View.VISIBLE) {
            hide()
        }

        onShow()
        scope.launch {
            val result = handleRequest { viewModel.getTags() }
            if (result.isFailure) {
                showToast("Could not get tags")
                onHide()
                return@launch
            }

            if (viewModel.tags.value!!.isNotEmpty()) {
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
        onHide()
    }

    private fun addTag(position: Int) {
        println("dialogAddTag")
        onTagAdd(viewModel.tags.value!![position])
    }

    private fun removeTag(position: Int) {
        println("dialogRemoveTag")
        onTagRemove(viewModel.tags.value!![position])
    }

    private fun saveTag(position: Int) {
        println("dialogSaveTag")
        onTagChange(viewModel.tags.value!![position])
    }

    private fun deleteTag(position: Int) {
        println("dialogDeleteTag")
        onTagRemove(viewModel.tags.value!![position])
    }

    private fun dialogCreateNewTag() {
        println("createNewTag")
    }
}
