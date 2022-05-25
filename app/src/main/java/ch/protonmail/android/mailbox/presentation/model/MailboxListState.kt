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

package ch.protonmail.android.mailbox.presentation.model

import me.proton.core.util.kotlin.EMPTY_STRING

sealed class MailboxListState {

    object Loading : MailboxListState()

    data class Error(
        val error: String = EMPTY_STRING,
        val throwable: Throwable? = null,
        val isOffline: Boolean = false
    ) : MailboxListState()

    /**
     * @property shouldResetPosition if `true` the list should be scrolled to its top, for example if the location has
     *  changed
     *
     * @property isFreshData if `true` we already refreshed from remote, otherwise we're relying on cache
     */
    data class Data(
        val items: List<MailboxItemUiModel>,
        val isFreshData: Boolean,
        val shouldResetPosition: Boolean = false
    ) : MailboxListState()

    data class DataRefresh(val lastFetchedItemsIds: List<String>) : MailboxListState()
}
