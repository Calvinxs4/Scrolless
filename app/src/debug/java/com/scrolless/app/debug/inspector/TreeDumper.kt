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

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/** Appends one [DumpFormat] line per accessibility node, depth first. Reads structure only, never clicks. */
internal class TreeDumper(private val includeText: Boolean, private val out: StringBuilder) {
    var truncated: Boolean = false
        private set

    private var count = 0

    fun dump(node: AccessibilityNodeInfo, depth: Int = 0) {
        if (count >= MAX_NODES || depth > MAX_DEPTH) {
            truncated = true
            return
        }
        count++

        val bounds = Rect().also(node::getBoundsInScreen)
        val stateDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) node.stateDescription else null
        val line = DumpNode(
            depth = depth,
            className = node.className?.toString(),
            viewId = node.viewIdResourceName,
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom,
            flags = DumpFormat.flags(
                visible = node.isVisibleToUser,
                enabled = node.isEnabled,
                selected = node.isSelected,
                checked = node.isChecked,
                clickable = node.isClickable,
                longClickable = node.isLongClickable,
                scrollable = node.isScrollable,
                focused = node.isFocused,
            ),
            childCount = node.childCount,
            text = node.text,
            contentDescription = node.contentDescription,
            stateDescription = stateDescription,
        )
        out.append(DumpFormat.line(line, includeText)).append('\n')

        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child -> dump(child, depth + 1) }
        }
    }

    companion object {
        const val MAX_NODES = 4000
        const val MAX_DEPTH = 80
    }
}
