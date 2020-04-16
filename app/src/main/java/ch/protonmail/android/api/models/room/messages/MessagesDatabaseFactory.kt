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
package ch.protonmail.android.api.models.room.messages

import android.content.Context
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.protonmail.android.core.ProtonMailApplication

@Database(entities = [Attachment::class, Message::class, Label::class], version = 8)
@TypeConverters(value = [MessagesTypesConverter::class, AttachmentTypesConverter::class])
abstract class MessagesDatabaseFactory : RoomDatabase() {
	abstract fun getDatabase():MessagesDatabase

	companion object {
        private const val DEFAULT_DATABASE_FILENAME = "MessagesDatabase.db"
		private val DATABASES = mutableMapOf<String, MessagesDatabaseFactory>()
		private val DATABASES_SEARCH = mutableMapOf<String, MessagesDatabaseFactory>()

		@JvmOverloads
		@Synchronized
		fun getInstance(context:Context, username: String? = null): MessagesDatabaseFactory {
			val name = username ?: ProtonMailApplication.getApplication().userManager?.username
			return if (name.isNullOrBlank()) {
				buildInMemoryDatabase(context)
			} else {
				DATABASES.getOrPut(name) { buildDatabase(context, name) }
			}
		}

		@Synchronized
		fun getSearchDatabase(context: Context): MessagesDatabaseFactory {
			val username = ProtonMailApplication.getApplication().userManager?.username
			return if (username.isNullOrBlank()) {
				buildInMemoryDatabase(context)
			} else {
				DATABASES_SEARCH.getOrPut(username) { buildInMemoryDatabase(context) }
			}
		}

		@Synchronized
		fun deleteDb(context: Context, username: String) {
			val databaseFile = context.getDatabasePath("${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$DEFAULT_DATABASE_FILENAME")
			val databaseFileShm = context.getDatabasePath("${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$DEFAULT_DATABASE_FILENAME-shm")
			val databaseFileWal = context.getDatabasePath("${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$DEFAULT_DATABASE_FILENAME-wal")
			if (databaseFile.exists()) {
				databaseFile.delete()
			}
			if (databaseFileShm.exists()) {
				databaseFileShm.delete()
			}
			if (databaseFileWal.exists()) {
				databaseFileWal.delete()
			}
			DATABASES.remove(username)
		}

        private fun buildDatabase(context: Context, username: String): MessagesDatabaseFactory {
			// migrate old single-user database to multi-user
			context.getDatabasePath(DEFAULT_DATABASE_FILENAME).renameTo(context.getDatabasePath("${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$DEFAULT_DATABASE_FILENAME"))
			context.getDatabasePath("$DEFAULT_DATABASE_FILENAME-shm").renameTo(context.getDatabasePath("${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$DEFAULT_DATABASE_FILENAME-shm"))
			context.getDatabasePath("$DEFAULT_DATABASE_FILENAME-wal").renameTo(context.getDatabasePath("${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$DEFAULT_DATABASE_FILENAME-wal"))

			return Room.databaseBuilder(context.applicationContext,
					MessagesDatabaseFactory::class.java,
					"${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-$DEFAULT_DATABASE_FILENAME")
					.fallbackToDestructiveMigration()
					.build()
		}

		@VisibleForTesting
		fun buildInMemoryDatabase(context: Context) =
                Room.inMemoryDatabaseBuilder(context.applicationContext,
                        MessagesDatabaseFactory::class.java)
                        .fallbackToDestructiveMigration()
                        .build()
	}
}
