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
import ch.protonmail.android.notifications.data.remote.fcm.FcmTokenManager
import ch.protonmail.android.notifications.data.remote.fcm.UnregisterDeviceWorker
import ch.protonmail.android.usecase.delete.ClearUserData
import ch.protonmail.android.usecase.fetch.LaunchInitialDataFetch
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.JobManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.account.domain.entity.isDisabled
import me.proton.core.account.domain.entity.isReady
import me.proton.core.account.domain.entity.isStepNeeded
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getAccounts
import me.proton.core.accountmanager.presentation.AccountManagerObserver
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountDisabled
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionForceLogout
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.accountmanager.presentation.onUserAddressKeyCheckFailed
import me.proton.core.accountmanager.presentation.onUserKeyCheckFailed
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.onAddAccountResult
import me.proton.core.domain.entity.Product
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserManager
import me.proton.core.util.android.sharedpreferences.clearAll
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AccountStateManager @Inject constructor(
    private val product: Product,
    private val requiredAccountType: AccountType,
    private val accountManager: AccountManager,
    private val userManager: UserManager,
    private val eventManager: EventManager,
    private val jobManager: JobManager,
    private val authOrchestrator: AuthOrchestrator,
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

    private val mutableStateFlow = MutableStateFlow(State.Processing)

    enum class State { Processing, AccountNeeded, PrimaryExist }

    val state = mutableStateFlow.asStateFlow()

    init {
        // Handle basic account state with lifecycle provided via constructor.
        observeAccountStateWithInternalLifecycle()

        // Change mutable state according all accounts states.
        accountManager.getAccounts()
            .flowWithLifecycle(lifecycle, minActiveState = Lifecycle.State.CREATED)
            .onEach { accounts ->
                when {
                    accounts.isEmpty() || accounts.all { it.isDisabled() } -> {
                        onAccountNeeded()
                        mutableStateFlow.tryEmit(State.AccountNeeded)
                    }
                    accounts.any { it.isReady() } -> mutableStateFlow.tryEmit(State.PrimaryExist)
                    accounts.any { it.isStepNeeded() } -> mutableStateFlow.tryEmit(State.Processing)
                }
            }.launchIn(scope)
    }

    private suspend fun getAccountOrNull(userId: UserId) = getAccount(userId).firstOrNull()

    private fun observeAccountManager(lifecycle: Lifecycle): AccountManagerObserver =
        accountManager.observe(lifecycle, Lifecycle.State.CREATED)

    /**
     * Observe all accounts states that can be solved without any workflow ([AuthOrchestrator]).
     */
    private fun observeAccountStateWithInternalLifecycle() {
        observeAccountManager(lifecycle)
            .onSessionForceLogout { userManager.lock(it.userId) }
            .onAccountTwoPassModeFailed { accountManager.disableAccount(it.userId) }
            .onAccountCreateAddressFailed { accountManager.disableAccount(it.userId) }
            .onAccountRemoved { onAccountDisabled(it) }
            .onAccountDisabled { onAccountDisabled(it) }
            .onAccountReady { onAccountReady(it) }
            .onUserKeyCheckFailed {
                Timber.e("Account has invalid user key (user id = ${it.userId.id})")
            }
            .onUserAddressKeyCheckFailed {
                Timber.e("Account has invalid address key (user id = ${it.userId.id}")
            }
    }

    /**
     * Observe all accounts states that can be solved with [AuthOrchestrator], starting corresponding workflow.
     *
     * For example, SecondFactor Workflow, TwoPassMode Workflow or ChooseAddress Workflow.
     */
    private fun observeAccountStateWithExternalLifecycle(lifecycle: Lifecycle) {
        observeAccountManager(lifecycle)
            .onSessionSecondFactorNeeded { authOrchestrator.startSecondFactorWorkflow(it) }
            .onAccountTwoPassModeNeeded { authOrchestrator.startTwoPassModeWorkflow(it) }
            .onAccountCreateAddressNeeded { authOrchestrator.startChooseAddressWorkflow(it) }
    }

    fun register(context: ComponentActivity) {
        authOrchestrator.register(context)
        observeAccountStateWithExternalLifecycle(context.lifecycle)
    }

    fun onAddAccountClosed(block: () -> Unit) {
        authOrchestrator.onAddAccountResult { result -> if (result == null) block() }
    }

    fun getAccount(userId: UserId) = accountManager.getAccount(userId)

    fun signOut(userId: UserId) = scope.launch {
        accountManager.disableAccount(userId)
    }

    fun signOutAll() = scope.launch {
        accountManager.getAccounts(AccountState.Ready)
            .first()
            .forEach { accountManager.disableAccount(it.userId) }
    }

    fun signOutPrimary() = scope.launch {
        getPrimaryUserId().first()?.let { signOut(it) }
    }

    fun signIn(userId: UserId? = null) = scope.launch {
        val account = userId?.let { getAccountOrNull(it) }
        authOrchestrator.startLoginWorkflow(
            requiredAccountType = requiredAccountType,
            username = account?.username
        )
    }

    fun addAccount() = scope.launch {
        authOrchestrator.startAddAccountWorkflow(
            requiredAccountType = requiredAccountType,
            creatableAccountType = requiredAccountType,
            product = product
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
            else -> Unit
        }
    }

    fun remove(userId: UserId) = scope.launch {
        accountManager.removeAccount(userId)
    }

    fun onAccountSwitched() = getPrimaryUserId().scan(AccountSwitch()) { previous, currentUserId ->
        AccountSwitch(
            previous = previous.current?.userId?.let { getAccountOrNull(it) },
            current = currentUserId?.let { getAccountOrNull(it) }
        )
    }.filter { it.previous != null && it.current != it.previous }

    // region Primary User Id

    // Currently, Old UserManager is the source of truth for current primary user id.
    fun getPrimaryUserId() = oldUserManager.primaryUserId

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
        val prefs = oldUserManager.preferencesFor(account.userId)
        val initialized = prefs.getBoolean(Constants.Prefs.PREF_USER_INITIALIZED, false)
        if (!initialized) {
            prefs.edit { putBoolean(Constants.Prefs.PREF_USER_INITIALIZED, true) }
            // Launch Initial Data Fetch.
            launchInitialDataFetch.invoke(
                userId = account.userId,
                shouldRefreshDetails = true,
                shouldRefreshContacts = true
            )
            // FCM Register (see MailboxActivity.checkRegistration).
            fcmTokenManagerFactory.create(prefs).setTokenSent(false)
        }
    }

    private suspend fun onAccountDisabled(account: Account) = withContext(NonCancellable + dispatchers.Io) {
        // Only clear user once.
        val prefs = oldUserManager.preferencesFor(account.userId)
        val initialized = prefs.getBoolean(Constants.Prefs.PREF_USER_INITIALIZED, false)
        if (initialized) {
            // FCM Unregister.
            unregisterDeviceWorkerEnqueuer(account.userId, account.sessionId)
            // Clear Data/State.
            clearUserData.invoke(account.userId)
            eventManager.clearState(account.userId)
            prefs.clearAll(/*excludedKeys*/ PREF_PIN, Constants.Prefs.PREF_USER_NAME)
        }
    }

    // endregion

    data class AccountSwitch(val previous: Account? = null, val current: Account? = null)
}
