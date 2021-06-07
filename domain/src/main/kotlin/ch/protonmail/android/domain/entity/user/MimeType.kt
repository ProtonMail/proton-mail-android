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

package ch.protonmail.android.domain.entity.user

import me.proton.core.util.kotlin.equalsNoCase

enum class MimeType(val string: String) {
    ALL("*/*"),
    HTML("text/html"),
    PLAIN_TEXT("text/plain"),
    IMAGE("image/*"),
    UNKNOWN_FILE("application/octet-stream"),
    MULTIPART_MIXED("multipart/mixed");

    companion object {

        fun fromString(string: String?): MimeType =
            values().find { it.string equalsNoCase string }
                ?: UNKNOWN_FILE
    }
}
