/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.settings.presentation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import arrow.core.Either
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.UNLIMITED_ATTACHMENT_STORAGE
import ch.protonmail.android.featureflags.FeatureFlagsManager
import ch.protonmail.android.settings.domain.usecase.GetMailSettings
import ch.protonmail.android.settings.presentation.model.SettingsEnum
import ch.protonmail.android.utils.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
        accountSettingsActivityViewModel.getRecoveryEmailFlow()
            .onEach {
                setValue(
                    SettingsEnum.RECOVERY_EMAIL,
                    it
                )
            }
            .launchIn(lifecycleScope)
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
            val attachmentSizeUsed = attachmentSizeUsedBytes.toFloat() / Constants.BYTE_TO_MEGABYTE_RATIO
            val storageValueString = if (mAttachmentStorageValue == UNLIMITED_ATTACHMENT_STORAGE) {
                String.format(
                    getString(R.string.settings_storage_limit_unlimited_value),
                    attachmentSizeUsed
                )
            } else {
                String.format(
                    getString(R.string.settings_storage_limit_mb_value),
                    mAttachmentStorageValue,
                    attachmentSizeUsed
                )
            }
            setValue(SettingsEnum.LOCAL_STORAGE_LIMIT, storageValueString)
        }

        setupViewMode()
    }

    /**
     * Shows the current ViewMode setting and listen for user-triggered changes
     * This will only have an effect when `FeatureFlags.isChangeViewModeFeatureEnabled()` is true.
     * When feature flag is false, no item will be shown in settings and this will have no effect.
     */
    private fun setupViewMode() {
        lifecycleScope.launch {
            accountSettingsActivityViewModel.getMailSettings()
                .filterIsInstance<Either.Right<GetMailSettings.Result>>()
                .map { it.b }
                .filterIsInstance<GetMailSettings.Result.Success>()
                .onEach { result ->
                    val mailSettings = result.mailSettings
                    Timber.d("MailSettings ViewMode = ${mailSettings.viewMode}")

                    setEnabled(
                        SettingsEnum.CONVERSATION_MODE,
                        mailSettings.viewMode?.enum == ViewMode.ConversationGrouping
                    )
                }.launchIn(lifecycleScope)
        }
        setupViewModeChangedListener()
    }

    private fun setupViewModeChangedListener() {
        setToggleListener(SettingsEnum.CONVERSATION_MODE) { _, isEnabled ->
            val viewMode = if (isEnabled) ViewMode.ConversationGrouping else ViewMode.NoConversationGrouping
            accountSettingsActivityViewModel.changeViewMode(viewMode)
        }
    }
}
