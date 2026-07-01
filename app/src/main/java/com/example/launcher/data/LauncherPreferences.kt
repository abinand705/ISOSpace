package com.example.launcher.data

import android.content.Context
import android.content.SharedPreferences
import com.example.launcher.model.*
import org.json.JSONArray
import org.json.JSONObject

class LauncherPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("isospace_launcher_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SETTINGS_ACCENT = "settings_accent_color"
        private const val KEY_SETTINGS_ICON_PACK = "settings_icon_pack"
        private const val KEY_SETTINGS_DOCK_POS = "settings_dock_position"
        private const val KEY_SETTINGS_DOCK_SIZE = "settings_dock_size"
        private const val KEY_SETTINGS_COLUMNS = "settings_columns"
        private const val KEY_SETTINGS_ROWS = "settings_rows"
        private const val KEY_SETTINGS_ANIM_SPEED = "settings_anim_speed"
        private const val KEY_SETTINGS_AUTOHIDE = "settings_dock_autohide"
        private const val KEY_SETTINGS_LABELS = "settings_show_labels"
        private const val KEY_SETTINGS_WALLPAPER = "settings_wallpaper"
        
        private const val KEY_G_SWIPE_UP = "gesture_swipe_up"
        private const val KEY_G_SWIPE_DOWN = "gesture_swipe_down"
        private const val KEY_G_DOUBLE_TAP = "gesture_double_tap"

        private const val KEY_WIDGETS = "launcher_widgets_json"
        private const val KEY_APPS_CUSTOM = "launcher_apps_custom_json"
        
        // Custom user-defined app categories
        private const val KEY_CUSTOM_CATEGORIES = "launcher_custom_categories"

        private const val KEY_CUSTOM_ACCENT = "custom_accent_color_hex"
        private const val KEY_CUSTOM_BG = "custom_bg_color_hex"
        private const val KEY_USE_CUSTOM_COLORS = "use_custom_colors"
        private const val KEY_KERNEL_BOOT_LOGS = "kernel_boot_logs_enabled"
        private const val KEY_TERMINAL_PW = "terminal_password"
        private const val KEY_TURBO_PERFORMANCE = "turbo_performance_mode"
        private const val KEY_EDGE_SWIPE_SENSITIVITY = "edge_swipe_sensitivity"
    }

    // Default widgets list for ISOSpace Launcher on first boot
    private fun getDefaultWidgets(): List<WidgetConfig> {
        return listOf(
            WidgetConfig("w_clock", WidgetType.ISOSPACE_CLOCK, xGrid = 0, yGrid = 0, widthSpan = 4, heightSpan = 1),
            WidgetConfig("w_sys", WidgetType.SYSTEM_MONITOR, xGrid = 0, yGrid = 1, widthSpan = 2, heightSpan = 2),
            WidgetConfig("w_bash", WidgetType.BASH_TERMINAL, xGrid = 2, yGrid = 1, widthSpan = 2, heightSpan = 2, metadata = "# echo 'Welcome to ISOSpace!'\n- Dynamic workspace configured.\n- Tap settings to customize.")
        )
    }

    fun loadSettings(): LauncherSettings {
        val accentStr = prefs.getString(KEY_SETTINGS_ACCENT, AccentColor.ISOSPACE_ORANGE.name) ?: AccentColor.ISOSPACE_ORANGE.name
        val iconPackStr = prefs.getString(KEY_SETTINGS_ICON_PACK, IconPack.YARU_CLASSIC.name) ?: IconPack.YARU_CLASSIC.name
        val dockPosStr = prefs.getString(KEY_SETTINGS_DOCK_POS, DockPosition.LEFT.name) ?: DockPosition.LEFT.name
        
        val accent = try { AccentColor.valueOf(accentStr) } catch (e: Exception) { AccentColor.ISOSPACE_ORANGE }
        val iconPack = try { IconPack.valueOf(iconPackStr) } catch (e: Exception) { IconPack.YARU_CLASSIC }
        val dockPos = try { DockPosition.valueOf(dockPosStr) } catch (e: Exception) { DockPosition.LEFT }

        val swipeUpStr = prefs.getString(KEY_G_SWIPE_UP, GestureAction.OPEN_DRAWER.name) ?: GestureAction.OPEN_DRAWER.name
        val swipeDownStr = prefs.getString(KEY_G_SWIPE_DOWN, GestureAction.OPEN_CONTROL_CENTER.name) ?: GestureAction.OPEN_CONTROL_CENTER.name
        val doubleTapStr = prefs.getString(KEY_G_DOUBLE_TAP, GestureAction.OPEN_SETTINGS.name) ?: GestureAction.OPEN_SETTINGS.name

        return LauncherSettings(
            accentColor = accent,
            iconPack = iconPack,
            dockPosition = dockPos,
            dockIconSizeDp = prefs.getInt(KEY_SETTINGS_DOCK_SIZE, 54),
            desktopColumns = prefs.getInt(KEY_SETTINGS_COLUMNS, 4),
            desktopRows = prefs.getInt(KEY_SETTINGS_ROWS, 5),
            animationSpeedMultiplier = prefs.getFloat(KEY_SETTINGS_ANIM_SPEED, 1.0f),
            isDockAutohideEnabled = prefs.getBoolean(KEY_SETTINGS_AUTOHIDE, false),
            showIconLabels = prefs.getBoolean(KEY_SETTINGS_LABELS, true),
            backgroundWallpaperUrl = prefs.getString(KEY_SETTINGS_WALLPAPER, "dynamic_gradient") ?: "dynamic_gradient",
            gestureSwipeUp = try { GestureAction.valueOf(swipeUpStr) } catch(e: Exception) { GestureAction.OPEN_DRAWER },
            gestureSwipeDown = try { GestureAction.valueOf(swipeDownStr) } catch(e: Exception) { GestureAction.OPEN_CONTROL_CENTER },
            gestureDoubleTap = try { GestureAction.valueOf(doubleTapStr) } catch(e: Exception) { GestureAction.OPEN_SETTINGS },
            customAccentColorHex = prefs.getLong(KEY_CUSTOM_ACCENT, 0xFFE95420),
            customBgColorHex = prefs.getLong(KEY_CUSTOM_BG, 0xFF300A24),
            useCustomColors = prefs.getBoolean(KEY_USE_CUSTOM_COLORS, false),
            kernelBootLogsEnabled = prefs.getBoolean(KEY_KERNEL_BOOT_LOGS, false),
            terminalPassword = prefs.getString(KEY_TERMINAL_PW, "isospaceroot") ?: "isospaceroot",
            turboPerformanceMode = prefs.getBoolean(KEY_TURBO_PERFORMANCE, false),
            edgeSwipeSensitivity = prefs.getInt(KEY_EDGE_SWIPE_SENSITIVITY, 14),
            dockColorHex = prefs.getLong("settings_dock_color", 0xE61A1A1A)
        )
    }

    fun saveSettings(settings: LauncherSettings) {
        prefs.edit()
            .putString(KEY_SETTINGS_ACCENT, settings.accentColor.name)
            .putString(KEY_SETTINGS_ICON_PACK, settings.iconPack.name)
            .putString(KEY_SETTINGS_DOCK_POS, settings.dockPosition.name)
            .putInt(KEY_SETTINGS_DOCK_SIZE, settings.dockIconSizeDp)
            .putInt(KEY_SETTINGS_COLUMNS, settings.desktopColumns)
            .putInt(KEY_SETTINGS_ROWS, settings.desktopRows)
            .putFloat(KEY_SETTINGS_ANIM_SPEED, settings.animationSpeedMultiplier)
            .putBoolean(KEY_SETTINGS_AUTOHIDE, settings.isDockAutohideEnabled)
            .putBoolean(KEY_SETTINGS_LABELS, settings.showIconLabels)
            .putString(KEY_SETTINGS_WALLPAPER, settings.backgroundWallpaperUrl)
            .putString(KEY_G_SWIPE_UP, settings.gestureSwipeUp.name)
            .putString(KEY_G_SWIPE_DOWN, settings.gestureSwipeDown.name)
            .putString(KEY_G_DOUBLE_TAP, settings.gestureDoubleTap.name)
            .putLong(KEY_CUSTOM_ACCENT, settings.customAccentColorHex)
            .putLong(KEY_CUSTOM_BG, settings.customBgColorHex)
            .putBoolean(KEY_USE_CUSTOM_COLORS, settings.useCustomColors)
            .putBoolean(KEY_KERNEL_BOOT_LOGS, settings.kernelBootLogsEnabled)
            .putString(KEY_TERMINAL_PW, settings.terminalPassword)
            .putBoolean(KEY_TURBO_PERFORMANCE, settings.turboPerformanceMode)
            .putInt(KEY_EDGE_SWIPE_SENSITIVITY, settings.edgeSwipeSensitivity)
            .putLong("settings_dock_color", settings.dockColorHex)
            .apply()
    }

    fun loadWidgets(): List<WidgetConfig> {
        val jsonStr = prefs.getString(KEY_WIDGETS, null)
        if (jsonStr.isNullOrEmpty()) {
            return getDefaultWidgets()
        }
        val list = mutableListOf<WidgetConfig>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val typeStr = obj.getString("type")
                val xGrid = obj.getInt("xGrid")
                val yGrid = obj.getInt("yGrid")
                val widthSpan = obj.optInt("widthSpan", 2)
                val heightSpan = obj.optInt("heightSpan", 2)
                val metadata = obj.optString("metadata", "")
                
                val type = try { WidgetType.valueOf(typeStr) } catch(e: Exception) { WidgetType.ISOSPACE_CLOCK }
                list.add(WidgetConfig(id, type, xGrid, yGrid, widthSpan, heightSpan, metadata))
            }
        } catch (e: Exception) {
            return getDefaultWidgets()
        }
        return list
    }

    fun saveWidgets(widgets: List<WidgetConfig>) {
        try {
            val array = JSONArray()
            for (w in widgets) {
                val obj = JSONObject()
                obj.put("id", w.id)
                obj.put("type", w.type.name)
                obj.put("xGrid", w.xGrid)
                obj.put("yGrid", w.yGrid)
                obj.put("widthSpan", w.widthSpan)
                obj.put("heightSpan", w.heightSpan)
                obj.put("metadata", w.metadata)
                array.put(obj)
            }
            prefs.edit().putString(KEY_WIDGETS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCustomApps(): List<AppItem> {
        val jsonStr = prefs.getString(KEY_APPS_CUSTOM, null)
        if (jsonStr.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<AppItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(AppItem(
                    packageName = obj.getString("packageName"),
                    label = obj.getString("label"),
                    originalLabel = obj.optString("originalLabel", obj.getString("label")),
                    category = obj.getString("category"),
                    customIconResId = if (obj.isNull("customIconResId")) null else obj.getString("customIconResId"),
                    customColor = if (obj.isNull("customColor")) null else obj.getLong("customColor"),
                    isHidden = obj.optBoolean("isHidden", false),
                    counter = obj.optInt("counter", 0)
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveCustomApps(apps: List<AppItem>) {
        try {
            val array = JSONArray()
            for (app in apps) {
                val obj = JSONObject()
                obj.put("packageName", app.packageName)
                obj.put("label", app.label)
                obj.put("originalLabel", app.originalLabel)
                obj.put("category", app.category)
                obj.put("customIconResId", app.customIconResId ?: JSONObject.NULL)
                obj.put("customColor", app.customColor ?: JSONObject.NULL)
                obj.put("isHidden", app.isHidden)
                obj.put("counter", app.counter)
                array.put(obj)
            }
            prefs.edit().putString(KEY_APPS_CUSTOM, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCustomCategories(): List<String> {
        val raw = prefs.getString(KEY_CUSTOM_CATEGORIES, null)
        if (raw.isNullOrEmpty()) {
            return listOf("Entertainment", "Work", "Productivity")
        }
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            return listOf("Entertainment", "Work", "Productivity")
        }
        return list
    }

    fun saveCustomCategories(categories: List<String>) {
        try {
            val array = JSONArray()
            for (cat in categories) {
                array.put(cat)
            }
            prefs.edit().putString(KEY_CUSTOM_CATEGORIES, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadEnabledDeviceApps(): Set<String> {
        return prefs.getStringSet("enabled_device_apps", null) ?: emptySet()
    }

    fun saveEnabledDeviceApps(packages: Set<String>) {
        prefs.edit().putStringSet("enabled_device_apps", packages).apply()
    }

    fun loadUninstalledVirtualApps(): Set<String> {
        return prefs.getStringSet("uninstalled_virtual_apps", emptySet()) ?: emptySet()
    }

    fun saveUninstalledVirtualApps(packages: Set<String>) {
        prefs.edit().putStringSet("uninstalled_virtual_apps", packages).apply()
    }

    fun loadDockApps(): List<String> {
        val defaultDock = listOf(
            "com.dummy.gallery",
            "com.dummy.notes",
            "com.dummy.files",
            "com.dummy.settings",
            "com.dummy.terminal",
            "com.dummy.vlc"
        )
        val jsonStr = prefs.getString("dock_apps_list_json", null)
        if (jsonStr.isNullOrEmpty()) {
            return defaultDock
        }
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            return defaultDock
        }
        return list
    }

    fun saveDockApps(dockApps: List<String>) {
        try {
            val array = JSONArray()
            for (pkg in dockApps) {
                array.put(pkg)
            }
            prefs.edit().putString("dock_apps_list_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Sandbox Security & Hidden Encrypted Folder Support ---
    fun isSandboxRegistered(): Boolean {
        return prefs.getBoolean("sandbox_registered", false)
    }

    fun registerSandbox(username: String, pass: String, sizeLimitGb: Int, question: String = "", answer: String = ""): Boolean {
        return prefs.edit()
            .putBoolean("sandbox_registered", true)
            .putString("sandbox_username", username)
            .putString("sandbox_password", pass)
            .putInt("sandbox_size_limit_gb", sizeLimitGb)
            .putString("sandbox_security_question", question)
            .putString("sandbox_security_answer", answer)
            .commit()
    }

    fun verifySandbox(username: String, pass: String): Boolean {
        val savedUser = prefs.getString("sandbox_username", "") ?: ""
        val savedPass = prefs.getString("sandbox_password", "") ?: ""
        return username.trim() == savedUser && pass == savedPass
    }

    fun getSandboxUsername(): String {
        return prefs.getString("sandbox_username", "") ?: ""
    }

    fun getSandboxPassword(): String {
        return prefs.getString("sandbox_password", "") ?: ""
    }

    fun getSandboxSecurityQuestion(): String {
        return prefs.getString("sandbox_security_question", "First pet's name?") ?: "First pet's name?"
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val savedAnswer = prefs.getString("sandbox_security_answer", "") ?: ""
        if (savedAnswer.isBlank()) return true
        return answer.trim().lowercase() == savedAnswer.trim().lowercase()
    }

    fun resetSandboxPassword(newPass: String): Boolean {
        return prefs.edit()
            .putString("sandbox_password", newPass)
            .commit()
    }

    fun updateSandboxCredentials(username: String, pass: String): Boolean {
        return prefs.edit()
            .putString("sandbox_username", username)
            .putString("sandbox_password", pass)
            .commit()
    }

    fun getSandboxSizeLimitGb(): Int {
        return prefs.getInt("sandbox_size_limit_gb", 10)
    }

    fun isTutorialCompleted(): Boolean {
        return prefs.getBoolean("gesture_tutorial_completed", false)
    }

    fun setTutorialCompleted(completed: Boolean): Boolean {
        return prefs.edit().putBoolean("gesture_tutorial_completed", completed).commit()
    }
}
