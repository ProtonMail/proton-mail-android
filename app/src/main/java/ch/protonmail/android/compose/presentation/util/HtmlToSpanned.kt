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

package ch.protonmail.android.compose.presentation.util

import android.text.Spanned
import androidx.core.text.HtmlCompat
import ch.protonmail.android.utils.extensions.substring
import javax.inject.Inject

/**
 * Convert and Html [String] to a [Spanned]
 */
class HtmlToSpanned @Inject constructor() {

    operator fun invoke(html: String): Spanned {
        val headlessHtml = removeHead(html)
        return HtmlCompat.fromHtml(headlessHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    private fun removeHead(html: String): String {
        val beforeHead = html.substring(end = HEAD_START_TAG, ignoreCase = true)
        val hasHeadTag = beforeHead.length < html.length
        if (hasHeadTag.not()) {
            return html
        }
        val afterHead = html.substring(start = HEAD_END_TAG, ignoreCase = true)
        return beforeHead + afterHead
    }

    private companion object {

        const val HEAD_START_TAG = "<head>"
        const val HEAD_END_TAG = "</head>"
    }
}
