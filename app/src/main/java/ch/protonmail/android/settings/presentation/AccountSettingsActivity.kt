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
package ch.protonmail.android.settings.presentation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.activities.settings.BaseSettingsActivity
import ch.protonmail.android.activities.settings.SettingsEnum
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.core.Constants
import ch.protonmail.android.featureflags.FeatureFlagsManager
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.app
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.equalsNoCase
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AccountSettingsActivity : BaseSettingsActivity() {

    private val accountSettingsActivityViewModel: AccountSettingsActivityViewModel by viewModels()

    @Inject
    lateinit var featureFlags: FeatureFlagsManager

    override fun getLayoutId(): Int = R.layout.activity_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        mSnackLayout = findViewById(R.id.layout_no_connectivity_info)
    }

    override fun onResume() {
        super.onResume()
        setUpSettingsItems(R.raw.acc_settings_structure)
        renderViews()
    }

    override fun renderViews() {
        refreshSettings(
            if (featureFlags.isChangeViewModeFeatureEnabled()) {
                settingsUiList
            } else {
                settingsUiList.filterNot { it.settingId equalsNoCase SettingsEnum.CONVERSATION_MODE.name }
            }
        )

        val plan = app.organization?.planName
        val planType = Constants.PlanType.fromString(plan ?: "")
        val planName = when {
            plan.isNullOrEmpty() -> resources.getStringArray(R.array.account_type_names)[0]
            planType == Constants.PlanType.PLUS -> resources.getStringArray(R.array.account_type_names)[1]
            planType == Constants.PlanType.VISIONARY -> resources.getStringArray(R.array.account_type_names)[2]
            planType == Constants.PlanType.PROFESSIONAL -> resources.getStringArray(R.array.account_type_names)[3]
            else -> ""
        }
        setValue(SettingsEnum.SUBSCRIPTION, getString(R.string.protonmail) + " " + planName)

        val (used, total) = with(user.dedicatedSpace) { used.l to total.l }
        val usedSpace = UiUtil.readableFileSize(used.toLong())
        val maxSpace = UiUtil.readableFileSize(total.toLong())
        setValue(SettingsEnum.MAILBOX_SIZE, getString(R.string.storage_used, usedSpace, maxSpace))

        if (user.addresses.hasAddresses) {
            selectedAddress = checkNotNull(user.addresses.primary)
            mSignature = selectedAddress.signature?.s ?: EMPTY_STRING
            setValue(SettingsEnum.DEFAULT_EMAIL, selectedAddress.email.s)
        }

        val snoozeValue =
            if (userManager.isCurrentUserSnoozeScheduledEnabled()) getString(R.string.scheduled_snooze_on)
            else getString(R.string.scheduled_snooze_off)
        setValue(SettingsEnum.NOTIFICATION_SNOOZE, snoozeValue)

        mAttachmentStorageValue = legacyUser.maxAttachmentStorage

        lifecycleScope.launch {
            val attachmentSizeUsedBytes = attachmentMetadataDao.getAllAttachmentsSizeUsed().first() ?: 0
            val attachmentSizeUsed = attachmentSizeUsedBytes / (1000.0 * 1000.0)
            setValue(
                SettingsEnum.LOCAL_STORAGE_LIMIT,
                String.format(getString(R.string.storage_value), mAttachmentStorageValue, attachmentSizeUsed)
            )
        }

        setupViewMode()
    }

    /**
     * Shows the current ViewMode setting and listen for user-triggered changes
     * This will only have an effect when `FeatureFlags.isChangeViewModeFeatureEnabled()` is true.
     * When feature flag is false, no item will be shown in settings and this will have no effect.
     */
    private fun setupViewMode() {
        val mailSettings = mUserManager.getCurrentUserMailSettingsBlocking()
        Timber.d("MailSettings ViewMode = ${mailSettings?.viewMode}")

        setEnabled(SettingsEnum.CONVERSATION_MODE, mailSettings?.viewMode == ViewMode.ConversationGrouping)
        setupViewModeChangedListener(mailSettings)
    }

    private fun setupViewModeChangedListener(mailSettings: MailSettings?) {
        setToggleListener(SettingsEnum.CONVERSATION_MODE) { _, isEnabled ->
            mailSettings?.viewMode = if (isEnabled) ViewMode.ConversationGrouping else ViewMode.NoConversationGrouping
            mailSettings?.saveBlocking(
                SecureSharedPreferences.getPrefsForUser(this@AccountSettingsActivity, user.id)
            )
            accountSettingsActivityViewModel.changeViewMode(mailSettings?.viewMode ?: ViewMode.ConversationGrouping)
        }
    }
}
