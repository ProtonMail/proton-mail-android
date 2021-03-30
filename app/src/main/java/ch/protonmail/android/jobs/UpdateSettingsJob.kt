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

import android.text.TextUtils
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.events.AuthStatus
import ch.protonmail.android.events.SettingsChangedEvent
import ch.protonmail.android.featureflags.FeatureFlagsManager
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.ConstantTime
import com.birbit.android.jobqueue.Params
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import java.util.ArrayList

class UpdateSettingsJob(
    private val displayChanged: Boolean = false,
    private val newDisplayName: String = "",
    private val signatureChanged: Boolean = false,
    private val newSignature: String = "",
    private val sortAliasChanged: Boolean = false,
    private val actionLeftSwipeChanged: Boolean = false,
    private val actionRightSwipeChanged: Boolean = false,
    private val backPressed: Boolean = false,
    private val addressId: String = "",
    private val featureFlags: FeatureFlagsManager = FeatureFlagsManager()
) : ProtonMailBaseJob(Params(Priority.LOW).requireNetwork()) {

    @Throws(Throwable::class)
    override fun onRun() {
        try {
            val user = getUserManager().requireCurrentLegacyUserBlocking()
            if (sortAliasChanged) {
                val addresses = getUserManager().user.addresses
                val aliasSize = addresses.size
                val newAliasesOrder = ArrayList<String>(aliasSize)
                for (i in 0 until aliasSize) {
                    newAliasesOrder.add(addresses[i].id)
                }
                getApi().updateAlias(newAliasesOrder)
            }
            if (displayChanged && addressId.isNotBlank()) {
                val addresses = user.addresses
                for (address in addresses) {
                    if (address.id == addressId) {
                        val responseBody = getApi().editAddress(address.id, newDisplayName, address.signature)
                        Timber.d(
                            "editAddress address: ${address.email} " +
                                "new display name: $newDisplayName " +
                                "response: ${responseBody.code}"
                        )
                        address.displayName = newDisplayName
                        break
                    }
                }
                user.setAddresses(addresses)
                user.save()
            }
            if (signatureChanged && !TextUtils.isEmpty(addressId)) {
                val addresses = user.addresses
                for (address in user.addresses) {
                    if (address.id == addressId) {
                        getApi().editAddress(address.id, address.displayName, newSignature)
                        address.signature = newSignature
                        break
                    }
                }
                user.setAddresses(addresses)
                user.save()
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

        if (featureFlags.isChangeViewModeFeatureEnabled()) {
            getApi().updateViewMode(mailSettings.viewMode)
        }
    }
}
