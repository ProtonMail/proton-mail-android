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

package ch.protonmail.android.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import timber.log.Timber
import javax.inject.Inject

class ProtonCalendarUtils @Inject constructor(private val context: Context) {

    /**
     * If ProtonCalendar is installed but outdated (can't handle ICS files) we shouldn't show the button.
     */
    fun shouldShowProtonCalendarButton(packageManager: PackageManager): Boolean {

        val protonCalendarIsInstalled = packageManager
            .getLaunchIntentForPackage(packageName) != null

        val protonCalendarIntent = Intent(actionOpenIcs).apply {
            setDataAndType(
                Uri.parse(intentContentUri),
                mimeType
            )
            setPackage(packageName)
        }

        val protonCalendarCanHandleIcs = packageManager.resolveActivity(protonCalendarIntent, 0) != null

        return if (protonCalendarIsInstalled) {
            protonCalendarCanHandleIcs
        } else true
    }

    /**
     * @return [true] if Proton Calendar is installed, [false] otherwise and tries to open Play Store
     */
    fun openPlayStoreIfNotInstalled(): Boolean {

        val protonCalendarIsInstalled = context.packageManager.getLaunchIntentForPackage(packageName) != null

        return if (!protonCalendarIsInstalled) {
            val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = Uri.parse("market://details?id=$packageName")
            }

            try {
                context.startActivity(playStoreIntent)
            } catch (e: ActivityNotFoundException) {
                Timber.d(e, "play store not installed when opening Proton Calendar with CTA")
            }

            false

        } else true
    }

    /**
     * @return original recipient email extracted from message headers
     */
    fun extractRecipientEmail(headers: String): String? {
        return headers.split("\n").firstOrNull {
            it.startsWith("$recipientEmailHeader:")
        }?.substringAfter("$recipientEmailHeader:")?.trim()
    }

    companion object {
        const val packageName = "me.proton.android.calendar"
        const val actionOpenIcs = "me.proton.android.calendar.intent.action.CTA_OPEN_ICS"
        const val mimeType = "text/calendar"
        const val recipientEmailHeader = "X-Original-To"
        const val intentContentUri = "content://proton/calendar/intent/check/invite.ics"
        const val intentExtraSenderEmail = "me.proton.android.calendar.intent.extra.ICS_SENDER_EMAIL"
        const val intentExtraRecipientEmail = "me.proton.android.calendar.intent.extra.ICS_RECIPIENT_EMAIL"
    }

}
