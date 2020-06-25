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
package ch.protonmail.android.utils.extensions

/**
 * Created by kadrikj on 9/13/18. */

/**
 * Executes code block if the object is null
 */
infix fun <T> T?.ifNull(block: () -> Unit) {
    if (this == null) {
        block()
    }
}

fun <T> T?.ifNullElse(blockNull: () -> Unit, blockElse: () -> Unit) {
    if (this == null) {
        blockNull()
    } else {
        blockElse()
    }
}

fun <T, U> T?.ifNullElseReturn(blockNull: () -> U, blockElse: (T) -> U): U {
    return if (this == null) {
        blockNull()
    } else {
        blockElse(this)
    }
}

fun <T> List<T>?.ifEmptyElse(blockEmpty: () -> Unit, blockElse: () -> Unit) {
    if (this == null || size == 0) {
        blockEmpty()
    } else {
        blockElse()
    }
}

infix fun <T> T?.notNull(variableName: String): T {
    if (this == null)
        throw RuntimeException("$variableName is null")
    return this
}
