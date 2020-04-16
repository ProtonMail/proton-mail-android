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

import androidx.lifecycle.*
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.core.ProtonMailApplication
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Kamil Rajtar on 20.08.18.  */
class NavigationViewModel(
        private val databaseProvider: DatabaseProvider
) : ViewModel() {

    var notificationsCounterLiveData = MutableLiveData<Map<String, Int>>()

    init {
        notificationsCounts()
    }

    fun reloadDependencies() {
        locationsListLiveData = databaseProvider.provideCountersDao().findAllUnreadLocations()
        unreadLabelsLiveData = databaseProvider.provideCountersDao().findAllUnreadLabels()
    }

    fun selector(label: Label): Int = label.order

    private fun labelsLiveData() : LiveData<List<Label>> {
        return Transformations.map(databaseProvider.provideMessagesDao().getAllLabels()) { list -> list.sortedBy { selector(it) } }
    }

    private var locationsListLiveData = databaseProvider.provideCountersDao().findAllUnreadLocations()
    private var unreadLabelsLiveData = databaseProvider.provideCountersDao().findAllUnreadLabels()

    fun labelsWithUnreadCounterLiveData() : MediatorLiveData<MutableList<LabelWithUnreadCounter>> {
        return LabelsWithUnreadCounterLiveData(labelsLiveData(), unreadLabelsLiveData)
    }

    fun locationsUnreadLiveData() = Transformations.map(locationsListLiveData) {
        it.map { locationCounter -> locationCounter.id to locationCounter.count }.toMap()
    }

    fun notificationsCounts() {
        val loggedInUserNames = AccountManager.getInstance(ProtonMailApplication.getApplication()).getLoggedInUsers()
        viewModelScope.launch {
            val notificationsCounter = HashMap<String, Int>()
            withContext(IO) {
                loggedInUserNames.forEach {
                    notificationsCounter[it] = databaseProvider.provideNotificationsDao(it).count()
                }
                notificationsCounterLiveData.postValue(notificationsCounter)
            }
        }
    }

    /** [ViewModelProvider.Factory] for [NavigationViewModel] */
    class Factory(
            private val databaseProvider: DatabaseProvider
    ) : ViewModelProvider.Factory {

        /** @return new instance of [NavigationViewModel] casted as T */
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST") // NavigationViewModel is T, since T is ViewModel
            return NavigationViewModel(databaseProvider) as T
        }
    }
}
