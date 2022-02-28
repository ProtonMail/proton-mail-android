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
package ch.protonmail.android.contacts

import android.content.Context
import ch.protonmail.android.R
import ch.protonmail.android.utils.setDefaultIfEmpty

class ErrorResponse(val message: String, val type: ErrorEnum) {

    fun getMessage(context: Context): String {
        return when (type) {
            ErrorEnum.INVALID_EMAIL -> context.resources.getString(R.string.invalid_email)
            ErrorEnum.INVALID_EMAIL_LIST -> context.resources.getString(R.string.empty_emails)
            ErrorEnum.INVALID_GROUP_LIST -> context.resources.getString(R.string.no_groups)
            ErrorEnum.DEFAULT_ERROR -> context.resources.getString(R.string.default_error_message)
            ErrorEnum.SERVER_ERROR -> message.setDefaultIfEmpty(
                context.resources.getString(R.string.message_details_load_failure)
            )
            ErrorEnum.DEFAULT -> context.resources.getString(R.string.message_details_load_failure)
        }
    }
}

enum class ErrorEnum {
    INVALID_EMAIL, INVALID_EMAIL_LIST, INVALID_GROUP_LIST, DEFAULT_ERROR, SERVER_ERROR, DEFAULT
}
