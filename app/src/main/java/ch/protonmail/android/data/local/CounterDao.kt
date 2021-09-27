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
package ch.protonmail.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.protonmail.android.data.local.model.COLUMN_COUNTER_ID
import ch.protonmail.android.data.local.model.TABLE_UNREAD_LABEL_COUNTERS
import ch.protonmail.android.data.local.model.TABLE_UNREAD_LOCATION_COUNTERS
import ch.protonmail.android.data.local.model.UnreadLabelCounter
import ch.protonmail.android.data.local.model.UnreadLocationCounter

@Dao
abstract class CounterDao {

    @Query("SELECT * FROM $TABLE_UNREAD_LABEL_COUNTERS WHERE $COLUMN_COUNTER_ID=:labelId")
    abstract fun findUnreadLabelById(labelId: String): UnreadLabelCounter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertUnreadLabel(unreadLabel: UnreadLabelCounter)

    //region Unread Locations Counters
    @Query("SELECT * FROM $TABLE_UNREAD_LOCATION_COUNTERS WHERE $COLUMN_COUNTER_ID=:locationId")
    abstract fun findUnreadLocationById(locationId: Int): UnreadLocationCounter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertUnreadLocation(unreadLocation: UnreadLocationCounter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAllUnreadLocations(unreadLocations: Collection<UnreadLocationCounter>)
    //endregion
}
