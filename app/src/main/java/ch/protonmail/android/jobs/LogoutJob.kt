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
package ch.protonmail.android.jobs

import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.fcm.FcmUtil
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.Logger
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint

class LogoutJob(username: String) : ProtonMailBaseJob(Params(Priority.HIGH).persist(), username) {

    @Throws(Throwable::class)
    override fun onRun() {
        if (!getQueueNetworkUtil().isConnected()) {
            AppUtil.postEventOnUi(LogoutEvent(Status.NO_NETWORK))
        } else {
            // Unregister GCM only if this is the last user on device
            // TODO this is workaround for server unregistering all sessions for this device
            // TODO instead of just session for this user
            val accountManager = AccountManager.getInstance(ProtonMailApplication.getApplication())
            val loggedInUsers = accountManager.getLoggedInUsers()
            if (loggedInUsers.isEmpty() || (loggedInUsers.size == 1 && loggedInUsers[0] == username)) {
                Logger.doLog("unregistering from GCM")
                val registrationId = FcmUtil.getRegistrationId()
                if (registrationId.isNotEmpty()) {
                    getApi().unregisterDevice(registrationId)
                }
                AppUtil.postEventOnUi(LogoutEvent(Status.SUCCESS, username))
            }
            accountManager.clear()
            // Revoke access token through API
            if (username.isNotEmpty()) {
                getApi().revokeAccess(username)
            }
        }
        AppUtil.deleteSecurePrefs(username, getUserManager().nextLoggedInAccountOtherThanCurrent == null)
        getUserManager().getTokenManager(username)?.clear()
        TokenManager.clearInstance(username)
    }

    override fun onProtonCancel(cancelReason: Int, throwable: Throwable?) {
        AccountManager.getInstance(ProtonMailApplication.getApplication()).clear()
    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint? {
        return if (!(throwable.cause is IllegalArgumentException && (throwable.cause as IllegalArgumentException).message == "value == null"))
            RetryConstraint.RETRY
        else
            RetryConstraint.CANCEL
    }

    override fun getRetryLimit(): Int {
        return 3
    }
}
