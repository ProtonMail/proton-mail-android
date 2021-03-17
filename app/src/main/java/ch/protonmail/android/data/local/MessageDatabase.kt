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
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.AttachmentTypesConverter
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessagesTypesConverter
import ch.protonmail.android.domain.entity.Id
import me.proton.core.util.kotlin.unsupported

@Database(
	entities = [Attachment::class, Message::class, Label::class],
	version = 8
)
@TypeConverters(value = [MessagesTypesConverter::class, AttachmentTypesConverter::class])
abstract class MessageDatabase : RoomDatabase() {

	abstract fun getDao(): MessageDao

	companion object : DatabaseFactory<MessageDatabase>(
		MessageDatabase::class,
		"MessagesDatabase.db"
	) {
		private val searchCache = mutableMapOf<Id, MessageDatabase>()

		@JvmOverloads
		@Synchronized
		@Deprecated(
			"Use with user Id",
			ReplaceWith("MessagesDatabase.getInstance(context, userId)"),
			DeprecationLevel.ERROR
		)
		fun getInstance(context:Context, username: String? = null): MessageDatabase =
			unsupported

		@Synchronized
		fun getSearchDatabase(context: Context, userId: Id): MessageDatabase =
			searchCache.getOrPut(userId) { buildInMemoryDatabase(context) }

		@Synchronized
		@Deprecated(
			"Use with user Id",
			ReplaceWith("MessagesDatabase.getSearchDatabase(context, userId)"),
			DeprecationLevel.ERROR
		)
		fun getSearchDatabase(context: Context): MessageDatabase {
			unsupported
		}

		@Synchronized
		@Deprecated(
			"Use with user Id",
			ReplaceWith("MessagesDatabase.deleteDatabase(context, userId)"),
			DeprecationLevel.ERROR
		)
		fun deleteDb(context: Context, username: String) {
			unsupported
		}
	}
}
