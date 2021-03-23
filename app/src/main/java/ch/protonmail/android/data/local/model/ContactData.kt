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
package ch.protonmail.android.data.local.model

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Random

// region constants
const val TABLE_CONTACT_DATA = "contact_data"
const val COLUMN_CONTACT_DATA_ID = "ID"
const val COLUMN_CONTACT_DATA_NAME = "Name"
const val COLUMN_CONTACT_DATA_LABEL_IDS = "LabelIDs"
const val COLUMN_CONTACT_DATA_SIZE = "Size"
const val COLUMN_CONTACT_DATA_MODIFY_TIME = "ModifyTime"
const val COLUMN_CONTACT_DATA_CREATE_TIME = "CreateTime"
const val COLUMN_CONTACT_DATA_UID = "UID"
// endregion

@Entity(
	tableName = TABLE_CONTACT_DATA,
	indices = [
		Index(COLUMN_CONTACT_DATA_ID, unique = true),
		Index(COLUMN_CONTACT_DATA_NAME, unique = false)
	]
)
data class ContactData @JvmOverloads constructor(

	@SerializedName(COLUMN_CONTACT_DATA_ID)
	@ColumnInfo(name = COLUMN_CONTACT_DATA_ID)
	var contactId: String?,

	@SerializedName(COLUMN_CONTACT_DATA_NAME)
	@ColumnInfo(name = COLUMN_CONTACT_DATA_NAME)
	var name: String,

	@SerializedName(COLUMN_CONTACT_DATA_LABEL_IDS)
	@Ignore
	private var labelIds: List<String>? = null,

	@SerializedName(COLUMN_CONTACT_DATA_UID)
	@Ignore
	private val uid: String? = null,

	@SerializedName(COLUMN_CONTACT_DATA_CREATE_TIME)
	@Ignore
	private val createTime: Long = 0,

	@SerializedName(COLUMN_CONTACT_DATA_MODIFY_TIME)
	@Ignore
	private val modifyTime: Long = 0,

	@SerializedName(COLUMN_CONTACT_DATA_SIZE)
	@Ignore
	private var size: Int = 0

) : Serializable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = BaseColumns._ID)
    var dbId: Long? = null

    companion object {
        @Deprecated("Use ContactIdGenerator instead to make Unit Testing possible")
        fun generateRandomContactId(): String {
            val random = Random(System.nanoTime())
            val randomOneSec = random.nextInt()
            return "${-(System.currentTimeMillis() + randomOneSec)}"
        }
    }
}
