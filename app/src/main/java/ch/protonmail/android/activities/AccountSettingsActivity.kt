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
import ch.protonmail.android.R
import ch.protonmail.android.activities.settings.BaseSettingsActivity
import ch.protonmail.android.activities.settings.SettingsEnum
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.UiUtil
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.*
import java.util.*


class AccountSettingsActivity : BaseSettingsActivity() {

    override fun getLayoutId(): Int {
        return R.layout.activity_settings
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setTitle(R.string.account_settings)
        }

        mSnackLayout = findViewById(R.id.layout_no_connectivity_info)

        setUpSettingsItems(R.raw.acc_settings_structure)
        renderViews()
    }

    override fun onResume() {
        super.onResume()
        setUpSettingsItems(R.raw.acc_settings_structure)
        renderViews()
    }

    override fun renderViews() {
        refreshSettings(settingsUiList.minus(settingsUiList.filter { it.settingId == SettingsEnum.SEARCH.name.toLowerCase(Locale.ENGLISH) }))

        val plan = ProtonMailApplication.getApplication().organization?.planName
        val planType = Constants.PlanType.fromString(plan ?: "")
        val planName = when {
            plan.isNullOrEmpty() -> resources.getStringArray(R.array.account_type_names)[0]
            planType == Constants.PlanType.PLUS -> resources.getStringArray(R.array.account_type_names)[1]
            planType == Constants.PlanType.VISIONARY -> resources.getStringArray(R.array.account_type_names)[2]
            planType == Constants.PlanType.PROFESSIONAL -> resources.getStringArray(R.array.account_type_names)[3]
            else -> ""
        }
        setValue(SettingsEnum.SUBSCRIPTION, getString(R.string.protonmail) + " " + planName)

        mRecoveryEmail = mUserManager.userSettings?.notificationEmail ?: ""
        setValue(SettingsEnum.RECOVERY_EMAIL, if (!TextUtils.isEmpty(mRecoveryEmail)) mRecoveryEmail else getString(R.string.none))

        val usedSpace = UiUtil.readableFileSize(user.usedSpace)
        val maxSpace = UiUtil.readableFileSize(user.maxSpace)
        setValue(SettingsEnum.MAILBOX_SIZE, getString(R.string.storage_used, usedSpace, maxSpace))

        if (user.addresses != null && user.addresses.size > 0) {
            mSelectedAddress = user.addresses[0]
            mSignature = mSelectedAddress.signature
            setValue(SettingsEnum.DEFAULT_EMAIL, mSelectedAddress.email)
        }

        setValue(SettingsEnum.NOTIFICATION_SNOOZE, if (mUserManager.isSnoozeScheduledEnabled()) getString(R.string.scheduled_snooze_on) else getString(R.string.scheduled_snooze_off))

        mAttachmentStorageValue = user.maxAttachmentStorage

        val scope = CoroutineScope(IO)
        scope.launch {
            val attachmentSizeUsed = attachmentMetadataDatabase!!.getAllAttachmentsSizeUsed() / (1000.0 * 1000.0)
            setValue(SettingsEnum.LOCAL_STORAGE_LIMIT, String.format(getString(R.string.storage_value), mAttachmentStorageValue, attachmentSizeUsed))
        }

        mPinValue = user.isUsePin && !TextUtils.isEmpty(mUserManager.getMailboxPin())
        val autoLockSettingValue = if (mPinValue) getString(R.string.enabled) else getString(R.string.disabled)
        setValue(SettingsEnum.AUTO_LOCK, autoLockSettingValue)

        setValue(SettingsEnum.APP_VERSION, String.format(getString(R.string.app_version_code), AppUtil.getAppVersionName(this), AppUtil.getAppVersionCode(this)))
    }
}
