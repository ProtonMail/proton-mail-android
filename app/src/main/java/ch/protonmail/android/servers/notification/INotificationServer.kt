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
package ch.protonmail.android.servers.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.net.Uri
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.sendingFailedNotifications.SendingFailedNotification
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.api.models.room.notifications.Notification as RoomNotification

interface INotificationServer {

    fun createRetrievingNotificationsNotification(): Notification

    fun createEmailsChannel(): String

    fun createAttachmentsChannel(): String

    fun createAccountChannel(): String

    fun notifyUserLoggedOut(user: User?)

    fun notifyVerificationNeeded(
        username: String,
        messageSubject: String?,
        messageId: String?,
        messageInline: Boolean,
        messageAddressId: String?
    )

    fun notifyAboutAttachment(
        filename: String,
        uri: Uri,
        mimeType: String?,
        showNotification: Boolean
    )

    /**
     * Show a Notification for a SINGLE new Email. This will be called ONLY if there are not other
     * unread Notifications
     *
     * @param userManager // FIXME: FIND A BETTER SOLUTION - [UserManager] cannot be instantiated on Main Thread :/
     * @param user current logged [User]
     * @param message [Message] received to show to the user
     * @param messageId [String] id for retrieve the [Message] details
     * @param notificationBody [String] body of the Notification
     * @param sender [String] name of the sender of the email
     */
    @SuppressLint("LongParameterList")
    fun notifySingleNewEmail(
        userManager: UserManager,
        user: User,
        message: Message?,
        messageId: String,
        notificationBody: String?,
        sender: String,
        primaryUser: Boolean
    )

    fun notifyMultipleUnreadEmail(
        userManager: UserManager,
        loggedInUser: User,
        unreadNotifications: List<RoomNotification>
    )

    fun notifySingleErrorSendingMessage(
        error: String,
        username: String
    )

    fun notifyMultipleErrorSendingMessage(
        unreadSendingFailedNotifications: List<SendingFailedNotification>,
        user: User
    )

    fun notifySaveDraftError(errorMessage: String, messageSubject: String?, username: String)
}
