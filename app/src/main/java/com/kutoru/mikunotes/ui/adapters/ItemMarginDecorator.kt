package com.kutoru.mikunotes.ui.adapters

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

interface ItemMarginDecorator {
    class Tags(
        private val itemSpacing: Int,
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.left = itemSpacing
            }
        }
    }

    class Files(
        private val itemSpacing: Int,
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            val itemPos = parent.getChildAdapterPosition(view)

            if (itemPos != 0 && itemPos != 1) {
                outRect.top = itemSpacing
            }

            if (itemPos % 2 == 0) {
                outRect.right = itemSpacing / 2
            } else {
                outRect.left = itemSpacing / 2
            }
        }
    }

    class Notes(
        private val itemSpacing: Int,
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.top = itemSpacing
            }
        }
    }
}
