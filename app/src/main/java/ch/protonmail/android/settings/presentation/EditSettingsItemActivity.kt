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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.events.SettingsChangedEvent
import ch.protonmail.android.jobs.UpdateSettingsJob
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.security.domain.usecase.GetIsPreventTakingScreenshots
import ch.protonmail.android.security.domain.usecase.SavePreventTakingScreenshots
import ch.protonmail.android.settings.data.AccountSettingsRepository
import ch.protonmail.android.uiModel.SettingsItemUiModel
import ch.protonmail.android.utils.extensions.showToast
import com.google.gson.Gson
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_edit_settings_item.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// region constants
const val EXTRA_SETTINGS_ITEM_TYPE = "EXTRA_SETTINGS_ITEM_TYPE"
const val EXTRA_SETTINGS_ITEM_VALUE = "EXTRA_SETTINGS_ITEM_VALUE"
// endregion

enum class SettingsItem {
    PRIVACY,
    PUSH_NOTIFICATIONS,
    CONNECTIONS_VIA_THIRD_PARTIES,
    COMBINED_CONTACTS,
    AUTO_DOWNLOAD_MESSAGES,
    BACKGROUND_SYNC
}

@AndroidEntryPoint
class EditSettingsItemActivity : BaseSettingsActivity() {

    @Inject
    lateinit var savePreventTakingScreenshots: SavePreventTakingScreenshots

    @Inject
    lateinit var getIsPreventTakingScreenshots: GetIsPreventTakingScreenshots

    @Inject
    lateinit var accountSettingsRepository: AccountSettingsRepository

    private val mailSettings by lazy {
        checkNotNull(userManager.getCurrentUserMailSettingsBlocking())
    }
    private var settingsItemType: SettingsItem = SettingsItem.PRIVACY
    private var settingsItemValue: String? = null
    private var actionBarTitle: Int = -1
    private var initializedRemote = false
    private var initializedEmbedded = false

    override fun getLayoutId(): Int = R.layout.activity_edit_settings_item

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        settingsItemType = intent.getSerializableExtra(EXTRA_SETTINGS_ITEM_TYPE) as SettingsItem
        settingsItemValue = intent.getStringExtra(EXTRA_SETTINGS_ITEM_VALUE)

        mSnackLayout = findViewById(R.id.layout_no_connectivity_info)

        val jsonSettingsListResponse =
            resources.openRawResource(R.raw.edit_settings_structure).bufferedReader().use { it.readText() }

        val gson = Gson()
        val settingsUiList =
            gson.fromJson(jsonSettingsListResponse, Array<Array<SettingsItemUiModel>>::class.java).asList()
        setUpSettingsItems(settingsUiList[settingsItemType.ordinal].asList())

        renderViews()

        if (actionBar != null && actionBarTitle > 0) {
            actionBar.setTitle(actionBarTitle)
        }
    }

    override fun onResume() {
        super.onResume()
        renderViews()
    }

    override fun onStop() {
        super.onStop()
        initializedRemote = true
        initializedEmbedded = true
        setToggleListener(SettingsEnum.LINK_CONFIRMATION, null)
        setToggleListener(SettingsEnum.PREVENT_SCREENSHOTS, null)
        setToggleListener(SettingsEnum.SHOW_REMOTE_IMAGES, null)
        setToggleListener(SettingsEnum.SHOW_EMBEDDED_IMAGES, null)
    }

    override fun renderViews() {

        when (settingsItemType) {
            SettingsItem.PRIVACY -> {

                mAutoDownloadGcmMessages = legacyUser.isGcmDownloadMessageDetails
                setValue(
                    SettingsEnum.AUTO_DOWNLOAD_MESSAGES,
                    if (mAutoDownloadGcmMessages) getString(R.string.enabled) else getString(R.string.disabled)
                )

                mBackgroundSyncValue = legacyUser.isBackgroundSync
                setValue(
                    SettingsEnum.BACKGROUND_REFRESH,
                    if (mBackgroundSyncValue) getString(R.string.enabled) else getString(R.string.disabled)
                )

                lifecycleScope.launch {
                    setEnabled(
                        SettingsEnum.LINK_CONFIRMATION,
                        accountSettingsRepository.getShouldShowLinkConfirmationSetting(user.id)

                    )
                }
                setToggleListener(SettingsEnum.LINK_CONFIRMATION) { view: View, isChecked: Boolean ->
                    lifecycleScope.launch {
                        if (view.isPressed && isChecked != accountSettingsRepository.getShouldShowLinkConfirmationSetting(
                                user.id
                            )) {
                            accountSettingsRepository.saveShouldShowLinkConfirmationSetting(isChecked, user.id)
                        }
                    }
                }

                setEnabled(SettingsEnum.PREVENT_SCREENSHOTS, getIsPreventTakingScreenshots.blocking())
                setToggleListener(SettingsEnum.PREVENT_SCREENSHOTS) { view: View, isChecked: Boolean ->
                    if (view.isPressed && isChecked != getIsPreventTakingScreenshots.blocking()) {
                        savePreventTakingScreenshots.blocking(shouldPrevent = isChecked)
                    }
                }

                setEnabled(SettingsEnum.SHOW_REMOTE_IMAGES, mailSettings.showImagesFrom.includesRemote())
                setToggleListener(SettingsEnum.SHOW_REMOTE_IMAGES) { view: View, isChecked: Boolean ->
                    if (view.isPressed && isChecked != mailSettings.showImagesFrom.includesRemote()) {
                        initializedRemote = false
                    }

                    if (!initializedRemote) {
                        mailSettings.showImagesFrom = mailSettings.showImagesFrom.toggleRemote()

                        mailSettings.saveBlocking(SecureSharedPreferences.getPrefsForUser(this, user.id))
                        val job = UpdateSettingsJob()
                        mJobManager.addJobInBackground(job)
                    }
                }

                setEnabled(SettingsEnum.SHOW_EMBEDDED_IMAGES, mailSettings.showImagesFrom.includesEmbedded())
                setToggleListener(SettingsEnum.SHOW_EMBEDDED_IMAGES) { view: View, isChecked: Boolean ->
                    if (view.isPressed && isChecked != mailSettings.showImagesFrom.includesEmbedded()) {
                        initializedEmbedded = false
                    }

                    if (!initializedEmbedded) {
                        mailSettings.showImagesFrom = mailSettings.showImagesFrom.toggleEmbedded()

                        mailSettings.saveBlocking(SecureSharedPreferences.getPrefsForUser(this, user.id))
                        val job = UpdateSettingsJob()
                        mJobManager.addJobInBackground(job)
                    }
                }

                actionBarTitle = R.string.privacy
            }
            SettingsItem.AUTO_DOWNLOAD_MESSAGES -> {

                setValue(
                    SettingsEnum.AUTO_DOWNLOAD_MESSAGES,
                    getString(R.string.auto_download_messages_subtitle)
                )
                setEnabled(SettingsEnum.AUTO_DOWNLOAD_MESSAGES, legacyUser.isGcmDownloadMessageDetails)

                setToggleListener(SettingsEnum.AUTO_DOWNLOAD_MESSAGES) { _: View, isChecked: Boolean ->
                    if (isChecked != legacyUser.isGcmDownloadMessageDetails) {
                        legacyUser.isGcmDownloadMessageDetails = isChecked
                    }
                }

                actionBarTitle = R.string.auto_download_messages_title
            }
            SettingsItem.BACKGROUND_SYNC -> {

                setValue(
                    SettingsEnum.BACKGROUND_SYNC,
                    getString(R.string.background_sync_subtitle)
                )
                setEnabled(SettingsEnum.BACKGROUND_SYNC, legacyUser.isBackgroundSync)

                setToggleListener(SettingsEnum.BACKGROUND_SYNC) { _: View, isChecked: Boolean ->
                    if (isChecked != legacyUser.isBackgroundSync) {
                        legacyUser.isBackgroundSync = isChecked
                    }
                }

                actionBarTitle = R.string.settings_background_sync
            }
            SettingsItem.PUSH_NOTIFICATIONS -> {
                setValue(SettingsEnum.EXTENDED_NOTIFICATION, getString(R.string.extended_notifications_description))
                setEnabled(SettingsEnum.EXTENDED_NOTIFICATION, legacyUser.isNotificationVisibilityLockScreen)

                setToggleListener(SettingsEnum.EXTENDED_NOTIFICATION) { _: View, isChecked: Boolean ->
                    if (isChecked != legacyUser.isNotificationVisibilityLockScreen) {
                        legacyUser.isNotificationVisibilityLockScreen = isChecked
                    }
                }

                mNotificationOptionValue = legacyUser.notificationSetting
                val notificationOption =
                    resources.getStringArray(R.array.notification_options)[mNotificationOptionValue]
                setValue(SettingsEnum.NOTIFICATION_SETTINGS, notificationOption)
                showIcon(SettingsEnum.NOTIFICATION_SETTINGS)

                actionBarTitle = R.string.push_notifications
            }
            SettingsItem.COMBINED_CONTACTS -> {
                setValue(
                    SettingsEnum.COMBINED_CONTACTS,
                    getString(R.string.turn_combined_contacts_on)
                )
                setEnabled(SettingsEnum.COMBINED_CONTACTS, legacyUser.combinedContacts)

                setToggleListener(SettingsEnum.COMBINED_CONTACTS) { _: View, isChecked: Boolean ->
                    if (isChecked != legacyUser.combinedContacts) {
                        legacyUser.combinedContacts = isChecked
                    }
                }

                actionBarTitle = R.string.combined_contacts
            }
            SettingsItem.CONNECTIONS_VIA_THIRD_PARTIES -> {
                setValue(
                    SettingsEnum.ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES,
                    getString(R.string.allow_secure_connections_via_third_parties_settings_description)
                )
                setEnabled(
                    SettingsEnum.ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES,
                    legacyUser.allowSecureConnectionsViaThirdParties
                )

                setToggleListener(SettingsEnum.ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES) { _, isChecked ->
                    legacyUser.allowSecureConnectionsViaThirdParties = isChecked

                    if (!isChecked) {
                        mNetworkUtil.networkConfigurator.reconfigureProxy(null)
                        legacyUser.usingDefaultApi = true
                    }
                }

                actionBarTitle = R.string.connections_via_third_parties
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            saveAndFinish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        saveAndFinish()
    }

    private fun View.getAllViews(): List<View> {
        if (this !is ViewGroup || childCount == 0) return listOf(this)

        return children
            .toList()
            .flatMap { it.getAllViews() }
            .plus(this as View)
    }

    private fun saveAndFinish() {
        val intent = Intent()

        for (child in settingsRecyclerView.getAllViews()) {
            child.clearFocus()
        }

        intent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, settingsItemType)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            renderViews()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Subscribe
    fun onSettingsChangedEvent(event: SettingsChangedEvent) {
        progressBar.visibility = View.GONE
        if (event.success) {
            if (event.isBackPressed) {
                val intent = Intent()
                    .putExtra(EXTRA_SETTINGS_ITEM_TYPE, settingsItemType)
                setResult(RESULT_OK, intent)
                finish()
            }
        } else {
            showToast(event.error)
        }
    }
}
