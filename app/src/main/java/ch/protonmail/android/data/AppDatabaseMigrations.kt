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

package ch.protonmail.android.data

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.data.entity.AccountMetadataEntity
import me.proton.core.account.data.entity.SessionDetailsEntity
import me.proton.core.account.data.entity.SessionEntity
import me.proton.core.accountmanager.data.db.AccountManagerDatabase
import me.proton.core.data.room.db.extension.open
import me.proton.core.data.room.db.extension.openAndClose
import me.proton.core.humanverification.data.entity.HumanVerificationEntity
import me.proton.core.key.data.entity.KeySaltEntity
import me.proton.core.key.data.entity.PublicAddressEntity
import me.proton.core.key.data.entity.PublicAddressKeyEntity
import me.proton.core.mailsettings.data.entity.MailSettingsEntity
import me.proton.core.user.data.entity.AddressEntity
import me.proton.core.user.data.entity.AddressKeyEntity
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.data.entity.UserKeyEntity
import timber.log.Timber

object AppDatabaseMigrations {

    /**
     * Copy Core DB into Proton Mail DB.
     */
    fun initialMigration(context: Context, coreDatabase: AccountManagerDatabase) = object : Migration(0, 1) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Timber.v("Initial core db migration")
            // Force any migration for coreDatabase by opening then closing it.
            coreDatabase.openAndClose()

            // End current transaction (from FrameworkSQLiteOpenHelper.onUpgrade(db, version, mNewVersion))
            if (database.inTransaction()) {
                database.setTransactionSuccessful()
                database.endTransaction()
            }
            // Attach old Core DB to current DB (cannot be done within a transaction).
            val coreDbPath = context.getDatabasePath(AccountManagerDatabase.name).path
            database.execSQL("ATTACH DATABASE '$coreDbPath' AS coreDb")
            // Begin transaction for attached migration.
            database.beginTransaction()

            // Import all data from Core DB to current DB.
            listOf(
                AccountEntity::class.simpleName,
                AccountMetadataEntity::class.simpleName,
                AddressEntity::class.simpleName,
                AddressKeyEntity::class.simpleName,
                UserEntity::class.simpleName,
                UserKeyEntity::class.simpleName,
                HumanVerificationEntity::class.simpleName,
                KeySaltEntity::class.simpleName,
                MailSettingsEntity::class.simpleName,
                PublicAddressEntity::class.simpleName,
                PublicAddressKeyEntity::class.simpleName,
                SessionDetailsEntity::class.simpleName,
                SessionEntity::class.simpleName
            ).forEach { table ->
                Timber.v("Insert table $table")
                database.execSQL("INSERT INTO main.$table SELECT * FROM coreDb.$table")
            }

            // End current transaction to detach coreDb.
            if (database.inTransaction()) {
                database.setTransactionSuccessful()
                database.endTransaction()
            }
            database.execSQL("DETACH DATABASE coreDb")
            database.beginTransaction()

            // Clear old Core tables.
            coreDatabase.open()
            coreDatabase.runInTransaction { coreDatabase.clearAllTables() }
            coreDatabase.close()
        }
    }
}
