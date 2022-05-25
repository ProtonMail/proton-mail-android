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

package ch.protonmail.android.usecase.fetch

import android.content.Context
import ch.protonmail.android.api.services.MessagesService
import ch.protonmail.android.core.Constants
import ch.protonmail.android.worker.FetchContactsDataWorker
import ch.protonmail.android.worker.FetchContactsEmailsWorker
import ch.protonmail.android.worker.FetchMailSettingsWorker
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

private const val FETCH_CONTACT_DELAY_MS: Long = 2000

/**
 * Launches fetching of initial data required after the first login
 */
class LaunchInitialDataFetch @Inject constructor(
    private val context: Context,
    private val fetchContactsDataWorker: FetchContactsDataWorker.Enqueuer,
    private val fetchContactsEmailsWorker: FetchContactsEmailsWorker.Enqueuer,
    private val fetchMailSettingsWorker: FetchMailSettingsWorker.Enqueuer,
) {

    operator fun invoke(
        userId: UserId,
        shouldRefreshDetails: Boolean = true,
        shouldRefreshContacts: Boolean = true
    ) {
        Timber.v("LaunchInitialDataFetch started")
        MessagesService.startFetchFirstPage(
            context,
            userId,
            Constants.MessageLocationType.INBOX,
            shouldRefreshDetails,
            null,
            false
        )
        fetchMailSettingsWorker.enqueue()

        if (shouldRefreshContacts) {
            fetchContactsDataWorker.enqueue()
            fetchContactsEmailsWorker.enqueue(FETCH_CONTACT_DELAY_MS)
        }
    }
}
