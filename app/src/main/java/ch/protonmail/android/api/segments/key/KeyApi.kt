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
package ch.protonmail.android.api.segments.key

import androidx.annotation.WorkerThread
import ch.protonmail.android.api.models.KeysSetupBody
import ch.protonmail.android.api.models.PublicKeyResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.SinglePasswordChange
import ch.protonmail.android.api.models.UserInfo
import ch.protonmail.android.api.models.address.KeyActivationBody
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import java.io.IOException
import java.util.ArrayList
import java.util.Collections

class KeyApi (private val service : KeyService) : BaseApi(), KeyApiSpec {

    @Throws(IOException::class)
    override fun getPublicKeysBlocking(email: String): PublicKeyResponse {
        return ParseUtils.parse(service.getPublicKeysBlocking(email).execute())
    }

    override suspend fun getPublicKeys(email: String): PublicKeyResponse =
        service.getPublicKeys(email)

    @WorkerThread
    @Throws(Exception::class)
    override fun getPublicKeys(emails: Collection<String>): Map<String, PublicKeyResponse?> {
        if (emails.isEmpty()) {
            return Collections.emptyMap()
        }
        val service = service
        val list = ArrayList(emails)
        return executeAll(list.map { contactId -> service.getPublicKeysBlocking(contactId) })
                .mapIndexed { i, resp -> list[i] to resp }.toMap()
    }

    @Throws(Exception::class)
    override fun updatePrivateKeys(
        body: SinglePasswordChange
    ): ResponseBody = ParseUtils.parse(service.updatePrivateKeys(body).execute())

    @Throws(Exception::class)
    override fun activateKey(
        keyActivationBody: KeyActivationBody,
        keyId: String
    ): ResponseBody = ParseUtils.parse(service.activateKey(keyActivationBody, keyId).execute())

    @Throws(Exception::class)
    override suspend fun activateKeyLegacy(
        keyActivationBody: KeyActivationBody,
        keyId: String
    ): ResponseBody = service.activateKeyLegacy(keyActivationBody, keyId)

    @Throws(IOException::class)
    override fun setupKeys(
        keysSetupBody: KeysSetupBody
    ): UserInfo = ParseUtils.parse(service.setupKeys(keysSetupBody).execute())
}
