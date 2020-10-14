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
package ch.protonmail.android.core

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.annotation.IntDef
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.api.local.SnoozeSettings
import ch.protonmail.android.api.models.LoginInfoResponse
import ch.protonmail.android.api.models.LoginResponse
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.UserInfo
import ch.protonmail.android.api.models.UserSettings
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.api.services.LoginService
import ch.protonmail.android.api.services.LogoutService
import ch.protonmail.android.di.BackupSharedPreferences
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.events.ForceSwitchedAccountEvent
import ch.protonmail.android.events.ForceSwitchedAccountNotifier
import ch.protonmail.android.events.GenerateKeyPairEvent
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.extensions.app
import com.squareup.otto.Produce
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Singleton

// region constants
const val LOGIN_STATE_NOT_INITIALIZED = 0
const val LOGIN_STATE_LOGIN_FINISHED = 2
const val LOGIN_STATE_TO_INBOX = 3

const val PREF_PIN = "mailbox_pin"
const val PREF_USERNAME = "username"
/**
 * When user login successfully PREF_IS_LOGGED_IN  = true
 * When user logout PREF_IS_LOGGED_IN = false
 */
private const val PREF_IS_LOGGED_IN = "is_logged_in"
private const val PREF_REMEMBER_MAILBOX_LOGIN = "remember_mailbox_login"
const val PREF_LOGIN_STATE = "login_state"
private const val PREF_MAILBOX_PASSWORD = "mailbox_password"
private const val PREF_KEY_SALT = "key_salt"
private const val PREF_PIN_INCORRECT_ATTEMPTS = "mailbox_pin_incorrect_attempts"
private const val PREF_IS_FIRST_LOGIN = "is_first_login"
private const val PREF_IS_FIRST_MAILBOX_LOAD_AFTER_LOGIN = "is_first_mailbox_load_after_login"
private const val PREF_CHECK_TIMESTAMP = "check_timestamp_float"
const val PREF_SHOW_STORAGE_LIMIT_WARNING = "show_storage_limit_warning"
const val PREF_SHOW_STORAGE_LIMIT_REACHED = "show_storage_limit_reached"
private const val PREF_IS_FIRST_MESSAGE_DETAILS = "is_first_message_details"
private const val PREF_APP_VERSION = "app_version"
private const val PREF_ENGAGEMENT_SHOWN = "engagement_shown"
// endregion

/**
 * UserManager handles behavior of the current primary account, as well as some multi-account behaviors
 */
@Singleton
class UserManager @Inject constructor(
    val context: Context,
    @DefaultSharedPreferences private val prefs: SharedPreferences,
    @BackupSharedPreferences private val backupPrefs: SharedPreferences
) {

    private val userReferences = HashMap<String, User>()
    private var mCheckTimestamp: Float = 0.toFloat()
    private var mMailboxPassword: String? = null
//    private var mMailboxPin: String? = null
    private val app: ProtonMailApplication = context.app
    private var mGeneratingKeyPair: Boolean = false

    var privateKey: String? = null
        set(privateKey) {
            field = privateKey
            if (privateKey != null) {
                mGenerateKeyPairEvent = GenerateKeyPairEvent(true, null)
            }
            if (mGenerateKeyPairEvent != null) {
                AppUtil.postEventOnUi(mGenerateKeyPairEvent)
            }
            if (mCreateUserOnKeyPairGenerationFinish) {
                LoginService.startCreateUser(mNewUserUsername, mNewUserPassword, mNewUserUpdateMe,
                        mNewUserTokenType, mNewUserToken)
            } else {
                mCreateUserOnKeyPairGenerationFinish = false
            }
        }

    var userSettings: UserSettings? = null
        set(value) {
            if (value == null) {
                field = value
                return
            }
            if (value.username == null) { // API model UserSettings doesn't contain username, we set it manually
                value.username = user.username
            }
            field = value
            if (field != null) {
                field!!.save()
            }
        }
    var mailSettings: MailSettings? = null
        set(value) {
            if (value == null) {
                field = value
                return
            }
            if (value.username == null) { // API model MailSettings doesn't contain username, we set it manually
                value.username = user.username
            }
            field = value
            field!!.save()
        }

    fun getMailSettings(username: String): MailSettings? {
        return MailSettings.load(username)
    }

    var snoozeSettings: SnoozeSettings? = null

    @Inject
    lateinit var openPgp: OpenPGP

    private var mNewUserUsername: String? = null
    private var mNewUserPassword: ByteArray? = null
    private var mNewUserUpdateMe: Boolean = false
    private var mNewUserTokenType: Constants.TokenType? = null
    private var mNewUserToken: String? = null
    private var mCreateUserOnKeyPairGenerationFinish: Boolean = false

    /**
     * Returns the username of the next available (if any) account other than current. Currently this
     * works randomly hopefully it will get refactored by same factor (maybe how often the account
     * is used recently or something else).
     * @return the username of the account that is the second one after the current primary logged
     * in account.
     */
    val nextLoggedInAccountOtherThanCurrent: String?
        get() {
            val currentActiveAccount = username
            val accountManager = AccountManager.getInstance(context)
            return accountManager.getNextLoggedInAccountOtherThan(currentActiveAccount, currentActiveAccount)
        }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(PREF_IS_LOGGED_IN, false)
        set(isLoggedIn) = prefs.edit().putBoolean(PREF_IS_LOGGED_IN, isLoggedIn).apply()

    val isFirstLogin: Boolean
        get() = prefs.getBoolean(PREF_IS_FIRST_LOGIN, true) &&
                prefs.getInt(PREF_APP_VERSION, Integer.MIN_VALUE) != AppUtil.getAppVersionCode(context)

    val isFirstMailboxLoad: Boolean
        get() = prefs.getBoolean(PREF_IS_FIRST_MAILBOX_LOAD_AFTER_LOGIN, true)

    val isFirstMessageDetails: Boolean
        get() = prefs.getBoolean(PREF_IS_FIRST_MESSAGE_DETAILS, true)

    val isEngagementShown: Boolean
        get() = backupPrefs.getBoolean(PREF_ENGAGEMENT_SHOWN, false)

    /**
     * @return username of currently "active" user
     */
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val username: String
        get() = prefs.getString(PREF_USERNAME, "")!!

    val incorrectPinAttempts: Int
        get() {
            val secureSharedPreferences = app.secureSharedPreferences
            return secureSharedPreferences.getInt(PREF_PIN_INCORRECT_ATTEMPTS, 0)
        }

    /**
     * Gets key salt for current user.
     */
    val keySalt: String?
        get() {
            val secureSharedPreferences = app.getSecureSharedPreferences(username)
            return secureSharedPreferences.getString(PREF_KEY_SALT, null)
        }

    var loginState: Int
        @LoginState
        get() {
            val secureSharedPreferences = app.getSecureSharedPreferences(username)
            return secureSharedPreferences.getInt(PREF_LOGIN_STATE, LOGIN_STATE_NOT_INITIALIZED)
        }
        set(@LoginState status) {
            val secureSharedPreferences = app.getSecureSharedPreferences(username)
            secureSharedPreferences.edit().putInt(PREF_LOGIN_STATE, status).apply()
        }

    /**
     * Use this method to get settings for currently active User.
     */
    var user: User
        @Synchronized get() = getUser(username)
        set(user) {
            val username = user.username
            if (!username.isBlank()) {
                userReferences[username] = user
                user.save()
            }
        }

    val tokenManager: TokenManager?
        get() {
            return if (!username.isBlank()) getTokenManager(username) else null
        }

    @Nullable
    fun getUsernameBySessionId(@NonNull sessionId: String): String? {
        for (username in AccountManager.getInstance(context).getLoggedInUsers()) {
            if (sessionId == getTokenManager(username)?.uid) {
                return username
            }
        }
        return null
    }

    var checkTimestamp: Float
        get() = mCheckTimestamp
        set(checkTimestamp) {
            if (checkTimestamp > mCheckTimestamp) {
                mCheckTimestamp = checkTimestamp
                prefs.edit().putFloat(PREF_CHECK_TIMESTAMP, mCheckTimestamp).apply()
            }
        }

    private var mGenerateKeyPairEvent: GenerateKeyPairEvent? = null


    val isBackgroundSyncEnabled: Boolean
        get() = user.isBackgroundSync


    fun isSnoozeScheduledEnabled(): Boolean {
        return snoozeSettings!!.getScheduledSnooze(username)
    }

    // Very important: don't setSnoozeQuick to false if it already is false otherwise it will
    // keep saving, saving can be super slow on users with a lot of accounts!
    fun isSnoozeQuickEnabled(): Boolean {
        if (snoozeSettings!!.snoozeQuick && snoozeSettings!!.snoozeQuickEndTime - System.currentTimeMillis() <= 0) {
            setSnoozeQuick(false, 0)
        }
        return snoozeSettings!!.snoozeQuick
    }

    @IntDef(LOGIN_STATE_NOT_INITIALIZED, LOGIN_STATE_LOGIN_FINISHED, LOGIN_STATE_TO_INBOX)
    internal annotation class LoginState

    /**
     * Do not instantiate on Main thread!
     *
     * @see MIGRATE_FROM_BUILD_CONFIG_FIELD_DOC
     */
    init {
        val prefs = app.defaultSharedPreferences
        val currentAppVersion = AppUtil.getAppVersionCode(app)
        val previousVersion = prefs.getInt(Constants.Prefs.PREF_APP_VERSION, Integer.MIN_VALUE)
        // if this version requires the user to be logged out when updating
        // and if every single previous version should be force logged out
        // or any specific previous version should be logged out
        if (previousVersion != currentAppVersion) {

            // Removed check for updates where we need to logout as it was always false. See doc ref in method header
            if (false) {
                val pin = getMailboxPin()
                logoutOffline()
                savePin(pin)
            } else {
                loadSettings(username)
            }
        } else {
            loadSettings(username)
            mCheckTimestamp = this.prefs.getFloat(PREF_CHECK_TIMESTAMP, 0f)
        }
        app.bus.register(this)
    }

    private fun reset() {
        mCheckTimestamp = 0f
        userReferences.remove(username)
        mMailboxPassword = null
//        mMailboxPin = null
        app.eventManager.clearState()
    }

    fun generateKeyPair(username: String, domain: String, password: ByteArray, bits: Int) {
        mGenerateKeyPairEvent = GenerateKeyPairEvent(false, null)
        mGeneratingKeyPair = true
        LoginService.startGenerateKeys(username, domain, password, bits)
    }

    fun createUser(
            username: String,
            password: ByteArray,
            updateMe: Boolean,
            tokenType: Constants.TokenType,
            token: String
    ) {
        if (this.privateKey == null && mGeneratingKeyPair) {
            mCreateUserOnKeyPairGenerationFinish = true
            mNewUserUsername = username
            mNewUserPassword = password
            mNewUserUpdateMe = updateMe
            mNewUserTokenType = tokenType
            mNewUserToken = token
            return
        }
        LoginService.startCreateUser(username, password, updateMe, tokenType, token)
    }

    fun info(username: String, password: ByteArray?) {
        LoginService.startInfo(username, password, 2)
    }

    fun login(
            username: String,
            password: ByteArray,
            response: LoginInfoResponse?,
            fallbackAuthVersion: Int,
            signUp: Boolean
    ) {
        LoginService.startLogin(username, password, response, fallbackAuthVersion, signUp)
    }

    fun twoFA(
            username: String,
            password: ByteArray,
            twoFactor: String?,
            infoResponse: LoginInfoResponse?,
            loginResponse: LoginResponse?,
            fallbackAuthVersion: Int,
            signUp: Boolean,
            isConnecting: Boolean
    ) {
        LoginService.start2FA(username, password, twoFactor, infoResponse, loginResponse, fallbackAuthVersion,
                signUp, isConnecting)
    }

    fun mailboxLogin(username: String, mailboxPassword: String, keySalt: String?, signUp: Boolean) {
        LoginService.startMailboxLogin(username, mailboxPassword, keySalt, signUp)
    }

    fun setupAddress(domain: String) {
        LoginService.startSetupAddress(domain)
    }

    fun setupKeys(addressId: String, password: ByteArray) {
        LoginService.startSetupKeys(addressId, password)
    }

    fun connectAccountLogin(
            username: String,
            password: ByteArray,
            twoFactor: String?,
            response: LoginInfoResponse?,
            fallbackAuthVersion: Int
    ) {
        LoginService.startConnectAccount(username, password, twoFactor, response, fallbackAuthVersion)
    }

    fun connectAccountMailboxLogin(username: String, currentPrimary: String, mailboxPassword: String, keySalt: String) {
        LoginService.startConnectAccountMailboxLogin(username, currentPrimary, mailboxPassword, keySalt)
    }

    fun switchToAccount(username: String) {
        setUsernameAndReload(username)
        user = User.load(username)
        loadSettings(username)
    }

    fun removeAccount(username: String, clearDoneListener: (() -> Unit)? = null) {
        logoutAccount(username, clearDoneListener)
        val accountManager = AccountManager.getInstance(context)
        accountManager.removeFromSaved(username)
    }

    /**
     * Logs the supplied account out.
     * @param username the username of the account to be logged out.
     */
    fun logoutAccount(username: String) {
        logoutAccount(username, null)
    }

    /**
     * Logs the supplied account out.
     * @param username the username of the account to be logged out.
     */
    fun logoutAccount(username: String, clearDoneListener: (() -> Unit)?) {
        val accountManager = AccountManager.getInstance(context)
        val currentPrimary = this.username
        val nextLoggedInAccount = accountManager.getNextLoggedInAccountOtherThan(username, currentPrimary)
                ?: // fallback to "last user logout"
                return logoutLastActiveAccount()
        LogoutService.startLogout(false, username)
        accountManager.onSuccessfulLogout(username)
        AppUtil.deleteSecurePrefs(username, false)
        AppUtil.deleteDatabases(context, username, clearDoneListener)
        setUsernameAndReload(nextLoggedInAccount)
        app.eventManager.clearState(username)
        app.clearPaymentMethods()
    }

    @JvmOverloads
    fun logoutLastActiveAccount(clearDoneListener: (() -> Unit)? = null) {
        isLoggedIn = false
        loginState = LOGIN_STATE_NOT_INITIALIZED
        AppUtil.deleteDatabases(app.applicationContext, username, clearDoneListener)
        saveBackupSettings()
        LogoutService.startLogout(true, username)
        setRememberMailboxLogin(false)
        firstLoginRemove()
        reset()
        AppUtil.deleteSecurePrefs(username, true)
        AppUtil.deletePrefs()
        AppUtil.deleteBackupPrefs()
        AppUtil.postEventOnUi(LogoutEvent(Status.SUCCESS))
        app.clearPaymentMethods()
    }

    @JvmOverloads
    fun logoutOffline(usernameToLogout: String? = null) {
        val username = usernameToLogout ?: this.username
        if (username.isEmpty()) {
            return
        }
        app.clearPaymentMethods()
        val nextLoggedInAccount = nextLoggedInAccountOtherThanCurrent
        val accountManager = AccountManager.getInstance(context)
        if (!accountManager.getLoggedInUsers().contains(username)) {
            return
        }
        if (nextLoggedInAccount == null) {
            isLoggedIn = false
            loginState = LOGIN_STATE_NOT_INITIALIZED
            saveBackupSettings()
            LogoutService.startLogout(false)
            setRememberMailboxLogin(false)
            AppUtil.deleteSecurePrefs(username, true)
            AppUtil.deletePrefs()
            AppUtil.deleteBackupPrefs()
            firstLoginRemove()
            reset()
            tokenManager?.clear()
            AppUtil.deleteDatabases(app.applicationContext, username)
            AppUtil.postEventOnUi(LogoutEvent(Status.SUCCESS))
            TokenManager.clearAllInstances()
        } else {
            accountManager.onSuccessfulLogout(username)
            AppUtil.deleteSecurePrefs(username, false)
            AppUtil.deleteDatabases(app.applicationContext, username)
            setUsernameAndReload(nextLoggedInAccount)
            val event = ForceSwitchedAccountEvent(nextLoggedInAccount, username)
            ForceSwitchedAccountNotifier.notifier.postValue(event)
            TokenManager.clearInstance(username)
        }
    }

    @Synchronized
    private fun saveBackupSettings() {
        val user = user
        user.saveNotificationSettingsBackup()
        user.saveAutoLogoutBackup()
        user.saveAutoLockPINPeriodBackup()
        user.saveUsePinBackup()
        user.saveUseFingerprintBackup()
        user.saveNotificationVisibilityLockScreenSettingsBackup()
        user.saveRingtoneBackup()
        user.saveCombinedContactsBackup()
    }

    private fun setRememberMailboxLogin(remember: Boolean) {
        prefs.edit().putBoolean(PREF_REMEMBER_MAILBOX_LOGIN, remember).apply()
    }

    fun firstLoginDone() {
        prefs.edit().putBoolean(PREF_IS_FIRST_LOGIN, false).apply()
    }

    private fun firstLoginRemove() {
        prefs.edit().remove(PREF_IS_FIRST_LOGIN).apply()
    }

    fun firstMailboxLoadDone() {
        prefs.edit().putBoolean(PREF_IS_FIRST_MAILBOX_LOAD_AFTER_LOGIN, false).apply()
    }

    fun firstMessageDetailsDone() {
        prefs.edit().putBoolean(PREF_IS_FIRST_MESSAGE_DETAILS, false).apply()
    }

    fun engagementDone() {
        backupPrefs.edit().putBoolean(PREF_ENGAGEMENT_SHOWN, true).apply()
    }

    fun engagementShowNextTime() {
        backupPrefs.edit().putBoolean(PREF_ENGAGEMENT_SHOWN, false).apply()
    }

    /**
     * This sets the primary user of the application.
     */
    @Synchronized
    fun setUsernameAndReload(username: String) {
        // TODO if it's possible at one point, we need to handle successful login in one place and
        //  make all those setters of shared pref values (like is_logged_in) private to this class
        prefs.edit().putString(PREF_USERNAME, username).apply()
        val currentUsername = this.username
        if (currentUsername != username) {
            clearBackupPrefs()
            savePin("")
        }
        backupPrefs.edit().putString(PREF_USERNAME, username).apply()
        engagementDone() // we set this to done since it is the same person that has switched account

        AccountManager.getInstance(context).onSuccessfulLogin(username)
        mMailboxPassword = null
    }

    fun increaseIncorrectPinAttempt() {
        val secureSharedPreferences = app.secureSharedPreferences
        var attempts = secureSharedPreferences.getInt(PREF_PIN_INCORRECT_ATTEMPTS, 0)
        secureSharedPreferences.edit().putInt(PREF_PIN_INCORRECT_ATTEMPTS, ++attempts).apply()
    }

    fun resetPinAttempts() {
        val secureSharedPreferences = app.secureSharedPreferences
        secureSharedPreferences.edit().putInt(PREF_PIN_INCORRECT_ATTEMPTS, 0).apply()
    }

    fun savePin(mailboxPin: String?) {
        val secureSharedPreferences = app.secureSharedPreferences
        secureSharedPreferences.edit().putString(PREF_PIN, mailboxPin).apply()
//        mMailboxPin = mailboxPin
    }

    fun getMailboxPin(): String? {
        return app.secureSharedPreferences.getString(PREF_PIN, "")
    }

    @JvmOverloads
    fun saveMailboxPassword(mailboxPassword: ByteArray, userName: String = username) {
        val secureSharedPreferences = app.getSecureSharedPreferences(userName)
        secureSharedPreferences.edit().putString(PREF_MAILBOX_PASSWORD, String(mailboxPassword)).apply()
        mMailboxPassword = String(mailboxPassword) // TODO passphrase
    }

    @JvmOverloads
    fun saveKeySalt(keysSalt: String?, userName: String = username) {
        val secureSharedPreferences = app.getSecureSharedPreferences(userName)
        secureSharedPreferences.edit().putString(PREF_KEY_SALT, keysSalt).apply()
    }

    @JvmOverloads
    fun getMailboxPassword(userName: String = username): ByteArray? {
        val secureSharedPreferences = app.getSecureSharedPreferences(userName) /*TODO passphrase*/
        return secureSharedPreferences.getString(PREF_MAILBOX_PASSWORD, null)?.toByteArray(Charsets.UTF_8)
    }

    fun accessTokenExists(): Boolean {
        val exists = tokenManager?.let {
            !TextUtils.isEmpty(it.authAccessToken)
        }
        return exists?: false
    }

    @JvmOverloads
    fun setUserInfo(
            userInfo: UserInfo,
            username: String? = null,
            mailSettings: MailSettings,
            userSettings: UserSettings,
            addresses: List<Address>
    ) {
        val user = userInfo.user
        user.username = username?: this.username
        user.setAddressIdEmail()
        user.setAddresses(addresses)
        this.mailSettings = mailSettings
        this.userSettings = userSettings
        this.snoozeSettings = SnoozeSettings.load(user.username)
        user.save()
        this.user = user
    }

    /**
     * Use this method to get User's settings for other users than currently active.
     *
     * @return [User] object for given username, might have empty values if user was not saved before
     */
    @Synchronized
    fun getUser(username: String): User {
        if (!userReferences.containsKey(username)) {
            val newUser = User.load(username)
            if (!username.isBlank()) {
                userReferences[username] = newUser
            }
            return newUser
        }
        return userReferences[username]!!
    }

    fun canConnectAccount(): Boolean {
        val accountManager = AccountManager.getInstance(context)
        val freeLoggedInUsersList = accountManager.getLoggedInUsers().map {
            userReferences[it]
        }.filter {
            !(it?.isPaidUser ?: false)
        }
        return freeLoggedInUsersList.size <= 1
    }

    fun getTokenManager(username: String): TokenManager? {
        val tokenManager = TokenManager.getInstance(username, openPgp)
        // make sure the private key is here
        tokenManager?.let {
            if (it.encPrivateKey.isNullOrBlank()) {
                val user = getUser(username)
                for (key in user.keys) {
                    if (key.isPrimary) {
                        it.encPrivateKey = key.privateKey // it's needed for verification later
                        break
                    }
                }
            }
        }
        return tokenManager
    }

    fun canShowStorageLimitWarning(): Boolean {
        val secureSharedPreferences = app.getSecureSharedPreferences(username)
        return secureSharedPreferences.getBoolean(PREF_SHOW_STORAGE_LIMIT_WARNING, true)
    }

    fun setShowStorageLimitWarning(value: Boolean) {
        val secureSharedPreferences = app.getSecureSharedPreferences(username)
        secureSharedPreferences.edit().putBoolean(PREF_SHOW_STORAGE_LIMIT_WARNING, value).apply()
    }

    fun canShowStorageLimitReached(): Boolean {
        val secureSharedPreferences = app.getSecureSharedPreferences(username)
        return secureSharedPreferences.getBoolean(PREF_SHOW_STORAGE_LIMIT_REACHED, true)
    }

    fun setShowStorageLimitReached(value: Boolean) {
        val secureSharedPreferences = app.getSecureSharedPreferences(username)
        secureSharedPreferences.edit().putBoolean(PREF_SHOW_STORAGE_LIMIT_REACHED, value).apply()
    }

    @Produce
    fun produceKeyPairEvent(): GenerateKeyPairEvent? {
        return mGenerateKeyPairEvent
    }

    fun resetGenerateKeyPairEvent() {
        this.mGenerateKeyPairEvent = GenerateKeyPairEvent(false, null)
    }


    private fun clearBackupPrefs() {
        backupPrefs.edit().clear().apply()
    }


    fun setSnoozeScheduled(
            isOn: Boolean,
            startTimeHour: Int,
            startTimeMinute: Int,
            endTimeHour: Int,
            endTimeMinute: Int,
            repeatingDays: String
    ) {
        snoozeSettings!!.snoozeScheduled = isOn
        snoozeSettings!!.snoozeScheduledStartTimeHour = startTimeHour
        snoozeSettings!!.snoozeScheduledStartTimeMinute = startTimeMinute
        snoozeSettings!!.snoozeScheduledEndTimeHour = endTimeHour
        snoozeSettings!!.snoozeScheduledEndTimeMinute = endTimeMinute
        snoozeSettings!!.snoozeScheduledRepeatingDays = repeatingDays
        snoozeSettings!!.save(username)
    }

    fun setSnoozeQuick(isOn: Boolean, minutesFromNow: Int) {
        snoozeSettings!!.snoozeQuick = isOn
        snoozeSettings!!.snoozeQuickEndTime = System.currentTimeMillis() + minutesFromNow * 60 * 1000
        snoozeSettings!!.saveQuickSnoozeBackup(username)
        snoozeSettings!!.saveQuickSnoozeEndTimeBackup(username)
        snoozeSettings!!.save(username)
    }

    private fun loadSettings(username : String){
        if (username.isBlank()) {
            return
        }
        userSettings = UserSettings.load(username)
        mailSettings = MailSettings.load(username)
        snoozeSettings = SnoozeSettings.load(username)
        user.autoLockPINPeriod
    }

    fun removeEmptyUserReferences() {
        userReferences.remove("")
    }
}
