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

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectorFilesTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `file names start with a sortable timestamp`() {
        assertEquals("dump_19700101_000000_000_button.txt", InspectorFiles.fileName(0L, "button", utc))
        assertEquals("dump_19700101_000001_500_auto.txt", InspectorFiles.fileName(1_500L, "auto", utc))
    }

    @Test
    fun `file names are in chronological order when sorted alphabetically`() {
        val older = InspectorFiles.fileName(1_000L, "button", utc)
        val newer = InspectorFiles.fileName(2_000L, "auto", utc)
        assertTrue(older < newer)
    }

    @Test
    fun `trigger names cannot inject path characters`() {
        assertEquals("dump_19700101_000000_000_x.txt", InspectorFiles.fileName(0L, "../x", utc))
        assertEquals("dump_19700101_000000_000_dump.txt", InspectorFiles.fileName(0L, "///", utc))
    }

    @Test
    fun `nothing is deleted while under the limit`() {
        assertEquals(emptyList<String>(), InspectorFiles.filesToDelete(listOf("b", "a"), max = 2))
    }

    @Test
    fun `the oldest files beyond the limit are deleted`() {
        val names = listOf("dump_3", "dump_1", "dump_4", "dump_2")
        assertEquals(listOf("dump_1", "dump_2"), InspectorFiles.filesToDelete(names, max = 2))
    }
}
