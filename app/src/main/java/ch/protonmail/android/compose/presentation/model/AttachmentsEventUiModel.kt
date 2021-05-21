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
import ch.protonmail.android.attachments.domain.model.ImportAttachmentResult

/**
 * Describes an action on Attachment
 */
sealed class AttachmentsEventUiModel {

    object None : AttachmentsEventUiModel()

    /**
     * One or a set of attachments is being imported
     * @see ImportAttachmentResult
     */
    data class Import(val results: List<ImportAttachmentResult>) : AttachmentsEventUiModel()

    /**
     * One attachment has been removed by User Interaction
     */
    data class Remove(val fileUri: Uri) : AttachmentsEventUiModel()
}
