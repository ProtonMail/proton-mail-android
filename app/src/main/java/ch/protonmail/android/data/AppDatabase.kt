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
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ch.protonmail.android.labels.data.local.LabelDao
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.notifications.data.local.NotificationDao
import ch.protonmail.android.notifications.data.local.model.NotificationEntity
import me.proton.core.account.data.db.AccountConverters
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.data.entity.AccountMetadataEntity
import me.proton.core.account.data.entity.SessionDetailsEntity
import me.proton.core.account.data.entity.SessionEntity
import me.proton.core.challenge.data.db.ChallengeConverters
import me.proton.core.challenge.data.db.ChallengeDatabase
import me.proton.core.challenge.data.entity.ChallengeFrameEntity
import me.proton.core.contact.data.local.db.ContactConverters
import me.proton.core.contact.data.local.db.ContactDatabase
import me.proton.core.contact.data.local.db.entity.ContactCardEntity
import me.proton.core.contact.data.local.db.entity.ContactEmailEntity
import me.proton.core.contact.data.local.db.entity.ContactEmailLabelEntity
import me.proton.core.contact.data.local.db.entity.ContactEntity
import me.proton.core.crypto.android.keystore.CryptoConverters
import me.proton.core.data.room.db.BaseDatabase
import me.proton.core.data.room.db.CommonConverters
import me.proton.core.featureflag.data.db.FeatureFlagDatabase
import me.proton.core.featureflag.data.entity.FeatureFlagEntity
import me.proton.core.humanverification.data.db.HumanVerificationConverters
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.humanverification.data.entity.HumanVerificationEntity
import me.proton.core.key.data.db.KeySaltDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.key.data.entity.KeySaltEntity
import me.proton.core.key.data.entity.PublicAddressEntity
import me.proton.core.key.data.entity.PublicAddressKeyEntity
import me.proton.core.mailsettings.data.db.MailSettingsDatabase
import me.proton.core.mailsettings.data.entity.MailSettingsEntity
import me.proton.core.observability.data.db.ObservabilityDatabase
import me.proton.core.observability.data.entity.ObservabilityEventEntity
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.payment.data.local.entity.GooglePurchaseEntity
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserConverters
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.user.data.entity.AddressEntity
import me.proton.core.user.data.entity.AddressKeyEntity
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.data.entity.UserKeyEntity
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsConverters
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import me.proton.core.usersettings.data.entity.OrganizationEntity
import me.proton.core.usersettings.data.entity.OrganizationKeysEntity
import me.proton.core.usersettings.data.entity.UserSettingsEntity
import timber.log.Timber

@Database(
    entities = [
        // Core
        AccountEntity::class,
        AccountMetadataEntity::class,
        AddressEntity::class,
        AddressKeyEntity::class,
        HumanVerificationEntity::class,
        KeySaltEntity::class,
        MailSettingsEntity::class,
        PublicAddressEntity::class,
        PublicAddressKeyEntity::class,
        SessionEntity::class,
        SessionDetailsEntity::class,
        UserEntity::class,
        UserKeyEntity::class,
        UserSettingsEntity::class,
        OrganizationEntity::class,
        OrganizationKeysEntity::class,
        ContactEntity::class,
        ContactCardEntity::class,
        ContactEmailEntity::class,
        ContactEmailLabelEntity::class,
        FeatureFlagEntity::class,
        ChallengeFrameEntity::class,
        GooglePurchaseEntity::class,
        ObservabilityEventEntity::class,
        // Mail
        LabelEntity::class,
        NotificationEntity::class
    ],
    version = AppDatabase.version,
    exportSchema = true
)
@TypeConverters(
    // Core
    CommonConverters::class,
    AccountConverters::class,
    UserConverters::class,
    CryptoConverters::class,
    HumanVerificationConverters::class,
    UserSettingsConverters::class,
    ContactConverters::class,
    ChallengeConverters::class,
    // Mail
    ProtonMailConverters::class
)
internal abstract class AppDatabase :
    BaseDatabase(),
    AccountDatabase,
    AddressDatabase,
    ContactDatabase,
    HumanVerificationDatabase,
    KeySaltDatabase,
    MailSettingsDatabase,
    OrganizationDatabase,
    PublicAddressDatabase,
    UserDatabase,
    UserSettingsDatabase,
    FeatureFlagDatabase,
    ChallengeDatabase,
    PaymentDatabase,
    ObservabilityDatabase {

    abstract fun labelDao(): LabelDao
    abstract fun notificationDao(): NotificationDao

    companion object {

        const val version = 13
        private const val name = "proton-mail.db"

        private fun getDbCreationCallback(context: Context): Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Timber.v("New $name DB created")

                // Initial migration from "old" core DB to the new Mail app db.
                AppDatabaseMigrations.initialMigration(context).migrate(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Timber.v("$name DB open")
            }
        }

        private fun getMigrations(): Array<Migration> {
            val migrations = arrayOf(
                AppDatabaseMigrations.MIGRATION_1_2,
                AppDatabaseMigrations.MIGRATION_2_3,
                AppDatabaseMigrations.MIGRATION_3_4,
                AppDatabaseMigrations.MIGRATION_4_5,
                AppDatabaseMigrations.MIGRATION_5_6,
                AppDatabaseMigrations.MIGRATION_6_7,
                AppDatabaseMigrations.MIGRATION_7_8,
                AppDatabaseMigrations.MIGRATION_8_9,
                AppDatabaseMigrations.MIGRATION_9_10,
                AppDatabaseMigrations.MIGRATION_10_11,
                AppDatabaseMigrations.MIGRATION_11_12,
                AppDatabaseMigrations.MIGRATION_12_13,
            )
            Timber.v("Db migrations list size ${migrations.size}")
            return migrations
        }

        fun buildDatabase(context: Context): AppDatabase {
            Timber.v("Building $name database")

            return databaseBuilder<AppDatabase>(context, name)
                .apply {
                    addCallback(getDbCreationCallback(context))
                    addMigrations(*getMigrations())
                }
                .build()
        }

        @VisibleForTesting
        fun buildInMemoryDatabase(context: Context) =
            Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java)
                .fallbackToDestructiveMigration()
                .build()

    }
}
