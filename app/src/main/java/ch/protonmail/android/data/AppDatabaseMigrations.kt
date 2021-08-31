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
import me.proton.core.humanverification.data.entity.HumanVerificationEntity
import me.proton.core.key.data.entity.KeySaltEntity
import me.proton.core.key.data.entity.PublicAddressEntity
import me.proton.core.key.data.entity.PublicAddressKeyEntity
import me.proton.core.mailsettings.data.entity.MailSettingsEntity
import me.proton.core.user.data.entity.AddressEntity
import me.proton.core.user.data.entity.AddressKeyEntity
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.data.entity.UserKeyEntity
import me.proton.core.usersettings.data.entity.UserSettingsEntity
import timber.log.Timber

object AppDatabaseMigrations {

    /**
     * Copy Core DB into Proton Mail DB.
     */
    fun initialMigration(context: Context) = object : Migration(0, 1) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Timber.v("Initial core db migration")

            // End current transaction (from FrameworkSQLiteOpenHelper.onUpgrade(db, version, mNewVersion))
            database.setTransactionSuccessful()
            database.endTransaction()
            // Attach old Core DB to current DB (cannot be done within a transaction).
            val coreDbFile = context.getDatabasePath("db-account-manager")
            val coreDbPath = coreDbFile.path
            database.execSQL("ATTACH DATABASE '$coreDbPath' AS coreDb")
            // Begin transaction for attached migration.
            database.beginTransaction()

            // Import all data from Core DB to current DB.
            listOf(
                AccountEntity::class.simpleName,
                AccountMetadataEntity::class.simpleName,
                AddressEntity::class.simpleName,
                AddressKeyEntity::class.simpleName,
                HumanVerificationEntity::class.simpleName,
                KeySaltEntity::class.simpleName,
                MailSettingsEntity::class.simpleName,
                PublicAddressEntity::class.simpleName,
                PublicAddressKeyEntity::class.simpleName,
                SessionDetailsEntity::class.simpleName,
                SessionEntity::class.simpleName,
                UserEntity::class.simpleName,
                UserKeyEntity::class.simpleName,
                UserSettingsEntity::class.simpleName
            ).forEach { table ->
                Timber.v("Insert table $table")
                runCatching {
                    database.execSQL("INSERT INTO main.$table SELECT * FROM coreDb.$table")
                }.onFailure {
                    Timber.i("Insert table $table has failed, probably core db it does not exist, ${it.message}")
                }
            }

            // End current transaction to detach coreDb.
            database.setTransactionSuccessful()
            database.endTransaction()
            database.execSQL("DETACH DATABASE coreDb")
            // Begin transaction as it should be for a migration/onUpgrade.
            database.beginTransaction()

            // Delete Core Database.
            coreDbFile.delete()
        }
    }
}
