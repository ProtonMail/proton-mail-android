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

import android.content.Context
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.utils.extensions.app
import me.proton.core.util.kotlin.unsupported

@Database(
    entities = [
        UnreadLabelCounter::class,
        UnreadLocationCounter::class,
        TotalLabelCounter::class,
        TotalLocationCounter::class],
    version = 1
)
abstract class CountersDatabaseFactory : RoomDatabase() {
    abstract fun getDatabase(): CountersDatabase

    companion object {
        private const val DEFAULT_DATABASE_FILENAME = "UnreadCountersDatabase.db"
        private val DATABASES = mutableMapOf<Id, CountersDatabaseFactory>()

        @Synchronized
        fun getInstance(context: Context, userId: Id): CountersDatabaseFactory =
            DATABASES.getOrPut(userId) { buildDatabase(context, userId) }

        @JvmOverloads
        @Synchronized
        @Deprecated(
            "Use with user Id",
            ReplaceWith("getInstance(context, userId)"),
            DeprecationLevel.ERROR
        )
        fun getInstance(context: Context, username: String? = null): CountersDatabaseFactory {
            unsupported
        }

        @Synchronized
        fun deleteDb(context: Context, userId: Id) {
            val username = usernameForUserId(context, userId)

            val baseFileName = databaseName(username)
            val databaseFile = context.getDatabasePath(baseFileName)
            val databaseFileShm = context.getDatabasePath("$baseFileName-shm")
            val databaseFileWal = context.getDatabasePath("$baseFileName-wal")

            if (databaseFile.exists()) databaseFile.delete()
            if (databaseFileShm.exists()) databaseFileShm.delete()
            if (databaseFileWal.exists()) databaseFileWal.delete()

            DATABASES.remove(userId)
        }

        @Synchronized
        @Deprecated(
            "Use with user Id",
            ReplaceWith("deleteDb(context, userId)"),
            DeprecationLevel.ERROR
        )
        fun deleteDb(context: Context, username: String) {
            unsupported
        }

        private fun buildDatabase(context: Context, userId: Id): CountersDatabaseFactory {
            val username = usernameForUserId(context, userId)

            // region migrate old single-user database to multi-user
            val baseFileName = databaseName(username)

            context.getDatabasePath(DEFAULT_DATABASE_FILENAME)
                .renameTo(context.getDatabasePath(baseFileName))

            context.getDatabasePath("$DEFAULT_DATABASE_FILENAME-shm")
                .renameTo(context.getDatabasePath("$baseFileName-shm"))

            context.getDatabasePath("$DEFAULT_DATABASE_FILENAME-wal")
                .renameTo(context.getDatabasePath("$baseFileName-wal"))
            // endregion

            return Room.databaseBuilder(context.applicationContext, CountersDatabaseFactory::class.java, baseFileName)
                .fallbackToDestructiveMigration()
                .build()
        }

        @Deprecated(
            "Use with user Id",
            ReplaceWith("buildDatabase(context, userId)"),
            DeprecationLevel.ERROR
        )
        private fun buildDatabase(context: Context, username: String): CountersDatabaseFactory {
            unsupported
        }

        private fun usernameForUserId(context: Context, userId: Id): String {
            val user = context.app.userManager.getUserBlocking(userId)
            return user.name.s
        }

        private fun databaseName(username: String) =
            "${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$DEFAULT_DATABASE_FILENAME"
    }
}
