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

package ch.protonmail.android.worker

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.CONTACTS_PAGE_SIZE
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.events.ContactsFetchedEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * Work Manager Worker responsible for fetching contacts.
 *
 *  InputData has to contain non-null values for:
 *  labelId
 *
 * @see androidx.work.WorkManager
 */
class FetchContactsDataWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ProtonMailApiManager,
    private val contactsDao: ContactsDao,
    @DefaultSharedPreferences private val prefs: SharedPreferences,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        withContext(dispatchers.Io) {
            Timber.v("Fetch Contacts Worker started thread:${Thread.currentThread().name}")
            var page = 0
            var response = api.fetchContacts(page, CONTACTS_PAGE_SIZE)
            var status = Status.FAILED
            response.contacts?.let { contacts ->
                val total = response.total
                var fetched = contacts.size
                while (total > fetched) {
                    ++page
                    response = api.fetchContacts(page, CONTACTS_PAGE_SIZE)
                    val contactDataList = response.contacts
                    if (contactDataList.isNullOrEmpty()) {
                        break
                    }
                    contacts.addAll(contactDataList)
                    fetched = contacts.size
                }
                status = try {
                    contactsDao.saveAllContactsData(contacts)
                    Status.SUCCESS
                } finally {
                    prefs.edit().putBoolean(Constants.Prefs.PREF_CONTACTS_LOADING, false).apply()
                }
            }
            AppUtil.postEventOnUi(ContactsFetchedEvent(status))

            Result.success()
        }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {
        fun enqueue(): Operation {
            val networkConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val networkWorkRequest = OneTimeWorkRequestBuilder<FetchContactsDataWorker>()
                .setConstraints(networkConstraints)
                .build()
            Timber.v("Scheduling Fetch Contacts Worker")
            return workManager.enqueue(networkWorkRequest)
        }
    }
}
