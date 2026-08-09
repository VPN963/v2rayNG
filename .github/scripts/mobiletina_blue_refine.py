from pathlib import Path
import re

root = Path('V2rayNG/app/src/main')
BLUE = '#1976D2'
BLUE_LIGHT = '#90CAF9'
BLUE_DARK = '#0D47A1'

# activity_main.xml
p = root / 'res/layout/activity_main.xml'
s = p.read_text()
s = s.replace('app:indicatorColor="#B85B00"', f'app:indicatorColor="{BLUE}"')
s = s.replace('android:layout_marginStart="20dp"\n                            android:layout_marginEnd="6dp"', 'android:layout_marginStart="4dp"\n                            android:layout_marginEnd="4dp"')
s = s.replace('android:layout_marginStart="6dp"\n                            android:layout_marginEnd="20dp"', 'android:layout_marginStart="4dp"\n                            android:layout_marginEnd="4dp"')
s = s.replace('android:layout_width="156dp"\n                        android:layout_height="156dp"', 'android:layout_width="172dp"\n                        android:layout_height="172dp"')
s = s.replace('app:fabCustomSize="156dp"\n                        app:maxImageSize="58dp"', 'app:fabCustomSize="172dp"\n                        app:maxImageSize="64dp"')
s = s.replace('app:tabIndicatorColor="#FF6D00"', f'app:tabIndicatorColor="{BLUE}"')
s = s.replace('android:layout_height="128dp"', 'android:layout_height="134dp"', 1)
s = s.replace('android:layout_height="96dp"\n                        android:layout_gravity="bottom"', 'android:layout_height="102dp"\n                        android:layout_gravity="bottom"', 1)
s = s.replace('android:layout_height="36dp"\n                        android:layout_gravity="top|center_horizontal"\n                        android:layout_marginTop="60dp"\n                        android:backgroundTint="#8A4600"\n                        android:minHeight="36dp"', f'android:layout_height="40dp"\n                        android:layout_gravity="top|center_horizontal"\n                        android:layout_marginTop="62dp"\n                        android:backgroundTint="{BLUE}"\n                        android:minHeight="40dp"')
s = s.replace('app:cornerRadius="19dp"', 'app:cornerRadius="20dp"', 1)
s = s.replace('android:layout_marginTop="99dp"', 'android:layout_marginTop="106dp"', 1)
p.write_text(s)

# colors.xml
p = root / 'res/values/colors.xml'
s = p.read_text()
s = s.replace('<color name="colorConfigType">#f97910</color>', f'<color name="colorConfigType">{BLUE}</color>')
s = s.replace('<color name="color_fab_active">#f97910</color>', f'<color name="color_fab_active">{BLUE}</color>')
s = s.replace('<color name="md_theme_secondary">#f97910</color>', f'<color name="md_theme_secondary">{BLUE}</color>')
s = s.replace('<color name="md_theme_secondaryContainer">#FFE8D6</color>', '<color name="md_theme_secondaryContainer">#D6E9FF</color>')
s = s.replace('<color name="md_theme_onSecondaryContainer">#2B1700</color>', '<color name="md_theme_onSecondaryContainer">#001D36</color>')
p.write_text(s)

# values-night/colors.xml
p = root / 'res/values-night/colors.xml'
s = p.read_text()
s = s.replace('<color name="color_fab_active">#f97910</color>', f'<color name="color_fab_active">{BLUE}</color>')
s = s.replace('<color name="md_theme_secondary">#FFB687</color>', f'<color name="md_theme_secondary">{BLUE_LIGHT}</color>')
s = s.replace('<color name="md_theme_onSecondary">#4E2600</color>', '<color name="md_theme_onSecondary">#003258</color>')
s = s.replace('<color name="md_theme_secondaryContainer">#6F3800</color>', f'<color name="md_theme_secondaryContainer">{BLUE_DARK}</color>')
s = s.replace('<color name="md_theme_onSecondaryContainer">#FFE8D6</color>', '<color name="md_theme_onSecondaryContainer">#D6E9FF</color>')
p.write_text(s)

# MainRecyclerAdapter: selected indicator orange -> blue resource
p = root / 'java/com/v2ray/ang/ui/MainRecyclerAdapter.kt'
s = p.read_text()
s = s.replace('if (guid == MmkvManager.getSelectServer()) Color.rgb(255, 109, 0) else Color.TRANSPARENT', 'if (guid == MmkvManager.getSelectServer()) ContextCompat.getColor(context, R.color.color_fab_active) else Color.TRANSPARENT')
p.write_text(s)

# MainActivity: hide idle ping/server below Auto FAB.
p = root / 'java/com/v2ray/ang/ui/MainActivity.kt'
s = p.read_text()
needle = '''        binding.tvAutoStatus.text = status
        binding.tvAutoPing.text = when {
            smartConnecting && smartCountdownSeconds > 0 -> smartCountdownSeconds.toString()
            smartConnecting -> getString(R.string.mobiletina_testing)
            running && !lastConnectedPing.isNullOrBlank() -> lastConnectedPing
            ping > 0L -> ping.toString()
            ping < 0L -> getString(R.string.mobiletina_ping_inactive)
            profile != null -> getString(R.string.mobiletina_tap_for_ping)
            else -> getString(R.string.mobiletina_ping_unknown)
        }
'''
replacement = '''        binding.tvAutoStatus.text = status
        val showAutoDetails = running || smartConnecting
        binding.tvAutoPing.visibility = if (showAutoDetails) View.VISIBLE else View.GONE
        binding.tvAutoServer.visibility = if (showAutoDetails) View.VISIBLE else View.GONE
        binding.tvAutoServer.text = if (showAutoDetails) profile?.remarks.orEmpty() else ""
        binding.tvAutoPing.text = when {
            smartConnecting && smartCountdownSeconds > 0 -> smartCountdownSeconds.toString()
            smartConnecting -> getString(R.string.mobiletina_testing)
            running && !lastConnectedPing.isNullOrBlank() -> lastConnectedPing
            running && ping > 0L -> ping.toString()
            running && ping < 0L -> getString(R.string.mobiletina_ping_inactive)
            running -> getString(R.string.mobiletina_tap_for_ping)
            else -> ""
        }
'''
if needle not in s:
    raise SystemExit('Auto detail block not found')
s = s.replace(needle, replacement, 1)
p.write_text(s)

print('Applied MobileTina blue refinement patch')
