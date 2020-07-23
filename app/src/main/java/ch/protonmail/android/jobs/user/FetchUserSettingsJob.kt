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

import com.birbit.android.jobqueue.Params

import ch.protonmail.android.api.models.address.AddressKeyActivationWorker
import ch.protonmail.android.api.models.address.AddressesResponse
import ch.protonmail.android.api.models.UserSettingsResponse
import ch.protonmail.android.api.models.MailSettingsResponse
import ch.protonmail.android.api.models.UserInfo
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.user.UserSettingsEvent
import ch.protonmail.android.jobs.*
import ch.protonmail.android.utils.AppUtil

/**
 * Created by dino on 10/10/17.
 */

class FetchUserSettingsJob(username: String? = null) : ProtonMailBaseJob(Params(Priority.HIGH).groupBy(Constants.JOB_GROUP_MISC), username) {

    @Throws(Throwable::class)
    override fun onRun() {
        val userInfo: UserInfo
        val userSettings: UserSettingsResponse
        val mailSettings: MailSettingsResponse
        val addresses: AddressesResponse

        if (username != null) {
            userInfo = mApi.fetchUserInfo(username!!)
            userSettings = mApi.fetchUserSettings(username!!)
            mailSettings = mApi.fetchMailSettings(username!!)
            addresses = mApi.fetchAddresses(username!!)
            mUserManager.setUserInfo(userInfo, username, mailSettings.mailSettings, userSettings.userSettings, addresses.addresses)

            if (username == mUserManager.username) {
                // if primary
                AppUtil.deleteDatabases(ProtonMailApplication.getApplication(), username, true)
                mJobManager.addJobInBackground(FetchByLocationJob(Constants.MessageLocationType.INBOX, null, true, null, false))
                mJobManager.addJobInBackground(FetchContactsEmailsJob(2000))
                mJobManager.addJobInBackground(FetchContactsDataJob())
            } else {
                AppUtil.deleteDatabases(ProtonMailApplication.getApplication(), username, false)
            }
        } else {
            userInfo = mApi.fetchUserInfo()
            userSettings = mApi.fetchUserSettings()
            mailSettings = mApi.fetchMailSettings()
            addresses = mApi.fetchAddresses()
            mUserManager.setUserInfo(userInfo, mailSettings = mailSettings.mailSettings, userSettings = userSettings.userSettings, addresses = addresses.addresses)

            val user = mUserManager.user
            AddressKeyActivationWorker.activateAddressKeysIfNeeded(applicationContext, addresses.addresses, user.username)
            user.notificationSetting = mUserManager.user.notificationSetting
            user.save()
        }
        AppUtil.postEventOnUi(UserSettingsEvent(userSettings.userSettings))
    }
}
