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

package ch.protonmail.android.domain.usecase.credentials

import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.repository.CredentialRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * Get / Observe all the saved credentials or for a single [EmailAddress]
 */
class GetSavedCredentials @Inject constructor (
    private val dispatchers: DispatcherProvider,
    private val repository: CredentialRepository
) {

    /**
     * Get all the credentials store
     */
    operator fun invoke(interval: Duration = DEFAULT_INTERVAL) = flow {
        while (true) {
            emit(repository.getAll())
            delay(interval)
        }
    }.flowOn(dispatchers.Io).distinctUntilChanged()

    /**
     * Get credentials stored for a single user
     */
    operator fun invoke(emailAddress: EmailAddress, interval: Duration = DEFAULT_INTERVAL) = flow {
        while (true) {
            emit(repository[emailAddress])
            delay(interval)
        }
    }.flowOn(dispatchers.Io).distinctUntilChanged()

    private companion object {
        val DEFAULT_INTERVAL = 30.seconds
    }
}
