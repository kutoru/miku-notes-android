package com.kutoru.mikunotes.ui

import android.content.Context
import android.graphics.drawable.TransitionDrawable
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.updatePadding
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.logic.ANIMATION_TRANSITION_TIME

class EditTextLarge(
    context: Context,
    attrs: AttributeSet,
) : FrameLayout(context, attrs) {

    private val animationDuration = ANIMATION_TRANSITION_TIME
    private val etLargeText: EditText

    var text
        get() = etLargeText.text?.toString() ?: ""
        set(value) = etLargeText.setText(value)

    init {
        inflate(context, R.layout.content_edit_text_large, this)

        val divider1: View = findViewById(R.id.divider1)
        etLargeText = findViewById(R.id.etLargeText)
        val divider2: View = findViewById(R.id.divider2)

        setInputOnFocusChange(
            etLargeText, divider1, divider2,
        )

        val typedAttrs = context.theme.obtainStyledAttributes(
            attrs, R.styleable.EditTextLarge, 0, 0,
        )

        val hint = typedAttrs.getString(R.styleable.EditTextLarge_hint)
        val multiline = typedAttrs.getBoolean(R.styleable.EditTextLarge_multiline, false)
        val textSize = typedAttrs.getDimensionPixelSize(R.styleable.EditTextLarge_textSize, 0)
        val paddingVertical = typedAttrs.getDimensionPixelSize(R.styleable.EditTextLarge_paddingVertical, 0)

        if (hint != null) {
            etLargeText.hint = hint
        }

        if (multiline) {
            val params = etLargeText.layoutParams
            params.height = 0
            etLargeText.layoutParams = params

            etLargeText.inputType =
                InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

            etLargeText.isSingleLine = false
            etLargeText.isVerticalScrollBarEnabled = true
            etLargeText.isNestedScrollingEnabled = true
        }

        if (textSize > 0) {
            etLargeText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        }

        if (paddingVertical != 0) {
            etLargeText.updatePadding(
                top = paddingVertical,
                bottom = paddingVertical,
            )
        }

        typedAttrs.recycle()
    }

    private fun setInputOnFocusChange(inputView: EditText, dividerTop: View, dividerBottom: View) {
        inputView.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            val transTop = dividerTop.background as TransitionDrawable
            val transBottom = dividerBottom.background as TransitionDrawable

            if (hasFocus) {
                transTop.startTransition(animationDuration)
                transBottom.startTransition(animationDuration)
            } else {
                transTop.reverseTransition(animationDuration)
                transBottom.reverseTransition(animationDuration)
            }
        }
    }
}
