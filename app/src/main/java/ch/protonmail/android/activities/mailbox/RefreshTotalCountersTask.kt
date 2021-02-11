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
package ch.protonmail.android.activities.mailbox

import android.os.AsyncTask

import ch.protonmail.android.api.models.room.counters.CounterDao
import ch.protonmail.android.api.models.room.counters.TotalLabelCounter
import ch.protonmail.android.api.models.room.counters.TotalLocationCounter

/**
 * Created by Kamil Rajtar on 21.08.18.
 */
internal class RefreshTotalCountersTask(private val counterDao:CounterDao,
                                        private val locationCounters:List<TotalLocationCounter>,
                                        private val labelCounters:List<TotalLabelCounter>):AsyncTask<Void,Void,Void>() {

	override fun doInBackground(vararg voids:Void):Void? {
		counterDao.refreshTotalCounters(locationCounters,labelCounters)
		return null
	}
}
