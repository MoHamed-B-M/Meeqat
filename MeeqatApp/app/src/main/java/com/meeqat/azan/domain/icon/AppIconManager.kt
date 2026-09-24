package com.meeqat.azan.domain.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import com.meeqat.azan.MainActivity

enum class AppIconColor(val key: String, val displayName: String, val color: Color, val aliasSuffix: String?) {
    Emerald("emerald", "Deep Emerald", Color(0xFF0D2C2A), null),
    Teal("teal", "Teal", Color(0xFF1B4D46), "Teal"),
    Gold("gold", "Gold", Color(0xFFD9AD6A), "Gold"),
    Sand("sand", "Warm Sand", Color(0xFF8B7D6B), "Sand");

    companion object {
        fun fromKey(key: String): AppIconColor = entries.find { it.key == key } ?: Emerald
    }
}

class AppIconManager(private val context: Context) {

    fun current(): AppIconColor {
        // Resolve enabled alias by querying PackageManager
        val pm = context.packageManager
        val aliases = listOf(
            "com.meeqat.azan.LauncherGold" to AppIconColor.Gold,
            "com.meeqat.azan.LauncherTeal" to AppIconColor.Teal,
            "com.meeqat.azan.LauncherSand" to AppIconColor.Sand,
            "com.meeqat.azan.LauncherEmerald" to AppIconColor.Emerald,
        )
        for ((alias, color) in aliases) {
            val state = pm.getComponentEnabledSetting(ComponentName(context, alias))
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return color
        }
        // Default is MainActivity (emerald)
        return AppIconColor.Emerald
    }

    fun setIcon(color: AppIconColor) {
        val pm = context.packageManager
        val main = ComponentName(context, MainActivity::class.java)
        val emeraldAlias = ComponentName(context, "com.meeqat.azan.LauncherEmerald")
        val goldAlias = ComponentName(context, "com.meeqat.azan.LauncherGold")
        val tealAlias = ComponentName(context, "com.meeqat.azan.LauncherTeal")
        val sandAlias = ComponentName(context, "com.meeqat.azan.LauncherSand")

        fun set(c: ComponentName, enabled: Boolean) {
            pm.setComponentEnabledSetting(
                c,
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        // Disable all first
        set(emeraldAlias, false); set(goldAlias, false); set(tealAlias, false); set(sandAlias, false)
        when (color) {
            AppIconColor.Emerald -> set(main, true) // default
            AppIconColor.Gold -> { set(main, false); set(goldAlias, true) }
            AppIconColor.Teal -> { set(main, false); set(tealAlias, true) }
            AppIconColor.Sand -> { set(main, false); set(sandAlias, true) }
        }
        // Ensure at least one launcher is enabled
        if (color == AppIconColor.Emerald) {
            // Keep MainActivity enabled
        }
    }
}
