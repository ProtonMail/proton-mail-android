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

package ch.protonmail.android.usecase.crypto

import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.user.UserKey
import ch.protonmail.android.feature.user.getCurrentUserMailboxPassword
import ch.protonmail.android.utils.crypto.OpenPGP
import com.proton.gopenpgp.crypto.Crypto
import javax.inject.Inject

class GenerateTokenAndSignature @Inject constructor (
    private val userManager: UserManager,
    private val openPgp: OpenPGP
) {
    suspend operator fun invoke(orgKeys: UserKey?): TokenAndSignature {
        val user = userManager.getCurrentUserBlocking()
        val secret = openPgp.randomToken()
        val tokenString = secret.joinToString("") { String.format("%02x", (it.toInt() and 0xff)) }
        val binMessage = Crypto.newPlainMessageFromString(tokenString)
        val armoredPrivateKey: String? = user?.keys?.primaryKey?.privateKey?.string
        val mailboxPassword = userManager.getCurrentUserMailboxPassword()
        val unlockedUserKey = Crypto.newKeyFromArmored(armoredPrivateKey).unlock(mailboxPassword)
        val tokenKeyRing = Crypto.newKeyRing(unlockedUserKey)

        if (orgKeys != null) {
            val unlockedOrgKey = Crypto.newKeyFromArmored(orgKeys.privateKey.content.s).unlock(mailboxPassword)
            tokenKeyRing.addKey(unlockedOrgKey)
        }

        val token = tokenKeyRing.encrypt(binMessage, null).armored
        val signature = tokenKeyRing.signDetached(binMessage).armored
        tokenKeyRing.clearPrivateParams()
        return TokenAndSignature(token, signature)
    }
}

data class TokenAndSignature(val token: String, val signature: String)
