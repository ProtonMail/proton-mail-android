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

package ch.protonmail.android.util

import android.view.View
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.material.textfield.TextInputEditText
import me.proton.core.presentation.ui.view.ProtonInput
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf.allOf

/**
 * Matches a view with given [id], which is a [TextInputEditText]
 * @see withId
 */
fun withTextInputEditTextId(id: Int): Matcher<View> =
    allOf(
        withId(id),
        withClassName(Matchers.`is`(TextInputEditText::class.qualifiedName))
    )

/**
 * Matches a [TextInputEditText] inside a [ProtonInput] with given [id]
 */
fun withProtonInputEditTextId(id: Int): Matcher<View> =
    allOf(
        isDescendantOfA(withId(id)),
        withClassName(Matchers.`is`(TextInputEditText::class.qualifiedName))
    )
