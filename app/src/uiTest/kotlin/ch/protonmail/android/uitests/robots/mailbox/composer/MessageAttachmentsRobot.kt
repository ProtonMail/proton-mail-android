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
package ch.protonmail.android.uitests.robots.mailbox.composer

import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatImageButton
import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.MockAddAttachmentIntent
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions

/**
 * Class represents Message Attachments.
 */
open class MessageAttachmentsRobot {

    fun addImageCaptureAttachment(@IdRes drawable: Int): ComposerRobot =
        mockCameraImageCapture(drawable).navigateUpToComposerView()

    fun addTwoImageCaptureAttachments(
        @IdRes firstDrawable: Int,
        @IdRes secondDrawable: Int
    ): ComposerRobot =
        mockCameraImageCapture(firstDrawable)
            .mockCameraImageCapture(secondDrawable)
            .navigateUpToComposerView()

    fun addFileAttachment(@IdRes drawable: Int): ComposerRobot =
        mockFileAttachment(drawable).navigateUpToComposerView()

    private fun navigateUpToComposerView(): ComposerRobot {
        UIActions.allOf.clickViewWithParentIdAndClass(R.id.toolbar, AppCompatImageButton::class.java)
        return ComposerRobot()
    }

    private fun mockCameraImageCapture(@IdRes drawableId: Int): MessageAttachmentsRobot {
        UIActions.wait.forViewWithId(takePhotoIconId)
        MockAddAttachmentIntent.mockCameraImageCapture(takePhotoIconId, drawableId)
        return this
    }

    private fun mockFileAttachment(@IdRes drawable: Int): MessageAttachmentsRobot {
        UIActions.wait.forViewWithId(addAttachmentIconId)
        MockAddAttachmentIntent.mockChooseAttachment(addAttachmentIconId, drawable)
        return this
    }

    companion object {
        private const val takePhotoIconId = R.id.take_photo
        private const val addAttachmentIconId = R.id.attach_file
    }
}
