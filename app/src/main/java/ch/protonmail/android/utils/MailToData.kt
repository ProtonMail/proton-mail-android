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
@file:JvmName("MailToUtils") // Name for Java
@file:Suppress("FunctionName")

package ch.protonmail.android.utils

import android.net.Uri

// region constants
/**
 * [String] scheme for mailto
 * We don't use [android.net.MailTo.MAILTO_SCHEME] since is "mailto:" but [Uri.getScheme] does not
 * include the semicolon ':'
 */
const val MAILTO_SCHEME = "mailto"

/** A replacement to [android.net.MailTo] that is pretty broken */
data class MailToData(
    val addresses: List<String>,
    val cc: List<String>,
    val subject: String,
    val body: String,
    val bcc: List<String> = emptyList()
)
