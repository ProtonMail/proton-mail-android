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

package ch.protonmail.android.feature.account

import android.content.Context
import androidx.core.content.edit
import ch.protonmail.android.core.Constants
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.auth.domain.usecase.PostLoginAccountSetup
import me.proton.core.auth.presentation.DefaultUserCheck
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User

class SetupAccountUserCheck(
    context: Context,
    accountManager: AccountManager,
    userManager: UserManager,
    private val oldUserManager: ch.protonmail.android.core.UserManager,
) : DefaultUserCheck(context, accountManager, userManager) {

    override suspend fun invoke(user: User): PostLoginAccountSetup.UserCheckResult {
        // Workaround: Make sure we have the preference user name by userId.
        // See DatabaseFactory.usernameForUserId.
        oldUserManager.preferencesFor(user.userId).edit(commit = true) {
            putString(Constants.Prefs.PREF_USER_NAME, user.name)
        }
        // Workaround: Make sure this uninitialized User is fresh.
        oldUserManager.clearCache()
        return super.invoke(user)
    }
}
