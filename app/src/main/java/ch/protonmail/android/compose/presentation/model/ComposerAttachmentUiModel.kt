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

package ch.protonmail.android.compose.presentation.model

import android.net.Uri
import androidx.annotation.DrawableRes
import ch.protonmail.android.domain.entity.Bytes

/**
 * An Ui Model for an attachment shown in the attachments' list in Composer
 */
sealed class ComposerAttachmentUiModel {

    abstract val id: Uri

    /**
     * File is about to be imported
     */
    data class Idle(
        override val id: Uri
    ) : ComposerAttachmentUiModel()

    /**
     * We have file data, but this can still be an error
     * @see state
     * @see State
     */
    data class Data(
        override val id: Uri,
        val displayName: String,
        val extension: String,
        val size: Bytes,
        @DrawableRes val icon: Int,
        val state: State
    ) : ComposerAttachmentUiModel()

    /**
     * We are unable to get file data
     */
    data class NoFileInfo(
        override val id: Uri
    ) : ComposerAttachmentUiModel()


    enum class State {
        Ready,
        Importing,
        Error
    }
}
