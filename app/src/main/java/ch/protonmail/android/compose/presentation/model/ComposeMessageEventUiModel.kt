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

import ch.protonmail.android.attachments.domain.model.UriPair
import ch.protonmail.android.compose.presentation.ui.ComposeMessageKotlinActivity
import ch.protonmail.android.ui.view.DaysHoursPair

/**
 * Describes an event for [ComposeMessageKotlinActivity]
 */
sealed class ComposeMessageEventUiModel {

    /**
     * Attachments for Composer has been changed
     */
    data class OnAttachmentsChange(val attachments: List<ComposerAttachmentUiModel>) : ComposeMessageEventUiModel()

    /**
     * Password has been changed
     */
    data class OnPasswordChange(val hasPassword: Boolean) : ComposeMessageEventUiModel()

    /**
     * Password change has been requested and previous password is ready
     */
    data class OnPasswordChangeRequest(val currentPassword: MessagePasswordUiModel) : ComposeMessageEventUiModel()

    /**
     * Expiration has changed
     */
    data class OnExpirationChange(val hasExpiration: Boolean) : ComposeMessageEventUiModel()

    /**
     * Expiration change has been requested and previous expiration is ready
     */
    data class OnExpirationChangeRequest(val currentExpiration: DaysHoursPair) : ComposeMessageEventUiModel()

    /**
     * Uri has been created for a photo to be taken from camera
     */
    data class OnPhotoUriReady(val uri: UriPair) : ComposeMessageEventUiModel()
}
