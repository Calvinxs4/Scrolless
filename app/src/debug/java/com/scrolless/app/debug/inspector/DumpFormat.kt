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

/** One accessibility node, reduced to plain values so formatting can be unit-tested without Android. */
internal data class DumpNode(
    val depth: Int,
    val className: String?,
    val viewId: String?,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val flags: String,
    val childCount: Int,
    val text: CharSequence?,
    val contentDescription: CharSequence?,
    val stateDescription: CharSequence?,
)

/**
 * Text format of an inspector dump. One node per line, indented by depth:
 * `class id=<view id> [l,t][r,b] f=<flags> c=<children> text=.. desc=.. state=..`
 *
 * Bounds use the same `[l,t][r,b]` notation as the XML fixtures already used by the unit tests,
 * so a dump can later be turned into a test fixture.
 */
internal object DumpFormat {
    const val MAX_TEXT_CHARS = 40

    /** V visible, E enabled, S selected, C checked, K clickable, L long-clickable, R scrollable, F focused. */
    fun flags(
        visible: Boolean,
        enabled: Boolean,
        selected: Boolean,
        checked: Boolean,
        clickable: Boolean,
        longClickable: Boolean,
        scrollable: Boolean,
        focused: Boolean,
    ): String = buildString {
        if (visible) append('V')
        if (enabled) append('E')
        if (selected) append('S')
        if (checked) append('C')
        if (clickable) append('K')
        if (longClickable) append('L')
        if (scrollable) append('R')
        if (focused) append('F')
    }.ifEmpty { "-" }

    /**
     * Without [includeText] only the length is kept (`#12`), so no user content is written to disk.
     * With it, the text is flattened to one line and cut at [MAX_TEXT_CHARS] characters.
     */
    fun text(value: CharSequence?, includeText: Boolean): String {
        if (value == null) return "-"
        if (!includeText) return "#${value.length}"
        val flat = value.toString().replace('\n', ' ').replace('\r', ' ').replace('"', '\'')
        val shown = if (flat.length > MAX_TEXT_CHARS) flat.take(MAX_TEXT_CHARS) + "…" else flat
        return "\"$shown\""
    }

    fun line(node: DumpNode, includeText: Boolean): String {
        val indent = "  ".repeat(node.depth)
        val id = node.viewId?.substringAfter(":id/") ?: "-"
        val bounds = "[${node.left},${node.top}][${node.right},${node.bottom}]"
        return "$indent${node.className ?: "?"} id=$id $bounds f=${node.flags} c=${node.childCount} " +
            "text=${text(node.text, includeText)} " +
            "desc=${text(node.contentDescription, includeText)} " +
            "state=${text(node.stateDescription, includeText)}"
    }
}
