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
package ch.protonmail.android.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import ch.protonmail.android.R
import ch.protonmail.android.utils.extensions.showToast
import timber.log.Timber

/**
 * An utility for Google Chrome
 *
 * @author Davide Farella
 */
// object ChromeUtils {

/** Name of Google Chrome package  */
private const val CHROME_PACKAGE_NAME = "com.android.chrome"

/** Send the user to Chrome settings page if installed but disabled, else to Play Store's page */
fun FragmentActivity.redirectToChrome() {
    finish()

    try {
        packageManager.getPackageInfo(CHROME_PACKAGE_NAME, 0)

        // Chrome is installed but may be disabled
        showToast(R.string.error_chrome_disabled)
        openChromeSettings()
    } catch (e: PackageManager.NameNotFoundException) {
        Timber.i(e, "redirectToChrome exception")
        // Chrome is not installed
        showToast(R.string.error_chrome_not_found)
        openChromeOnStore()
    }
}

/** Open the Store on Google Chrome page, if any store available  */
@VisibleForTesting
internal fun FragmentActivity.openChromeOnStore() {
    try {
        val uri = Uri.parse("market://details?id=$CHROME_PACKAGE_NAME")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (notFoundException: ActivityNotFoundException) {
        Timber.i(notFoundException, "openChromeOnStore exception")
        // Store not found!
    }
}

/** Open the Settings on Google Chrome page  */
@VisibleForTesting
internal fun FragmentActivity.openChromeSettings() {
    val uri = Uri.fromParts("package", CHROME_PACKAGE_NAME, null)
    val intent = Intent()
        .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(uri)
    startActivity(intent)
}
// }
