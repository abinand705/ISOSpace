package com.example.launcher.model

import androidx.compose.ui.graphics.vector.ImageVector

// Core App representation
data class AppItem(
    val packageName: String,
    val label: String,
    val originalLabel: String = label,
    val category: String, // "System", "Social", "Media", "Utilities", "Games", "Development", "Custom"
    val customIconResId: String? = null, // Custom visual icon indicator
    val customColor: Long? = null, // Hex color overlay if requested
    val isHidden: Boolean = false,
    val counter: Int = 0 // Notification badge simulation
)

enum class WidgetType {
    SYSTEM_MONITOR,   // Displays CPU (simulated), RAM, Storage, Temp
    ISOSPACE_CLOCK,     // Classic bold/semilight ISOSpace style clock & calendar
    BASH_TERMINAL,    // Quick Notes terminal style with keyboard input
    WEATHER_INFO,     // Desktop micro-weather widget
    QUICK_FOLDER      // Customizable app folder widget
}

data class WidgetConfig(
    val id: String,
    val type: WidgetType,
    val xGrid: Int,
    val yGrid: Int,
    val widthSpan: Int = 2,
    val heightSpan: Int = 2,
    val metadata: String = "" // Custom configuration like notes text or zip/city
)

enum class LauncherGesture {
    SWIPE_UP,      // Default: Open App Drawer (GNOME Dash)
    SWIPE_DOWN,    // Default: Open Control Center / System Menu
    DOUBLE_TAP,    // Default: Open Launcher Settings
    LONG_PRESS,    // Default: Organize Widgets
    TWO_FINGER_PINCH // Default: Clear Desktop Workspace
}

enum class GestureAction {
    OPEN_DRAWER,
    OPEN_CONTROL_CENTER,
    OPEN_SETTINGS,
    LOCK_SCREEN,
    TOGGLE_DOCK_REVEAL,
    MINIMIZE_ALL,
    DO_NOTHING
}

enum class DockPosition {
    LEFT,
    RIGHT,
    BOTTOM,
    HIDDEN
}

enum class AccentColor(val label: String, val hexAccent: Long, val hexThemeBg: Long) {
    MISTY_MOUNTAIN("Misty Alpine", 0xFF527E5C, 0xFF1B2C1E),
    ISOSPACE_ORANGE("ISOSpace Orange", 0xFFE95420, 0xFF300A24),
    AUBERGINE_SLATE("Warm Aubergine", 0xFF77216F, 0xFF2C001E),
    ROYAL_CYAN("GNOME Cyan", 0xFF125B5C, 0xFF1C1C1C),
    EMERALD_GREEN("Mint Minty", 0xFF2E7D32, 0xFF1A2E1A),
    SUNFLOWER_YELLOW("Yaru Gold", 0xFFF57C00, 0xFF2D2319)
}

enum class IconPack(val label: String) {
    YARU_CLASSIC("ISOSpace Default"),
    NEON_FLAT("Neon (Glow & Square)"),
    RETRO_GNOME("Retro (GNOME 2 Classic)"),
    OUTLINE_STYLISH("Minimal Outline"),
    MATERIAL_ROUNDED("Material Rounded")
}

data class LauncherSettings(
    val accentColor: AccentColor = AccentColor.MISTY_MOUNTAIN,
    val iconPack: IconPack = IconPack.YARU_CLASSIC,
    val dockPosition: DockPosition = DockPosition.LEFT,
    val dockIconSizeDp: Int = 54,
    val desktopColumns: Int = 4,
    val desktopRows: Int = 5,
    val animationSpeedMultiplier: Float = 1.0f,
    val isBlurEnabled: Boolean = true,
    val isDockAutohideEnabled: Boolean = false,
    val gestureSwipeUp: GestureAction = GestureAction.OPEN_DRAWER,
    val gestureSwipeDown: GestureAction = GestureAction.OPEN_CONTROL_CENTER,
    val gestureDoubleTap: GestureAction = GestureAction.OPEN_SETTINGS,
    val backgroundWallpaperUrl: String = "misty_mountain", // Builtin canvas, url, or simple
    val showIconLabels: Boolean = true,
    val customAccentColorHex: Long = 0xFFE95420,
    val customBgColorHex: Long = 0xFF300A24,
    val useCustomColors: Boolean = false,
    val kernelBootLogsEnabled: Boolean = false,
    val terminalPassword: String = "isoroot",
    val turboPerformanceMode: Boolean = false,
    val edgeSwipeSensitivity: Int = 14,
    val dockColorHex: Long = 0xE61A1A1A
) {
    val activeAccentHex: Long
        get() = if (useCustomColors) customAccentColorHex else accentColor.hexAccent

    val activeThemeBgHex: Long
        get() = if (useCustomColors) customBgColorHex else accentColor.hexThemeBg
}
