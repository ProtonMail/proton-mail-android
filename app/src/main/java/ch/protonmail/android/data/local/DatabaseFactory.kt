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

import android.content.Context
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import ch.protonmail.android.core.Constants
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.prefs.SecureSharedPreferences
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
        val username = CounterDatabase.usernameForUserId(context, userId)

        val baseFileName = CounterDatabase.databaseName(username)
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
        val username = usernameForUserId(context, userId)

        val baseFileName = databaseName(username)

        return Room.databaseBuilder(context.applicationContext, databaseClass.java, baseFileName)
            .fallbackToDestructiveMigration()
            .addMigrations(*migrations)
            .build()
    }

    protected fun usernameForUserId(context: Context, userId: UserId): String {
        val prefs = SecureSharedPreferences.getPrefsForUser(context, userId)
        return checkNotNull(prefs.getString(Constants.Prefs.PREF_USER_NAME, null))
    }

    protected fun databaseName(username: String) =
        "${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$baseDatabaseName"

    @VisibleForTesting
    fun buildInMemoryDatabase(context: Context) =
        Room.inMemoryDatabaseBuilder(context.applicationContext, databaseClass.java)
            .fallbackToDestructiveMigration()
            .build()
}
