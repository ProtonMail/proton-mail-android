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

package ch.protonmail.android.notifications.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration

interface NotificationDatabase : Database {

    companion object {

        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create NotificationEntity table
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `NotificationEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `notificationTitle` TEXT NOT NULL, `notificationBody` TEXT NOT NULL, `url` TEXT, `type` TEXT NOT NULL, PRIMARY KEY(`messageId`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_NotificationEntity_messageId` ON `NotificationEntity` (`messageId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_NotificationEntity_userId` ON `NotificationEntity` (`userId`)"
                )

            }
        }
    }
}
