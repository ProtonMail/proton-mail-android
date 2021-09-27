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

import ch.protonmail.android.api.services.MessagesService
import ch.protonmail.android.core.Constants
import ch.protonmail.android.worker.FetchContactsDataWorker
import ch.protonmail.android.worker.FetchContactsEmailsWorker
import ch.protonmail.android.worker.FetchMailSettingsWorker
import timber.log.Timber
import javax.inject.Inject

private const val FETCH_CONTACT_DELAY_MS: Long = 2000

/**
 * Launches fetching of initial data required after the first login
 */
class LaunchInitialDataFetch @Inject constructor(
    private val fetchContactsDataWorker: FetchContactsDataWorker.Enqueuer,
    private val fetchContactsEmailsWorker: FetchContactsEmailsWorker.Enqueuer,
    private val fetchMailSettingsWorker: FetchMailSettingsWorker.Enqueuer
) {

    operator fun invoke(
        shouldRefreshDetails: Boolean = true,
        shouldRefreshContacts: Boolean = true
    ) {
        Timber.v("LaunchInitialDataFetch started")
        MessagesService.startFetchLabels()
        MessagesService.startFetchFirstPage(Constants.MessageLocationType.INBOX, shouldRefreshDetails, null, false)
        fetchMailSettingsWorker.enqueue()

        if (shouldRefreshContacts) {
            fetchContactsDataWorker.enqueue()
            fetchContactsEmailsWorker.enqueue(FETCH_CONTACT_DELAY_MS)
        }
    }
}
