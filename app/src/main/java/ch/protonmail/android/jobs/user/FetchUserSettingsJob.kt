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

import ch.protonmail.android.api.models.MailSettingsResponse
import ch.protonmail.android.api.models.UserInfo
import ch.protonmail.android.api.models.UserSettingsResponse
import ch.protonmail.android.api.models.address.AddressKeyActivationWorker
import ch.protonmail.android.api.models.address.AddressesResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.user.UserSettingsEvent
import ch.protonmail.android.jobs.FetchByLocationJob
import ch.protonmail.android.jobs.Priority
import ch.protonmail.android.jobs.ProtonMailBaseJob
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Params
import kotlin.time.seconds

class FetchUserSettingsJob(
    username: String? = null
) : ProtonMailBaseJob(Params(Priority.HIGH).groupBy(Constants.JOB_GROUP_MISC), username) {

    @Throws(Throwable::class)
    override fun onRun() {
        val fetchContactsEmails = entryPoint.fetchContactsEmailsWorkerEnqueuer()
        val fetchContactsData = entryPoint.fetchContactsDataWorkerEnqueuer()

        val userInfo: UserInfo
        val userSettings: UserSettingsResponse
        val mailSettings: MailSettingsResponse
        val addresses: AddressesResponse

        if (username != null) {
            userInfo = getApi().fetchUserInfoBlocking(username!!)
            userSettings = getApi().fetchUserSettings(username!!)
            mailSettings = getApi().fetchMailSettingsBlocking(username!!)
            addresses = getApi().fetchAddressesBlocking(username!!)
            getUserManager().setUserInfo(
                userInfo, username, mailSettings.mailSettings,
                userSettings.userSettings, addresses.addresses
            )

            val user = getUserManager().user
            AddressKeyActivationWorker.activateAddressKeysIfNeeded(
                applicationContext,
                addresses.addresses, user.username
            )
            user.notificationSetting = getUserManager().user.notificationSetting
            user.save()

            if (username == getUserManager().username) {
                // if primary
                AppUtil.deleteDatabases(ProtonMailApplication.getApplication(), username, true)
                getJobManager().addJobInBackground(
                    FetchByLocationJob(
                        Constants.MessageLocationType.INBOX,
                        null, true, null, false
                    )
                )
                fetchContactsData.enqueue()
                fetchContactsEmails.enqueue(2.seconds.toLongMilliseconds())
            } else {
                AppUtil.deleteDatabases(ProtonMailApplication.getApplication(), username, false)
            }
        } else {
            userInfo = getApi().fetchUserInfoBlocking()
            userSettings = getApi().fetchUserSettings()
            mailSettings = getApi().fetchMailSettingsBlocking()
            addresses = getApi().fetchAddressesBlocking()
            getUserManager().setUserInfo(
                userInfo, mailSettings = mailSettings.mailSettings,
                userSettings = userSettings.userSettings, addresses = addresses.addresses
            )

            val user = getUserManager().user
            AddressKeyActivationWorker.activateAddressKeysIfNeeded(
                applicationContext,
                addresses.addresses, user.username
            )
            user.notificationSetting = getUserManager().user.notificationSetting
            user.save()
        }
        AppUtil.postEventOnUi(UserSettingsEvent(userSettings.userSettings))
    }
}
