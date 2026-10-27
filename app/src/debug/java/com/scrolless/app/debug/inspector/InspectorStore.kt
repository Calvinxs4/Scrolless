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
package com.scrolless.app.debug.inspector

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Where dumps live (app-private storage) plus the two inspector switches. */
internal object InspectorStore {
    private const val PREFS = "inspector_prefs"
    private const val KEY_AUTO = "auto_capture"
    private const val KEY_TEXT = "include_text"

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    private val prefs: SharedPreferences
        get() = requireNotNull(appContext) { "InspectorStore.init was not called" }
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Automatic capture while TikTok is open. Off by default. */
    var autoCapture: Boolean
        get() = appContext != null && prefs.getBoolean(KEY_AUTO, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO, value).apply()

    /** Keep screen texts in dumps. Off by default because texts can contain personal data. */
    var includeText: Boolean
        get() = appContext != null && prefs.getBoolean(KEY_TEXT, false)
        set(value) = prefs.edit().putBoolean(KEY_TEXT, value).apply()

    private fun dumpDir(): File = File(requireNotNull(appContext).filesDir, "inspector").apply { mkdirs() }

    fun write(trigger: String, content: String, nowMillis: Long = System.currentTimeMillis()): File {
        val dir = dumpDir()
        val file = File(dir, InspectorFiles.fileName(nowMillis, trigger))
        file.writeText(content)
        InspectorFiles.filesToDelete(dir.list().orEmpty().toList()).forEach { File(dir, it).delete() }
        return file
    }

    fun list(): List<File> = dumpDir().listFiles { file -> file.isFile && file.name.endsWith(".txt") }
        .orEmpty()
        .sortedBy { it.name }

    fun clear() {
        list().forEach { it.delete() }
    }

    /** Bundles every dump into one zip in the cache folder, ready for the share sheet. */
    fun zipForSharing(): File {
        val dir = File(requireNotNull(appContext).cacheDir, "share").apply { mkdirs() }
        val zip = File(dir, "tiktok-focus-dumps.zip")
        ZipOutputStream(zip.outputStream().buffered()).use { out ->
            list().forEach { file ->
                out.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
        }
        return zip
    }
}
