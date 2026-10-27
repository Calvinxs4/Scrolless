/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.debug

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.debug.inspector.InspectorStore
import com.scrolless.app.debug.inspector.TreeDumper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

/**
 * Debug-only screen inspector. It saves the accessibility tree of the TikTok window to a local text
 * file so the real view IDs, tab states and screen structure can be studied on a physical device.
 *
 * Nothing leaves the phone by itself: the app has no internet permission and files are only shared
 * when the user taps "Compartilhar" in the inspector screen. Texts on screen are replaced by their
 * length unless the user turns "incluir textos" on.
 *
 * Triggers: the accessibility button (one dump per tap) or, if enabled, an automatic capture
 * at most every [AUTO_INTERVAL_MS] while TikTok sends events.
 */
internal object InspectorHook {
    private const val BUTTON = "button"
    private const val AUTO = "auto"
    private const val AUTO_INTERVAL_MS = 2_000L
    private const val TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"

    private val tikTokPackages: Set<String> =
        (BlockableApp.TIKTOK.getPackageIds() + BlockableApp.TIKTOK_LITE.getPackageIds()).toSet()

    private var buttonController: AccessibilityButtonController? = null
    private var buttonCallback: AccessibilityButtonController.AccessibilityButtonCallback? = null
    private var lastAutoCaptureAt = 0L

    @Volatile
    private var lastActivity: String? = null

    fun attach(service: AccessibilityService) {
        InspectorStore.init(service)
        // Ask Android to show the accessibility button for this service (flags can change at runtime).
        service.serviceInfo?.let { info ->
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON
            service.serviceInfo = info
        }
        val controller = service.accessibilityButtonController
        val callback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController) {
                capture(service, BUTTON)
            }
        }
        controller.registerAccessibilityButtonCallback(callback)
        buttonController = controller
        buttonCallback = callback
    }

    fun detach() {
        val controller = buttonController
        val callback = buttonCallback
        if (controller != null && callback != null) {
            runCatching { controller.unregisterAccessibilityButtonCallback(callback) }
        }
        buttonController = null
        buttonCallback = null
    }

    fun onEvent(service: AccessibilityService, event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in tikTokPackages) return

        val className = event.className?.toString()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && className?.endsWith("Activity") == true) {
            lastActivity = className
        }

        if (!InspectorStore.autoCapture) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastAutoCaptureAt < AUTO_INTERVAL_MS) return
        lastAutoCaptureAt = now
        capture(service, AUTO)
    }

    private fun capture(service: AccessibilityService, trigger: String) {
        try {
            val includeText = InspectorStore.includeText
            val windowLines = StringBuilder()
            val trees = StringBuilder()
            var treeCount = 0

            for (window in service.windows.orEmpty()) {
                val root = window.root
                val packageName = root?.packageName?.toString()
                windowLines.append("window id=${window.id} type=${window.type} layer=${window.layer} ")
                    .append("active=${window.isActive} focused=${window.isFocused} pkg=${packageName ?: "-"}\n")
                val isTikTokApplication = packageName != null && packageName in tikTokPackages &&
                    window.type == AccessibilityWindowInfo.TYPE_APPLICATION
                if (root != null && isTikTokApplication) {
                    trees.append("--- tree of window ${window.id} ---\n")
                    val dumper = TreeDumper(includeText, trees)
                    dumper.dump(root)
                    if (dumper.truncated) trees.append("(truncated at ${TreeDumper.MAX_NODES} nodes)\n")
                    treeCount++
                }
            }

            // Automatic captures only make sense when there is a TikTok tree to record.
            if (treeCount == 0 && trigger == AUTO) return

            val metrics = service.resources.displayMetrics
            val header = buildString {
                append("# TikTok Focus inspector dump v1\n")
                append("time=").append(SimpleDateFormat(TIME_PATTERN, Locale.US).format(Date())).append('\n')
                append("trigger=").append(trigger).append('\n')
                append("includeText=").append(includeText).append('\n')
                append("device=${Build.MANUFACTURER} ${Build.MODEL}; sdk=${Build.VERSION.SDK_INT}; ")
                append("locale=${Locale.getDefault().toLanguageTag()}\n")
                append("display=${metrics.widthPixels}x${metrics.heightPixels}; density=${metrics.density}\n")
                append("tiktok=${tikTokVersions(service)}\n")
                append("lastActivity=${lastActivity ?: "-"}\n")
                append("windows:\n").append(windowLines)
                if (treeCount == 0) append("(no readable TikTok application window)\n")
            }

            val file = InspectorStore.write(trigger, header + trees)
            if (trigger == BUTTON) {
                Toast.makeText(service, "Dump salvo: ${file.name}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Timber.w(e, "Inspector capture failed")
            if (trigger == BUTTON) {
                Toast.makeText(service, "Falha ao salvar o dump", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun tikTokVersions(service: AccessibilityService): String = tikTokPackages.mapNotNull { packageName ->
        try {
            val info = service.packageManager.getPackageInfo(packageName, 0)
            "$packageName=${info.versionName}(${PackageInfoCompat.getLongVersionCode(info)})"
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }.joinToString("; ").ifEmpty { "not-visible-or-not-installed" }
}
