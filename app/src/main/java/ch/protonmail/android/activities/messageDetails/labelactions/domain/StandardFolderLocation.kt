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

package ch.protonmail.android.activities.messageDetails.labelactions.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants

// based on Constants.MessageLocationType
enum class StandardFolderLocation(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val title: Int? = null
) {

    ARCHIVE(
        Constants.MessageLocationType.ARCHIVE.toString(), R.drawable.ic_archive_24dp, R.string.archive
    ),
    INBOX(
        Constants.MessageLocationType.INBOX.toString(), R.drawable.ic_inbox_24dp, R.string.inbox
    ),
    SPAM(
        Constants.MessageLocationType.SPAM.toString(), R.drawable.ic_spam_24dp, R.string.spam
    ),
    TRASH(
        Constants.MessageLocationType.TRASH.toString(), R.drawable.ic_trash_24dp, R.string.trash
    )

}
