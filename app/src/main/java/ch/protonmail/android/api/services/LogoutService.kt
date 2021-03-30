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
package ch.protonmail.android.api.services

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.worker.LogoutWorker
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.TagConstraint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

// region constants
private const val ACTION_LOGOUT_ONLINE = "action.logout.online"
private const val ACTION_LOGOUT_OFFLINE = "action.logout.offline"
private const val EXTRA_USER_ID = "extra.user.id"
private const val EXTRA_FCM_REGISTRATION_ID = "extra.fcm.registration.id"
// endregion

@AndroidEntryPoint
class LogoutService : JobIntentService() {

    @Inject
    internal lateinit var accountManager: AccountManager

    @Inject
    internal lateinit var jobManager: JobManager

    @Inject
    internal lateinit var userManager: UserManager

    @Inject
    internal lateinit var api: ProtonMailApiManager

    @Inject
    internal lateinit var logoutWorkerEnqueuer: LogoutWorker.Enqueuer

    override fun onHandleWork(intent: Intent) {
        val userId = Id(checkNotNull(intent.getStringExtra(EXTRA_USER_ID)))
        when (intent.action) {
            ACTION_LOGOUT_ONLINE -> logoutOnline(userId, intent.getStringExtra(EXTRA_FCM_REGISTRATION_ID))
            ACTION_LOGOUT_OFFLINE -> logoutOffline(userId)
        }
    }

    private fun logoutOffline(userId: Id) {
        try {
            runBlocking { accountManager.disableAccount(UserId(userId.s)) }
            TokenManager.clearInstance(userId)
            jobManager.cancelJobs(TagConstraint.ALL)
            jobManager.clear()
        } catch (ioException: IOException) {
            Timber.e(ioException, "LogoutOffline exception")
        }
    }

    private fun logoutOnline(userId: Id, fcmRegistrationId: String?) {
        jobManager.cancelJobs(TagConstraint.ALL)
        jobManager.clear()
        logoutWorkerEnqueuer.enqueue(userId, fcmRegistrationId)
    }

    companion object {

        @JvmOverloads
        fun startLogout(context: Context, userId: Id, online: Boolean, fcmRegistrationId: String? = null) {
            Timber.v("Start Logout for user: $userId - online: $online")
            val intent = Intent(context, LogoutService::class.java)
                .setAction(if (online) ACTION_LOGOUT_ONLINE else ACTION_LOGOUT_OFFLINE)
                .putExtra(EXTRA_USER_ID, userId.s)
                .putExtra(EXTRA_FCM_REGISTRATION_ID, fcmRegistrationId)
            enqueueWork(context, LogoutService::class.java, Constants.JOB_INTENT_SERVICE_ID_LOGOUT, intent)
        }

    }
}
