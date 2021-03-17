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
package ch.protonmail.android.activities.navigation

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.domain.entity.Id
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.DispatcherProvider

class NavigationViewModel @ViewModelInject constructor(
    private val dispatchers: DispatcherProvider,
    private val accountManager: AccountManager,
    private val databaseProvider: DatabaseProvider
) : ViewModel() {

    var notificationsCounterLiveData = MutableLiveData<Map<Id, Int>>()

    init {
        notificationsCounts()
    }

    fun reloadDependencies() {
        locationsListLiveData = databaseProvider.provideCounterDao().findAllUnreadLocations()
        unreadLabelsLiveData = databaseProvider.provideCounterDao().findAllUnreadLabels()
    }

    private fun labelsLiveData(): LiveData<List<Label>> =
        databaseProvider.provideMessageDao()
            .getAllLabels()
            .map { list -> list.sortedBy { it.order } }

    private var locationsListLiveData = databaseProvider.provideCounterDao().findAllUnreadLocations()
    private var unreadLabelsLiveData = databaseProvider.provideCounterDao().findAllUnreadLabels()

    fun labelsWithUnreadCounterLiveData(): MediatorLiveData<MutableList<LabelWithUnreadCounter>> =
        LabelsWithUnreadCounterLiveData(labelsLiveData(), unreadLabelsLiveData)

    fun locationsUnreadLiveData(): LiveData<Map<Int, Int>> =
        locationsListLiveData.map {
            it.map { locationCounter -> locationCounter.id to locationCounter.count }.toMap()
        }

    fun notificationsCounts() {
        viewModelScope.launch(dispatchers.Io) {
            val notificationsCounters = accountManager.allLoggedIn()
                .map { it to databaseProvider.provideNotificationsDao(it).count() }
                .toMap()

            notificationsCounterLiveData.postValue(notificationsCounters)
        }
    }
}
