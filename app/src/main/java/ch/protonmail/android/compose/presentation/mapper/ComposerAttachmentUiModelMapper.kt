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

package ch.protonmail.android.compose.presentation.mapper

import ch.protonmail.android.attachments.domain.model.AttachmentFileInfo
import ch.protonmail.android.attachments.domain.model.ImportAttachmentResult
import ch.protonmail.android.compose.presentation.model.ComposerAttachmentUiModel
import ch.protonmail.android.compose.presentation.model.ComposerAttachmentUiModel.Icon
import ch.protonmail.android.compose.presentation.model.ComposerAttachmentUiModel.State
import ch.protonmail.android.domain.entity.FileType
import ch.protonmail.android.mapper.UiModelMapper
import javax.inject.Inject

/**
 * An [UiModelMapper] that maps from [ImportAttachmentResult] to [ComposerAttachmentUiModel]
 */
class ComposerAttachmentUiModelMapper @Inject constructor() :
    UiModelMapper<ImportAttachmentResult, ComposerAttachmentUiModel> {

    override fun ImportAttachmentResult.toUiModel(): ComposerAttachmentUiModel {
        return when (this) {
            is ImportAttachmentResult.Idle -> toIdle()
            is ImportAttachmentResult.OnInfo -> toImporting(fileInfo)
            is ImportAttachmentResult.Success -> toReady(fileInfo)
            is ImportAttachmentResult.CantRead -> toError()
            is ImportAttachmentResult.CantWrite -> toError(fileInfo)
        }
    }

    private fun ImportAttachmentResult.Idle.toIdle() =
        ComposerAttachmentUiModel.Idle(id = originalFileUri)

    private fun ImportAttachmentResult.toImporting(fileInfo: AttachmentFileInfo) =
        toDataWithState(fileInfo, State.Importing)

    private fun ImportAttachmentResult.toReady(fileInfo: AttachmentFileInfo) =
        toDataWithState(fileInfo, State.Ready)

    private fun ImportAttachmentResult.toError(fileInfo: AttachmentFileInfo? = null) =
        if (fileInfo != null)
            toDataWithState(fileInfo, State.Error)
        else
            ComposerAttachmentUiModel.NoFileInfo(id = originalFileUri)

    private fun ImportAttachmentResult.toDataWithState(fileInfo: AttachmentFileInfo, state: State) =
        ComposerAttachmentUiModel.Data(
            id = originalFileUri,
            displayName = fileInfo.fileName.s,
            extension = fileInfo.extension,
            size = fileInfo.size,
            icon = iconFor(fileInfo.extension),
            state = state
        )

    private fun iconFor(extension: String): Icon =
        when (FileType.byExtension(extension)) {
            FileType.Generic -> Icon.Generic
            FileType.Archive -> Icon.Archive
            FileType.Audio -> Icon.Audio
            FileType.Doc -> Icon.Doc
            FileType.Image -> Icon.Image
            FileType.Keynote -> Icon.Keynote
            FileType.Numbers -> Icon.Numbers
            FileType.Pages -> Icon.Pages
            FileType.Pdf -> Icon.Pdf
            FileType.Presentation -> Icon.Presentation
            FileType.Video -> Icon.Video
            FileType.Xls -> Icon.Xls
            FileType.Xml -> Icon.Xml
        }
}
