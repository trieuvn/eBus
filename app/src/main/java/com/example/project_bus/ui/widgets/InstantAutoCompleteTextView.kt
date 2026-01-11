package com.example.project_bus.ui.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

/**
 * AutoCompleteTextView that always thinks it has enough chars to filter.
 * This allows calling showDropDown() even when the input is empty.
 */
class InstantAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    override fun enoughToFilter(): Boolean = true
}
