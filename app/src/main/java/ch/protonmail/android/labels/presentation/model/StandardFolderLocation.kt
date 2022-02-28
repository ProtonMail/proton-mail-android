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

package ch.protonmail.android.labels.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants

/**
 * Describes standard labels list UI items used in [ManageLabelsActionAdapter].
 */
enum class StandardFolderLocation(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val title: Int? = null
) {

    ARCHIVE(
        Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString(),
        R.drawable.ic_archive,
        R.string.archive
    ),
    INBOX(
        Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
        R.drawable.ic_inbox,
        R.string.inbox
    ),
    SPAM(
        Constants.MessageLocationType.SPAM.messageLocationTypeValue.toString(),
        R.drawable.ic_fire,
        R.string.spam
    ),
    TRASH(
        Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString(),
        R.drawable.ic_trash,
        R.string.trash
    )
}
