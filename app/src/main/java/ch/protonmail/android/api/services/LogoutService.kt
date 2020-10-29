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

import android.content.Intent
import androidx.core.app.JobIntentService
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.worker.LogoutWorker
import ch.protonmail.libs.core.utils.takeIfNotBlank
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.TagConstraint
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

// region constants
private const val ACTION_LOGOUT_ONLINE = "ACTION_LOGOUT_ONLINE"
private const val ACTION_LOGOUT_OFFLINE = "ACTION_LOGOUT_OFFLINE"
private const val EXTRA_USERNAME = "EXTRA_USERNAME"
private const val EXTRA_FCM_REGISTRATION_ID = "EXTRA_FCM_REGISTRATION_ID"
// endregion

@AndroidEntryPoint
class LogoutService : JobIntentService() {

    @Inject
    internal lateinit var jobManager: JobManager

    @Inject
    internal lateinit var networkUtils: QueueNetworkUtil

    @Inject
    internal lateinit var userManager: UserManager

    @Inject
    internal lateinit var api: ProtonMailApiManager

    @Inject
    internal lateinit var logoutWorkerEnqueuer: LogoutWorker.Enqueuer

    override fun onHandleWork(intent: Intent) {
        when (intent.action) {
            ACTION_LOGOUT_ONLINE -> logoutOnline(
                intent.getStringExtra(EXTRA_USERNAME),
                intent.getStringExtra(EXTRA_FCM_REGISTRATION_ID) ?: ""
            )
            ACTION_LOGOUT_OFFLINE -> logoutOffline(intent.getStringExtra(EXTRA_USERNAME))
        }
    }

    private fun logoutOffline(username: String?) {
        try {
            val hasMailboxPassword = username?.takeIfNotBlank()?.let {
                userManager.getMailboxPassword(username)?.let { psw ->
                    String(psw).isNotBlank()
                }
            } ?: false
            if (hasMailboxPassword) {
                username!! // Can not be null if `hasMailboxPassword` is `true`
                api.revokeAccessBlocking(username)
                TokenManager.clearInstance(username)
            }
            jobManager.cancelJobs(TagConstraint.ALL)
            jobManager.clear()
        } catch (ioException: IOException) {
            Timber.e(ioException, "LogoutOffline exception")
        }
    }

    private fun logoutOnline(username: String?, fcmRegistrationId: String) {
        jobManager.cancelJobs(TagConstraint.ALL)
        jobManager.clear()
        logoutWorkerEnqueuer.enqueue(username ?: userManager.username, fcmRegistrationId)
    }

    companion object {

        @JvmOverloads
        fun startLogout(online: Boolean, username: String? = null, fcmRegistrationId: String? = null) {
            Timber.v("start Logout online:$online, username:$username")
            val context = ProtonMailApplication.getApplication()
            val intent = Intent(context, LogoutService::class.java)
            intent.action = if (online) ACTION_LOGOUT_ONLINE else ACTION_LOGOUT_OFFLINE
            intent.putExtra(EXTRA_USERNAME, username)
            intent.putExtra(EXTRA_FCM_REGISTRATION_ID, fcmRegistrationId)
            enqueueWork(context, LogoutService::class.java, Constants.JOB_INTENT_SERVICE_ID_LOGOUT, intent)
        }
    }
}
