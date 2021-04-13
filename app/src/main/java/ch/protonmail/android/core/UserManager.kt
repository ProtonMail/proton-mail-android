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
import ch.protonmail.android.R
import ch.protonmail.android.api.local.SnoozeSettings
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.models.User
import ch.protonmail.android.di.BackupSharedPreferences
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.user.Plan
import ch.protonmail.android.domain.util.orThrow
import ch.protonmail.android.domain.util.suspendRunCatching
import ch.protonmail.android.feature.account.allLoggedIn
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.usecase.LoadLegacyUser
import ch.protonmail.android.usecase.LoadUser
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.obfuscateUsername
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.user.domain.UserManager
import me.proton.core.util.android.sharedpreferences.clearAll
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.android.sharedpreferences.minusAssign
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.minutes
import ch.protonmail.android.domain.entity.user.User as NewUser

// region constants
const val PREF_PIN = "mailbox_pin"
private const val PREF_CURRENT_USER_ID = "prefs.current.user.id"
const val PREF_USERNAME = "username"

private const val PREF_KEY_SALT = "key_salt"
private const val PREF_PIN_INCORRECT_ATTEMPTS = "mailbox_pin_incorrect_attempts"
private const val PREF_IS_FIRST_MAILBOX_LOAD_AFTER_LOGIN = "is_first_mailbox_load_after_login"
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
    internal val coreUserManager: UserManager,
    internal val coreKeyStoreCrypto: KeyStoreCrypto,
    private val coreAccountManager: AccountManager,
    private val loadUser: LoadUser,
    private val loadLegacyUser: LoadLegacyUser,
    @DefaultSharedPreferences private val prefs: SharedPreferences,
    @BackupSharedPreferences private val backupPrefs: SharedPreferences,
    @Deprecated(
        "UserManager is never using this, but it's just providing for other classes, so this " +
            "should be injected directly there"
    )
    val openPgp: OpenPGP,
    private val secureSharedPreferencesFactory: SecureSharedPreferences.Factory,
    private val dispatchers: DispatcherProvider
) {
    private val app: ProtonMailApplication = context.app

    suspend fun getCurrentUserMailSettings(): MailSettings? =
        currentUserId?.let { getMailSettings(it) }

    @Deprecated("Use suspend function", ReplaceWith("getCurrentUserMailSettings()"))
    fun getCurrentUserMailSettingsBlocking(): MailSettings? =
        runBlocking { getCurrentUserMailSettings() }

    @Deprecated("Use suspend function", ReplaceWith("getCurrentUserMailSettings()"))
    fun requireCurrentUserMailSettingsBlocking(): MailSettings =
       requireNotNull(getCurrentUserMailSettingsBlocking())

    suspend fun getMailSettings(userId: Id): MailSettings =
        MailSettings.load(preferencesFor(userId))

    @Deprecated("Use suspend function", ReplaceWith("getMailSettings(userId)"))
    fun getMailSettingsBlocking(userId: Id): MailSettings =
        runBlocking { getMailSettings(userId) }

    val snoozeSettings: SnoozeSettings?
        get() = runBlocking { currentUserId?.let { SnoozeSettings.load(preferencesFor(it)) } }

    val isFirstMailboxLoad: Boolean
        get() = prefs.getBoolean(PREF_IS_FIRST_MAILBOX_LOAD_AFTER_LOGIN, true)

    val isFirstMessageDetails: Boolean
        get() = prefs.getBoolean(PREF_IS_FIRST_MESSAGE_DETAILS, true)

    val isEngagementShown: Boolean
        get() = backupPrefs.getBoolean(PREF_ENGAGEMENT_SHOWN, false)

    val currentUserId: Id?
        get() =  runBlocking { coreAccountManager.getPrimaryUserId().firstOrNull()?.let { Id(it.id) } }

    private val currentUserPreferences
        get() = currentUserId?.let(::preferencesFor)

    private inline fun <T> withCurrentUserPreferences(block: (SharedPreferences) -> T): T? {
        currentUserPreferences ?: Timber.e("No current user set")
        return currentUserPreferences?.let(block)
    }

    val incorrectPinAttempts: Int
        get() {
            val secureSharedPreferences = app.secureSharedPreferences
            return secureSharedPreferences.getInt(PREF_PIN_INCORRECT_ATTEMPTS, 0)
        }

    suspend fun getPreviousCurrentUserId(): Id? = coreAccountManager.getPreviousPrimaryUserId()?.let { Id(it.id) }

    @Deprecated("Use suspend function", ReplaceWith("getPreviousCurrentUserId()"))
    fun getPreviousCurrentUserIdBlocking(): Id? = runBlocking { getPreviousCurrentUserId() }

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
    val user: User
         get() = requireNotNull(getCurrentLegacyUserBlocking())

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

    fun firstMailboxLoadDone() {
        prefs.edit().putBoolean(PREF_IS_FIRST_MAILBOX_LOAD_AFTER_LOGIN, false).apply()
    }

    fun firstMessageDetailsDone() {
        prefs.edit().putBoolean(PREF_IS_FIRST_MESSAGE_DETAILS, false).apply()
    }

    fun engagementDone() {
        backupPrefs.edit().putBoolean(PREF_ENGAGEMENT_SHOWN, true).apply()
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
    }

    fun getMailboxPin(): String? =
        app.secureSharedPreferences.getString(PREF_PIN, "")

    suspend fun saveKeySalt(userId: Id, keysSalt: String?) {
        withContext(dispatchers.Io) {
            val secureSharedPreferences = preferencesFor(userId)
            secureSharedPreferences[PREF_KEY_SALT] = keysSalt
        }
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("saveKeySalt(userId, keySalt)")
    )
    fun saveKeySaltBlocking(userId: Id, keysSalt: String?) {
        runBlocking {
            saveKeySalt(userId, keysSalt)
        }
    }

    suspend fun saveTempKeySalt(keysSalt: String?) {
        saveKeySalt(TEMP_USER_ID, keysSalt)
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("saveTempKeySalt(keySalt)")
    )
    @JvmOverloads
    fun saveTempKeySaltBlocking(keysSalt: String?) {
        runBlocking {
            saveKeySalt(TEMP_USER_ID, keysSalt)
        }
    }

    @Synchronized
    suspend fun getUser(userId: Id): NewUser =
        loadUser(userId).orThrow()

    @Deprecated("Suspended function should be used instead", ReplaceWith("getUser(userId)"))
    fun getUserBlocking(userId: Id): NewUser =
        runBlocking { getUser(userId) }

    /**
     * Note, returned [User] might have empty values if user was not saved before
     */
    @Synchronized
    suspend fun getLegacyUser(userId: Id): User =
        loadLegacyUser(userId).orThrow()

    @Synchronized
    suspend fun getLegacyUserOrNull(userId: Id): User? =
        loadLegacyUser(userId).orNull()

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
        val freeLoggedInUserCount = coreAccountManager.allLoggedIn().count {
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

    fun clearBackupPrefs() {
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

    fun preferencesFor(userId: Id) =
        secureSharedPreferencesFactory.userPreferences(userId)

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

    private companion object {

        val TEMP_USER_ID = Id("temp")
    }
}
