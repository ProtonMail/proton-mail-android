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
@file:JvmName("TextExtensions") // Java name

package ch.protonmail.android.utils.extensions

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.StringRes
import studio.forface.viewstatestore.ViewState

/*
 * A file containing extensions for Text
 * Author: Davide Farella
 */

// region constants
private const val DEFAULT_TOAST_LENGTH = Toast.LENGTH_LONG
private const val DEFAULT_TOAST_GRAVITY = Gravity.BOTTOM
// endregion

/**
 * An extension for show a [Toast] within a [Context]
 * @param messageRes [StringRes] of message to show
 * @param length [Int] length of the [Toast]. Default is [DEFAULT_TOAST_LENGTH]
 * @param gravity [Int] gravity for the [Toast]. Default is [DEFAULT_TOAST_GRAVITY]
 */
@JvmOverloads
fun Context.showToast(
        @StringRes messageRes: Int,
        length: Int = DEFAULT_TOAST_LENGTH,
        gravity: Int = DEFAULT_TOAST_GRAVITY
) {
    @Suppress("SENSELESS_COMPARISON") // It could be `null` if called from Java
    if (this != null) {
        Toast.makeText(this, messageRes, length).apply {
            if (gravity != DEFAULT_TOAST_GRAVITY) {
                setGravity(gravity, 0, 0)
            }
        }.show()
    }
}

/**
 * An extension for show a [Toast] within a [Context]
 * @param message [CharSequence] message to show
 * @param length [Int] length of the [Toast]. Default is [DEFAULT_TOAST_LENGTH]
 * @param gravity [Int] gravity for the [Toast]. Default is [DEFAULT_TOAST_GRAVITY]
 */
@JvmOverloads
fun Context.showToast(
        message: CharSequence,
        length: Int = DEFAULT_TOAST_LENGTH,
        gravity: Int = DEFAULT_TOAST_GRAVITY
) {
    @Suppress("SENSELESS_COMPARISON") // It could be `null` if called from Java
    if (this != null) {
        Toast.makeText(this, message, length).apply {
            if (gravity != DEFAULT_TOAST_GRAVITY) {
                setGravity(gravity, 0, 0)
            }
        }.show()
    }
}

/**
 * An extension for show a [Toast] within a [Context]
 * @param error [ViewState.Error] containing the message to show
 * @param length [Int] length of the [Toast]. Default is [DEFAULT_TOAST_LENGTH]
 * @param gravity [Int] gravity for the [Toast]. Default is [DEFAULT_TOAST_GRAVITY]
 */
@JvmOverloads
fun Context.showToast(
        error: ViewState.Error,
        length: Int = DEFAULT_TOAST_LENGTH,
        gravity: Int = DEFAULT_TOAST_GRAVITY
) {
    showToast(error.getMessage(this), length = length, gravity = gravity)
}
