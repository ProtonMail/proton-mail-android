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
import android.net.Uri
import timber.log.Timber

object ProtonCalendarUtils {

    private const val protonCalendarPackageName = "me.proton.android.calendar"

    /**
     * @return [true] if Proton Calendar is installed, [false] otherwise and tries to open Play Store
     */
    fun openPlayStoreIfNotInstalled(context: Context): Boolean {

        val protonCalendarIntent = context.packageManager.getLaunchIntentForPackage(protonCalendarPackageName)

        return if (protonCalendarIntent == null) {
            val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = Uri.parse("market://details?id=$protonCalendarPackageName")
            }

            try {
                context.startActivity(playStoreIntent)
            } catch (e: ActivityNotFoundException) {
                Timber.d("play store not installed when opening Proton Calendar with CTA")
            }

            false

        } else true
    }

}
