package com.kutoru.mikunotes.ui

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.logic.ApiService
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.adapters.TagDialogAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NoteTagDialog(
    context: Context,
    root: ConstraintLayout,
    private val apiService: ApiService,
    private val onTagAdd: (tag: Tag) -> Unit,
    private val onTagRemove: (tag: Tag) -> Unit,
    private val onTagChange: (tag: Tag) -> Unit,
) {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val adapter: TagDialogAdapter
    private val dialogView: View
    private val inputManager: InputMethodManager?
    private lateinit var tags: MutableList<Tag>

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
            tags = apiService.getTags("Could not get tags") ?: return@launch

            tags = mutableListOf(
                Tag(1723018115, 1, "tag name 1", null, 1), Tag(1723018115, 2, "tag name 2", null, 1),
                Tag(1723018115, 3, "tag name 3", null, 1), Tag(1723018115, 4, "tag name 4", null, 1),
                Tag(1723018115, 5, "tag name 5", null, 1), Tag(1723018115, 6, "tag name 6", null, 1),
                Tag(1723018115, 7, "tag name 7", null, 1), Tag(1723018115, 8, "tag name 8", null, 1),
                Tag(1723018115, 9, "tag name 9", null, 1), Tag(1723018115, 10, "tag name 10", null, 1),
                Tag(1723018115, 11, "tag name 11", null, 1), Tag(1723018115, 12, "tag name 12", null, 1),
                Tag(1723018115, 13, "tag name 13", null, 1), Tag(1723018115, 14, "tag name 14", null, 1),
                Tag(1723018115, 15, "tag name 15", null, 1), Tag(1723018115, 16, "tag name 16", null, 1),
            )
            println("tags: $tags")

            adapter.noteTagIds = noteTags.map { it.id }
            adapter.tags = tags
            adapter.notifyDataSetChanged()

            if (tags.isNotEmpty()) {
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
        onTagAdd(tags[position])
    }

    private fun removeTag(position: Int) {
        println("dialogRemoveTag")
        onTagRemove(tags[position])
    }

    private fun saveTag(position: Int) {
        println("dialogSaveTag")
        onTagChange(tags[position])
    }

    private fun deleteTag(position: Int) {
        println("dialogDeleteTag")
        onTagRemove(tags[position])
    }

    private fun dialogCreateNewTag() {
        println("createNewTag")
    }

    fun cancelJob() {
        job.cancel()
    }
}
