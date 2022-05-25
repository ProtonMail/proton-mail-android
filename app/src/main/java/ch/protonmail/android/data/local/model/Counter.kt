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
package ch.protonmail.android.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

const val COLUMN_COUNTER_ID = "id"
const val COLUMN_COUNTER_COUNT = "count"

const val TABLE_TOTAL_LABEL_COUNTERS = "totalLabelCounters"
const val TABLE_TOTAL_LOCATION_COUNTERS = "totalLocationCounters"
const val TABLE_UNREAD_LABEL_COUNTERS = "unreadLabelCounters"
const val TABLE_UNREAD_LOCATION_COUNTERS = "unreadLocationCounters"

open class Counter<K : Any>(

    @PrimaryKey
    @ColumnInfo(name = COLUMN_COUNTER_ID)
    val id: K,

    @ColumnInfo(name = COLUMN_COUNTER_COUNT)
    var count: Int = 0
) {

    fun increment() {
        count += 1
    }

    fun increment(by: Int) {
        count += by
    }

    fun decrement() {
        if (count > 0) {
            count -= 1
        }
    }

    fun decrement(by: Int) {
        if (count - by >= 0) {
            count -= by
        }
    }
}


@Entity(tableName = TABLE_TOTAL_LABEL_COUNTERS)
class TotalLabelCounter(id: String, count: Int = 0) : Counter<String>(id, count)

@Entity(tableName = TABLE_TOTAL_LOCATION_COUNTERS)
class TotalLocationCounter(id: Int, count: Int = 0) : Counter<Int>(id, count)

@Entity(tableName = TABLE_UNREAD_LABEL_COUNTERS)
class UnreadLabelCounter(id: String, count: Int = 0) : Counter<String>(id, count)

@Entity(tableName = TABLE_UNREAD_LOCATION_COUNTERS)
class UnreadLocationCounter(id: Int, count: Int = 0) : Counter<Int>(id, count)
