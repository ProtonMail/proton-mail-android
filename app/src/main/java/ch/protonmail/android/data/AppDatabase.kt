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
import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ch.protonmail.android.labels.data.db.LabelEntity
import me.proton.core.account.data.db.AccountConverters
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.data.entity.AccountMetadataEntity
import me.proton.core.account.data.entity.SessionDetailsEntity
import me.proton.core.account.data.entity.SessionEntity
import me.proton.core.crypto.android.keystore.CryptoConverters
import me.proton.core.data.room.db.BaseDatabase
import me.proton.core.data.room.db.CommonConverters
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
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserConverters
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.user.data.entity.AddressEntity
import me.proton.core.user.data.entity.AddressKeyEntity
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.data.entity.UserKeyEntity
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
        // Mail - Labels
        LabelEntity::class,
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
    // Mail
    ProtonMailConverters::class
)
abstract class AppDatabase :
    BaseDatabase(),
    AccountDatabase,
    UserDatabase,
    AddressDatabase,
    KeySaltDatabase,
    HumanVerificationDatabase,
    PublicAddressDatabase,
    MailSettingsDatabase {

    // abstract fun contactDao(): ContactDao
    // abstract fun messagesDao(): MessageDao

    companion object {

        const val version = 1
        private const val name = "proton-mail.db"

        private fun getDbCreationCallback(context: Context): Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Timber.v("New $name DB created")

                // Initial migration from "old" core DB to the new Mail app db.
                val migration01 = AppDatabaseMigrations.initialMigration(context)
                migration01.migrate(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Timber.v("$name DB open")
            }
        }

        private fun getMigrations(): Array<Migration> {
            val migrations = emptyArray<Migration>()
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

    }
}
