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

// based on Constants.MessageLocationType
sealed class StandardFolderLocation(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val title: Int? = null
) {

    object Archive : StandardFolderLocation(ID_ARCHIVE_FOLDER, R.drawable.ic_archive_24dp, R.string.archive)
    object Inbox : StandardFolderLocation(ID_INBOX_FOLDER, R.drawable.ic_inbox_24dp, R.string.inbox)
    object Spam : StandardFolderLocation(ID_SPAM_FOLDER, R.drawable.ic_spam_24dp, R.string.spam)
    object Trash : StandardFolderLocation(ID_TRASH_FOLDER, R.drawable.ic_trash_24dp, R.string.trash)
    data class CustomFolder(val folderId: String) : StandardFolderLocation(folderId, R.drawable.ic_folder_24dp)

    companion object {

        const val ID_ARCHIVE_FOLDER = "id_archive_folder"
        const val ID_INBOX_FOLDER = "id_inbox_folder"
        const val ID_SPAM_FOLDER = "id_spam_folder"
        const val ID_TRASH_FOLDER = "id_trash_folder"
    }
}
