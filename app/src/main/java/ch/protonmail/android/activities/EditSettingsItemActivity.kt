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

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.children
import butterknife.OnClick
import ch.protonmail.android.R
import ch.protonmail.android.activities.settings.BaseSettingsActivity
import ch.protonmail.android.activities.settings.SettingsEnum
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.AuthStatus
import ch.protonmail.android.events.SettingsChangedEvent
import ch.protonmail.android.jobs.UpdateSettingsJob
import ch.protonmail.android.uiModel.SettingsItemUiModel
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.isValidEmail
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.views.CustomFontEditText
import com.google.gson.Gson
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.activity_edit_settings_item.*

// region constants
const val EXTRA_SETTINGS_ITEM_TYPE = "EXTRA_SETTINGS_ITEM_TYPE"
const val EXTRA_SETTINGS_ITEM_VALUE = "EXTRA_SETTINGS_ITEM_VALUE"
// endregion

enum class SettingsItem {
    DISPLAY_NAME_AND_SIGNATURE,
    PRIVACY,
    LABELS_AND_FOLDERS,
    SWIPE,
    PUSH_NOTIFICATIONS,
    COMBINED_CONTACTS,
    RECOVERY_EMAIL,
    AUTO_DOWNLOAD_MESSAGES,
    BACKGROUND_SYNC
}

class EditSettingsItemActivity : BaseSettingsActivity() {

    private var settingsItemType: SettingsItem = SettingsItem.DISPLAY_NAME_AND_SIGNATURE
    private var settingsItemValue: String? = null
    private var title: String? = null
    private var recoveryEmailValue: String? = null
    private var actionBarTitle: Int = -1
    private var mailSettings: MailSettings = MailSettings()
    private var initializedRemote = false
    private var initializedEbedded = false

    override fun getLayoutId(): Int {
        return R.layout.activity_edit_settings_item
    }

    override fun onStop() {
        super.onStop()
        initializedRemote = true
        initializedEbedded = true
        enableFeatureSwitch.setOnCheckedChangeListener(null)
        setToggleListener(SettingsEnum.LINK_CONFIRMATION, null)
        setToggleListener(SettingsEnum.PREVENT_SCREENSHOTS, null)
        setToggleListener(SettingsEnum.SHOW_REMOTE_IMAGES, null)
        setToggleListener(SettingsEnum.SHOW_EMBEDDED_IMAGES, null)
    }

    override fun onResume() {
        super.onResume()
        renderViews()
    }


    private val isValidNewConfirmEmail: Boolean
        get() {
            val newRecoveryEmail = newRecoveryEmail!!.text.toString().trim()
            val newConfirmRecoveryEmail = newRecoveryEmailConfirm!!.text.toString().trim()
            return if (TextUtils.isEmpty(newRecoveryEmail) && TextUtils.isEmpty(newConfirmRecoveryEmail)) {
                true
            } else (newRecoveryEmail == newConfirmRecoveryEmail && newRecoveryEmail.isValidEmail())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        settingsItemType = intent.getSerializableExtra(EXTRA_SETTINGS_ITEM_TYPE) as SettingsItem
        settingsItemValue = intent.getStringExtra(EXTRA_SETTINGS_ITEM_VALUE)

        mSnackLayout = findViewById(R.id.layout_no_connectivity_info)

        val oldSettings = arrayOf(SettingsItem.RECOVERY_EMAIL, SettingsItem.AUTO_DOWNLOAD_MESSAGES, SettingsItem.BACKGROUND_SYNC)

        if (settingsItemType !in oldSettings) {
            val jsonSettingsListResponse = resources.openRawResource(R.raw.edit_settings_structure).bufferedReader().use { it.readText() }

            val gson = Gson()
            val settingsUiList = gson.fromJson(jsonSettingsListResponse, Array<Array<SettingsItemUiModel>>::class.java).asList()
            setUpSettingsItems(settingsUiList[settingsItemType.ordinal].asList())
        }

        mailSettings = mUserManager.mailSettings!!
        renderViews()

        if (actionBar != null && actionBarTitle > 0) {
            actionBar.setTitle(actionBarTitle)
        }
    }

    override fun renderViews() {

        when (settingsItemType) {
            SettingsItem.RECOVERY_EMAIL -> {
                settingsRecyclerViewParent.visibility = View.GONE
                recoveryEmailValue = settingsItemValue
                if (!TextUtils.isEmpty(recoveryEmailValue)) {
                    (currentRecoveryEmail as TextView).text = recoveryEmailValue
                } else {
                    (currentRecoveryEmail as TextView).text = getString(R.string.not_set)
                }
                recoveryEmailParent.visibility = View.VISIBLE
                header.visibility = View.GONE
                title = getString(R.string.edit_notification_email)
                actionBarTitle = R.string.recovery_email
            }
            SettingsItem.DISPLAY_NAME_AND_SIGNATURE -> {

                mSelectedAddress = user.addresses[0]
                val newAddressId = user.defaultAddress.id
                val currentSignature = mSelectedAddress.signature

                if (!TextUtils.isEmpty(mDisplayName)) {
                    setValue(SettingsEnum.DISPLAY_NAME, mDisplayName)
                }

                setEditTextListener(SettingsEnum.DISPLAY_NAME) {
                    var newDisplayName = (it as CustomFontEditText).text.toString()

                    val containsBannedChars = newDisplayName.matches(".*[<>].*".toRegex())
                    if (containsBannedChars) {
                        showToast(R.string.display_name_banned_chars, Toast.LENGTH_SHORT, Gravity.CENTER)
                        newDisplayName = user.getDisplayNameForAddress(user.addressId).toString()
                    }


                    val displayChanged = newDisplayName != mDisplayName

                    if (displayChanged) {
                        user.displayName = newDisplayName

                        user.save()
                        mUserManager.user = user
                        mDisplayName = newDisplayName

                        val job = UpdateSettingsJob(displayChanged = displayChanged, newDisplayName = newDisplayName, addressId = newAddressId)
                        mJobManager.addJobInBackground(job)
                    }
                }

                if (!TextUtils.isEmpty(currentSignature)) {
                    setValue(SettingsEnum.SIGNATURE, currentSignature!!)
                }
                setEnabled(SettingsEnum.SIGNATURE, user.isShowSignature)


                val currentMobileSignature = user.mobileSignature
                if (!TextUtils.isEmpty(currentMobileSignature)) {
                    setValue(SettingsEnum.MOBILE_SIGNATURE, currentMobileSignature!!)
                }
                if (user.isPaidUserSignatureEdit) {
                    setEnabled(SettingsEnum.MOBILE_SIGNATURE, user.isShowMobileSignature)
                } else {
                    setEnabled(SettingsEnum.MOBILE_SIGNATURE, true)
                    setSettingDisabled(SettingsEnum.MOBILE_SIGNATURE, true, getString(R.string.mobile_signature_is_premium))
                }

                setEditTextListener(SettingsEnum.SIGNATURE) {
                    val newSignature = (it as CustomFontEditText).text.toString()
                    val signatureChanged = newSignature != currentSignature

                    user.save()
                    mUserManager.user = user

                    if (signatureChanged) {
                        val job = UpdateSettingsJob(signatureChanged = signatureChanged, newSignature = newSignature, addressId = newAddressId)
                        mJobManager.addJobInBackground(job)
                    }
                }

                setToggleListener(SettingsEnum.SIGNATURE) { _: View, isChecked: Boolean ->
                    user.isShowSignature = isChecked

                    user.save()
                    mUserManager.user = user
                }

                setEditTextListener(SettingsEnum.MOBILE_SIGNATURE) {
                    val newMobileSignature = (it as CustomFontEditText).text.toString()
                    val mobileSignatureChanged = newMobileSignature != currentMobileSignature

                    if (mobileSignatureChanged) {
                        user.mobileSignature = newMobileSignature

                        user.save()
                        mUserManager.user = user
                    }
                }


                setToggleListener(SettingsEnum.MOBILE_SIGNATURE) { _: View, isChecked: Boolean ->
                    user.isShowMobileSignature = isChecked

                    user.save()
                    mUserManager.user = user
                }


                actionBarTitle = R.string.display_name_n_signature
            }
            SettingsItem.PRIVACY -> {

                mAutoDownloadGcmMessages = user.isGcmDownloadMessageDetails
                setValue(SettingsEnum.AUTO_DOWNLOAD_MESSAGES, if (mAutoDownloadGcmMessages) getString(R.string.enabled) else getString(R.string.disabled))

                mBackgroundSyncValue = user.isBackgroundSync
                setValue(SettingsEnum.BACKGROUND_REFRESH, if (mBackgroundSyncValue) getString(R.string.enabled) else getString(R.string.disabled))
                setEnabled(SettingsEnum.LINK_CONFIRMATION, sharedPreferences!!.getBoolean(Constants.Prefs.PREF_HYPERLINK_CONFIRM, true))

                setToggleListener(SettingsEnum.LINK_CONFIRMATION) { view: View, isChecked: Boolean ->
                    if (view.isPressed && isChecked != sharedPreferences!!.getBoolean(Constants.Prefs.PREF_HYPERLINK_CONFIRM, true))
                        sharedPreferences!!.edit().putBoolean(Constants.Prefs.PREF_HYPERLINK_CONFIRM, isChecked).apply()
                }

                setEnabled(SettingsEnum.PREVENT_SCREENSHOTS, user.isPreventTakingScreenshots)
                setToggleListener(SettingsEnum.PREVENT_SCREENSHOTS) { view: View, isChecked: Boolean ->
                    if (view.isPressed && isChecked != user.isPreventTakingScreenshots) {
                        user.isPreventTakingScreenshots = isChecked
                        val infoSnack = UiUtil.showInfoSnack(mSnackLayout, this, R.string.changes_affected_after_closing)
                        infoSnack.show()

                        user.save()
                        mUserManager.user = user
                    }
                }

                setEnabled(SettingsEnum.SHOW_REMOTE_IMAGES, mailSettings.showImages == 1 || mailSettings.showImages == 3)
                setToggleListener(SettingsEnum.SHOW_REMOTE_IMAGES) { view: View, isChecked: Boolean ->
                    if(view.isPressed && isChecked != (mailSettings.showImages == 1 || mailSettings.showImages == 3)) {initializedRemote = false}

                    if (!initializedRemote) {
                        if (isChecked && mailSettings.showImages == 0) {
                            mailSettings.showImages = 1
                        } else if (isChecked && mailSettings.showImages == 2) {
                            mailSettings.showImages = 3
                        } else {
                            mailSettings.showImages = mailSettings.showImages - 1
                        }

                        mailSettings.save()
                        mUserManager.mailSettings = mailSettings
                        val job = UpdateSettingsJob(mailSettings = mailSettings)
                        mJobManager.addJobInBackground(job)
                    }
                }

                setEnabled(SettingsEnum.SHOW_EMBEDDED_IMAGES, mailSettings.showImages == 2 || mailSettings.showImages == 3)
                setToggleListener(SettingsEnum.SHOW_EMBEDDED_IMAGES) { view: View, isChecked: Boolean ->
                    if(view.isPressed && isChecked != (mailSettings.showImages == 2 || mailSettings.showImages == 3)) {initializedEbedded = false}

                    if (!initializedEbedded) {
                        if (isChecked && mailSettings.showImages == 0) {
                            mailSettings.showImages = 2
                        } else if (isChecked && mailSettings.showImages == 1) {
                            mailSettings.showImages = 3
                        } else {
                            mailSettings.showImages = mailSettings.showImages - 2
                        }

                        mailSettings.save()
                        mUserManager.mailSettings = mailSettings
                        val job = UpdateSettingsJob(mailSettings = mailSettings)
                        mJobManager.addJobInBackground(job)
                    }
                }


                actionBarTitle = R.string.privacy
            }
            SettingsItem.AUTO_DOWNLOAD_MESSAGES -> {
                settingsRecyclerViewParent.visibility = View.GONE
                featureTitle.text = getString(R.string.auto_download_messages_title)
                enableFeatureSwitch.isChecked = user.isGcmDownloadMessageDetails

                enableFeatureSwitch.setOnCheckedChangeListener { view, isChecked ->

                    val gcmDownloadDetailsChanged = user.isGcmDownloadMessageDetails != isChecked
                    if (view.isPressed && gcmDownloadDetailsChanged) {
                        user.isGcmDownloadMessageDetails = isChecked

                        user.save()
                        mUserManager.user = user
                    }
                }
                descriptionParent.visibility = View.VISIBLE
                description.text = getString(R.string.auto_download_messages_subtitle)
                actionBarTitle = R.string.auto_download_messages_title
            }
            SettingsItem.BACKGROUND_SYNC -> {
                settingsRecyclerViewParent.visibility = View.GONE
                featureTitle.text = getString(R.string.settings_background_sync)
                enableFeatureSwitch.isChecked = user.isBackgroundSync

                enableFeatureSwitch.setOnCheckedChangeListener { view, isChecked ->

                    if (view.isPressed && (isChecked != user.isBackgroundSync)) {
                        user.isBackgroundSync = isChecked
                        if (user.isBackgroundSync) {
                            val alarmReceiver = AlarmReceiver()
                            alarmReceiver.setAlarm(ProtonMailApplication.getApplication())

                        }
                        user.save()
                        mUserManager.user = user
                    }
                }

                descriptionParent.visibility = View.VISIBLE
                description.text = getString(R.string.background_sync_subtitle)
                actionBarTitle = R.string.settings_background_sync
            }
            SettingsItem.SWIPE -> {
                setValue(SettingsEnum.SWIPE_FROM_RIGHT, getString(SwipeAction.values()[mUserManager.mailSettings!!.rightSwipeAction].actionDescription))
                setValue(SettingsEnum.SWIPE_FROM_LEFT, getString(SwipeAction.values()[mUserManager.mailSettings!!.leftSwipeAction].actionDescription))
                actionBarTitle = R.string.swiping_gesture
            }
            SettingsItem.LABELS_AND_FOLDERS -> {
                actionBarTitle = R.string.labels_and_folders
            }
            SettingsItem.PUSH_NOTIFICATIONS -> {
                setValue(SettingsEnum.EXTENDED_NOTIFICATION, getString(R.string.extended_notifications_description))
                setEnabled(SettingsEnum.EXTENDED_NOTIFICATION, user.isNotificationVisibilityLockScreen)

                setToggleListener(SettingsEnum.EXTENDED_NOTIFICATION) { _: View, isChecked: Boolean ->
                    if (isChecked != user.isNotificationVisibilityLockScreen) {
                        user.isNotificationVisibilityLockScreen = isChecked

                        user.save()
                        mUserManager.user = user
                    }
                }

                mNotificationOptionValue = user.notificationSetting
                val notificationOption = resources.getStringArray(R.array.notification_options)[mNotificationOptionValue]
                setValue(SettingsEnum.NOTIFICATION_SETTINGS, notificationOption)

                actionBarTitle = R.string.push_notifications
            }
            SettingsItem.COMBINED_CONTACTS -> {
                setValue(SettingsEnum.COMBINED_CONTACTS, getString(R.string.turn_combined_contacts_on))
                setEnabled(SettingsEnum.COMBINED_CONTACTS, user.combinedContacts)

                setToggleListener(SettingsEnum.COMBINED_CONTACTS) { _: View, isChecked: Boolean ->
                    if (isChecked != user.combinedContacts) {
                        user.combinedContacts = isChecked

                        user.save()
                        mUserManager.user = user
                    }
                }

                actionBarTitle = R.string.combined_contacts
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            saveLastInteraction()
            saveAndFinish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        saveLastInteraction()
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
        if (settingsItemType == SettingsItem.RECOVERY_EMAIL) {
            intent.putExtra(EXTRA_SETTINGS_ITEM_VALUE, recoveryEmailValue)
        }

        setResult(Activity.RESULT_OK, intent)
        saveLastInteraction()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            renderViews()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @OnClick(R.id.save_new_email)
    fun onSaveNewRecoveryClicked() {
        if (isValidNewConfirmEmail) {
            showEmailChangeDialog(false)
        } else {
            showToast(R.string.recovery_emails_invalid, Toast.LENGTH_SHORT)
        }
    }

    @Subscribe
    fun onSettingsChangedEvent(event: SettingsChangedEvent) {
        progressBar.visibility = View.GONE
        if (event.status == AuthStatus.SUCCESS) {
            val user = mUserManager.user
            if (settingsItemType == SettingsItem.RECOVERY_EMAIL) {
                settingsItemValue = recoveryEmailValue
                if (TextUtils.isEmpty(recoveryEmailValue)) {
                    mUserManager.userSettings!!.notificationEmail = resources.getString(R.string.not_set)
                } else {
                    mUserManager.userSettings!!.notificationEmail = recoveryEmailValue
                }
                user.save()
            }
            if (!event.isBackPressed) {
                enableRecoveryEmailInput()
            } else {
                val intent = Intent()
                intent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, settingsItemType)
                intent.putExtra(EXTRA_SETTINGS_ITEM_VALUE, recoveryEmailValue)
                setResult(RESULT_OK, intent)
                saveLastInteraction()
                finish()
            }
        } else {
            recoveryEmailValue = settingsItemValue
            when (event.status) {
                AuthStatus.INVALID_SERVER_PROOF -> {
                    showToast(R.string.invalid_server_proof, Toast.LENGTH_SHORT)
                }
                AuthStatus.FAILED -> {
                    showToast(event.error)
                }
                else -> {
                    showToast(event.error)
                }
            }
        }
    }

    private fun disableRecoveryEmailInput() {
        newRecoveryEmail.isFocusable = false
        newRecoveryEmail.isFocusableInTouchMode = false
    }

    private fun enableRecoveryEmailInput() {
        newRecoveryEmail.isFocusable = true
        newRecoveryEmail.isFocusableInTouchMode = true
        currentRecoveryEmail.setText(recoveryEmailValue)
        newRecoveryEmail.setText("")
        newRecoveryEmailConfirm.setText("")
    }

    private fun showEmailChangeDialog(backPressed: Boolean) {
        val hasTwoFactor = mUserManager.userSettings!!.twoFactor == 1
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.layout_password_confirm, null)
        val password = dialogView.findViewById<EditText>(R.id.current_password)
        val twoFactorCode = dialogView.findViewById<EditText>(R.id.two_factor_code)
        if (hasTwoFactor) {
            twoFactorCode.visibility = View.VISIBLE
        }
        builder.setView(dialogView)
        builder.setPositiveButton(R.string.okay) { dialog, _ ->
            val passString = password.text.toString()
            var twoFactorString = ""
            if (hasTwoFactor) {
                twoFactorString = twoFactorCode.text.toString()
            }
            if (TextUtils.isEmpty(passString) || TextUtils.isEmpty(twoFactorString) && hasTwoFactor) {
                showToast(R.string.password_not_valid, Toast.LENGTH_SHORT)
                newRecoveryEmail.setText("")
                newRecoveryEmailConfirm.setText("")
                dialog.cancel()
            } else {
                progressBar.visibility = View.VISIBLE
                recoveryEmailValue = newRecoveryEmail.text.toString()
                val job = UpdateSettingsJob(notificationEmailChanged = true, newEmail = recoveryEmailValue!!, backPressed = backPressed, password = passString.toByteArray() /*TODO passphrase*/, twoFactor = twoFactorString)
                mJobManager.addJobInBackground(job)
                dialog.dismiss()
                disableRecoveryEmailInput()
            }
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.cancel()
            if (backPressed) {
                saveLastInteraction()
                finish()
            }
        }
        val alert = builder.create()
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.iron_gray))
            val positiveButton = alert.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.new_purple_dark))
            positiveButton.text = getString(R.string.enter)
        }
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }
}
