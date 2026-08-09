package com.v2ray.ang.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs

/** Intercepts only deliberate horizontal swipes, leaving vertical RecyclerView scrolling untouched. */
class MobileTinaModeContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var downX = 0f
    private var downY = 0f
    private var intercepting = false
    private var swipeListener: ((direction: Int) -> Unit)? = null

    fun setOnModeSwipeListener(listener: (direction: Int) -> Unit) {
        swipeListener = listener
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                intercepting = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (abs(dx) > 36f && abs(dx) > abs(dy) * 1.35f) {
                    intercepting = true
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!intercepting) return super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            val dx = event.x - downX
            if (abs(dx) > 80f) swipeListener?.invoke(if (dx < 0f) 1 else -1)
            intercepting = false
        }
        return true
    }
}
