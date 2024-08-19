package com.kutoru.mikunotes.ui.note

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.TagViewModel
import com.kutoru.mikunotes.ui.adapters.ItemMarginDecorator
import com.kutoru.mikunotes.ui.adapters.TagDialogAdapter
import kotlinx.coroutines.launch

class NoteTagDialog(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    root: ViewGroup,
    showAddButtons: Boolean,

    private val viewModel: TagViewModel,
    private val handleRequest: suspend (suspend () -> Any) -> Result<Any>,
    private val showToast: (String?) -> Unit,

    var onShow: (() -> Unit)? = null,
    var onHide: (() -> Unit)? = null,
    private val onTagAdd: ((tag: Tag) -> Unit)? = null,
    private val onTagRemove: ((tag: Tag) -> Unit)? = null,
    private val onTagChange: ((tag: Tag) -> Unit)? = null,
) {

    private val adapter: TagDialogAdapter
    private val dialogView: View
    private val inputManager: InputMethodManager?
    private val rvDialogTags: RecyclerView
    private val tvDialogTagNoTags: TextView

    val isShown get() = dialogView.visibility == View.VISIBLE

    init {
        inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

        adapter = TagDialogAdapter(
            listOf(),
            listOf(),
            showAddButtons,
            inputManager,
            if (showAddButtons) ::addTag else null,
            if (showAddButtons) ::removeTag else null,
            ::saveTag,
            ::deleteTag,
        )

        dialogView = View.inflate(context, R.layout.dialog_add_tag, null)
        val ibDialogTagBack: ImageButton = dialogView.findViewById(R.id.ibDialogTagBack)
        rvDialogTags = dialogView.findViewById(R.id.rvDialogTagList)
        tvDialogTagNoTags = dialogView.findViewById(R.id.tvDialogTagNoTags)
        val btnDialogTagAdd: Button = dialogView.findViewById(R.id.btnDialogTagAdd)

        viewModel.tags.observe(lifecycleOwner) {
            adapter.tags = it
            adapter.notifyDataSetChanged()
        }

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                if (adapter.tags.isEmpty()) {
                    rvDialogTags.visibility = View.INVISIBLE
                    tvDialogTagNoTags.visibility = View.VISIBLE
                } else {
                    tvDialogTagNoTags.visibility = View.INVISIBLE
                    rvDialogTags.visibility = View.VISIBLE
                }

                super.onChanged()
            }
        })


        ibDialogTagBack.setOnClickListener {
            hide()
        }

        rvDialogTags.adapter = adapter
        rvDialogTags.addItemDecoration(ItemMarginDecorator.Notes(
            context.resources.getDimension(R.dimen.margin).toInt(),
        ))

        btnDialogTagAdd.setOnClickListener {
            createNewTag()
        }

        dialogView.visibility = View.GONE
        root.addView(dialogView)

        val viewParams = dialogView.layoutParams
        viewParams.height = -1
        viewParams.width = -1
        dialogView.layoutParams = viewParams
    }

    fun show(noteTags: List<Tag>? = null) {
        adapter.noteTagIds = noteTags?.map { it.id } ?: listOf()
        if (isShown) {
            hide()
        }

        onShow?.invoke()

        viewModel.viewModelScope.launch {
            val result = handleRequest { viewModel.getTags() }
            if (result.isFailure) {
                showToast("Could not get tags")
                onHide?.invoke()
                return@launch
            }

            if (viewModel.tags.value!!.isNotEmpty()) {
                rvDialogTags.scrollToPosition(0)
            }

            dialogView.visibility = View.VISIBLE
        }
    }

    fun hide() {
        dialogView.visibility = View.GONE
        inputManager?.hideSoftInputFromWindow(dialogView.windowToken, 0)
        onHide?.invoke()
    }

    private fun addTag(position: Int) {
        println("dialogAddTag")
        onTagAdd?.invoke(viewModel.tags.value!![position])
    }

    private fun removeTag(position: Int) {
        println("dialogRemoveTag")
        onTagRemove?.invoke(viewModel.tags.value!![position])
    }

    private fun saveTag(position: Int, tagName: String) {
        if (tagName.isBlank()) {
            showToast("Cannot save empty tags")
            return
        }

        viewModel.viewModelScope.launch {
            val tag = adapter.tags[position]
            val result = if (tag.id == -1) {
                handleRequest { viewModel.postTags(tagName) }
            } else {
                handleRequest { viewModel.patchTags(tag.id, tagName) }
            }

            if (result.isFailure) {
                showToast("Could not save the tag")
            } else {
                showToast("Tag saved")
            }
        }
    }

    private fun deleteTag(position: Int) {
        val tag = adapter.tags[position]
        if (tag.id == -1) {
            val newList = adapter.tags.toMutableList()
            newList.removeLast()
            adapter.tags = newList
            adapter.notifyDataSetChanged()
            return
        }

        viewModel.viewModelScope.launch {
            val result = handleRequest { viewModel.deleteTags(tag.id) }
            if (result.isFailure) {
                showToast("Could not delete the tag")
                return@launch
            }
        }
    }

    private fun createNewTag() {
        if (adapter.tags.isEmpty() || adapter.tags.last().id != -1) {
            adapter.tags = (viewModel.tags.value ?: listOf()) + Tag(0, -1, "", null, 0)
            adapter.notifyDataSetChanged()
            adapter.focusNewTag()
        } else {
            adapter.focusExistingNewTag()
        }

        rvDialogTags.smoothScrollToPosition(adapter.itemCount - 1)
    }
}
