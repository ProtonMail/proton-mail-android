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
import androidx.annotation.IntDef
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.R
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.api.local.SnoozeSettings
import ch.protonmail.android.api.models.LoginInfoResponse
import ch.protonmail.android.api.models.LoginResponse
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.UserSettings
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.api.services.LoginService
import ch.protonmail.android.api.services.LogoutService
import ch.protonmail.android.di.BackupSharedPreferences
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.Plan
import ch.protonmail.android.domain.util.orThrow
import ch.protonmail.android.domain.util.suspendRunCatching
import ch.protonmail.android.events.ForceSwitchedAccountNotifier
import ch.protonmail.android.events.GenerateKeyPairEvent
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.events.SwitchUserEvent
import ch.protonmail.android.fcm.FcmTokenManager
import ch.protonmail.android.mapper.bridge.UserBridgeMapper
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.usecase.LoadLegacyUser
import ch.protonmail.android.usecase.LoadUser
import ch.protonmail.android.usecase.delete.ClearUserData
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.obfuscateUsername
import com.squareup.otto.Produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.util.android.sharedpreferences.clearAll
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.android.sharedpreferences.minusAssign
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.invoke
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.minutes
import ch.protonmail.android.domain.entity.user.User as NewUser

// region constants
const val LOGIN_STATE_NOT_INITIALIZED = 0
const val LOGIN_STATE_LOGIN_FINISHED = 2
const val LOGIN_STATE_TO_INBOX = 3

const val PREF_PIN = "mailbox_pin"
private const val PREF_CURRENT_USER_ID = "prefs.current.user.id"
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
    private val context: Context,
    private val accountManager: AccountManager,
    private val clearUserData: ClearUserData,
    private val loadUser: LoadUser,
    private val loadLegacyUser: LoadLegacyUser,
    private val userMapper: UserBridgeMapper,
    @DefaultSharedPreferences private val prefs: SharedPreferences,
    @BackupSharedPreferences private val backupPrefs: SharedPreferences,
    @Deprecated("UserManager is never using this, but it's just providing for other classes, so this " +
        "should be injected directly there")
    val openPgp: OpenPGP,
    private val fcmTokenManagerFactory: FcmTokenManager.Factory,
    private val dispatchers: DispatcherProvider
) {

    // TODO caching strategy should be in the repository
    private val cachedUsers = mutableMapOf<Id, NewUser>()

    // TODO caching strategy should be in the repository
    private val cachedLegacyUsers = mutableMapOf<Id, User>()

    private var _checkTimestamp: Float = 0.toFloat()
    private var _mailboxPassword: String? = null
    private val app: ProtonMailApplication = context.app
    private var generatingKeyPair: Boolean = false

    var privateKey: String? = null
        set(privateKey) {
            field = privateKey
            if (privateKey != null) {
                generateKeyPairEvent = GenerateKeyPairEvent(true, null)
            }
            if (generateKeyPairEvent != null) {
                AppUtil.postEventOnUi(generateKeyPairEvent)
            }
            if (mCreateUserOnKeyPairGenerationFinish) {
                LoginService.startCreateUser(
                    mNewUserUsername, mNewUserPassword, mNewUserUpdateMe,
                    mNewUserTokenType, mNewUserToken
                )
            } else {
                mCreateUserOnKeyPairGenerationFinish = false
            }
        }

    var userSettings: UserSettings? = null
        set(value) {
            field = value
            if (value != null) withCurrentUserPreferences(value::save)
        }

    suspend fun getCurrentUserMailSettings(): MailSettings? =
        currentUserId?.let { getMailSettings(it) }

    @Deprecated("Use suspend function", ReplaceWith("getCurrentUserMailSettings()"))
    fun getCurrentUserMailSettingsBlocking(): MailSettings? =
        runBlocking { getCurrentUserMailSettings() }

    suspend fun getMailSettings(userId: Id): MailSettings =
        MailSettings.load(SecureSharedPreferences.getPrefsForUser(context, userId))

    @Deprecated("Use suspend function", ReplaceWith("getMailSettings(userId)"))
    fun getMailSettingsBlocking(userId: Id): MailSettings =
        runBlocking { getMailSettings(userId) }

    var snoozeSettings: SnoozeSettings? = null

    private var mNewUserUsername: String? = null
    private var mNewUserPassword: ByteArray? = null
    private var mNewUserUpdateMe: Boolean = false
    private var mNewUserTokenType: Constants.TokenType? = null
    private var mNewUserToken: String? = null
    private var mCreateUserOnKeyPairGenerationFinish: Boolean = false

    /**
     * @return [Id] of next logged in User.
     *   `null` if there is not logged in User, beside the current ( if any )
     *   @see currentUserId
     */
    suspend fun getNextLoggedInUser(): Id? {
        val allLoggedIn = accountManager.allLoggedIn()
        val currentIndex = allLoggedIn.indexOf(currentUserId)
        val nextIndex = (currentIndex + 1).takeIf { it < allLoggedIn.size } ?: 0
        return if (nextIndex != currentIndex)
            allLoggedIn.elementAtOrNull(nextIndex)
        else null
    }

    @Deprecated("Use suspend function", ReplaceWith("getNextLoggedInUser()"))
    fun getNextLoggedInUserBlocking(): Id? =
        runBlocking { getNextLoggedInUser() }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(PREF_IS_LOGGED_IN, false)
        set(isLoggedIn) = prefs.edit().putBoolean(PREF_IS_LOGGED_IN, isLoggedIn).apply()

    val isFirstLogin: Boolean
        get() = prefs.getBoolean(PREF_IS_FIRST_LOGIN, true) &&
            prefs.getInt(PREF_APP_VERSION, Integer.MIN_VALUE) != BuildConfig.VERSION_CODE

    val isFirstMailboxLoad: Boolean
        get() = prefs.getBoolean(PREF_IS_FIRST_MAILBOX_LOAD_AFTER_LOGIN, true)

    val isFirstMessageDetails: Boolean
        get() = prefs.getBoolean(PREF_IS_FIRST_MESSAGE_DETAILS, true)

    val isEngagementShown: Boolean
        get() = backupPrefs.getBoolean(PREF_ENGAGEMENT_SHOWN, false)

    var currentUserId: Id?
        get() = prefs.get<String>(PREF_CURRENT_USER_ID)?.let(::Id)
        private set(value) {
            prefs[PREF_CURRENT_USER_ID] = value?.s
        }

    private val currentUserPreferences
        get() = currentUserId?.let { SecureSharedPreferences.getPrefsForUser(context, it) }

    private inline fun <T> withCurrentUserPreferences(block: (SharedPreferences) -> T): T? {
        currentUserPreferences ?: Timber.e("No current user set")
        return currentUserPreferences?.let(block)
    }

    @Deprecated("Use 'currentUserId'", ReplaceWith("currentUserId"), DeprecationLevel.ERROR)
    val username: String
        get() = prefs.getString(PREF_USERNAME, "")!!

    val incorrectPinAttempts: Int
        get() {
            val secureSharedPreferences = app.secureSharedPreferences
            return secureSharedPreferences.getInt(PREF_PIN_INCORRECT_ATTEMPTS, 0)
        }

    val currentUserKeySalt: String?
        get() = withCurrentUserPreferences { it[PREF_KEY_SALT] }

    @Deprecated("Use 'currentUser' variant", ReplaceWith("currentUserKeySalt"))
    val keySalt: String?
        get() = currentUserKeySalt

    /**
     * @return [Int] representing [LoginState] for current user
     *   `null` if there is no current user set
     *   @see currentUserId
     */
    var currentUserLoginState: Int
        @LoginState
        get() = withCurrentUserPreferences { it[PREF_LOGIN_STATE] ?: LOGIN_STATE_NOT_INITIALIZED }
            ?: LOGIN_STATE_NOT_INITIALIZED
        set(@LoginState value) {
            withCurrentUserPreferences {
                it[PREF_LOGIN_STATE] = value
            }
        }

    @Deprecated("Use 'currentUser' variant", ReplaceWith("currentUserLoginState"))
    var loginState: Int
        @LoginState
        get() = currentUserLoginState
        set(@LoginState status) {
            currentUserLoginState = status
        }

    fun requireCurrentUserId(): Id =
        checkNotNull(currentUserId)

    suspend fun getCurrentUser(): NewUser? =
        currentUserId?.let {
            runCatching { getUser(it) }
                .getOrElse {
                    Timber.d("Cannot load user", it)
                    null
                }
        }

    suspend fun requireCurrentUser(): NewUser =
        getUser(requireCurrentUserId())

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("getCurrentUser()")
    )
    fun getCurrentUserBlocking(): NewUser? =
        runBlocking { getCurrentUser() }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("requireCurrentUser()")
    )
    fun requireCurrentUserBlocking(): NewUser =
        runBlocking { getUser(requireCurrentUserId()) }

    suspend fun getCurrentLegacyUser(): User? =
        currentUserId?.let {
            runCatching { getLegacyUser(it) }
                .getOrElse {
                    Timber.d("Cannot load user", it)
                    null
                }
        }

    suspend fun requireCurrentLegacyUser(): User =
        getLegacyUser(requireCurrentUserId())

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("getCurrentLegacyUser()")
    )
    fun getCurrentLegacyUserBlocking(): User? =
        runBlocking { getCurrentLegacyUser() }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("requireCurrentOldUser()")
    )
    fun requireCurrentLegacyUserBlocking(): User =
        runBlocking { getLegacyUser(requireCurrentUserId()) }

    /**
     * Use this method to get settings for currently active User.
     */
    @get:Deprecated("Use ''getCurrentLegacyUser'", ReplaceWith("getCurrentLegacyUser()"))
    @set:Deprecated("Use 'setCurrentUser' with User Id", ReplaceWith("setCurrentUser(userId)"))
    var user: User
        @Synchronized get() = requireNotNull(getCurrentLegacyUserBlocking())
        set(user) {
            setCurrentUserBlocking(Id(user.id))
        }

    suspend fun getCurrentUserTokenManager(): TokenManager? =
        currentUserId?.let { getTokenManager(it) }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("getCurrentUserTokenManager()")
    )
    fun getCurrentUserTokenManagerBlocking(): TokenManager? =
        runBlocking {
            getCurrentUserTokenManager()
        }

    @Deprecated("Use 'currentUser' variant", ReplaceWith("getCurrentUserTokenManager()"))
    val tokenManager: TokenManager?
        get() = runBlocking { getCurrentUserTokenManager() }

    suspend fun getUserIdBySessionId(sessionId: String): Id? =
        accountManager.allLoggedIn().find {
            sessionId == getTokenManager(it).uid
        }

    @Deprecated("Use suspend function", ReplaceWith("getUserIdBySessionId(sessionId)"))
    fun getUserIdBySessionIdBlocking(sessionId: String): Id? =
        runBlocking { getUserIdBySessionId(sessionId) }

    var checkTimestamp: Float
        get() = _checkTimestamp
        set(checkTimestamp) {
            if (checkTimestamp > _checkTimestamp) {
                _checkTimestamp = checkTimestamp
                prefs.edit().putFloat(PREF_CHECK_TIMESTAMP, _checkTimestamp).apply()
            }
        }

    private var generateKeyPairEvent: GenerateKeyPairEvent? = null

    suspend fun isCurrentUserBackgroundSyncEnabled(): Boolean {
        val userId = requireNotNull(currentUserId)
        return getLegacyUser(userId).isBackgroundSync
    }

    @Deprecated("Use 'currentUser' variant", ReplaceWith("isCurrentUserBackgroundSyncEnabled()"))
    val isBackgroundSyncEnabled: Boolean
        get() = user.isBackgroundSync

    fun isCurrentUserSnoozeScheduledEnabled(): Boolean {
        val userId = requireNotNull(currentUserId)
        return requireNotNull(snoozeSettings).getScheduledSnooze(preferencesFor(userId))
    }

    @Deprecated("Use 'currentUser' variant", ReplaceWith("isCurrentUserSnoozeScheduledEnabled()"))
    fun isSnoozeScheduledEnabled(): Boolean =
        isCurrentUserSnoozeScheduledEnabled()

    // Very important: don't setSnoozeQuick to false if it already is false otherwise it will
    // keep saving, saving can be super slow on users with a lot of accounts!
    suspend fun isSnoozeQuickEnabled(): Boolean {
        if (snoozeSettings!!.snoozeQuick && snoozeSettings!!.snoozeQuickEndTime - System.currentTimeMillis() <= 0) {
            setSnoozeQuick(false, 0)
        }
        return snoozeSettings!!.snoozeQuick
    }

    // Very important: don't setSnoozeQuick to false if it already is false otherwise it will
    // keep saving, saving can be super slow on users with a lot of accounts!
    @Deprecated("Use suspend function", ReplaceWith("isSnoozeQuickEnabled"))
    fun isSnoozeQuickEnabledBlocking(): Boolean =
        runBlocking { isSnoozeQuickEnabled() }

    @IntDef(LOGIN_STATE_NOT_INITIALIZED, LOGIN_STATE_LOGIN_FINISHED, LOGIN_STATE_TO_INBOX)
    internal annotation class LoginState

    /**
     * Do not instantiate on Main thread!
     *
     * @see MIGRATE_FROM_BUILD_CONFIG_FIELD_DOC
     */
    init {
        val currentAppVersion = BuildConfig.VERSION_CODE
        val previousVersion = prefs.getInt(Constants.Prefs.PREF_APP_VERSION, Integer.MIN_VALUE)
        // if this version requires the user to be logged out when updating
        // and if every single previous version should be force logged out
        // or any specific previous version should be logged out
        runBlocking { currentUserId?.let { loadSettings(it) } }
        if (previousVersion == currentAppVersion) {
            _checkTimestamp = this.prefs.getFloat(PREF_CHECK_TIMESTAMP, 0f)
        }
        app.bus.register(this)
    }

    @Deprecated("Suspended function should be used instead", ReplaceWith("resetReferences"))
    private fun resetReferencesBlocking() = runBlocking { resetReferences() }

    private suspend fun resetReferences() = withContext(dispatchers.Io) {
        _checkTimestamp = 0f
        currentUserId?.let { cachedLegacyUsers -= it }
        _mailboxPassword = null
//        mMailboxPin = null
        app.eventManager.clearState()
    }

    fun generateKeyPair(username: String, domain: String, password: ByteArray, bits: Int) {
        generateKeyPairEvent = GenerateKeyPairEvent(false, null)
        generatingKeyPair = true
        LoginService.startGenerateKeys(username, domain, password, bits)
    }

    fun createUser(
        username: String,
        password: ByteArray,
        updateMe: Boolean,
        tokenType: Constants.TokenType,
        token: String
    ) {
        if (privateKey == null && generatingKeyPair) {
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
        LoginService.startLogin(
            username, password, response, fallbackAuthVersion, signUp
        )
    }

    fun twoFA(
        userId: Id,
        username: Name,
        password: ByteArray,
        twoFactor: String?,
        infoResponse: LoginInfoResponse?,
        loginResponse: LoginResponse?,
        fallbackAuthVersion: Int,
        signUp: Boolean,
        isConnecting: Boolean
    ) {
        LoginService.start2FA(
            userId,
            username,
            password,
            twoFactor,
            infoResponse,
            loginResponse,
            fallbackAuthVersion,
            signUp,
            isConnecting
        )
    }

    // used for two-password mode. Since already the user is logged in, we should have the userID by now
    fun mailboxLogin(userId: Id, mailboxPassword: String, keySalt: String?, signUp: Boolean) {
        LoginService.startMailboxLogin(userId, mailboxPassword, keySalt, signUp)
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

    // used for two-password mode. Since already the user is logged in, we should have the userID by now
    // TODO: find better name for its purpose
    suspend fun connectAccountMailboxLogin(
        userId: Id,
        currentPrimaryUserId: Id?,
        mailboxPassword: String,
        keySalt: String
    ) {
        LoginService.startConnectAccountMailboxLogin(userId, currentPrimaryUserId, mailboxPassword, keySalt)
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("connectAccountMailboxLoginBlocking(userId, currentPrimaryId, mailboxPassword, keySalt)")
    )
    fun connectAccountMailboxLoginBlocking(
        userId: Id,
        currentPrimaryUserId: Id,
        mailboxPassword: String,
        keySalt: String
    ) = runBlocking {
        connectAccountMailboxLogin(userId, currentPrimaryUserId, mailboxPassword, keySalt)
    }

    suspend fun switchTo(userId: Id) {
        setCurrentUser(userId)
        user = loadLegacyUser(userId).orThrow()
        loadSettings(userId)
    }

    suspend fun logoutAndRemove(userId: Id) {
        logout(userId)
        accountManager.remove(userId)
    }

    @Deprecated("Use suspend function", ReplaceWith("logoutAndRemove(userId)"))
    fun logoutAndRemoveBlocking(userId: Id) {
        runBlocking {
            logoutAndRemove(userId)
        }
    }

    suspend fun logout(userId: Id) = withContext(dispatchers.Io) {
        val nextLoggedIn = getNextLoggedInUser()
            // fallback to "last user logout"
            ?: return@withContext logoutLastActiveAccount()
        LogoutService.startLogout(context, userId, false)
        accountManager.setLoggedOut(userId)
        AppUtil.deleteSecurePrefs(preferencesFor(userId), false)
        launch {
            clearUserData(userId)
        }
        setCurrentUser(nextLoggedIn)
        app.eventManager.clearState(userId)
    }

    @Deprecated("Use suspend function", ReplaceWith("logout(userId)"))
    fun logoutBlocking(userId: Id) {
        runBlocking { logout(userId) }
    }

    /**
     * Log out the only active ( logged in ) user
     */
    suspend fun logoutLastActiveAccount() {
        val userId = requireCurrentUserId()
        saveCurrentUserBackupSettings()
        clearUserData(userId)
        isLoggedIn = false
        currentUserLoginState = LOGIN_STATE_NOT_INITIALIZED
        // Passing FCM token already here to prevent it being deleted from shared prefs before worker starts
        val fcmTokenManager = fcmTokenManagerFactory.create(preferencesFor(userId))
        LogoutService.startLogout(context, userId, true, fcmTokenManager.getTokenBlocking()?.value)
        setRememberMailboxLogin(false)
        firstLoginRemove()
        resetReferencesBlocking()
        AppUtil.deleteSecurePrefs(requireNotNull(currentUserPreferences), true)
        AppUtil.deletePrefs()
        AppUtil.deleteBackupPrefs()
        AppUtil.postEventOnUi(LogoutEvent(Status.SUCCESS))
    }

    @Deprecated("Use suspend function", ReplaceWith("logoutLastActiveAccount()"))
    fun logoutLastActiveAccountBlocking() {
        runBlocking { logoutLastActiveAccount() }
    }

    suspend fun logoutOffline(userId: Id) = withContext(dispatchers.Io) {
        val nextUserId = getNextLoggedInUser()

        if (currentUserId !in accountManager.allLoggedIn()) {
            return@withContext
        }

        if (nextUserId == null) {
            isLoggedIn = false
            currentUserLoginState = LOGIN_STATE_NOT_INITIALIZED
            saveCurrentUserBackupSettings()
            LogoutService.startLogout(context, userId, false)
            setRememberMailboxLogin(false)
            AppUtil.deleteSecurePrefs(preferencesFor(userId), true)
            AppUtil.deletePrefs()
            AppUtil.deleteBackupPrefs()
            firstLoginRemove()
            resetReferences()
            getCurrentUserTokenManager()?.clear()
            clearUserData(userId)
            AppUtil.postEventOnUi(LogoutEvent(Status.SUCCESS))
            TokenManager.clearAllInstances()
        } else {
            val oldUser = requireCurrentUser()
            accountManager.setLoggedOut(userId)
            AppUtil.deleteSecurePrefs(preferencesFor(userId), false)
            clearUserData(userId)
            setCurrentUser(nextUserId)
            val nextUser = loadUser(nextUserId).orThrow()
            val event = SwitchUserEvent(from = oldUser.id to oldUser.name, to = nextUser.id to nextUser.name)
            ForceSwitchedAccountNotifier.notifier.postValue(event)
            TokenManager.clearInstance(userId)
        }
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("logoutOffline()")
    )
    fun logoutOfflineBlocking(userId: Id) = runBlocking {
        logoutOffline(userId)
    }

    suspend fun logoutCurrentUserOffline() {
        logoutOffline(requireCurrentUserId())
    }

    @Deprecated("Use suspend function", ReplaceWith("logoutCurrentUserOffline"))
    fun logoutCurrentUserOfflineBlocking() {
        runBlocking { logoutOffline(requireCurrentUserId()) }
    }

    private suspend fun saveCurrentUserBackupSettings() = withContext(dispatchers.Io) {
        getCurrentLegacyUser()?.apply {
            saveNotificationSettingsBackup()
            saveAutoLogoutBackup()
            saveAutoLockPINPeriodBackup()
            saveUsePinBackup()
            saveUseFingerprintBackup()
            saveNotificationVisibilityLockScreenSettingsBackup()
            saveRingtoneBackup()
            saveCombinedContactsBackup()
        }
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

    @Synchronized
    suspend fun setCurrentUser(userId: Id) = withContext(dispatchers.Io) {
        // TODO if it's possible at one point, we need to handle successful login in one place and
        //  make all those setters of shared pref values (like is_logged_in) private to this class
        val prevUserId = currentUserId
        currentUserId = userId
        backupPrefs[PREF_CURRENT_USER_ID] = userId.s
        if (userId != prevUserId) {
            clearBackupPrefs()
            savePin("")
        }
        engagementDone() // we set this to done since it is the same person that has switched account

        accountManager.setLoggedIn(requireCurrentUserId())
        _mailboxPassword = null
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("setCurrentUser(userId)")
    )
    fun setCurrentUserBlocking(userId: Id) {
        runBlocking { setCurrentUser(userId) }
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

    fun getMailboxPin(): String? =
        app.secureSharedPreferences.getString(PREF_PIN, "")

    suspend fun saveMailboxPassword(userId: Id, mailboxPassword: ByteArray) = withContext(dispatchers.Io) {
        val secureSharedPreferences = SecureSharedPreferences.getPrefsForUser(context, userId)
        secureSharedPreferences[PREF_MAILBOX_PASSWORD] = String(mailboxPassword)
        // TODO: this should not be saved, mailbox password should not be persisted in memory, especially for this
        //  class, especially in a multi-user context
        _mailboxPassword = String(mailboxPassword)
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("saveMailboxPassword(userId, mailboxPassword)")
    )
    @JvmOverloads
    fun saveMailboxPasswordBlocking(userId: Id? = currentUserId, mailboxPassword: ByteArray) = runBlocking {
        if(userId == null) {
            // TODO: how to save the mailbox password if the userId is not available yet
        } else {
            saveMailboxPassword(userId, mailboxPassword)
        }
    }

    suspend fun saveKeySalt(userId: Id, keysSalt: String?) = withContext(dispatchers.Io) {
        val secureSharedPreferences = SecureSharedPreferences.getPrefsForUser(context, userId)
        secureSharedPreferences[PREF_KEY_SALT] = keysSalt
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("saveKeySalt(userId, keySalt)")
    )
    @JvmOverloads
    fun saveKeySaltBlocking(userId: Id? = currentUserId, keysSalt: String?) = runBlocking {
        if(userId == null) {
            // TODO: how to save the keySalt when the userId is not available yet

        } else {
            saveKeySalt(userId, keysSalt)
        }
    }

    fun getMailboxPassword(userId: Id): ByteArray? =
        preferencesFor(userId).get<String>(PREF_MAILBOX_PASSWORD)?.toByteArray(Charsets.UTF_8)

    fun getCurrentUserMailboxPassword(): ByteArray? =
        withCurrentUserPreferences { it.get<String>(PREF_MAILBOX_PASSWORD)?.toByteArray(Charsets.UTF_8) }

    fun accessTokenExists(): Boolean {
        val exists = tokenManager?.let {
            it.authAccessToken?.isNotEmpty()
        }
        return exists ?: false
    }

    suspend fun setUserDetails(
        user: User,
        addresses: List<Address>,
        mailSettings: MailSettings,
        userSettings: UserSettings
    ) = withContext(dispatchers.Io) {

        user.apply {
            setAddressIdEmail()
            setAddresses(addresses)
            save()
        }
        this@UserManager.apply {
            this.userSettings = userSettings
            this.snoozeSettings = SnoozeSettings.load(preferencesFor(Id(user.id)))
            this.user = user
        }
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("setUserDetails(user, addreses, mailSettings, userSettings)")
    )
    fun setUserDetailsBlocking(
        user: User,
        addresses: List<Address>,
        mailSettings: MailSettings,
        userSettings: UserSettings
    ) = runBlocking {
        currentUserId = Id(user.id)
        setUserDetails(user, addresses, mailSettings, userSettings)
    }

    @Synchronized
    suspend fun getUser(userId: Id): NewUser =
        cachedUsers.getOrPut(userId) {
            loadUser(userId).orThrow()
        }

    @Deprecated("Suspended function should be used instead", ReplaceWith("getUser(userId)"))
    fun getUserBlocking(userId: Id): NewUser =
        runBlocking { getUser(userId) }

    /**
     * Note, returned [User] might have empty values if user was not saved before
     */
    @Synchronized
    suspend fun getLegacyUser(userId: Id): User =
        cachedLegacyUsers.getOrPut(userId) {
            loadLegacyUser(userId).orThrow()
                // Also save to cachedUsers
                .also { legacyUser ->
                    runCatching { userMapper { legacyUser.toNewUser() } }
                        .onSuccess { cachedUsers[userId] = it }
                        .onFailure(Timber::e)
                }
        }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("getLegacyUser(userId)")
    )
    fun getLegacyUserBlocking(userId: Id) = runBlocking {
        getLegacyUser(userId)
    }

    /**
     * @return `true` if another account can be connected
     *   `false` if there are logged in more than one Free account
     *   @see NewUser.plans
     *   @see Plan.Mail.Free
     */
    suspend fun canConnectAnotherAccount(): Boolean {
        val freeLoggedInUserCount = accountManager.allLoggedIn().count {
            val user = suspendRunCatching { getUser(it) }
                .getOrNull()
                ?: return@count false
            Plan.Mail.Free in user.plans
        }
        return freeLoggedInUserCount <= 1
    }

    @Deprecated("Use suspend function", ReplaceWith("canConnectAnotherAccount"))
    fun canConnectAnotherAccountBlocking(): Boolean =
        runBlocking { canConnectAnotherAccount() }

    suspend fun getTokenManager(userId: Id): TokenManager {
        val tokenManager = TokenManager.getInstance(context, userId)
        // make sure the private key is here
        if (tokenManager.encPrivateKey.isNullOrBlank()) {
            val user = runCatching { getUser(userId) }
                .getOrElse { return tokenManager }
            // it's needed for verification later
            tokenManager.encPrivateKey = user.keys.primaryKey?.privateKey?.string
        }
        return tokenManager
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("getTokenManager(userId)")
    )
    fun getTokenManagerBlocking(userId: Id) = runBlocking {
        getTokenManager(userId)
    }

    fun canShowStorageLimitWarning(): Boolean =
        withCurrentUserPreferences { it[PREF_SHOW_STORAGE_LIMIT_WARNING] } ?: true

    fun setShowStorageLimitWarning(value: Boolean) {
        withCurrentUserPreferences {
            it[PREF_SHOW_STORAGE_LIMIT_WARNING] = value
        }
    }

    fun canShowStorageLimitReached(): Boolean =
        withCurrentUserPreferences { it[PREF_SHOW_STORAGE_LIMIT_REACHED] } ?: true

    fun setShowStorageLimitReached(value: Boolean) {
        withCurrentUserPreferences {
            it[PREF_SHOW_STORAGE_LIMIT_REACHED] = value
        }
    }

    @Produce
    fun produceKeyPairEvent(): GenerateKeyPairEvent? =
        generateKeyPairEvent

    fun resetGenerateKeyPairEvent() {
        generateKeyPairEvent = GenerateKeyPairEvent(false, null)
    }


    private fun clearBackupPrefs() {
        backupPrefs.clearAll()
    }

    /**
     * @throws IllegalStateException if [currentUserId] or [snoozeSettings] is `null`
     */
    suspend fun setSnoozeScheduled(
        isOn: Boolean,
        startTimeHour: Int,
        startTimeMinute: Int,
        endTimeHour: Int,
        endTimeMinute: Int,
        repeatingDays: String
    ) {
        val preferences = preferencesFor(requireCurrentUserId())
        checkNotNull(snoozeSettings).apply {
            snoozeScheduled = isOn
            snoozeScheduledStartTimeHour = startTimeHour
            snoozeScheduledStartTimeMinute = startTimeMinute
            snoozeScheduledEndTimeHour = endTimeHour
            snoozeScheduledEndTimeMinute = endTimeMinute
            snoozeScheduledRepeatingDays = repeatingDays
            save(preferences)
        }
    }

    /**
     * @throws IllegalStateException if [currentUserId] or [snoozeSettings] is `null`
     */
    @Deprecated(
        "Use suspend function",
        ReplaceWith(
            "setSnoozeScheduled(isOn, startTimeHour, startTimeMinute, endTimeHour, endTimeMinute, repeatingDays)"
        )
    )
    fun setSnoozeScheduledBlocking(
        isOn: Boolean,
        startTimeHour: Int,
        startTimeMinute: Int,
        endTimeHour: Int,
        endTimeMinute: Int,
        repeatingDays: String
    ) {
        runBlocking {
            setSnoozeScheduled(isOn, startTimeHour, startTimeMinute, endTimeHour, endTimeMinute, repeatingDays)
        }
    }

    /**
     * @throws IllegalStateException if [currentUserId] or [snoozeSettings] is `null`
     */
    suspend fun setSnoozeQuick(isOn: Boolean, minutesFromNow: Int) {
        val preferences = preferencesFor(requireCurrentUserId())
        checkNotNull(snoozeSettings).apply {
            snoozeQuick = isOn
            snoozeQuickEndTime = System.currentTimeMillis() + minutesFromNow.minutes.toLongMilliseconds()
            saveQuickSnoozeBackup(preferences)
            saveQuickSnoozeEndTimeBackup(preferences)
            save(preferences)
        }
    }

    /**
     * @throws IllegalStateException if [currentUserId] or [snoozeSettings] is `null`
     */
    @Deprecated("Use suspend function", ReplaceWith("setSnoozeQuick(isOn, minutesFromNow)"))
    fun setSnoozeQuickBlocking(isOn: Boolean, minutesFromNow: Int) {
        runBlocking { setSnoozeQuick(isOn, minutesFromNow) }
    }

    private suspend fun loadSettings(userId: Id) {
        withContext(dispatchers.Io) {
            val preferences = preferencesFor(userId)
            userSettings = UserSettings.load(preferences)
            snoozeSettings = SnoozeSettings.load(preferences)
            // Reload autoLockPINPeriod
            suspendRunCatching { getLegacyUser(userId) }
                .onSuccess { it.autoLockPINPeriod }
                // This is expected in some scenarios, as the user is not saved yet, so logging higher than info is
                //  not necessary
                .onFailure { Timber.i("Cannot load user settings") }
        }
    }

    fun didReachLabelsThreshold(numberOfLabels: Int): Boolean = getMaxLabelsAllowed() < numberOfLabels

    fun getMaxLabelsAllowed(): Int {
        val accountTypes = app.resources.getStringArray(R.array.account_type_names).asList()
        val maxLabelsPerPlanArray = app.resources.getIntArray(R.array.max_labels_per_plan).asList()
        val organization = app.organization

        var paidUser = false
        var planName = accountTypes[0] // free

        var maxLabelsAllowed = maxLabelsPerPlanArray[0] // free


        if (organization != null) {
            planName = organization.planName
            paidUser = user.isPaidUser && organization.planName.isNullOrEmpty().not()
        }
        if (!paidUser) {
            return maxLabelsAllowed
        }

        for (i in 1 until accountTypes.size) {
            val accountName = accountTypes[i]
            if (accountName.equals(planName, ignoreCase = true)) {
                maxLabelsAllowed = maxLabelsPerPlanArray[i]
                break
            }
        }

        return maxLabelsAllowed
    }

    private fun preferencesFor(userId: Id) =
        SecureSharedPreferences.getPrefsForUser(context, userId)

    class UsernameToIdMigration @Inject constructor(
        @DefaultSharedPreferences private val prefs: SharedPreferences,
        private val dispatchers: DispatcherProvider
    ) {

        suspend operator fun invoke(allUsernamesToIds: Map<String, Id>) {
            withContext(dispatchers.Io) {
                val currentUsername = prefs.get<String?>(PREF_USERNAME)?.takeIfNotBlank()
                if (currentUsername == null) {
                    Timber.v("Cannot load current username for UserManager migration")
                    return@withContext
                }
                Timber.v("Migrating UserManager for user: ${currentUsername.obfuscateUsername()}")

                prefs -= PREF_USERNAME

                val currentUserId = allUsernamesToIds.getValue(currentUsername)
                prefs[PREF_CURRENT_USER_ID] = currentUserId.s
            }
        }
    }
}
