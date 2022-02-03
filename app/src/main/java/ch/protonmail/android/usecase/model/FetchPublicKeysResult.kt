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

package ch.protonmail.android.usecase.model

import ch.protonmail.android.core.Constants

sealed interface FetchPublicKeysResult {

    data class Success(
        val email: String,
        val key: String,
        val recipientsType: Constants.RecipientLocationType,
        val isSendRetryRequired: Boolean = false
    ) : FetchPublicKeysResult

    data class Failure(
        val email: String,
        val recipientsType: Constants.RecipientLocationType,
        val error: Error
    ) : FetchPublicKeysResult {

        sealed interface Error {

            object Generic : Error

            @JvmInline
            value class WithMessage(val message: String) : Error
        }
    }
}
