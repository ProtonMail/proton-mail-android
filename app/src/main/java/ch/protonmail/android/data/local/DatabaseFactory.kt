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
import androidx.room.Room
import androidx.room.RoomDatabase
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.utils.extensions.app
import kotlin.reflect.KClass

open class DatabaseFactory<T : RoomDatabase>(
    private val databaseClass: KClass<T>,
    private val baseDatabaseName: String
) {

    private val cache = mutableMapOf<Id, T>()

    @Synchronized
    fun getInstance(context: Context, userId: Id): T =
        cache.getOrPut(userId) { buildDatabase(context, userId) }

    @Synchronized
    fun deleteDatabase(context: Context, userId: Id) {
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
        userId: Id
    ): T {
        val username = usernameForUserId(context, userId)

        val baseFileName = databaseName(username)
        migrateDatabase(context, baseFileName)

        return Room.databaseBuilder(context.applicationContext, databaseClass.java, baseFileName)
            .fallbackToDestructiveMigration()
            .build()
    }

    private fun migrateDatabase(context: Context, baseFileName: String) {

        context.getDatabasePath(baseFileName)
            .renameTo(context.getDatabasePath(baseFileName))

        context.getDatabasePath("$baseFileName-shm")
            .renameTo(context.getDatabasePath("$baseFileName-shm"))

        context.getDatabasePath("$baseFileName-wal")
            .renameTo(context.getDatabasePath("$baseFileName-wal"))
    }

    protected fun usernameForUserId(context: Context, userId: Id): String {
        val user = context.app.userManager.getUserBlocking(userId)
        return user.name.s
    }

    protected fun databaseName(username: String) =
        "${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$baseDatabaseName"
}
