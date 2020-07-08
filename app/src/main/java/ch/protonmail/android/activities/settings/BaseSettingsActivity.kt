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
package ch.protonmail.android.activities.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings.*
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.activities.*
import ch.protonmail.android.activities.labelsManager.EXTRA_MANAGE_FOLDERS
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity
import ch.protonmail.android.activities.mailbox.MailboxActivity
import ch.protonmail.android.adapters.SettingsAdapter
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabase
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabaseFactory
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory
import ch.protonmail.android.api.models.room.counters.CountersDatabase
import ch.protonmail.android.api.models.room.counters.CountersDatabaseFactory
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.android.api.models.room.notifications.NotificationsDatabase
import ch.protonmail.android.api.models.room.notifications.NotificationsDatabaseFactory
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.AttachmentFailedEvent
import ch.protonmail.android.events.ConnectivityEvent
import ch.protonmail.android.events.FetchLabelsEvent
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.user.MailSettingsEvent
import ch.protonmail.android.jobs.FetchByLocationJob
import ch.protonmail.android.jobs.OnFirstLoginJob
import ch.protonmail.android.servers.notification.CHANNEL_ID_EMAIL
import ch.protonmail.android.settings.pin.PinSettingsActivity
import ch.protonmail.android.uiModel.SettingsItemUiModel
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.CustomLocale
import ch.protonmail.android.utils.PREF_CUSTOM_APP_LANGUAGE
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.moveToLogin
import com.google.gson.Gson
import com.squareup.otto.Subscribe
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

// region constants
const val EXTRA_CURRENT_MAILBOX_LOCATION = "Extra_Current_Mailbox_Location"
const val EXTRA_CURRENT_MAILBOX_LABEL_ID = "Extra_Current_Mailbox_Label_ID"
// endregion

abstract class BaseSettingsActivity : BaseConnectivityActivity() {

    // region views
    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
    private val settingsRecyclerView by lazy { findViewById<RecyclerView>(R.id.settingsRecyclerView) }
    // endregion

    private val settingsAdapter = SettingsAdapter()

    var settingsUiList: List<SettingsItemUiModel> = ArrayList()

    var contactsDatabase: ContactsDatabase? = null
    var messagesDatabase: MessagesDatabase? = null
    private var searchDatabase: MessagesDatabase? = null
    private var notificationsDatabase: NotificationsDatabase? = null
    var countersDatabase: CountersDatabase? = null
    var attachmentMetadataDatabase: AttachmentMetadataDatabase? = null
    var pendingActionsDatabase: PendingActionsDatabase? = null
    var sharedPreferences: SharedPreferences? = null

    private var mMailboxLocation: Constants.MessageLocationType = Constants.MessageLocationType.INBOX
    private var mLabelId: String? = null
    var mBackgroundSyncValue: Boolean = false
    var mAttachmentStorageValue: Int = 0
    var mAutoDownloadGcmMessages: Boolean = false
    var mPinValue: Boolean = false
    var mRecoveryEmail: String = ""
    var mNotificationOptionValue: Int = 0
    lateinit var mSelectedAddress: Address
    var mDisplayName: String = ""
    var mSignature: String = ""
    lateinit var user: User

    private var canClick = AtomicBoolean(true)

    init {
        settingsAdapter.onItemClick = { settingItem ->

            if (!settingItem.isSection && (settingItem.settingType==SettingsItemUiModel.SettingsItemTypeEnum.DRILL_DOWN || settingItem.settingType==SettingsItemUiModel.SettingsItemTypeEnum.BUTTON)) {
                selectItem(settingItem.settingId)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ProtonMailApplication.getApplication().bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        ProtonMailApplication.getApplication().bus.unregister(this)
    }

    override fun onResume() {
        super.onResume()
        user = mUserManager.user
        settingsAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        saveLastInteraction()
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contactsDatabase = ContactsDatabaseFactory.getInstance(applicationContext).getDatabase()
        messagesDatabase = MessagesDatabaseFactory.getInstance(applicationContext).getDatabase()
        searchDatabase = MessagesDatabaseFactory.getSearchDatabase(applicationContext).getDatabase()
        notificationsDatabase = NotificationsDatabaseFactory.getInstance(applicationContext).getDatabase()
        countersDatabase = CountersDatabaseFactory.getInstance(applicationContext).getDatabase()
        attachmentMetadataDatabase = AttachmentMetadataDatabaseFactory.getInstance(applicationContext).getDatabase()
        pendingActionsDatabase = PendingActionsDatabaseFactory.getInstance(applicationContext).getDatabase()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@BaseSettingsActivity)

        mMailboxLocation = Constants.MessageLocationType.fromInt(intent.getIntExtra(EXTRA_CURRENT_MAILBOX_LOCATION, 0))
        mLabelId = intent.getStringExtra(EXTRA_CURRENT_MAILBOX_LABEL_ID)

        loadMailSettings()
        fetchOrganizationData()

        user = mUserManager.user

        mDisplayName = if (user.getDisplayNameForAddress(user.addressId)?.isEmpty()!!) user.defaultAddress.email else user.getDisplayNameForAddress(user.addressId)!!
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setSupportActionBar(toolbar)
    }

    abstract fun renderViews()

    private fun showCustomLocaleDialog() {
        val selectedLanguage = sharedPreferences!!.getString(PREF_CUSTOM_APP_LANGUAGE, "")
        val languageValues = resources.getStringArray(R.array.custom_language_values)
        var selectedLanguageIndex = 0
        for (i in languageValues.indices) {
            if (languageValues[i] == selectedLanguage) {
                selectedLanguageIndex = i
                break
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.custom_language_dialog_title)
        builder.setSingleChoiceItems(resources.getStringArray(R.array.custom_language_labels), selectedLanguageIndex) { dialog, which ->

            val language = resources.getStringArray(R.array.custom_language_values)[which]
            CustomLocale.setLanguage(this@BaseSettingsActivity, language)

            val recreatedMailboxIntent = Intent(this@BaseSettingsActivity, MailboxActivity::class.java)
            recreatedMailboxIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            dialog.dismiss()
            startActivity(recreatedMailboxIntent)

        }.setNegativeButton(R.string.cancel, null)

        val dialog = builder.create()
        dialog.show()
    }


    private fun showSortAliasDialog() {
        val defaultAddressIntent = AppUtil.decorInAppIntent(Intent(this, DefaultAddressActivity::class.java))
        startActivityForResult(defaultAddressIntent, SettingsEnum.DEFAULT_EMAIL.ordinal)
    }

    private fun selectItem(settingsId: String) {
        user = mUserManager.user
        when (SettingsEnum.valueOf(settingsId.toUpperCase(Locale.ENGLISH))) {
            SettingsEnum.ACCOUNT -> {
                val accountSettingsIntent = AppUtil.decorInAppIntent(Intent(this, AccountSettingsActivity::class.java))
                startActivityForResult(accountSettingsIntent, SettingsEnum.ACCOUNT.ordinal)
            }
            SettingsEnum.SUBSCRIPTION -> {
                val accountTypeIntent = AppUtil.decorInAppIntent(Intent(this, AccountTypeActivity::class.java))
                startActivity(accountTypeIntent)
            }
            SettingsEnum.PASSWORD_MANAGEMENT -> {
                val passwordManagerIntent = AppUtil.decorInAppIntent(Intent(this, ChangePasswordActivity::class.java))
                startActivityForResult(passwordManagerIntent, SettingsEnum.PASSWORD_MANAGEMENT.ordinal)
            }
            SettingsEnum.RECOVERY_EMAIL -> {
                val recoveryEmailIntent = AppUtil.decorInAppIntent(Intent(this, EditSettingsItemActivity::class.java))
                recoveryEmailIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.RECOVERY_EMAIL)
                recoveryEmailIntent.putExtra(EXTRA_SETTINGS_ITEM_VALUE, mRecoveryEmail)
                startActivityForResult(recoveryEmailIntent, SettingsEnum.RECOVERY_EMAIL.ordinal)
            }
            SettingsEnum.DEFAULT_EMAIL -> {
                showSortAliasDialog()
            }
            SettingsEnum.DISPLAY_NAME_N_SIGNATURE -> {
                val editSignatureIntent = Intent(this, EditSettingsItemActivity::class.java)
                editSignatureIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.DISPLAY_NAME_AND_SIGNATURE)
                startActivityForResult(AppUtil.decorInAppIntent(editSignatureIntent), SettingsEnum.DISPLAY_NAME_N_SIGNATURE.ordinal)
            }
            SettingsEnum.NOTIFICATION_SNOOZE -> {
                val notificationSnoozeIntent = AppUtil.decorInAppIntent(Intent(this, SnoozeNotificationsActivity::class.java))
                startActivityForResult(notificationSnoozeIntent, SettingsEnum.NOTIFICATION_SNOOZE.ordinal)
            }
            SettingsEnum.PRIVACY -> {
                val privacyIntent = AppUtil.decorInAppIntent(Intent(this, EditSettingsItemActivity::class.java))
                privacyIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.PRIVACY)
                startActivityForResult(privacyIntent, SettingsEnum.PRIVACY.ordinal)
            }
            SettingsEnum.AUTO_DOWNLOAD_MESSAGES -> {
                val gcmAutoDownloadIntent = Intent(this, EditSettingsItemActivity::class.java)
                gcmAutoDownloadIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.AUTO_DOWNLOAD_MESSAGES)
                startActivityForResult(AppUtil.decorInAppIntent(gcmAutoDownloadIntent), SettingsEnum.AUTO_DOWNLOAD_MESSAGES.ordinal)
            }
            SettingsEnum.BACKGROUND_REFRESH -> {
                val backgroundSyncIntent = Intent(this, EditSettingsItemActivity::class.java)
                backgroundSyncIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.BACKGROUND_SYNC)
                startActivityForResult(AppUtil.decorInAppIntent(backgroundSyncIntent), SettingsEnum.BACKGROUND_REFRESH.ordinal)
            }
            SettingsEnum.SEARCH -> {
            }
            SettingsEnum.LABELS_N_FOLDERS -> {
                val labelsNFoldersIntent = AppUtil.decorInAppIntent(Intent(this, EditSettingsItemActivity::class.java))
                labelsNFoldersIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.LABELS_AND_FOLDERS)
                startActivityForResult(labelsNFoldersIntent, SettingsEnum.LABELS_N_FOLDERS.ordinal)
            }
            SettingsEnum.LABELS_MANAGER -> {
                val labelsManagerIntent = AppUtil.decorInAppIntent(Intent(this, LabelsManagerActivity::class.java))
                startActivity(labelsManagerIntent)
            }
            SettingsEnum.FOLDERS_MANAGER -> {
                val foldersManagerIntent = AppUtil.decorInAppIntent(Intent(this, LabelsManagerActivity::class.java))
                foldersManagerIntent.putExtra(EXTRA_MANAGE_FOLDERS, true)
                startActivity(foldersManagerIntent)
            }
            SettingsEnum.SWIPING_GESTURE -> {
                val swipeGestureIntent = Intent(this, EditSettingsItemActivity::class.java)
                swipeGestureIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.SWIPE)
                startActivityForResult(AppUtil.decorInAppIntent(swipeGestureIntent), SettingsEnum.SWIPING_GESTURE.ordinal)
            }
            SettingsEnum.SWIPE_LEFT -> {
                val swipeLeftChooserIntent = Intent(this, SwipeChooserActivity::class.java)
                swipeLeftChooserIntent.putExtra(EXTRA_CURRENT_ACTION, mUserManager.mailSettings!!.leftSwipeAction)
                swipeLeftChooserIntent.putExtra(EXTRA_SWIPE_ID, SwipeType.LEFT)
                startActivityForResult(AppUtil.decorInAppIntent(swipeLeftChooserIntent),
                        SettingsEnum.SWIPE_LEFT.ordinal)
            }
            SettingsEnum.SWIPE_RIGHT -> {
                val rightLeftChooserIntent = Intent(this, SwipeChooserActivity::class.java)
                rightLeftChooserIntent.putExtra(EXTRA_CURRENT_ACTION, mUserManager.mailSettings!!.rightSwipeAction)
                rightLeftChooserIntent.putExtra(EXTRA_SWIPE_ID, SwipeType.RIGHT)
                startActivityForResult(AppUtil.decorInAppIntent(rightLeftChooserIntent),
                        SettingsEnum.SWIPE_RIGHT.ordinal)
            }
            SettingsEnum.LOCAL_STORAGE_LIMIT -> {
                val attachmentStorageIntent = Intent(this, AttachmentStorageActivity::class.java)
                attachmentStorageIntent.putExtra(AttachmentStorageActivity.EXTRA_SETTINGS_ATTACHMENT_STORAGE_VALUE, mAttachmentStorageValue)
                startActivityForResult(AppUtil.decorInAppIntent(attachmentStorageIntent), SettingsEnum.LOCAL_STORAGE_LIMIT.ordinal)
            }


            SettingsEnum.PUSH_NOTIFICATION -> {
                val privateNotificationsIntent = AppUtil.decorInAppIntent(Intent(this, EditSettingsItemActivity::class.java))
                privateNotificationsIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.PUSH_NOTIFICATIONS)
                startActivityForResult(privateNotificationsIntent, SettingsEnum.PUSH_NOTIFICATION.ordinal)
            }
            SettingsEnum.NOTIFICATION_SETTINGS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    intent.putExtra(EXTRA_CHANNEL_ID, CHANNEL_ID_EMAIL)
                    intent.putExtra(EXTRA_APP_PACKAGE, packageName)
                    startActivity(intent)
                } else {
                    mNotificationOptionValue = user.notificationSetting
                    val notificationSettingsIntent = Intent(this, NotificationSettingsActivity::class.java)
                    notificationSettingsIntent.putExtra(EXTRA_CURRENT_ACTION, mNotificationOptionValue)
                    startActivityForResult(AppUtil.decorInAppIntent(notificationSettingsIntent), SettingsEnum.NOTIFICATION_SETTINGS.ordinal)
                }
            }
            SettingsEnum.AUTO_LOCK -> {
                val pinManagerIntent = AppUtil.decorInAppIntent(Intent(this, PinSettingsActivity::class.java))
                startActivity(pinManagerIntent)
            }
            SettingsEnum.CONNECTIONS_VIA_THIRD_PARTIES -> {
                val allowSecureConnectionsViaThirdPartiesIntent = AppUtil.decorInAppIntent(Intent(this, EditSettingsItemActivity::class.java))
                allowSecureConnectionsViaThirdPartiesIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.CONNECTIONS_VIA_THIRD_PARTIES)
                startActivity(allowSecureConnectionsViaThirdPartiesIntent)
            }
            SettingsEnum.APP_LANGUAGE -> {
                showCustomLocaleDialog()
            }
            SettingsEnum.COMBINED_CONTACTS -> {
                val combinedContactsIntent = Intent(this, EditSettingsItemActivity::class.java)
                combinedContactsIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.COMBINED_CONTACTS)
                startActivity(AppUtil.decorInAppIntent(combinedContactsIntent))
            }
            SettingsEnum.APP_LOCAL_CACHE -> {
                showToast(R.string.processing_request, gravity = Gravity.CENTER)
                if (canClick.getAndSet(false)) {
                    run {
                        AppUtil.clearStorage(contactsDatabase, messagesDatabase, searchDatabase,
                                notificationsDatabase, countersDatabase, attachmentMetadataDatabase,
                                pendingActionsDatabase, true)
                        mJobManager.addJobInBackground(OnFirstLoginJob(true))
                        mJobManager.addJobInBackground(FetchByLocationJob(mMailboxLocation, mLabelId, true, null))
                    }
                }
            }
        }
    }

    protected fun setUpSettingsItems(jsonId: Int) {
        val jsonSettingsListResponse = resources.openRawResource(jsonId).bufferedReader().use { it.readText() }

        val gson = Gson()
        settingsUiList = gson.fromJson(jsonSettingsListResponse, Array<SettingsItemUiModel>::class.java).asList()
        settingsAdapter.items = settingsUiList
        settingsRecyclerView.layoutManager = LinearLayoutManager(this@BaseSettingsActivity)
        settingsRecyclerView.adapter = settingsAdapter
    }


    protected fun setUpSettingsItems(settingsList: List<SettingsItemUiModel>) {
        settingsUiList = settingsList
        settingsAdapter.items = settingsUiList
        settingsRecyclerView.layoutManager = LinearLayoutManager(this@BaseSettingsActivity)
        settingsRecyclerView.adapter = settingsAdapter
    }

    protected fun refreshSettings(settingsList: List<SettingsItemUiModel>) {
        settingsAdapter.items = settingsList
    }

    protected fun setToggleListener(settingType: SettingsEnum, listener: ((View, Boolean) -> Unit)?) {
        settingsAdapter.items.find { it.settingId == settingType.name.toLowerCase(Locale.ENGLISH) }?.apply { toggleListener = listener }
    }

    protected fun setEditTextListener(settingType: SettingsEnum, listener: (View) -> Unit) {
        settingsAdapter.items.find { it.settingId == settingType.name.toLowerCase(Locale.ENGLISH) }?.apply { editTextListener = listener }
    }

    protected fun setValue(settingType: SettingsEnum, settingValueNew: String) {
        settingsAdapter.items.find { it.settingId == settingType.name.toLowerCase(Locale.ENGLISH) }?.apply { settingValue = settingValueNew }
    }

    /**
     * Turns the value of setting with [settingType] ON or OFF.
     */
    protected fun setEnabled(settingType: SettingsEnum, settingValueEnabled: Boolean) {
        settingsAdapter.items.find { it.settingId == settingType.name.toLowerCase(Locale.ENGLISH) }?.apply { enabled = settingValueEnabled }
    }

    /**
     * Sets the setting with [settingType] to locked, so the user can't change. Usually if the account is on a free plan.
     */
    protected fun setSettingDisabled(settingType: SettingsEnum, settingDisabledNew: Boolean, description: String) {
        settingsAdapter.items.find { it.settingId == settingType.name.toLowerCase(Locale.ENGLISH) }?.apply {
            settingDisabled = settingDisabledNew
            settingsDescription = description
        }
    }

    protected fun setHeader(settingType: SettingsEnum, settingHeaderNew: String = "") {
        settingsAdapter.items.find { it.settingId == settingType.name.toLowerCase(Locale.ENGLISH) }?.apply {
            settingHeader = if (settingHeaderNew.isNotEmpty()) {
                settingHeaderNew
            } else {
                SettingsEnum.valueOf(settingType.name).getHeader(this@BaseSettingsActivity)
            }
            settingsHint = SettingsEnum.valueOf(settingType.name).getHint(this@BaseSettingsActivity)
        }
    }


    @Subscribe
    fun onConnectivityEvent(event: ConnectivityEvent) {
        if (!event.hasConnection()) {
            showNoConnSnack(callback = this)
        } else {
            mPingHasConnection = true
            hideNoConnSnack()
        }
    }

    @Subscribe
    fun onMailSettingsEvent(event: MailSettingsEvent) {
        loadMailSettings()
    }

    @Subscribe
    fun onAttachmentFailedEvent(event: AttachmentFailedEvent) {
        showToast(getString(R.string.attachment_failed) + " " + event.messageSubject + " " + event.attachmentName, Toast.LENGTH_SHORT)
    }

    @Subscribe
    fun onLogoutEvent(event: LogoutEvent) {
        moveToLogin()
    }

    open fun onLabelsLoadedEvent(event: FetchLabelsEvent) {
        if (!canClick.get()) {
            showToast(R.string.cache_cleared, gravity = Gravity.CENTER)
        }
        canClick.set(true)
    }
}
