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

import ch.protonmail.android.domain.entity.Credential
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.repository.CredentialRepository
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

/**
 * Update stored credentials or create a new one
 */
class UpdateCredentials @Inject constructor (
    private val dispatchers: DispatcherProvider,
    private val repository: CredentialRepository
) {

    suspend operator fun invoke(
        emailAddress: EmailAddress,
        credential: Credential
    ) = withContext(dispatchers.Io) {
        repository[emailAddress] = credential
    }
}
