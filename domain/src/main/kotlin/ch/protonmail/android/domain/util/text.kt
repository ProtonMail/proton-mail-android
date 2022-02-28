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

package ch.protonmail.android.domain.util

fun <T : CharSequence> checkNotEmpty(text: T?): T = text.let {
    checkNotNull(text)
    check(text.isNotEmpty())
    return text
}

inline fun <T : CharSequence> checkNotEmpty(text: T?, lazyMessage: () -> Any): T = text.let {
    checkNotNull(text, lazyMessage)
    check(text.isNotEmpty(), lazyMessage)
    return text
}

fun <T : CharSequence> checkNotBlank(text: T?): T = text.let {
    checkNotNull(text)
    check(text.isNotBlank())
    return text
}

inline fun <T : CharSequence> checkNotBlank(text: T?, lazyMessage: () -> Any): T = text.let {
    checkNotNull(text, lazyMessage)
    check(text.isNotBlank(), lazyMessage)
    return text
}

fun <T : CharSequence> requireNotEmpty(text: T?): T = text.let {
    requireNotNull(text)
    require(text.isNotEmpty())
    return text
}

inline fun <T : CharSequence> requireNotEmpty(text: T?, lazyMessage: () -> Any): T = text.let {
    requireNotNull(text, lazyMessage)
    require(text.isNotEmpty(), lazyMessage)
    return text
}

fun <T : CharSequence> requireNotBlank(text: T?): T = text.let {
    requireNotNull(text)
    require(text.isNotBlank())
    return text
}

inline fun <T : CharSequence> requireNotBlank(text: T?, lazyMessage: () -> Any): T = text.let {
    requireNotNull(text, lazyMessage)
    require(text.isNotBlank(), lazyMessage)
    return text
}
