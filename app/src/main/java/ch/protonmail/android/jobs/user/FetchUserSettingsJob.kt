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
package ch.protonmail.android.jobs.user

import ch.protonmail.android.api.models.address.AddressKeyActivationWorker
import ch.protonmail.android.core.Constants
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.events.user.UserSettingsEvent
import ch.protonmail.android.jobs.FetchByLocationJob
import ch.protonmail.android.jobs.Priority
import ch.protonmail.android.jobs.ProtonMailBaseJob
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Params
import timber.log.Timber
import kotlin.time.seconds

class FetchUserSettingsJob(
    userId: Id? = null
) : ProtonMailBaseJob(Params(Priority.HIGH).groupBy(Constants.JOB_GROUP_MISC), userId) {

    @Throws(Throwable::class)
    override fun onRun() {
        val fetchContactsEmails = entryPoint.fetchContactsEmailsWorkerEnqueuer()
        val fetchContactsData = entryPoint.fetchContactsDataWorkerEnqueuer()

        Timber.v("FetchUserSettingsJob started for user: $userId")

        val userId = userId
            ?: getUserManager().currentUserId
            ?: run {
                Timber.w("Cannot fetch user settings as there is no logged in user")
                return
            }

        val user = getUserManager().getLegacyUserBlocking(userId)
        val userSettings = getApi().fetchUserSettings(userId)
        val mailSettings = getApi().fetchMailSettingsBlocking(userId)
        val addresses = getApi().fetchAddressesBlocking(userId)
        getUserManager().setUserDetailsBlocking(
            user,
            addresses.addresses,
            mailSettings.mailSettings,
            userSettings.userSettings
        )

        AddressKeyActivationWorker.activateAddressKeysIfNeeded(
            applicationContext,
            addresses.addresses,
            userId
        )
        getUserManager().getCurrentLegacyUserBlocking()?.let { currentUser ->
            user.notificationSetting = currentUser.notificationSetting
        }
        user.save()

        val isCurrentUser = userId == getUserManager().currentUserId
        if (isCurrentUser) {
            AppUtil.deleteDatabases(applicationContext, userId, true)
            getJobManager().addJobInBackground(
                FetchByLocationJob(
                    Constants.MessageLocationType.INBOX,
                    null,
                    true,
                    null,
                    false
                )
            )
            fetchContactsData.enqueue()
            fetchContactsEmails.enqueue(2.seconds.toLongMilliseconds())
        } else {
            AppUtil.deleteDatabases(applicationContext, userId, false)
        }

        AppUtil.postEventOnUi(UserSettingsEvent(userSettings.userSettings))
    }
}
