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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DumpFormatTest {
    private val node = DumpNode(
        depth = 2,
        className = "android.widget.TextView",
        viewId = "com.zhiliaoapp.musically:id/title",
        left = 0,
        top = 97,
        right = 1080,
        bottom = 2160,
        flags = "VE",
        childCount = 3,
        text = "segredo do usuario",
        contentDescription = null,
        stateDescription = "selecionado",
    )

    @Test
    fun `flags lists only the attributes that are set in a fixed order`() {
        val flags = DumpFormat.flags(
            visible = true,
            enabled = true,
            selected = true,
            checked = false,
            clickable = true,
            longClickable = false,
            scrollable = false,
            focused = false,
        )
        assertEquals("VESK", flags)
    }

    @Test
    fun `flags is a dash when nothing is set`() {
        val flags = DumpFormat.flags(false, false, false, false, false, false, false, false)
        assertEquals("-", flags)
    }

    @Test
    fun `text keeps only the length unless texts are included`() {
        assertEquals("#5", DumpFormat.text("Hello", includeText = false))
        assertEquals("#0", DumpFormat.text("", includeText = false))
        assertEquals("-", DumpFormat.text(null, includeText = false))
        assertEquals("-", DumpFormat.text(null, includeText = true))
    }

    @Test
    fun `included text is flattened quoted and truncated`() {
        assertEquals("\"a b 'c'\"", DumpFormat.text("a\nb \"c\"", includeText = true))
        val long = "x".repeat(DumpFormat.MAX_TEXT_CHARS + 10)
        assertEquals("\"" + "x".repeat(DumpFormat.MAX_TEXT_CHARS) + "…\"", DumpFormat.text(long, includeText = true))
    }

    @Test
    fun `line shows depth view id and bounds without the package prefix`() {
        val line = DumpFormat.line(node, includeText = false)
        assertEquals(
            "    android.widget.TextView id=title [0,97][1080,2160] f=VE c=3 text=#18 desc=- state=#11",
            line,
        )
    }

    @Test
    fun `line never leaks screen text by default`() {
        val line = DumpFormat.line(node, includeText = false)
        assertFalse(line.contains("segredo"))
        assertFalse(line.contains("selecionado"))
    }

    @Test
    fun `line shows the text when the user opted in`() {
        val line = DumpFormat.line(node, includeText = true)
        assertTrue(line.contains("text=\"segredo do usuario\""))
    }

    @Test
    fun `missing class and id are written as placeholders`() {
        val line = DumpFormat.line(node.copy(depth = 0, className = null, viewId = null), includeText = false)
        assertTrue(line.startsWith("? id=- "))
    }

    @Test
    fun `a view id without the id marker is kept whole`() {
        val line = DumpFormat.line(node.copy(viewId = "player_view"), includeText = false)
        assertTrue(line.contains("id=player_view "))
    }
}
