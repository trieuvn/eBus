package com.example.project_bus.ui.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

/**
 * AutoCompleteTextView hiển thị gợi ý ngay khi focus/tap (không cần gõ 1 ký tự).
 *
 * Lưu ý: AutoCompleteTextView.setThreshold(0) không hoạt động như mong đợi vì
 * threshold <= 0 sẽ bị Android ép về 1, nên cách ổn định nhất là override enoughToFilter().
 */
class InstantAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    override fun enoughToFilter(): Boolean = true

    /**
     * Public helper để HomeActivity gọi mà không đụng vào performFiltering() (protected).
     */
    fun showAllSuggestions() {
        if (adapter == null) return
        // performFiltering() là protected, nên gọi được trong subclass.
        performFiltering(text, 0)
        showDropDown()
    }

    override fun onFocusChanged(
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            post { showAllSuggestions() }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = super.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP) {
            post { showAllSuggestions() }
        }
        return handled
    }
}
