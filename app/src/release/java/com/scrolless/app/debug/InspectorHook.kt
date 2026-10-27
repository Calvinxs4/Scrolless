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

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Release build: the screen inspector does not exist here, so every call does nothing.
 * The real implementation lives in the debug source set under the same name.
 */
@Suppress("UNUSED_PARAMETER")
internal object InspectorHook {
    fun attach(service: AccessibilityService) = Unit

    fun onEvent(service: AccessibilityService, event: AccessibilityEvent) = Unit

    fun detach() = Unit
}
