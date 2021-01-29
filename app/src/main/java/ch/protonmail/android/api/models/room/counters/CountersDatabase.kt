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

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

// TODO remove when we change name of this class to CountersDao and *Factory to *Database
typealias CountersDao = CountersDatabase

@Dao
abstract class CountersDatabase {

    //region Unread Labels Counters
    @Query("SELECT * FROM $TABLE_UNREAD_LABEL_COUNTERS")
    abstract fun findAllUnreadLabels(): LiveData<List<UnreadLabelCounter>>

    @Query("SELECT * FROM $TABLE_UNREAD_LABEL_COUNTERS WHERE ${COLUMN_COUNTER_ID}=:labelId")
    abstract fun findUnreadLabelById(labelId: String): UnreadLabelCounter?

    @Query("DELETE FROM $TABLE_UNREAD_LABEL_COUNTERS")
    abstract fun clearUnreadLabelsTable()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertUnreadLabel(unreadLabel: UnreadLabelCounter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAllUnreadLabels(unreadLabels: Collection<UnreadLabelCounter>)

    //endregion

    //region Unread Locations Counters
    @Query("SELECT * FROM $TABLE_UNREAD_LOCATION_COUNTERS WHERE ${COLUMN_COUNTER_ID}=:locationId")
    abstract fun findUnreadLocationById(locationId: Int): UnreadLocationCounter?

    @Query("SELECT * FROM $TABLE_UNREAD_LOCATION_COUNTERS")
    abstract fun findAllUnreadLocations(): LiveData<List<UnreadLocationCounter>>

    @Query("DELETE FROM $TABLE_UNREAD_LOCATION_COUNTERS")
    abstract fun clearUnreadLocationsTable()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertUnreadLocation(unreadLocation: UnreadLocationCounter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAllUnreadLocations(unreadLocations: Collection<UnreadLocationCounter>)
    //endregion

    @Transaction
    open fun updateUnreadCounters(
        locations: Collection<UnreadLocationCounter>,
        labels: Collection<UnreadLabelCounter>
    ) {
        clearUnreadLocationsTable()
        clearUnreadLabelsTable()
        insertAllUnreadLocations(locations)
        insertAllUnreadLabels(labels)
    }

    //region Total Label Counters
    @Query("SELECT * FROM $TABLE_TOTAL_LABEL_COUNTERS")
    abstract fun findAllTotalLabels(): LiveData<List<TotalLabelCounter>>

    @Query("SELECT * FROM $TABLE_TOTAL_LABEL_COUNTERS WHERE ${COLUMN_COUNTER_ID}=:labelId")
    abstract fun findTotalLabelById(labelId: String): TotalLabelCounter?

    @Query("DELETE FROM $TABLE_TOTAL_LABEL_COUNTERS")
    abstract fun clearTotalLabelsTable()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertTotalLabels(labels: Collection<TotalLabelCounter>)
    //endregion

    //region Total Location Counters
    @Query("SELECT * FROM $TABLE_TOTAL_LOCATION_COUNTERS WHERE ${COLUMN_COUNTER_ID}=:locationId")
    abstract fun findTotalLocationById(locationId: Int): TotalLocationCounter?

    @Query("SELECT * FROM $TABLE_TOTAL_LOCATION_COUNTERS")
    abstract fun findAllTotalLocations(): LiveData<List<TotalLocationCounter>>

    @Query("DELETE FROM $TABLE_TOTAL_LOCATION_COUNTERS")
    abstract fun clearTotalLocationsTable()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertTotalLocations(locations: Collection<TotalLocationCounter>)

    @Transaction
    open fun refreshTotalCounters(locations: Collection<TotalLocationCounter>, labels: List<TotalLabelCounter>) {
        refreshTotalLocationCounters(locations)
        refreshTotalLabelCounters(labels)
    }

    @Transaction
    protected open fun refreshTotalLabelCounters(labels: List<TotalLabelCounter>) {
        clearTotalLabelsTable()
        insertTotalLabels(labels)
    }

    @Transaction
    protected open fun refreshTotalLocationCounters(locations: Collection<TotalLocationCounter>) {
        clearTotalLocationsTable()
        insertTotalLocations(locations)
    }
    //endregion
}
