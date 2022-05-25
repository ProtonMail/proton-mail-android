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

package ch.protonmail.android.security.presentation

import android.app.Activity
import ch.protonmail.android.compose.presentation.ui.SetMessagePasswordActivity
import ch.protonmail.android.settings.pin.ChangePinActivity
import ch.protonmail.android.settings.pin.CreatePinActivity
import ch.protonmail.android.settings.pin.ValidatePinActivity
import me.proton.core.auth.presentation.ui.AuthActivity
import me.proton.core.usersettings.presentation.ui.PasswordManagementActivity
import javax.inject.Inject

class ScreenshotManager @Inject constructor() {

    fun shouldPreventScreenshot(activity: Activity): Boolean =
        when (activity) {
            is AuthActivity<*>,
            is ChangePinActivity,
            is CreatePinActivity,
            is PasswordManagementActivity,
            is SetMessagePasswordActivity,
            is ValidatePinActivity -> true
            else -> false
        }
}
