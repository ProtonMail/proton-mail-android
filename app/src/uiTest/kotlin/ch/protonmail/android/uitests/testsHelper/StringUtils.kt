/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonMail.
 * 
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.uitests.testsHelper

import androidx.annotation.IdRes
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Random

object StringUtils {

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    fun stringFromResource(@IdRes id: Int): String {
        return targetContext.getString(id)
    }

    fun stringFromResource(@IdRes id: Int, arg1: String): String {
        return targetContext.getString(id, arg1)
    }

    fun getAlphaNumericStringWithSpecialCharacters(length: Long = 10): String {
        val source = "aäbcdeëfghijklmnoöpqrstuuüvwxyz1234567890!@+_)(*&^%$#@!"
        return Random().ints(length, 0, source.length)
            .toArray()
            .map(source::get)
            .joinToString("")
    }
}