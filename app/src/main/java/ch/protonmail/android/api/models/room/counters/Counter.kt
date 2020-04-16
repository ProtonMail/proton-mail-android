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
package ch.protonmail.android.api.models.room.counters

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

// region constants
const val COLUMN_COUNTER_ID = "id"
const val COLUMN_COUNTER_COUNT = "count"
// endregion

/**
 * Created by Kamil Rajtar on 21.08.18.
 */

open class Counter<K:Any>(
		@PrimaryKey
		@ColumnInfo(name = COLUMN_COUNTER_ID)
		val id: K,
		@ColumnInfo(name = COLUMN_COUNTER_COUNT)
		var count: Int = 0) {

	fun increment() {
		count+=1
	}

	fun increment(by:Int) {
		count+=by
	}

	fun decrement() {
		if(count>0) {
			count-=1
		}
	}

	fun decrement(by:Int) {
		if(count-by>=0) {
			count-=by
		}
	}
}
