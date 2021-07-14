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

package ch.protonmail.android.feature.account

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.PREF_CURRENT_USER_ID
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.util.orThrow
import ch.protonmail.android.domain.util.requireNotBlank
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.libs.core.preferences.getString
import kotlinx.coroutines.runBlocking
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountDetails
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.account.domain.entity.SessionState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.keystore.encryptWith
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.entity.key.KeyId
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.network.domain.session.Session
import me.proton.core.network.domain.session.SessionId
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.user.domain.entity.AddressType
import me.proton.core.user.domain.entity.Delinquent
import me.proton.core.user.domain.entity.Role
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.entity.UserAddressKey
import me.proton.core.user.domain.entity.UserKey
import me.proton.core.util.kotlin.toBooleanOrFalse
import timber.log.Timber
import javax.inject.Inject

class CoreAccountManagerMigration @Inject constructor(
    private val context: Context,
    private val keyStoreCrypto: KeyStoreCrypto,
    private val accountManager: AccountManager,
    private val userManager: UserManager,
    private val oldAccountManager: ch.protonmail.android.api.AccountManager,
    @DefaultSharedPreferences private val appPrefs: SharedPreferences
) {

    data class Migration(
        val account: Account,
        val session: Session,
        val passphrase: String
    )

    fun migrateBlocking() = runBlocking {
        val loggedInUserIds = oldAccountManager.getLoggedIn().map { Id(it) }
        if (loggedInUserIds.isNotEmpty()) {
            val currentUserId = appPrefs.getString(PREF_CURRENT_USER_ID)?.let { Id(it) }
            migrateLoggedInAccounts(loggedInUserIds, currentUserId)
            oldAccountManager.clear()
        }
    }

    private suspend fun migrateLoggedInAccounts(userIds: List<Id>, currentUserId: Id?) {
        // Iterate on currentUserId at the end.
        userIds.sortedBy { it == currentUserId }.forEach { id ->
            val tokenManager = TokenManager.getInstance(context, id)
            val userPrefs = SecureSharedPreferences.getPrefsForUser(context, id)
            val username = userPrefs.getString(Constants.Prefs.PREF_USER_NAME)
            val passphrase = userPrefs.getString(PREF_MAILBOX_PASSWORD)
            runCatching {
                val session = Session(
                    sessionId = SessionId(requireNotBlank(tokenManager.sessionId)),
                    accessToken = requireNotBlank(tokenManager.accessToken),
                    refreshToken = requireNotBlank(tokenManager.refreshToken),
                    scopes = tokenManager.scope.split(" ")
                )
                val account = Account(
                    userId = UserId(id.s),
                    username = requireNotBlank(username),
                    email = null,
                    state = AccountState.Ready,
                    sessionId = session.sessionId,
                    sessionState = SessionState.Authenticated,
                    details = AccountDetails(
                        session = null,
                    )
                )
                Migration(account, session, requireNotBlank(passphrase))
            }.fold(
                onFailure = { Timber.w(it) },
                onSuccess = { migration ->
                    // Add account to core AccountManager.
                    accountManager.addAccount(account = migration.account, session = migration.session)
                    // Encrypt the passphrase.
                    val encryptedPassphrase = PlainByteArray(migration.passphrase.toByteArray()).use {
                        it.encryptWith(keyStoreCrypto)
                    }
                    runCatching {
                        // Migrate User/Addresses/Keys to Core.
                        val userId = migration.account.userId
                        val user = User.load(Id(userId.id), context, userManager, keyStoreCrypto).orThrow()
                        val addresses = user.addresses
                        userManager.addUser(
                            user = user.toCoreUser(encryptedPassphrase),
                            addresses = addresses.map { it.toCoreUserAddress(userId, encryptedPassphrase) }
                        )
                        // Unlock Account/User with provided passphrase.
                        userManager.unlockWithPassphrase(migration.account.userId, encryptedPassphrase)
                    }.onFailure {
                        // Nothing else to do than disabling this account.
                        accountManager.disableAccount(migration.account.userId)
                    }
                }
            )
            // Clear migrated data.
            tokenManager.clear()
            userPrefs.edit { remove(PREF_MAILBOX_PASSWORD) }
        }
    }

    companion object {

        private const val PREF_MAILBOX_PASSWORD = "mailbox_password"
    }
}

private fun User.toCoreUser(passphrase: EncryptedByteArray?): me.proton.core.user.domain.entity.User {
    val userId = UserId(id)
    return me.proton.core.user.domain.entity.User(
        userId = userId,
        email = defaultAddressEmail,
        name = name,
        displayName = displayName,
        currency = currency,
        credit = credit,
        usedSpace = usedSpace,
        maxSpace = maxSpace,
        maxUpload = maxUpload.toLong(),
        role = Role.map[role],
        private = private.toBooleanOrFalse(),
        subscribed = subscribed,
        services = services,
        delinquent = Delinquent.map[delinquentValue],
        keys = keys.map { it.toCoreUserKey(userId, passphrase) },
    )
}

private fun Keys.toCoreUserKey(userId: UserId, passphrase: EncryptedByteArray?) = UserKey(
    userId = userId,
    version = 0,
    activation = activation,
    keyId = KeyId(id),
    privateKey = PrivateKey(key = privateKey, isPrimary = isPrimary, passphrase = passphrase)
)

private fun Address.toCoreUserAddress(userId: UserId, passphrase: EncryptedByteArray?) = UserAddress(
    userId = userId,
    addressId = AddressId(id),
    email = email,
    displayName = displayName,
    signature = signature,
    domainId = domainId,
    canSend = send.toBooleanOrFalse(),
    canReceive = receive.toBooleanOrFalse(),
    enabled = status.toBooleanOrFalse(),
    type = AddressType.map[type],
    order = order,
    keys = keys.map { it.toCoreUserAddressKey(AddressId(id), passphrase) }
)

private fun Keys.toCoreUserAddressKey(addressId: AddressId, passphrase: EncryptedByteArray?) = UserAddressKey(
    addressId = addressId,
    version = 0,
    flags = flags,
    token = token,
    signature = signature,
    activation = activation,
    active = active.toBooleanOrFalse(),
    keyId = KeyId(id),
    privateKey = PrivateKey(key = privateKey, isPrimary = isPrimary, passphrase = passphrase)
)
