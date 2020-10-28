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

package ch.protonmail.android.usecase.fetch

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.usecase.model.EmailKeysRequest
import ch.protonmail.android.usecase.model.EmailKeysResult
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class FetchPublicKeys @Inject constructor(
    private val api: ProtonMailApiManager,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(
        requests: List<EmailKeysRequest>
    ): List<EmailKeysResult> =
        withContext(dispatchers.Io) {
            val result = mutableListOf<EmailKeysResult>()
            for (request in requests) {
                val publicKeys = getPublicKeys(request.emails.toSet())
                result.add(EmailKeysResult(publicKeys, request.recipientsType))
            }
            result
        }

    private suspend fun getPublicKeys(emailSet: Set<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (email in emailSet) {
            result[email] = ""
            val publicKey = api.getPublicKeys(email)
            for (key in publicKey.keys) {
                if (key.isAllowedForSending) {
                    result[email] = key.publicKey
                }
            }
        }
        result.remove(CODE_KEY)
        return result
    }

    companion object {
        private const val CODE_KEY = "Code"
    }

}
