from pathlib import Path
import re


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


root = Path('V2rayNG/app/src/main')

# MainActivity: optical centering, manual startup state and safe pre-warm.
main_path = root / 'java/com/v2ray/ang/ui/MainActivity.kt'
main = main_path.read_text()

main = replace_once(
    main,
    '    private var smartConnectJob: kotlinx.coroutines.Job? = null\n',
    '    private var smartConnectJob: kotlinx.coroutines.Job? = null\n'
    '    private var manualConnecting = false\n'
    '    private var manualPrewarmGuid: String? = null\n'
    '    private var manualPrewarmJob: kotlinx.coroutines.Job? = null\n',
    'manual connection state fields',
)

old_vpn_result = '''            if (result.resultCode == RESULT_OK) {
                startV2Ray(smart)
            } else if (smart) {
                markSmartConnectFailed()
            }'''
new_vpn_result = '''            if (result.resultCode == RESULT_OK) {
                startV2Ray(smart)
            } else if (smart) {
                markSmartConnectFailed()
            } else {
                manualConnecting = false
                refreshSelectedServerUi()
            }'''
main = replace_once(main, old_vpn_result, new_vpn_result, 'VPN permission result')

mode_pattern = re.compile(
    r'    private fun setupModeTabs\(\) \{.*?\nprivate fun setMode\(mode: Int, updateTab: Boolean = true\) \{',
    re.S,
)
mode_replacement = '''    private fun setupModeTabs() {
        binding.modeTabs.removeAllTabs()
        binding.modeTabs.addTab(createModeTab(R.string.mobiletina_mode_auto), true)
        binding.modeTabs.addTab(createModeTab(R.string.mobiletina_mode_manual), false)
        binding.modeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = setMode(tab.position, updateTab = false)
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        binding.modeContainer.setOnModeSwipeListener { direction ->
            if (direction > 0) setMode(MODE_MANUAL) else setMode(MODE_AUTO)
        }
        setMode(MODE_AUTO)
    }

    private fun createModeTab(textRes: Int): TabLayout.Tab {
        val density = resources.displayMetrics.density
        val label = TextView(this).apply {
            setText(textRes)
            gravity = android.view.Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            includeFontPadding = false
            textSize = 17f
            // Optical correction for the visible Persian glyph bounds.
            translationX = -7f * density
            setPadding(0, 0, 0, 0)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.colorTextPrimary))
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return binding.modeTabs.newTab().setText(textRes).setCustomView(label)
    }

    private fun setMode(mode: Int, updateTab: Boolean = true) {'''
main, count = mode_pattern.subn(mode_replacement, main, count=1)
if count != 1:
    raise SystemExit(f'mode tab block: expected one match, found {count}')

main = replace_once(
    main,
    '''            if (isRunning == true) {
                smartConnecting = false
                smartConnectionFailed = false
                smartCountdownSeconds = 0
                MobileTinaSessionLimiter.schedule(this)''',
    '''            if (isRunning == true) {
                manualConnecting = false
                smartConnecting = false
                smartConnectionFailed = false
                smartCountdownSeconds = 0
                MobileTinaSessionLimiter.schedule(this)''',
    'running observer manual reset',
)

old_manual = '''    private fun handleManualFabAction() {
        if (mainViewModel.isRunning.value == true) {
            V2RayServiceManager.stopVService(this)
            return
        }
        if (smartConnecting) return
        requestVpnPermissionAndStart(false)
    }'''
new_manual = '''    private fun handleManualFabAction() {
        if (mainViewModel.isRunning.value == true) {
            manualConnecting = false
            V2RayServiceManager.stopVService(this)
            return
        }
        if (smartConnecting || manualConnecting) return

        manualConnecting = true
        refreshSelectedServerUi()
        requestVpnPermissionAndStart(false)
    }'''
main = replace_once(main, old_manual, new_manual, 'manual FAB action')

old_start = '''    private fun startV2Ray(isSmartConnect: Boolean = false) {
        if (MmkvManager.getSelectServer().isNullOrEmpty()) {
            if (isSmartConnect) markSmartConnectFailed()
            toast(R.string.title_file_chooser)
            return
        }
        V2RayServiceManager.startVService(this)
        if (isSmartConnect) {
            lifecycleScope.launch {
                delay(7_000L)
                if (smartConnecting && mainViewModel.isRunning.value != true) markSmartConnectFailed()
            }
        }
    }'''
new_start = '''    private fun startV2Ray(isSmartConnect: Boolean = false) {
        if (MmkvManager.getSelectServer().isNullOrEmpty()) {
            if (isSmartConnect) {
                markSmartConnectFailed()
            } else {
                manualConnecting = false
                refreshSelectedServerUi()
            }
            toast(R.string.title_file_chooser)
            return
        }
        V2RayServiceManager.startVService(this)
        if (isSmartConnect) {
            lifecycleScope.launch {
                delay(7_000L)
                if (smartConnecting && mainViewModel.isRunning.value != true) markSmartConnectFailed()
            }
        } else {
            lifecycleScope.launch {
                delay(6_000L)
                if (manualConnecting && mainViewModel.isRunning.value != true) {
                    manualConnecting = false
                    refreshSelectedServerUi()
                }
            }
        }
    }'''
main = replace_once(main, old_start, new_start, 'manual start timeout')

main = replace_once(
    main,
    '''        val running = mainViewModel.isRunning.value == true

        binding.tvAutoServer.text = profile?.remarks.orEmpty()''',
    '''        val running = mainViewModel.isRunning.value == true

        if (!running && currentMode == MODE_MANUAL && selectedGuid.isNotBlank()) {
            prewarmManualConnection(selectedGuid)
        }

        binding.tvAutoServer.text = profile?.remarks.orEmpty()''',
    'manual prewarm trigger',
)

old_manual_fab = '''        binding.fab.setImageResource(if (running) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp)
        binding.fab.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, if (running) R.color.color_fab_active else R.color.color_fab_inactive)
        )'''
new_manual_fab = '''        binding.fab.setImageResource(if (running) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp)
        binding.fab.backgroundTintList = ColorStateList.valueOf(
            when {
                running -> ContextCompat.getColor(this, R.color.color_fab_active)
                manualConnecting -> Color.rgb(255, 193, 7)
                else -> ContextCompat.getColor(this, R.color.color_fab_inactive)
            }
        )'''
main = replace_once(main, old_manual_fab, new_manual_fab, 'manual FAB connecting state')

prewarm_method = '''
    private fun prewarmManualConnection(guid: String) {
        if (guid.isBlank() || guid == manualPrewarmGuid) return
        manualPrewarmGuid = guid
        manualPrewarmJob?.cancel()
        manualPrewarmJob = lifecycleScope.launch(Dispatchers.IO) {
            // Move one-time initialization/config work out of the tap-to-connect critical path.
            runCatching { V2RayServiceManager.isRunning() }
            runCatching { com.v2ray.ang.service.TProxyService.preloadNative() }
            // Warm-up only. The real config is rebuilt again at connect time,
            // so configuration and settings freshness are preserved.
            runCatching {
                com.v2ray.ang.handler.V2rayConfigManager.getV2rayConfig(
                    applicationContext,
                    guid
                )
            }
        }
    }

'''
main = replace_once(
    main,
    '    private fun pingLabel(ping: Long): String = when {',
    prewarm_method + '    private fun pingLabel(ping: Long): String = when {',
    'manual prewarm method insertion',
)
main_path.write_text(main)

# activity_main.xml: smaller mode strip and compact bottom dock with overlapping FAB.
layout_path = root / 'res/layout/activity_main.xml'
layout = layout_path.read_text()
layout = replace_once(
    layout,
    '                android:layout_height="72dp"\n                android:layoutDirection="rtl"',
    '                android:layout_height="64dp"\n                android:layoutDirection="rtl"',
    'mode tabs height',
)

start_marker = '''                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:background="@color/divider_color_light" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:background="#2B2B2B"'''
start = layout.find(start_marker)
if start < 0:
    raise SystemExit('manual bottom dock start marker not found')
end_marker = '''                </LinearLayout>
            </LinearLayout>
        </com.v2ray.ang.ui.MobileTinaModeContainer>'''
end = layout.find(end_marker, start)
if end < 0:
    raise SystemExit('manual bottom dock end marker not found')
end += len('                </LinearLayout>\n')

new_bottom = '''                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:background="@color/divider_color_light" />

                <FrameLayout
                    android:layout_width="match_parent"
                    android:layout_height="106dp"
                    android:clipChildren="false"
                    android:clipToPadding="false">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="80dp"
                        android:layout_gravity="bottom"
                        android:background="#2B2B2B"
                        android:gravity="center_horizontal"
                        android:orientation="vertical"
                        android:paddingStart="12dp"
                        android:paddingTop="25dp"
                        android:paddingEnd="12dp"
                        android:paddingBottom="2dp">

                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/btn_smart_connect"
                            android:layout_width="wrap_content"
                            android:layout_height="38dp"
                            android:backgroundTint="#8A4600"
                            android:minHeight="38dp"
                            android:paddingStart="18dp"
                            android:paddingEnd="18dp"
                            android:text="@string/mobiletina_manual_smart_connect_with_icon"
                            android:textAllCaps="false"
                            android:textColor="@android:color/white"
                            app:cornerRadius="20dp" />

                        <LinearLayout
                            android:id="@+id/manual_selected_row"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:gravity="center"
                            android:orientation="horizontal"
                            android:paddingTop="1dp">

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
                    </LinearLayout>

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
                </FrameLayout>
'''
layout = layout[:start] + new_bottom + layout[end:]
layout_path.write_text(layout)

# Smaller rounded rectangles for the Auto/Manual tabs.
bg_path = root / 'res/drawable/mobiletina_mode_tab_background.xml'
bg = bg_path.read_text()
bg = bg.replace('android:insetLeft="6dp"', 'android:insetLeft="10dp"')
bg = bg.replace('android:insetRight="6dp"', 'android:insetRight="10dp"')
bg = bg.replace('android:insetTop="8dp"', 'android:insetTop="6dp"')
bg = bg.replace('android:insetBottom="8dp"', 'android:insetBottom="6dp"')
bg = bg.replace('android:radius="24dp"', 'android:radius="22dp"')
bg_path.write_text(bg)

# TProxyService: allow safe JNI preloading and avoid dumping the complete config
# on the connection critical path.
tproxy_path = root / 'java/com/v2ray/ang/service/TProxyService.kt'
tproxy = tproxy_path.read_text()
tproxy = replace_once(
    tproxy,
    '''        init {
            System.loadLibrary("hev-socks5-tunnel")
        }
    }''',
    '''        init {
            System.loadLibrary("hev-socks5-tunnel")
        }

        /** Forces the JNI class/library to initialize before the first VPN start. */
        @JvmStatic
        fun preloadNative() = Unit
    }''',
    'TProxy preload method',
)
tproxy = replace_once(
    tproxy,
    '        Log.d(AppConfig.TAG, "HevSocks5Tunnel Config content:\\n$configContent")\n\n',
    '',
    'TProxy full config log removal',
)
tproxy_path.write_text(tproxy)
