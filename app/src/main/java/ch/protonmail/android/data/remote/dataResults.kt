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

package ch.protonmail.android.data.remote

import me.proton.core.domain.arch.DataResult

const val NO_MORE_ITEMS_EXCEPTION_MESSAGE = "No more items!"
const val OFFLINE_EXCEPTION_MESSAGE = "You're offline!"

/**
 * Represent a particular case of [DataResult.Error.Remote] for when remote returns an empty list of items
 */
val NoMoreItemsDataResult = DataResult.Error.Remote(
    NO_MORE_ITEMS_EXCEPTION_MESSAGE,
    NoMoreItemsException()
)

/**
 * Represent a particular case of [DataResult.Error.Remote] for when the user is offline
 */
val OfflineDataResult = DataResult.Error.Remote(
    OFFLINE_EXCEPTION_MESSAGE,
    OfflineException()
)

class NoMoreItemsException : IllegalStateException(NO_MORE_ITEMS_EXCEPTION_MESSAGE)
class OfflineException : IllegalStateException(OFFLINE_EXCEPTION_MESSAGE)
