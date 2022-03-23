/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailbox.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.Constants.MessageLocationType.INBOX
import ch.protonmail.android.core.Constants.MessageLocationType.SPAM
import ch.protonmail.android.core.Constants.MessageLocationType.TRASH

data class EmptyMailboxUiModel(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val imageRes: Int
) {

    companion object {

        fun fromLocation(location: MessageLocationType): EmptyMailboxUiModel {
            return when (location) {
                INBOX -> EmptyMailboxUiModel(
                    titleRes = R.string.mailbox_no_messages,
                    subtitleRes = R.string.mailbox_empty_inbox_subtitle,
                    imageRes = R.drawable.img_empty_mailbox
                )
                SPAM -> EmptyMailboxUiModel(
                    titleRes = R.string.mailbox_no_messages,
                    subtitleRes = R.string.mailbox_empty_spam_subtitle,
                    imageRes = R.drawable.img_no_messages_in_spam
                )
                TRASH -> EmptyMailboxUiModel(
                    titleRes = R.string.mailbox_no_messages,
                    subtitleRes = R.string.mailbox_empty_trash_subtitle,
                    imageRes = R.drawable.img_empty_trash
                )
                else -> EmptyMailboxUiModel(
                    titleRes = R.string.mailbox_no_messages,
                    subtitleRes = R.string.empty_folder,
                    imageRes = R.drawable.img_empty_folder
                )
            }
        }
    }
}
