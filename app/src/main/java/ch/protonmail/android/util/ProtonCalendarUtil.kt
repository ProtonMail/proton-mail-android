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

package ch.protonmail.android.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.EmailAddress
import java.util.Locale
import javax.inject.Inject

class ProtonCalendarUtil @Inject constructor(private val context: Context) {

    private val packageManager = context.packageManager

    fun isProtonCalendarInstalled() = packageManager
        .getLaunchIntentForPackage(PROTON_CALENDAR_PACKAGE_NAME) != null

    /**
     * @return `true` if Proton Calender is NOT installed or is installed and CAN handle ICS files.
     */
    fun shouldShowProtonCalendarButton(): Boolean {
        val protonCalendarIntent = Intent(ACTION_OPEN_ICS)
            .setDataAndType(Uri.parse(INTENT_CONTENT_URI_STRING), CALENDAR_MIME_TYPE)
            .setPackage(PROTON_CALENDAR_PACKAGE_NAME)

        val canCalendarHandleIcs = packageManager.resolveActivity(protonCalendarIntent, 0) != null

        return isProtonCalendarInstalled().not() || canCalendarHandleIcs
    }

    /**
     * @throws ActivityNotFoundException if Proton Calendar is not installed; ensure to call
     *  [isProtonCalendarInstalled] first
     */
    fun openIcsInProtonCalendar(uri: Uri, senderEmail: EmailAddress, recipientEmail: EmailAddress) {
        val intent = Intent(ACTION_OPEN_ICS).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            setDataAndType(uri, CALENDAR_MIME_TYPE)
            setPackage(PROTON_CALENDAR_PACKAGE_NAME)
            putExtra(EXTRA_SENDER_EMAIL, senderEmail.s)
            putExtra(EXTRA_RECIPIENT_EMAIL, recipientEmail.s)
        }

        context.startActivity(intent)
    }

    /**
     * @throws ActivityNotFoundException if Play Store is not installed
     */
    fun openProtonCalendarOnPlayStore() {
        val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.parse("market://details?id=$PROTON_CALENDAR_PACKAGE_NAME")
        }

        context.startActivity(playStoreIntent)
    }

    /**
     * @return `true` is [message] has any attachments that can be opened with Proton Calendar
     */
    fun hasCalendarAttachment(message: Message): Boolean =
        message.attachments.any(::isCalendarAttachment)

    /**
     * @return the first [Attachment] that can be opened with Proton Calendar or `null` if none is found
     */
    fun getCalendarAttachment(message: Message): Attachment? =
        message.attachments.find(::isCalendarAttachment)

    /**
     * @return the first [Attachment] that can be opened with Proton Calendar
     * @throws IllegalArgumentException throw if none is found
     */
    fun requireCalendarAttachment(message: Message): Attachment =
        requireNotNull(getCalendarAttachment(message)) {
            "No Proton Calendar attachment found. Total attachments: ${message.attachments.size}"
        }

    /**
     * @return original recipient email extracted from message headers if any, `null` otherwise
     */
    fun extractRecipientEmailOrNull(headers: String): EmailAddress? = headers
        .split("\n")
        .firstOrNull { it.startsWith("$RECIPIENT_EMAIL_HEADER:") }
        ?.substringAfter("$RECIPIENT_EMAIL_HEADER:")?.trim()
        ?.let(::EmailAddress)

    private fun isCalendarAttachment(attachment: Attachment): Boolean {
        val allMimeType = attachment.mimeType?.lowercase(Locale.getDefault())?.split(";")
            ?: return false
        return CALENDAR_MIME_TYPE in allMimeType
    }

    companion object {
        private const val PROTON_CALENDAR_PACKAGE_NAME = "me.proton.android.calendar"
        private const val ACTION_OPEN_ICS = "me.proton.android.calendar.intent.action.CTA_OPEN_ICS"
        private const val CALENDAR_MIME_TYPE = "text/calendar"
        private const val INTENT_CONTENT_URI_STRING = "content://proton/calendar/intent/check/invite.ics"

        private const val RECIPIENT_EMAIL_HEADER = "X-Original-To"

        @VisibleForTesting
        const val EXTRA_SENDER_EMAIL = "me.proton.android.calendar.intent.extra.ICS_SENDER_EMAIL"

        @VisibleForTesting
        const val EXTRA_RECIPIENT_EMAIL = "me.proton.android.calendar.intent.extra.ICS_RECIPIENT_EMAIL"
    }

}
