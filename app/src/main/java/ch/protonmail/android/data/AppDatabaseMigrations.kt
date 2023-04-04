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

package ch.protonmail.android.data

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ch.protonmail.android.notifications.data.local.NotificationDatabase
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.data.entity.AccountMetadataEntity
import me.proton.core.account.data.entity.SessionDetailsEntity
import me.proton.core.account.data.entity.SessionEntity
import me.proton.core.challenge.data.db.ChallengeDatabase
import me.proton.core.contact.data.local.db.ContactDatabase
import me.proton.core.featureflag.data.db.FeatureFlagDatabase
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.humanverification.data.entity.HumanVerificationEntity
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.key.data.entity.KeySaltEntity
import me.proton.core.key.data.entity.PublicAddressEntity
import me.proton.core.key.data.entity.PublicAddressKeyEntity
import me.proton.core.mailsettings.data.entity.MailSettingsEntity
import me.proton.core.observability.data.db.ObservabilityDatabase
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.user.data.entity.AddressEntity
import me.proton.core.user.data.entity.AddressKeyEntity
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.data.entity.UserKeyEntity
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import me.proton.core.usersettings.data.entity.UserSettingsEntity
import timber.log.Timber

@Suppress("ClassOrdering")
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

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            OrganizationDatabase.MIGRATION_0.migrate(database)
            ContactDatabase.MIGRATION_0.migrate(database)
            AddressDatabase.MIGRATION_2.migrate(database)
            PublicAddressDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_4.migrate(database)
            AddressDatabase.MIGRATION_3.migrate(database)
            UserDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            FeatureFlagDatabase.MIGRATION_0.migrate(database)
            FeatureFlagDatabase.MIGRATION_1.migrate(database)
            OrganizationDatabase.MIGRATION_1.migrate(database)
            NotificationDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ChallengeDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ChallengeDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            FeatureFlagDatabase.MIGRATION_2.migrate(database)
            FeatureFlagDatabase.MIGRATION_3.migrate(database)
            HumanVerificationDatabase.MIGRATION_1.migrate(database)
            HumanVerificationDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            PaymentDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_5.migrate(database)
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ObservabilityDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            OrganizationDatabase.MIGRATION_2.migrate(database)
        }
    }
}
