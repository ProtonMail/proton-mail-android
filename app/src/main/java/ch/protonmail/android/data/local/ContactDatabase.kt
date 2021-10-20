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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ch.protonmail.android.data.ProtonMailConverters
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_EMAILS_ID
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_EMAILS_LABEL_IDS
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.data.local.model.FullContactDetailsConverter
import ch.protonmail.android.data.local.model.TABLE_CONTACT_EMAILS
import me.proton.core.data.room.db.CommonConverters
import org.apache.commons.lang3.StringEscapeUtils

@Database(
    entities = [
        ContactData::class,
        ContactEmail::class,
        FullContactDetails::class,
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
        "ContactsDatabase.db",
        MIGRATION_1_2
    )
}

private val MIGRATION_1_2 = object : Migration(1, 2) {

    private val CONTACT_LABEL_TABLE = "ContactLabel"
    private val CONTACT_EMAILS_LABEL_JOIN_TABLE = "ContactEmailsLabelJoin"

    private val selectContactIdsToLabelIdsQuery =
        "SELECT $COLUMN_CONTACT_EMAILS_ID, $COLUMN_CONTACT_EMAILS_LABEL_IDS " +
            "from $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_LABEL_IDS <> ''"

    private fun updateLabelIdsQuery(labelIds: String, contactId: String) =
        "UPDATE $TABLE_CONTACT_EMAILS SET $COLUMN_CONTACT_EMAILS_LABEL_IDS = '$labelIds' " +
            "WHERE $COLUMN_CONTACT_EMAILS_ID = '$contactId'"

    override fun migrate(database: SupportSQLiteDatabase) {
        with(database) {
            fixCorruptedLabelIds()
            dropContactEmailsLabelJoinTable()
            dropContactLabelTable()
        }
    }

    private fun SupportSQLiteDatabase.fixCorruptedLabelIds() {
        query(selectContactIdsToLabelIdsQuery).use { cursor ->
            while (cursor.moveToNext()) {
                val contactId = cursor.getString(0)
                val corruptedLabelIds = cursor.getString(1)
                val fixedLabelIds = StringEscapeUtils.unescapeJava(corruptedLabelIds)
                execSQL(updateLabelIdsQuery(fixedLabelIds, contactId))
            }
        }
    }

    private fun SupportSQLiteDatabase.dropContactLabelTable() =
        execSQL("DROP TABLE $CONTACT_LABEL_TABLE")

    private fun SupportSQLiteDatabase.dropContactEmailsLabelJoinTable() =
        execSQL("DROP TABLE $CONTACT_EMAILS_LABEL_JOIN_TABLE")
}
