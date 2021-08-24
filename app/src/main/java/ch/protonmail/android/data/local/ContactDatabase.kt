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

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.protonmail.android.data.ProtonMailConverters
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.data.local.model.FullContactDetailsConverter
import ch.protonmail.android.data.local.model.LabelEntity
import me.proton.core.data.room.db.CommonConverters

@Database(
    entities = [
        ContactData::class,
        ContactEmail::class,
        LabelEntity::class,
        FullContactDetails::class,
        ContactEmailContactLabelJoin::class,
    ],
    version = 2
)
@TypeConverters(
    FullContactDetailsConverter::class,
    // Core - temp solution before migration to 1 db
    CommonConverters::class,
    ProtonMailConverters::class
)
abstract class ContactDatabase : RoomDatabase() {

    abstract fun getDao(): ContactDao

    companion object : DatabaseFactory<ContactDatabase>(
        ContactDatabase::class,
        "ContactsDatabase.db"
    )
}
