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

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.WorkInfo
import ch.protonmail.android.utils.extensions.filter
import ch.protonmail.android.worker.FetchContactsDataWorker
import ch.protonmail.android.worker.FetchContactsEmailsWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FetchContactsData @Inject constructor(
    private val fetchContactsDataWorker: FetchContactsDataWorker.Enqueuer,
    private val fetchContactsEmailsWorker: FetchContactsEmailsWorker.Enqueuer
) {

    operator fun invoke(): LiveData<Boolean> {
        fetchContactsEmailsWorker.enqueue(TimeUnit.SECONDS.toMillis(2))
            .filter { it?.state?.isFinished == true }
            .map { workInfo ->
                Timber.v("Finished contacts emails worker State ${workInfo.state}")
            }

        // we return just contact data status for now
        return fetchContactsDataWorker.enqueue()
            .filter { it?.state?.isFinished == true }
            .map { workInfo ->
                Timber.v("Finished contacts data worker State ${workInfo.state}")
                workInfo.state == WorkInfo.State.SUCCEEDED
            }
    }
}
