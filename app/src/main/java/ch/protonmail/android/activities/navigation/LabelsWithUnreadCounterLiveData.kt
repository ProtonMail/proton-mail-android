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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.UnreadLabelCounter

/**
 * Created by Kamil Rajtar on 17.08.18.  */
//TODO change MutableList to List after rewritting Notifications activity to kotlin
class LabelsWithUnreadCounterLiveData(labelsLiveData: LiveData<List<Label>>,
                                      countersLiveData: LiveData<List<UnreadLabelCounter>>) : MediatorLiveData<MutableList<LabelWithUnreadCounter>>() {
    var labels: List<Label>? = null
    var unreadCounters: List<UnreadLabelCounter>? = null

    init {
        addSource(labelsLiveData) {
            labels = it
            tryEmit()
        }
        addSource(countersLiveData) {
            unreadCounters = it
            tryEmit()
        }
    }

    private fun tryEmit() {
        val labels = labels ?: return
        value = labels.map { label ->
            val unseenCount = unreadCounters?.find { it.id == label.id }?.count ?: 0
            LabelWithUnreadCounter(label, unseenCount)
        }.toMutableList()
    }
}
