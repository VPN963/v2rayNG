from pathlib import Path
import re

root = Path('V2rayNG/app')

# 1) Make horizontal mode swipes deterministic in both directions.
container_path = root / 'src/main/java/com/v2ray/ang/ui/MobileTinaModeContainer.kt'
container_path.write_text('''package com.v2ray.ang.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Owns deliberate horizontal mode swipes while leaving vertical server-list scrolling untouched.
 * Direction: +1 = left-to-right, -1 = right-to-left.
 */
class MobileTinaModeContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val interceptThreshold = 24f * density
    private val triggerThreshold = 58f * density

    private var downX = 0f
    private var downY = 0f
    private var intercepting = false
    private var gestureTriggered = false
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
                gestureTriggered = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (abs(dx) >= interceptThreshold && abs(dx) > abs(dy) * 1.2f) {
                    intercepting = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> resetGesture()
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!intercepting) return super.onTouchEvent(event)

        if (event.actionMasked == MotionEvent.ACTION_MOVE && !gestureTriggered) {
            val dx = event.x - downX
            if (abs(dx) >= triggerThreshold) {
                gestureTriggered = true
                swipeListener?.invoke(if (dx > 0f) 1 else -1)
            }
        }

        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            resetGesture()
        }
        return true
    }

    private fun resetGesture() {
        intercepting = false
        gestureTriggered = false
        parent?.requestDisallowInterceptTouchEvent(false)
    }
}
''')

# 2) Map +1 (left-to-right) to Manual and -1 (right-to-left) to Auto.
main_path = root / 'src/main/java/com/v2ray/ang/ui/MainActivity.kt'
main = main_path.read_text()
old = '''        binding.modeContainer.setOnModeSwipeListener { direction ->
            // User preference: swipe LEFT -> RIGHT to go from Auto to Manual.
            if (direction < 0) setMode(MODE_MANUAL) else setMode(MODE_AUTO)
        }
'''
new = '''        binding.modeContainer.setOnModeSwipeListener { direction ->
            // +1 = left-to-right -> Manual, -1 = right-to-left -> Auto.
            if (direction > 0) setMode(MODE_MANUAL) else setMode(MODE_AUTO)
        }
'''
if old not in main:
    raise SystemExit('MainActivity swipe mapping block not found')
main_path.write_text(main.replace(old, new, 1))

# 3) Rebuild the manual dock with real spacing between controls.
layout_path = root / 'src/main/res/layout/activity_main.xml'
layout = layout_path.read_text()
start_marker = '''                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:background="@color/divider_color_light" />

                <FrameLayout'''
start = layout.find(start_marker)
if start < 0:
    raise SystemExit('Manual dock start not found')
end_marker = '''                </FrameLayout>
            </LinearLayout>
        </com.v2ray.ang.ui.MobileTinaModeContainer>'''
end = layout.find(end_marker, start)
if end < 0:
    raise SystemExit('Manual dock end not found')
end += len('                </FrameLayout>\n')

new_dock = '''                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:background="@color/divider_color_light" />

                <FrameLayout
                    android:layout_width="match_parent"
                    android:layout_height="128dp"
                    android:clipChildren="false"
                    android:clipToPadding="false">

                    <View
                        android:layout_width="match_parent"
                        android:layout_height="96dp"
                        android:layout_gravity="bottom"
                        android:background="#2B2B2B" />

                    <com.google.android.material.floatingactionbutton.FloatingActionButton
                        android:id="@+id/fab"
                        android:layout_width="58dp"
                        android:layout_height="58dp"
                        android:layout_gravity="top|center_horizontal"
                        android:contentDescription="@string/tasker_start_service"
                        android:src="@drawable/ic_play_24dp"
                        app:fabCustomSize="58dp"
                        app:maxImageSize="27dp"
                        app:shapeAppearanceOverlay="@style/MobileTinaCircleFab"
                        app:tint="@color/colorWhite" />

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btn_smart_connect"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:layout_gravity="top|center_horizontal"
                        android:layout_marginTop="60dp"
                        android:backgroundTint="#8A4600"
                        android:minHeight="36dp"
                        android:paddingStart="18dp"
                        android:paddingEnd="18dp"
                        android:text="@string/mobiletina_manual_smart_connect_with_icon"
                        android:textAllCaps="false"
                        android:textColor="@android:color/white"
                        app:cornerRadius="19dp" />

                    <LinearLayout
                        android:id="@+id/manual_selected_row"
                        android:layout_width="wrap_content"
                        android:layout_height="26dp"
                        android:layout_gravity="top|center_horizontal"
                        android:layout_marginTop="99dp"
                        android:gravity="center"
                        android:orientation="horizontal"
                        android:visibility="invisible">

                        <TextView
                            android:id="@+id/tv_manual_selected"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:ellipsize="end"
                            android:gravity="center"
                            android:maxLines="1"
                            android:maxWidth="230dp"
                            android:textAppearance="@style/TextAppearance.AppCompat.Small"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:paddingStart="7dp"
                            android:paddingEnd="7dp"
                            android:text="•"
                            android:textAppearance="@style/TextAppearance.AppCompat.Small" />

                        <TextView
                            android:id="@+id/tv_manual_ping"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:gravity="center"
                            android:text="@string/mobiletina_ping_unknown"
                            android:textAppearance="@style/TextAppearance.AppCompat.Small"
                            android:textStyle="bold" />
                    </LinearLayout>
                </FrameLayout>
'''
layout_path.write_text(layout[:start] + new_dock + layout[end:])

# 4) Expose the real base version to users.
gradle_path = root / 'build.gradle.kts'
gradle = gradle_path.read_text()
old_version = 'versionName = "2.3.3"'
new_version = 'versionName = "2.0.15"'
if old_version not in gradle:
    raise SystemExit('Expected versionName 2.3.3 not found')
gradle_path.write_text(gradle.replace(old_version, new_version, 1))

print('Applied deterministic two-way swipe, non-overlapping manual dock, and version 2.0.15.')
