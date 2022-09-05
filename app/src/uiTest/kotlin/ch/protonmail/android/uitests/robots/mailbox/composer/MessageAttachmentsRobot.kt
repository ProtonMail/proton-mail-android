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
package ch.protonmail.android.uitests.robots.mailbox.composer

import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatImageButton
import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.MockAddAttachmentIntent
import ch.protonmail.android.uitests.testsHelper.TestData
import me.proton.fusion.Fusion

/**
 * Class represents Message Attachments.
 */
open class MessageAttachmentsRobot : Fusion {

    fun addImageCaptureAttachment(@IdRes drawable: Int): MessageAttachmentsRobot =
        mockCameraImageCapture(drawable)

    fun addTwoImageCaptureAttachments(
        @IdRes firstDrawable: Int,
        @IdRes secondDrawable: Int
    ): ComposerRobot =
        mockCameraImageCapture(firstDrawable)
            .mockCameraImageCapture(secondDrawable)
            .navigateUpToComposer()

    fun addFileAttachment(@IdRes drawable: Int): MessageAttachmentsRobot {
        intent.init()
        mockFileAttachment(drawable)
        intent.release()
        return this
    }

    fun removeAttachmentAtPosition(position: Int): MessageAttachmentsRobot =
        waitForLoadingToBeNotVisible().removeAttachment(position)

    private fun removeAttachment(position: Int): MessageAttachmentsRobot {
        listView
            .onListItem()
            .inAdapterView(view.withId(R.id.attachment_list))
            .atPosition(position)
            .onChild(view.withId(R.id.remove)).click()
        return this
    }

    private fun waitForLoadingToBeNotVisible(): MessageAttachmentsRobot {
        view.withText(R.string.sync_attachments).waitForNotDisplayed()
        return this
    }

    fun navigateUpToComposer(): ComposerRobot {
        view.instanceOf(AppCompatImageButton::class.java).hasParent(view.withId(R.id.toolbar)).click()
        return ComposerRobot()
    }

    private fun mockCameraImageCapture(@IdRes drawableId: Int): MessageAttachmentsRobot {
        intent.init()
        view.withId(takePhotoIconId).waitForDisplayed()
        MockAddAttachmentIntent.mockCameraImageCapture(takePhotoIconId, drawableId)
        intent.release()
        return this
    }

    private fun mockFileAttachment(@IdRes drawable: Int): ComposerRobot {
        view.withId(addAttachmentIconId).checkIsDisplayed()
        MockAddAttachmentIntent.mockChooseAttachment(addAttachmentIconId, TestData.pngFile)
        return ComposerRobot()
    }

    companion object {

        private const val takePhotoIconId = R.id.take_photo
        private const val addAttachmentIconId = R.id.attach_file
    }
}
