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
package ch.protonmail.android.uitests.testsHelper

import androidx.annotation.IdRes
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Random

object StringUtils {

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    fun quantityStringFromResource(@IdRes id: Int, item: Int): String =
        targetContext.resources.getQuantityString(id, item)

    fun getNumericString(length: Long = 5): String {
        val source = "1234567890"
        return Random().ints(length, 0, source.length)
            .toArray()
            .map(source::get)
            .joinToString("")
    }

    fun getAlphaNumericStringWithSpecialCharacters(length: Long = 10): String {
        val source = "abcdefghijklmnopqrstuuvwxyz1234567890!@+_)(*&^%$#@!"
        return Random().ints(length, 0, source.length)
            .toArray()
            .map(source::get)
            .joinToString("")
    }

    fun getEmailString(length: Long = 10): String {
        val source = "abcdefghijklmnopqrstuuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!#\$%&'*+-=?^_`{|}~"
        return Random().ints(length, 0, source.length)
            .toArray()
            .map(source::get)
            .joinToString("")
    }

    fun fileContentAsString(
        fileName: String
    ): String = InstrumentationRegistry
        .getInstrumentation().context.assets.open(fileName).bufferedReader().use { it.readText() }
}
