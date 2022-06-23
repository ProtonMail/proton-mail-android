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

package ch.protonmail.android.data.local

import android.content.Context
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import ch.protonmail.android.core.Constants
import ch.protonmail.android.prefs.SecureSharedPreferences
import me.proton.core.domain.entity.UserId
import kotlin.reflect.KClass

open class DatabaseFactory<T : RoomDatabase>(
    private val databaseClass: KClass<T>,
    private val baseDatabaseName: String,
    private vararg val migrations: Migration
) {

    private val cache = mutableMapOf<UserId, T>()

    @Synchronized
    fun getInstance(context: Context, userId: UserId): T =
        cache.getOrPut(userId) { buildDatabase(context, userId) }

    @Synchronized
    fun deleteDatabase(context: Context, userId: UserId) {
        val baseFileName = databaseName(userId)
        val databaseFile = context.getDatabasePath(baseFileName)
        val databaseFileShm = context.getDatabasePath("$baseFileName-shm")
        val databaseFileWal = context.getDatabasePath("$baseFileName-wal")

        if (databaseFile.exists()) databaseFile.delete()
        if (databaseFileShm.exists()) databaseFileShm.delete()
        if (databaseFileWal.exists()) databaseFileWal.delete()

        cache.remove(userId)
    }

    private fun buildDatabase(
        context: Context,
        userId: UserId
    ): T {
        tryMigrateDatabase(context, userId)

        val baseFileName = databaseName(userId)

        return Room.databaseBuilder(context.applicationContext, databaseClass.java, baseFileName)
            .fallbackToDestructiveMigration()
            .addMigrations(*migrations)
            .build()
    }

    private fun tryMigrateDatabase(context: Context, userId: UserId) {
        val username = usernameForUserIdOrNull(context, userId)
            ?: return

        val oldBaseFileName = databaseName(username)
        val newBaseFileName = databaseName(userId)

        val databaseFile = context.getDatabasePath(oldBaseFileName)
        val databaseFileShm = context.getDatabasePath("$oldBaseFileName-shm")
        val databaseFileWal = context.getDatabasePath("$oldBaseFileName-wal")

        if (databaseFile.exists()) {
            databaseFile.renameTo(context.getDatabasePath(newBaseFileName))
        }
        if (databaseFileShm.exists()) {
            databaseFileShm.renameTo(context.getDatabasePath("$newBaseFileName-shm"))
        }
        if (databaseFileWal.exists()) {
            databaseFileWal.renameTo(context.getDatabasePath("$newBaseFileName-wal"))
        }
    }

    private fun usernameForUserIdOrNull(context: Context, userId: UserId): String? {
        val prefs = SecureSharedPreferences.getPrefsForUser(context, userId)
        return prefs.getString(Constants.Prefs.PREF_USER_NAME, null)
    }

    private fun databaseName(username: String) =
        "${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$baseDatabaseName"

    private fun databaseName(userId: UserId) =
        "${Base64.encodeToString(userId.id.toByteArray(), Base64.NO_WRAP)}-$baseDatabaseName"

    @VisibleForTesting
    fun buildInMemoryDatabase(context: Context) =
        Room.inMemoryDatabaseBuilder(context.applicationContext, databaseClass.java)
            .fallbackToDestructiveMigration()
            .build()
}
