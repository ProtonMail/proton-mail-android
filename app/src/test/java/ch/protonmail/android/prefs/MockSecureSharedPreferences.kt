/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.prefs

import io.mockk.MockKAnswerScope
import io.mockk.every
import io.mockk.mockk

fun mockSecureSharedPreferences(block: SecureSharedPreferences.() -> Unit = {}): SecureSharedPreferences = mockk {

    val m = mutableMapOf<String, Any?>()

    val editor: SecureSharedPreferences.Editor = MockSharedPreferencesEditor(m)

    @Suppress("UNCHECKED_CAST")
    fun <T, B> MockKAnswerScope<T, B>.getMap() =
        m.getOrElse(firstArg()) { if (nArgs >= 2) secondArg() else null } as B

    every { contains(any()) } answers { m.containsKey(firstArg()) }
    every { edit() } returns editor

    every { all } answers { m }
    every { getBoolean(any(), any()) } answers { getMap() }
    every { getFloat(any(), any()) } answers { getMap() }
    every { getInt(any(), any()) } answers { getMap() }
    every { getLong(any(), any()) } answers { getMap() }
    every { getString(any(), any()) } answers { getMap() }
    every { getStringOrNull(any()) } answers { getMap() }
    every { getStringSet(any(), any()) } answers { getMap() }

    block()
}

private fun MockSharedPreferencesEditor(
    m: MutableMap<String, Any?>
): SecureSharedPreferences.Editor = mockk {

    every { putBoolean(key = any(), value = any()) } answers { this@mockk.apply { m[firstArg()] = secondArg() } }
    every { putFloat(key = any(), value = any()) } answers { this@mockk.apply { m[firstArg()] = secondArg() } }
    every { putInt(key = any(), value = any()) } answers { this@mockk.apply { m[firstArg()] = secondArg() } }
    every { putLong(key = any(), value = any()) } answers { this@mockk.apply { m[firstArg()] = secondArg() } }
    every { putString(key = any(), value = any()) } answers { this@mockk.apply { m[firstArg()] = secondArg() } }
    every { putStringSet(key = any(), values = any()) } answers { this@mockk.apply { m[firstArg()] = secondArg() } }
    every { remove(s = any()) } answers { this@mockk.apply { m -= firstArg<String>() } }
    every { clear() } answers { this@mockk.apply { m.clear() } }
    every { apply() } answers { /* noop */ }
    every { commit() } answers { true }
}
