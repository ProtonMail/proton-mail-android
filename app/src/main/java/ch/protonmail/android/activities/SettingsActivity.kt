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
package ch.protonmail.android.activities

import android.os.Bundle
import android.text.TextUtils
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.R
import ch.protonmail.android.activities.settings.BaseSettingsActivity
import ch.protonmail.android.activities.settings.SettingsEnum
import ch.protonmail.android.events.FetchLabelsEvent
import ch.protonmail.android.utils.PREF_CUSTOM_APP_LANGUAGE
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import me.proton.core.util.kotlin.EMPTY_STRING

@AndroidEntryPoint
class SettingsActivity : BaseSettingsActivity() {

    override fun getLayoutId(): Int = R.layout.activity_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setTitle(R.string.settings)
        }

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        mSnackLayout = findViewById(R.id.layout_no_connectivity_info)

        setUpSettingsItems(R.raw.settings_structure)
        renderViews()
    }

    override fun onResume() {
        super.onResume()
        setUpSettingsItems(R.raw.settings_structure)
        renderViews()
    }

    override fun renderViews() {

        val primaryAddress = checkNotNull(user.addresses.primary)
        mDisplayName = primaryAddress.displayName?.s
            ?: primaryAddress.email.s
        setHeader(SettingsEnum.ACCOUNT, mDisplayName)

        if (user.addresses.hasAddresses) {
            selectedAddress = checkNotNull(user.addresses.primary)
            mSignature = selectedAddress.signature?.s ?: EMPTY_STRING
            setValue(SettingsEnum.ACCOUNT, selectedAddress.email.s)
        }

        val languageValues = resources.getStringArray(R.array.custom_language_values)
        val languageLabels = resources.getStringArray(R.array.custom_language_labels)
        val appLanguage = sharedPreferences!!.getString(PREF_CUSTOM_APP_LANGUAGE, "")
        if (appLanguage.isNullOrEmpty()) {
            setValue(SettingsEnum.APP_LANGUAGE, getString(R.string.auto_detect))
        } else {
            for (i in languageLabels.indices) {
                if (languageValues[i] == appLanguage) {
                    setValue(SettingsEnum.APP_LANGUAGE, languageLabels[i])
                    break
                }
            }
        }

        mPinValue = legacyUser.isUsePin && !TextUtils.isEmpty(mUserManager.getMailboxPin())
        val autoLockSettingValue = if (mPinValue) getString(R.string.enabled) else getString(R.string.disabled)
        setValue(SettingsEnum.AUTO_LOCK, autoLockSettingValue)

        val allowSecureConnectionsViaThirdPartiesSettingValue =
            if (legacyUser.allowSecureConnectionsViaThirdParties) getString(R.string.allowed) else getString(
                R.string.denied
            )
        setValue(SettingsEnum.CONNECTIONS_VIA_THIRD_PARTIES, allowSecureConnectionsViaThirdPartiesSettingValue)

        setValue(
            SettingsEnum.COMBINED_CONTACTS,
            if (legacyUser.combinedContacts) getString(R.string.enabled) else getString(R.string.disabled)
        )

        setValue(
            SettingsEnum.APP_VERSION,
            String.format(
                getString(R.string.app_version_code),
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            )
        )
    }

    @Subscribe
    override fun onLabelsLoadedEvent(event: FetchLabelsEvent) {
        super.onLabelsLoadedEvent(event)
    }
}
