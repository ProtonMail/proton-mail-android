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

package ch.protonmail.android.testAndroidInstrumented.matcher

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.test.espresso.matcher.BoundedDiagnosingMatcher
import com.google.android.material.shape.MaterialShapeDrawable
import org.hamcrest.Description
import org.hamcrest.Matcher
import java.util.Locale

fun withBackgroundColor(expectedColor: Int): Matcher<View> {
    return object : BoundedDiagnosingMatcher<View, View>(View::class.java) {
        var actualColor = 0
        var message: String? = null

        @RequiresApi(Build.VERSION_CODES.N)
        override fun matchesSafely(item: View, mismatchDescription: Description): Boolean {
            if (item.background == null) {
                message = item.id.toString() + " does not have a background"
                return false
            }
            actualColor = when (val background = item.background) {
                is ColorDrawable -> background.color
                is GradientDrawable -> background.color!!.defaultColor
                is MaterialShapeDrawable -> background.fillColor!!.defaultColor
                else -> TODO("not implemented ${background::class.simpleName}")
            }
            return actualColor == expectedColor
        }

        override fun describeMoreTo(description: Description) {
            if (actualColor != 0) {
                val expected = String.format(Locale.getDefault(), "#%06X", 0xFFFFFF and expectedColor)
                val actual = String.format(Locale.getDefault(), "#%06X", 0xFFFFFF and actualColor)
                message = """
                    Background color did not match: Expected $expected 
                    was $actual
                """
            }
            description.appendText(message)
        }
    }
}
