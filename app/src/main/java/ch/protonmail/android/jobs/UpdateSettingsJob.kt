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

import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.events.SettingsChangedEvent
import ch.protonmail.android.feature.user.updateAddressBlocking
import ch.protonmail.android.feature.user.updateOrderBlocking
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Params

class UpdateSettingsJob(
    private val newDisplayName: String? = null,
    private val newSignature: String? = null,
    private val addressIds: List<String>? = null,
    private val actionLeftSwipeChanged: Boolean = false,
    private val actionRightSwipeChanged: Boolean = false,
    private val backPressed: Boolean = false,
    private val addressId: Id? = null
) : ProtonMailBaseJob(Params(Priority.LOW).requireNetwork()) {

    @Throws(Throwable::class)
    override fun onRun() {
        try {
            val userId = getUserManager().requireCurrentUserId()
            if (addressIds != null) {
                getUserAddressManager().updateOrderBlocking(userId, addressIds.map { Id(it) })
                getUserManager().clearCache()
            }
            if ((newDisplayName != null || newSignature != null) && addressId != null) {
                getUserAddressManager().updateAddressBlocking(userId, addressId, newDisplayName, newSignature)
                getUserManager().clearCache()
            }
            val mailSettings = getUserManager().getCurrentUserMailSettingsBlocking()!!
            if (actionLeftSwipeChanged) {
                getApi().updateLeftSwipe(mailSettings.leftSwipeAction)
            }
            if (actionRightSwipeChanged) {
                getApi().updateRightSwipe(mailSettings.rightSwipeAction)
            }
            if (mailSettings != null) {
                updateMailSettings(mailSettings)
            }
            AppUtil.postEventOnUi(SettingsChangedEvent(true, backPressed, null))
        } catch (e: Exception) {
            AppUtil.postEventOnUi(SettingsChangedEvent(false, backPressed, e.message))
        }
    }

    private fun updateMailSettings(mailSettings: MailSettings) {
        getApi().updateAutoShowImages(mailSettings.showImages)
    }
}
