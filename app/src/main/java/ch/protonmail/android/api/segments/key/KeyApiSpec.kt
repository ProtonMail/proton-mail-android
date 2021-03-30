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
import ch.protonmail.android.api.models.PublicKeyResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.address.KeyActivationBody
import java.io.IOException

interface KeyApiSpec {

    @Throws(IOException::class)
    fun getPublicKeysBlocking(email: String): PublicKeyResponse

    suspend fun getPublicKeys(email: String): PublicKeyResponse

    @WorkerThread
    @Throws(Exception::class)
    fun getPublicKeys(emails: Collection<String>): Map<String, PublicKeyResponse?>

    @Throws(Exception::class)
    fun activateKey(keyActivationBody: KeyActivationBody, keyId: String): ResponseBody

    @Throws(Exception::class)
    suspend fun activateKeyLegacy(keyActivationBody: KeyActivationBody, keyId: String): ResponseBody
}
