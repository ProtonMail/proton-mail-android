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

package ch.protonmail.android.usecase.keys

import ch.protonmail.android.R
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.utils.resources.StringResourceResolver
import javax.inject.Inject

class LogOutIfNotAllActiveKeysAreDecryptable @Inject constructor(
    private val userManager: UserManager,
    private val areActiveKeysDecryptable: CheckIfActiveKeysAreDecryptable,
    private val userNotifier: UserNotifier,
    private val getStringResource: StringResourceResolver
) {

    operator fun invoke(): Boolean {
        return if (areActiveKeysDecryptable()) {
            false
        } else {
            userManager.logoutLastActiveAccount()
            userNotifier.showError(getStringResource(R.string.logged_out_description))
            true
        }
    }
}
