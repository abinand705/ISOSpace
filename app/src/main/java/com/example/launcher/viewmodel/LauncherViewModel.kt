package com.example.launcher.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.Context
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothAdapter
import android.media.AudioManager
import android.provider.Settings
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.data.LauncherPreferences
import com.example.launcher.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = LauncherPreferences(application)

    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _widgets = MutableStateFlow<List<WidgetConfig>>(emptyList())
    val widgets: StateFlow<List<WidgetConfig>> = _widgets.asStateFlow()

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val _installedDeviceApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedDeviceApps: StateFlow<List<AppItem>> = _installedDeviceApps.asStateFlow()

    private val _enabledDeviceApps = MutableStateFlow<Set<String>>(emptySet())
    val enabledDeviceApps: StateFlow<Set<String>> = _enabledDeviceApps.asStateFlow()

    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    private val _dockApps = MutableStateFlow<List<String>>(emptyList())
    val dockApps: StateFlow<List<String>> = _dockApps.asStateFlow()

    // UI overlays state
    private val _isDashOpen = MutableStateFlow(false)
    val isDashOpen: StateFlow<Boolean> = _isDashOpen.asStateFlow()

    private val _isControlCenterOpen = MutableStateFlow(false)
    val isControlCenterOpen: StateFlow<Boolean> = _isControlCenterOpen.asStateFlow()

    private val _isCustomizerOpen = MutableStateFlow(false)
    val isCustomizerOpen: StateFlow<Boolean> = _isCustomizerOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Control Center Simulated & Real States
    private val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val bluetoothManager = application.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val audioManager = application.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _brightness = MutableStateFlow(
        try {
            Settings.System.getInt(application.contentResolver, Settings.System.SCREEN_BRIGHTNESS).toFloat() / 255f
        } catch (e: Exception) {
            0.8f
        }
    )
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _volume = MutableStateFlow(
        audioManager?.let {
            val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) it.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max else 0.7f
        } ?: 0.7f
    )
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isWifiEnabled = MutableStateFlow(wifiManager?.isWifiEnabled ?: true)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothManager?.adapter?.isEnabled ?: false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _terminalNotesHistory = MutableStateFlow<List<String>>(emptyList())
    val terminalNotesHistory: StateFlow<List<String>> = _terminalNotesHistory.asStateFlow()

    private val _isSandboxUnlocked = MutableStateFlow(false)
    val isSandboxUnlocked: StateFlow<Boolean> = _isSandboxUnlocked.asStateFlow()

    private val _openedApps = MutableStateFlow<List<String>>(emptyList())
    val openedApps: StateFlow<List<String>> = _openedApps.asStateFlow()

    fun openApp(packageName: String) {
        val current = _openedApps.value.toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            _openedApps.value = current
        }
    }

    fun closeApp(packageName: String) {
        val current = _openedApps.value.toMutableList()
        if (current.remove(packageName)) {
            _openedApps.value = current
        }
    }

    fun clearOpenedApps() {
        _openedApps.value = emptyList()
    }

    fun logout() {
        _isSandboxUnlocked.value = false
        clearOpenedApps()
        _terminalNotesHistory.value = emptyList()
        _searchQuery.value = ""
        _isDashOpen.value = false
        _isControlCenterOpen.value = false
        _isCustomizerOpen.value = false
    }

    fun isSandboxRegistered(): Boolean {
        return prefs.isSandboxRegistered()
    }

    fun getSandboxUsername(): String {
        return prefs.getSandboxUsername()
    }

    fun getSandboxPassword(): String {
        return prefs.getSandboxPassword()
    }

    fun updateSandboxCredentials(username: String, pass: String): Boolean {
        return prefs.updateSandboxCredentials(username, pass)
    }

    fun getSandboxSizeLimitGb(): Int {
        return prefs.getSandboxSizeLimitGb()
    }

    fun registerSandbox(username: String, pass: String, sizeLimitGb: Int, question: String = "", answer: String = ""): Boolean {
        val success = prefs.registerSandbox(username, pass, sizeLimitGb, question, answer)
        if (success) {
            val context = getApplication<Application>()
            val sandboxDir = context.filesDir.resolve(".isospace_encrypted_sandbox")
            if (!sandboxDir.exists()) {
                sandboxDir.mkdirs()
            }
        }
        return success
    }

    fun getSandboxSecurityQuestion(): String {
        return prefs.getSandboxSecurityQuestion()
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        return prefs.verifySecurityAnswer(answer)
    }

    fun resetSandboxPassword(newPass: String): Boolean {
        return prefs.resetSandboxPassword(newPass)
    }

    fun verifySandbox(username: String, pass: String): Boolean {
        val success = prefs.verifySandbox(username, pass)
        if (success) {
            val context = getApplication<Application>()
            val sandboxDir = context.filesDir.resolve(".isospace_encrypted_sandbox")
            if (!sandboxDir.exists()) {
                sandboxDir.mkdirs()
            }
        }
        return success
    }

    fun unlockSandbox() {
        _isSandboxUnlocked.value = true
    }

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _settings.value = prefs.loadSettings()
            _widgets.value = prefs.loadWidgets()
            _customCategories.value = prefs.loadCustomCategories()
            _dockApps.value = prefs.loadDockApps()
            
            // Build / fetch custom applications list and scan device
            scanDeviceApps()
        }
    }

    private fun getDeviceInstalledApps(): List<AppItem> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return try {
            val list = pm.queryIntentActivities(intent, 0)
            list.mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == getApplication<Application>().packageName) return@mapNotNull null
                
                val label = resolveInfo.loadLabel(pm).toString()
                AppItem(
                    packageName = packageName,
                    label = label,
                    originalLabel = label,
                    category = "Applications",
                    isHidden = false
                )
            }.distinctBy { it.packageName }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun setDeviceAppEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = _enabledDeviceApps.value.toMutableSet()
            if (enabled) {
                current.add(packageName)
            } else {
                current.remove(packageName)
            }
            _enabledDeviceApps.value = current
            prefs.saveEnabledDeviceApps(current)
            scanDeviceApps()
        }
    }

    private fun scanDeviceApps() {
        val defaultApps = listOf(
            AppItem("com.android.vending", "Play Store", "Play Store", "Applications", null, 0xFF00E676, false, 0),
            AppItem("com.dummy.gallery", "Gallery", "Gallery", "Applications", null, 0xFF9C27B0, false, 0),
            AppItem("com.dummy.notes", "Notes", "Notes", "Applications", null, 0xFFFF9800, false, 0),
            AppItem("com.dummy.files", "Files", "Files", "Applications", null, 0xFF0288D1, false, 0),
            AppItem("com.dummy.camera", "Camera", "Camera", "Applications", null, 0xFF455A64, false, 0),
            AppItem("com.dummy.clock", "Clock", "Clock", "Applications", null, 0xFF26A69A, false, 0),
            AppItem("com.dummy.settings", "Settings", "Settings", "Applications", null, 0xFF77216F, false, 0),
            AppItem("com.dummy.terminal", "Terminal", "Terminal", "Applications", null, 0xFF333333, false, 0),
            AppItem("com.dummy.vlc", "VLC", "VLC", "Applications", null, 0xFFFF5722, false, 0),
            AppItem("com.dummy.browser", "Web Browser", "Web Browser", "Applications", null, 0xFFE05A10, false, 0),
            AppItem("com.dummy.calculator", "Calculator", "Calculator", "Applications", null, 0xFF2E7D32, false, 0),
            AppItem("com.dummy.telegram", "Telegram", "Telegram", "Applications", null, 0xFF2196F3, true, 0),
            AppItem("com.dummy.instagram", "Instagram", "Instagram", "Applications", null, 0xFFE1306C, true, 0),
            AppItem("com.dummy.whatsapp", "WhatsApp", "WhatsApp", "Applications", null, 0xFF25D366, true, 0),
            AppItem("com.dummy.youtube", "YouTube", "YouTube", "Applications", null, 0xFFFF0000, true, 0),
            AppItem("com.dummy.spotify", "Spotify", "Spotify", "Applications", null, 0xFF1DB954, true, 0),
            AppItem("com.dummy.maps", "Google Maps", "Google Maps", "Applications", null, 0xFF4CAF50, true, 0),
            AppItem("com.dummy.gmail", "Gmail", "Gmail", "Applications", null, 0xFFD44638, true, 0),
            AppItem("com.dummy.discord", "Discord", "Discord", "Applications", null, 0xFF5865F2, true, 0),
            AppItem("com.dummy.reddit", "Reddit", "Reddit", "Applications", null, 0xFFFF4500, true, 0)
        )

        val uninstalledVirtual = prefs.loadUninstalledVirtualApps()
        val defaultAppsFiltered = defaultApps.filter { it.packageName !in uninstalledVirtual }

        val physicalApps = getDeviceInstalledApps()
        _installedDeviceApps.value = physicalApps

        val enabledPackages = prefs.loadEnabledDeviceApps()
        _enabledDeviceApps.value = enabledPackages

        val visiblePhysicalApps = physicalApps.filter { it.packageName in enabledPackages }.map { app ->
            app.copy(
                packageName = "clone.${app.packageName}",
                label = "Cloned ${app.label}",
                originalLabel = "Cloned ${app.label}",
                customColor = 0xFF455A64L
            )
        }
        val combinedApps = defaultAppsFiltered + visiblePhysicalApps

        val customSaved = prefs.loadCustomApps()
        val customSavedMap = customSaved.associateBy { it.packageName }

        val newList = combinedApps.map { app ->
            val saved = customSavedMap[app.packageName]
            if (saved != null) {
                saved.copy(
                    packageName = app.packageName,
                    originalLabel = app.originalLabel
                )
            } else {
                app
            }
        }

        _apps.value = newList
    }

    // Settings Modification
    fun updateSettings(newSettings: LauncherSettings) {
        _settings.value = newSettings
        prefs.saveSettings(newSettings)
    }

    fun setAccentColor(accent: AccentColor) {
        updateSettings(_settings.value.copy(accentColor = accent))
    }

    fun setIconPack(pack: IconPack) {
        updateSettings(_settings.value.copy(iconPack = pack))
    }

    fun setDockPosition(pos: DockPosition) {
        updateSettings(_settings.value.copy(dockPosition = pos))
    }

    fun setDesktopLayout(cols: Int, rows: Int) {
        updateSettings(_settings.value.copy(desktopColumns = cols, desktopRows = rows))
    }

    fun setWallpaper(url: String) {
        updateSettings(_settings.value.copy(backgroundWallpaperUrl = url))
    }

    fun setDockAutohide(enabled: Boolean) {
        updateSettings(_settings.value.copy(isDockAutohideEnabled = enabled))
    }

    fun setShowIconLabels(enabled: Boolean) {
        updateSettings(_settings.value.copy(showIconLabels = enabled))
    }

    fun setGestures(up: GestureAction, down: GestureAction, tap: GestureAction) {
        updateSettings(_settings.value.copy(
            gestureSwipeUp = up,
            gestureSwipeDown = down,
            gestureDoubleTap = tap
        ))
    }

    // Category Management
    fun addCategory(catName: String) {
        if (catName.isBlank()) return
        val current = _customCategories.value.toMutableList()
        if (!current.contains(catName)) {
            current.add(catName)
            _customCategories.value = current
            prefs.saveCustomCategories(current)
        }
    }

    fun deleteCategory(catName: String) {
        val current = _customCategories.value.toMutableList()
        if (current.remove(catName)) {
            _customCategories.value = current
            prefs.saveCustomCategories(current)
            
            // Re-assign apps in that category back to "System" or "Utilities"
            val updatedApps = _apps.value.map { app ->
                if (app.category == catName) {
                    app.copy(category = "Utilities")
                } else app
            }
            _apps.value = updatedApps
            saveCustomAppsState()
        }
    }

    // App Customization
    fun customizeApp(packageName: String, newLabel: String, category: String, isHidden: Boolean) {
        val updatedApps = _apps.value.map { app ->
            if (app.packageName == packageName) {
                app.copy(label = newLabel, category = category, isHidden = isHidden)
            } else app
        }
        _apps.value = updatedApps
        saveCustomAppsState()
        scanDeviceApps()
    }

    fun clearNotificationBadge(packageName: String) {
        _apps.value = _apps.value.map { app ->
            if (app.packageName == packageName) {
                app.copy(counter = 0)
            } else app
        }
        saveCustomAppsState()
    }

    private fun saveCustomAppsState() {
        val onlyCustom = _apps.value.filter { app ->
            app.packageName.startsWith("com.dummy") ||
            app.packageName.startsWith("com.isospace") ||
            app.label != app.originalLabel || 
            app.category != "Applications" || 
            app.isHidden ||
            app.customIconResId != null ||
            app.customColor != null
        }
        prefs.saveCustomApps(onlyCustom)
    }

    fun pinToDock(packageName: String) {
        val current = _dockApps.value.toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            _dockApps.value = current
            prefs.saveDockApps(current)
        }
    }

    fun unpinFromDock(packageName: String) {
        val current = _dockApps.value.toMutableList()
        if (current.remove(packageName)) {
            _dockApps.value = current
            prefs.saveDockApps(current)
        }
    }

    fun uninstallVirtualApp(packageName: String) {
        val current = prefs.loadUninstalledVirtualApps().toMutableSet()
        current.add(packageName)
        prefs.saveUninstalledVirtualApps(current)
        unpinFromDock(packageName)
        scanDeviceApps()
    }

    // Widgets Management
    fun addWidget(type: WidgetType) {
        val current = _widgets.value.toMutableList()
        val id = "w_${type.name.lowercase()}_${UUID.randomUUID().toString().take(4)}"
        
        // Find first empty cell or slot on grid
        val columns = _settings.value.desktopColumns
        val rows = _settings.value.desktopRows
        
        // Simple placement algorithm: scan cells
        var placed = false
        for (y in 0 until rows) {
            for (x in 0 until columns - 1) {
                val isOverlap = current.any { w ->
                    val xOverlap = x < w.xGrid + w.widthSpan && x + 2 > w.xGrid
                    val yOverlap = y < w.yGrid + w.heightSpan && y + 2 > w.yGrid
                    xOverlap && yOverlap
                }
                if (!isOverlap) {
                    current.add(WidgetConfig(id, type, x, y, 2, 2))
                    placed = true
                    break
                }
            }
            if (placed) break
        }
        
        if (!placed) {
            // Append at the bottom anyway
            current.add(WidgetConfig(id, type, 0, 0, 2, 2))
        }
        
        _widgets.value = current
        prefs.saveWidgets(current)
    }

    fun removeWidget(id: String) {
        val current = _widgets.value.filter { it.id != id }
        _widgets.value = current
        prefs.saveWidgets(current)
    }

    fun moveWidget(id: String, x: Int, y: Int) {
        val current = _widgets.value.map { w ->
            if (w.id == id) {
                val boundsX = x.coerceIn(0, _settings.value.desktopColumns - w.widthSpan)
                val boundsY = y.coerceIn(0, _settings.value.desktopRows - w.heightSpan)
                w.copy(xGrid = boundsX, yGrid = boundsY)
            } else w
        }
        _widgets.value = current
        prefs.saveWidgets(current)
    }

    fun resizeWidget(id: String, widthSpan: Int, heightSpan: Int) {
        val current = _widgets.value.map { w ->
            if (w.id == id) {
                w.copy(widthSpan = widthSpan.coerceIn(1, 4), heightSpan = heightSpan.coerceIn(1, 4))
            } else w
        }
        _widgets.value = current
        prefs.saveWidgets(current)
    }

    fun updateWidgetMetadata(id: String, metadata: String) {
        val current = _widgets.value.map { w ->
            if (w.id == id) {
                w.copy(metadata = metadata)
            } else w
        }
        _widgets.value = current
        prefs.saveWidgets(current)
    }

    // Toggle UI States
    fun setDashOpen(open: Boolean) {
        _isDashOpen.value = open
        if (open) {
            _isControlCenterOpen.value = false
            _isCustomizerOpen.value = false
        }
    }

    fun setControlCenterOpen(open: Boolean) {
        _isControlCenterOpen.value = open
        if (open) {
            _isDashOpen.value = false
            _isCustomizerOpen.value = false
        }
    }

    fun setCustomizerOpen(open: Boolean) {
        _isCustomizerOpen.value = open
        if (open) {
            _isDashOpen.value = false
            _isControlCenterOpen.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun handleGestureTrigger(gesture: LauncherGesture) {
        val action = when (gesture) {
            LauncherGesture.SWIPE_UP -> _settings.value.gestureSwipeUp
            LauncherGesture.SWIPE_DOWN -> _settings.value.gestureSwipeDown
            LauncherGesture.DOUBLE_TAP -> _settings.value.gestureDoubleTap
            LauncherGesture.LONG_PRESS -> {
                // Custom trigger to toggle visual adjustments setting
                setCustomizerOpen(true)
                GestureAction.DO_NOTHING
            }
            LauncherGesture.TWO_FINGER_PINCH -> {
                // Clear any open dropdowns or overlays
                _isDashOpen.value = false
                _isControlCenterOpen.value = false
                _isCustomizerOpen.value = false
                GestureAction.DO_NOTHING
            }
        }

        executeGestureAction(action)
    }

    private fun executeGestureAction(action: GestureAction) {
        when (action) {
            GestureAction.OPEN_DRAWER -> setDashOpen(true)
            GestureAction.OPEN_CONTROL_CENTER -> setControlCenterOpen(true)
            GestureAction.OPEN_SETTINGS -> setCustomizerOpen(true)
            GestureAction.LOCK_SCREEN -> {
                // Simulated screen lock
                _isDashOpen.value = false
                _isControlCenterOpen.value = false
                _isCustomizerOpen.value = false
            }
            GestureAction.TOGGLE_DOCK_REVEAL -> {
                val currentAutohide = _settings.value.isDockAutohideEnabled
                updateSettings(_settings.value.copy(isDockAutohideEnabled = !currentAutohide))
            }
            GestureAction.MINIMIZE_ALL -> {
                _isDashOpen.value = false
                _isControlCenterOpen.value = false
                _isCustomizerOpen.value = false
            }
            GestureAction.DO_NOTHING -> {}
        }
    }

    // Real hardware controls and fallback actions
    fun updateBrightness(v: Float) {
        val target = v.coerceIn(0.01f, 1.0f)
        _brightness.value = target
        
        // Try updating system settings if permission is granted
        if (Settings.System.canWrite(getApplication())) {
            try {
                Settings.System.putInt(
                    getApplication<Application>().contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    (target * 255).toInt().coerceIn(1, 255)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateVolume(v: Float) {
        val target = v.coerceIn(0f, 1.0f)
        _volume.value = target
        audioManager?.let { am ->
            try {
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val volValue = (target * max).toInt().coerceIn(0, max)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, volValue, AudioManager.FLAG_SHOW_UI)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleWifi() {
        val nextVal = !_isWifiEnabled.value
        _isWifiEnabled.value = nextVal
        wifiManager?.let { wm ->
            try {
                wm.isWifiEnabled = nextVal
            } catch (e: Exception) {
                // If it fails (due to API 29+ restrictions), launch the system Wifi toggle Panel or Settings
                val context = getApplication<Application>()
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_WIFI)
                } else {
                    Intent(Settings.ACTION_WIFI_SETTINGS)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    try {
                        val fallbackIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallbackIntent)
                    } catch (e3: Exception) {
                        e3.printStackTrace()
                    }
                }
            }
        }
    }

    fun toggleBluetooth() {
        val nextVal = !_isBluetoothEnabled.value
        _isBluetoothEnabled.value = nextVal
        bluetoothManager?.adapter?.let { adapter ->
            try {
                if (nextVal) {
                    adapter.enable()
                } else {
                    adapter.disable()
                }
            } catch (e: Exception) {
                // If direct toggling fails or requires extra permissions, open the Bluetooth settings panel
                val context = getApplication<Application>()
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
    }

    fun isTutorialCompleted(): Boolean {
        return prefs.isTutorialCompleted()
    }

    fun setTutorialCompleted(completed: Boolean) {
        prefs.setTutorialCompleted(completed)
    }

    fun submitTerminalCommand(cmd: String) {
        if (cmd.isBlank()) return
        val cleanCmd = cmd.trim()
        val response = when {
            cleanCmd.startsWith("echo ") -> cleanCmd.removePrefix("echo ").replace("'", "").replace("\"", "")
            cleanCmd == "help" -> "Commands: help, uname, clear, echo [text], apps, widgets"
            cleanCmd == "uname" -> "Linux isospace-mobile 6.1.0-generic-arm64 #1 SMP ARCH"
            cleanCmd == "clear" -> {
                _terminalNotesHistory.value = emptyList()
                return
            }
            cleanCmd == "apps" -> "Installed Packages count: ${_apps.value.size}"
            cleanCmd == "widgets" -> "Custom desktop widgets: ${_widgets.value.size} active"
            else -> "bash: $cleanCmd: command not found"
        }
        val history = _terminalNotesHistory.value.toMutableList()
        history.add("$ " + cleanCmd)
        history.add(response)
        _terminalNotesHistory.value = history.takeLast(12) // limit log size
    }
}
