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

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** File naming and retention rules for inspector dumps, kept free of Android classes. */
internal object InspectorFiles {
    const val MAX_FILES = 80

    /** Sortable by name: the timestamp comes first, so alphabetical order is chronological order. */
    fun fileName(timeMillis: Long, trigger: String, zone: TimeZone = TimeZone.getDefault()): String {
        val format = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).apply { timeZone = zone }
        val safeTrigger = trigger.filter { it.isLetterOrDigit() }.ifEmpty { "dump" }
        return "dump_${format.format(Date(timeMillis))}_$safeTrigger.txt"
    }

    /** The oldest names beyond [max], so the phone never fills up with dumps. */
    fun filesToDelete(names: List<String>, max: Int = MAX_FILES): List<String> {
        val sorted = names.sorted()
        return if (sorted.size <= max) emptyList() else sorted.take(sorted.size - max)
    }
}
