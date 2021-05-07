/*
 * Copyright (c) 2020 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.feature.account

import androidx.activity.ComponentActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.api.segments.event.EventManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.PREF_PIN
import ch.protonmail.android.di.AppProcessLifecycleOwner
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.fcm.FcmTokenManager
import ch.protonmail.android.fcm.UnregisterDeviceWorker
import ch.protonmail.android.feature.user.waitPrimaryKeyPassphraseAvailable
import ch.protonmail.android.usecase.delete.ClearUserData
import ch.protonmail.android.usecase.fetch.LaunchInitialDataFetch
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.libs.core.preferences.clearAll
import com.birbit.android.jobqueue.JobManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.account.domain.entity.isDisabled
import me.proton.core.account.domain.entity.isReady
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.disableInitialNotReadyAccounts
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountDisabled
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionHumanVerificationNeeded
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.onLoginResult
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserManager
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountStateManager @Inject constructor(
    private val accountManager: AccountManager,
    private val userManager: UserManager,
    private var authOrchestrator: AuthOrchestrator,
    private val eventManager: EventManager,
    private val jobManager: JobManager,
    private val oldUserManager: ch.protonmail.android.core.UserManager,
    private val launchInitialDataFetch: LaunchInitialDataFetch,
    private val clearUserData: ClearUserData,
    private var fcmTokenManagerFactory: FcmTokenManager.Factory,
    private val unregisterDeviceWorkerEnqueuer: UnregisterDeviceWorker.Enqueuer,
    @AppProcessLifecycleOwner
    private val lifecycleOwner: LifecycleOwner,
    private val dispatchers: DispatcherProvider
) {

    private val scope = lifecycleOwner.lifecycleScope
    private val lifecycle = lifecycleOwner.lifecycle

    private val _state = MutableStateFlow(State.Processing as State)

    sealed class State {
        object Processing : State()
        object AccountNeeded : State()
        object PrimaryExist : State()
    }

    val state = _state.asStateFlow()

    init {
        with(authOrchestrator) {
            // Observe all Accounts States (need a registered authOrchestrator, see register).
            accountManager.observe(lifecycle, minActiveState = Lifecycle.State.CREATED)
                .onSessionHumanVerificationNeeded { startHumanVerificationWorkflow(it) }
                .onSessionSecondFactorNeeded { startSecondFactorWorkflow(it) }
                .onAccountTwoPassModeNeeded { startTwoPassModeWorkflow(it) }
                .onAccountCreateAddressNeeded { startChooseAddressWorkflow(it) }
                .onAccountTwoPassModeFailed { accountManager.disableAccount(it.userId) }
                .onAccountCreateAddressFailed { accountManager.disableAccount(it.userId) }
                .onAccountRemoved { onAccountDisabled(it) }
                .onAccountDisabled { onAccountDisabled(it) }
                .onAccountReady { onAccountReady(it) }
                .disableInitialNotReadyAccounts()
        }

        // Raise AccountNeeded on empty/disabled account list.
        accountManager.getAccounts()
            .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
            .onEach { accounts ->
                if (accounts.isEmpty() || accounts.all { it.isDisabled() }) {
                    onAccountNeeded()
                    _state.tryEmit(State.AccountNeeded)
                }
            }.launchIn(scope)
    }

    fun register(context: ComponentActivity) {
        authOrchestrator.register(context)
    }

    fun onLoginClosed(block: () -> Unit) {
        authOrchestrator.onLoginResult { result -> if (result == null) block() }
    }

    fun getAccount(userId: UserId) = accountManager.getAccount(userId)

    suspend fun getAccountOrNull(userId: UserId) = getAccount(userId).firstOrNull()

    /* Order: Primary account, ready account(s), other account(s). */
    fun getSortedAccounts() = accountManager.getAccounts().mapLatest { accounts ->
        val currentUser = getPrimaryUserId().firstOrNull()
        accounts
            .sortedByDescending { it.userId == currentUser }
            .sortedByDescending { it.isReady() }
    }

    fun logout(userId: UserId) = scope.launch {
        userManager.lock(userId)
        accountManager.disableAccount(userId)
    }

    fun logoutPrimary() = scope.launch {
        getPrimaryUserId().first()?.let { logout(it) }
    }

    fun login(userId: UserId? = null) = scope.launch {
        val account = userId?.let { getAccountOrNull(it) }
        authOrchestrator.startLoginWorkflow(
            requiredAccountType = AccountType.Internal,
            username = account?.username
        )
    }

    fun switch(userId: UserId) = scope.launch {
        val account = getAccountOrNull(userId) ?: return@launch
        when {
            account.isDisabled() -> authOrchestrator.startLoginWorkflow(
                requiredAccountType = AccountType.Internal,
                username = account.username
            )
            account.isReady() -> accountManager.setAsPrimary(userId)
        }
    }

    fun remove(userId: UserId) = scope.launch {
        accountManager.removeAccount(userId)
    }

    fun removeAll() = scope.launch {
        accountManager.getAccounts().first().forEach {
            accountManager.removeAccount(it.userId)
        }
    }

    data class AccountSwitch(val previous: Account? = null, val current: Account? = null)

    fun onAccountSwitched() = getPrimaryUserId().scan(AccountSwitch()) { previous, currentUserId ->
        AccountSwitch(
            previous = previous.current?.userId?.let { getAccountOrNull(it) },
            current = currentUserId?.let { getAccountOrNull(it) }
        )
    }.filter { it.previous != null && it.current != it.previous }

    // region Primary User Id

    // Currently, Old UserManager is the source of truth for current primary user id.
    fun getPrimaryUserId() = oldUserManager.primaryUserId

    fun getPrimaryUserIdValue() = oldUserManager.primaryUserId.value

    // endregion

    // region Deprecated

    @Deprecated(
        message = "Use UserId version of the function",
        replaceWith = ReplaceWith("getAccountOrNull(UserId(userId.s))", "me.proton.core.domain.entity.UserId")
    )
    suspend fun getAccountOrNull(userId: Id) = getAccountOrNull(UserId(userId.s))

    @Deprecated(
        message = "Use UserId version of the function",
        replaceWith = ReplaceWith("switch(UserId(userId.s))", "me.proton.core.domain.entity.UserId")
    )
    fun switch(userId: Id) = switch(UserId(userId.s))

    @Deprecated(
        message = "Use UserId version of the function",
        replaceWith = ReplaceWith("logout(UserId(userId.s))", "me.proton.core.domain.entity.UserId")
    )
    fun logout(userId: Id) = logout(UserId(userId.s))

    @Deprecated(
        message = "Use UserId version of the function",
        replaceWith = ReplaceWith("remove(UserId(userId.s))", "me.proton.core.domain.entity.UserId")
    )
    fun remove(userId: Id) = remove(UserId(userId.s))

    // endregion

    // region Legacy User Management & Cleaning.

    private suspend fun onAccountNeeded() = withContext(NonCancellable + dispatchers.Io) {
        eventManager.clearState()
        AppUtil.clearTasks(jobManager)
        AppUtil.deletePrefs()
        AppUtil.deleteBackupPrefs()
    }

    private suspend fun onAccountReady(account: Account) = withContext(NonCancellable + dispatchers.Io) {
        // Only initialize user once.
        val userId = Id(account.userId.id)
        val prefs = oldUserManager.preferencesFor(userId)
        val initialized = prefs.getBoolean(Constants.Prefs.PREF_USER_INITIALIZED, false)
        if (!initialized) {
            oldUserManager.preferencesFor(userId).edit {
                putBoolean(Constants.Prefs.PREF_USER_INITIALIZED, true)
                // See DatabaseFactory.usernameForUserId.
                putString(Constants.Prefs.PREF_USER_NAME, account.username)
            }
            // Workaround: Wait the primary key passphrase before proceeding.
            userManager.waitPrimaryKeyPassphraseAvailable(account.userId)
            // Workaround: Make sure this uninitialized User is fresh.
            oldUserManager.clearCache()
            // Launch Initial Data Fetch.
            launchInitialDataFetch.invoke(
                userId = userId,
                shouldRefreshDetails = true,
                shouldRefreshContacts = true
            )
            // FCM Register (see MailboxActivity.checkRegistration).
            fcmTokenManagerFactory.create(prefs).setTokenSent(false)
        }
        // We have a primary account.
        _state.tryEmit(State.PrimaryExist)
    }

    private suspend fun onAccountDisabled(account: Account) = withContext(NonCancellable + dispatchers.Io) {
        // Only clear user once.
        val userId = Id(account.userId.id)
        val prefs = oldUserManager.preferencesFor(userId)
        val initialized = prefs.getBoolean(Constants.Prefs.PREF_USER_INITIALIZED, false)
        if (initialized) {
            // FCM Unregister.
            unregisterDeviceWorkerEnqueuer(account.userId, account.sessionId)
            // Clear Data/State.
            clearUserData.invoke(userId)
            eventManager.clearState(userId)
            oldUserManager.preferencesFor(Id(account.userId.id)).clearAll(
                /*excludedKeys*/ PREF_PIN, Constants.Prefs.PREF_USER_NAME
            )
        }
    }

    // endregion
}
