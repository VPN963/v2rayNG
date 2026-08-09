from pathlib import Path
import re

root = Path('V2rayNG/app/src/main')

# ----------------------------- MainActivity -----------------------------
main_path = root / 'java/com/v2ray/ang/ui/MainActivity.kt'
main = main_path.read_text()

pattern = re.compile(
    r'    private fun setupModeTabs\(\) \{.*?\n    private fun setMode\(mode: Int, updateTab: Boolean = true\) \{.*?\n    \}',
    re.S,
)
replacement = '''    private fun setupModeTabs() {
        binding.btnModeManual.setOnClickListener { setMode(MODE_MANUAL) }
        binding.btnModeAuto.setOnClickListener { setMode(MODE_AUTO) }
        binding.modeContainer.setOnModeSwipeListener { direction ->
            // User preference: swipe LEFT -> RIGHT to go from Auto to Manual.
            if (direction < 0) setMode(MODE_MANUAL) else setMode(MODE_AUTO)
        }
        setMode(MODE_AUTO)
    }

    private fun setMode(mode: Int, updateTab: Boolean = true) {
        currentMode = mode.coerceIn(MODE_AUTO, MODE_MANUAL)
        binding.autoPanel.visibility = if (currentMode == MODE_AUTO) View.VISIBLE else View.GONE
        binding.manualPanel.visibility = if (currentMode == MODE_MANUAL) View.VISIBLE else View.GONE
        updateModeSelector()
        refreshSelectedServerUi()
    }

    private fun updateModeSelector() {
        val manualSelected = currentMode == MODE_MANUAL
        val autoSelected = currentMode == MODE_AUTO
        binding.btnModeManual.isSelected = manualSelected
        binding.btnModeAuto.isSelected = autoSelected

        fun style(button: com.google.android.material.button.MaterialButton, selected: Boolean) {
            button.backgroundTintList = ColorStateList.valueOf(
                if (selected) Color.rgb(86, 86, 91) else Color.rgb(34, 34, 38)
            )
            button.strokeColor = ColorStateList.valueOf(
                if (selected) Color.rgb(106, 106, 112) else Color.rgb(52, 52, 58)
            )
            button.setTextColor(Color.WHITE)
        }
        style(binding.btnModeManual, manualSelected)
        style(binding.btnModeAuto, autoSelected)
        binding.modeIndicatorManual.setBackgroundColor(if (manualSelected) Color.WHITE else Color.TRANSPARENT)
        binding.modeIndicatorAuto.setBackgroundColor(if (autoSelected) Color.WHITE else Color.TRANSPARENT)
    }'''
main, count = pattern.subn(replacement, main, count=1)
if count != 1:
    raise SystemExit(f'MainActivity mode block: expected 1 match, got {count}')

needle = '''        binding.tvAutoServer.text = profile?.remarks.orEmpty()
        binding.tvManualSelected.text = profile?.remarks.orEmpty()
        binding.tvManualPing.text = pingLabel(ping)
'''
replace = '''        binding.tvAutoServer.text = profile?.remarks.orEmpty()
        binding.tvManualSelected.text = profile?.remarks.orEmpty()
        binding.tvManualPing.text = pingLabel(ping)
        binding.manualSelectedRow.visibility = if (profile != null) View.VISIBLE else View.INVISIBLE
'''
if needle not in main:
    raise SystemExit('manual selected row visibility insertion point not found')
main = main.replace(needle, replace, 1)
main_path.write_text(main)

# ----------------------------- activity_main.xml -----------------------------
layout_path = root / 'res/layout/activity_main.xml'
layout = layout_path.read_text()

tab_pattern = re.compile(
    r'            <com\.google\.android\.material\.tabs\.TabLayout\n'
    r'                android:id="@\+id/mode_tabs".*?\n'
    r'                app:tabRippleColor="@android:color/transparent" />',
    re.S,
)
new_selector = '''            <LinearLayout
                android:id="@+id/mode_switch_container"
                android:layout_width="match_parent"
                android:layout_height="64dp"
                android:orientation="vertical"
                android:paddingStart="8dp"
                android:paddingEnd="8dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="58dp"
                    android:gravity="center"
                    android:layoutDirection="ltr"
                    android:orientation="horizontal">

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btn_mode_manual"
                        android:layout_width="0dp"
                        android:layout_height="52dp"
                        android:layout_weight="1"
                        android:layout_marginStart="2dp"
                        android:layout_marginEnd="2dp"
                        android:gravity="center"
                        android:insetLeft="0dp"
                        android:insetTop="0dp"
                        android:insetRight="0dp"
                        android:insetBottom="0dp"
                        android:minWidth="0dp"
                        android:minHeight="0dp"
                        android:padding="0dp"
                        android:text="@string/mobiletina_mode_manual"
                        android:textAllCaps="false"
                        android:textColor="@android:color/white"
                        android:textSize="17sp"
                        app:backgroundTint="#222226"
                        app:cornerRadius="24dp"
                        app:strokeColor="#34343A"
                        app:strokeWidth="1dp" />

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btn_mode_auto"
                        android:layout_width="0dp"
                        android:layout_height="52dp"
                        android:layout_weight="1"
                        android:layout_marginStart="2dp"
                        android:layout_marginEnd="2dp"
                        android:gravity="center"
                        android:insetLeft="0dp"
                        android:insetTop="0dp"
                        android:insetRight="0dp"
                        android:insetBottom="0dp"
                        android:minWidth="0dp"
                        android:minHeight="0dp"
                        android:padding="0dp"
                        android:text="@string/mobiletina_mode_auto"
                        android:textAllCaps="false"
                        android:textColor="@android:color/white"
                        android:textSize="17sp"
                        app:backgroundTint="#222226"
                        app:cornerRadius="24dp"
                        app:strokeColor="#34343A"
                        app:strokeWidth="1dp" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="3dp"
                    android:layoutDirection="ltr"
                    android:orientation="horizontal">

                    <FrameLayout
                        android:layout_width="0dp"
                        android:layout_height="3dp"
                        android:layout_weight="1">
                        <View
                            android:id="@+id/mode_indicator_manual"
                            android:layout_width="match_parent"
                            android:layout_height="3dp"
                            android:layout_marginStart="20dp"
                            android:layout_marginEnd="6dp"
                            android:background="@android:color/transparent" />
                    </FrameLayout>

                    <FrameLayout
                        android:layout_width="0dp"
                        android:layout_height="3dp"
                        android:layout_weight="1">
                        <View
                            android:id="@+id/mode_indicator_auto"
                            android:layout_width="match_parent"
                            android:layout_height="3dp"
                            android:layout_marginStart="6dp"
                            android:layout_marginEnd="20dp"
                            android:background="@android:color/transparent" />
                    </FrameLayout>
                </LinearLayout>
            </LinearLayout>'''
layout, count = tab_pattern.subn(new_selector, layout, count=1)
if count != 1:
    raise SystemExit(f'mode TabLayout replacement: expected 1 match, got {count}')

# Replace the whole manual dock with fixed-position children so content cannot clip.
start_marker = '''                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:background="@color/divider_color_light" />

                <FrameLayout'''
start = layout.find(start_marker)
if start < 0:
    raise SystemExit('manual dock start not found')
end_marker = '''                </FrameLayout>
            </LinearLayout>
        </com.v2ray.ang.ui.MobileTinaModeContainer>'''
end = layout.find(end_marker, start)
if end < 0:
    raise SystemExit('manual dock end not found')
end += len('                </FrameLayout>\n')

new_dock = '''                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:background="@color/divider_color_light" />

                <FrameLayout
                    android:layout_width="match_parent"
                    android:layout_height="116dp"
                    android:clipChildren="false"
                    android:clipToPadding="false">

                    <View
                        android:layout_width="match_parent"
                        android:layout_height="88dp"
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
                        android:layout_marginTop="50dp"
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
                        android:layout_height="28dp"
                        android:layout_gravity="top|center_horizontal"
                        android:layout_marginTop="86dp"
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
layout = layout[:start] + new_dock + layout[end:]
layout_path.write_text(layout)

print('Stable mode selector, reverse swipe, and fixed manual dock patch applied.')
