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
import ch.protonmail.android.core.Constants.RecipientLocationType
import ch.protonmail.android.usecase.model.FetchPublicKeysRequest
import ch.protonmail.android.usecase.model.FetchPublicKeysResult
import ch.protonmail.android.usecase.model.FetchPublicKeysResult.Failure
import ch.protonmail.android.usecase.model.FetchPublicKeysResult.Failure.Error
import ch.protonmail.android.usecase.model.FetchPublicKeysResult.Success
import ch.protonmail.android.utils.extensions.toPmResponseBodyOrNull
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.mapNotNullAsync
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
        requests.mapNotNullAsync { request ->
            getPublicKeys(request.emails.toSet(), request.recipientsType)
        }.flatten()
    }

    private suspend fun getPublicKeys(
        emailSet: Set<String>,
        location: RecipientLocationType
    ): List<FetchPublicKeysResult> {
        return emailSet.filterNot { it == CODE_KEY }.mapNotNullAsync { email ->
            runCatching { api.getPublicKeys(email) }
                .fold(
                    onSuccess = { response ->
                        val key = response.keys.find { it.isAllowedForSending }?.publicKey ?: EMPTY_STRING
                        Success(email, key, location)
                    },
                    onFailure = { throwable ->
                        Timber.w(throwable, "Unable to fetch public keys")
                        throwable.toPmResponseBodyOrNull()
                            ?.let { Failure(email, location, Error.WithMessage(it.error)) }
                            ?: Failure(email, location, Error.Generic)
                    }
                )
        }
    }
}
