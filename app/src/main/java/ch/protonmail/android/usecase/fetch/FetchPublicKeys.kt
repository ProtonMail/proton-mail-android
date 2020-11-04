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
import ch.protonmail.android.usecase.model.FetchPublicKeysRequest
import ch.protonmail.android.usecase.model.FetchPublicKeysResult
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

private const val CODE_KEY = "Code"

class FetchPublicKeys @Inject constructor(
    private val api: ProtonMailApiManager,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(
        requests: List<FetchPublicKeysRequest>
    ): List<FetchPublicKeysResult> = withContext(dispatchers.Io) {
        val result = mutableListOf<FetchPublicKeysResult>()
        for (request in requests) {
            val publicKeys = getPublicKeys(request.emails.toSet())
            if (publicKeys.isNotEmpty()) {
                result.add(FetchPublicKeysResult(publicKeys, request.recipientsType))
            }
        }
        result
    }

    private suspend fun getPublicKeys(emailSet: Set<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (email in emailSet) {
            runCatching {
                api.getPublicKeys(email)
            }
                .fold(
                    onSuccess = {
                        result[email] = ""
                        for (key in it.keys) {
                            if (key.isAllowedForSending) {
                                result[email] = key.publicKey
                            }
                        }
                    },
                    onFailure = { Timber.w(it, "Unable to fetch public keys") }
                )
        }
        result.remove(CODE_KEY)
        return result
    }
}
