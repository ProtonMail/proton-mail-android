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

package ch.protonmail.android.navigation.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.feature.account.AccountStateManager
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.usecase.IsAppInDarkMode
import ch.protonmail.android.utils.notifier.UserNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.report.presentation.entity.BugReportOutput
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class NavigationViewModel @Inject constructor(
    private val isAppInDarkMode: IsAppInDarkMode,
    private val secureSharedPreferencesFactory: SecureSharedPreferences.Factory,
    private val accountStateManager: AccountStateManager,
    private val userNotifier: UserNotifier,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val bugReportResultMessageMutableFlow = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val bugReportResultMessageFlow = bugReportResultMessageMutableFlow.asSharedFlow()

    suspend fun verifyPrimaryUserId(userId: UserId): Boolean = withContext(dispatchers.Io) {
        val prefs = secureSharedPreferencesFactory.userPreferences(userId)
        return@withContext if (prefs.getString(Constants.Prefs.PREF_USER_NAME, null) == null) {
            Timber.e("Did not find username for the current primary user id. Logging the user out.")
            accountStateManager.signOut(userId)
            userNotifier.showError(R.string.logged_out_description)
            false
        } else {
            true
        }
    }

    fun isAppInDarkMode(context: Context) = isAppInDarkMode.invoke(context)

    fun onBugReportSent(bugReportOutput: BugReportOutput) {
        bugReportResultMessageMutableFlow.tryEmit(
            if (bugReportOutput is BugReportOutput.SuccessfullySent) R.string.received_report
            else R.string.not_received_report
        )
    }
}
