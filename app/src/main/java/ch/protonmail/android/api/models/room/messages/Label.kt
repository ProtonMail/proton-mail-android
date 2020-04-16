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

import androidx.room.*

// region constants
const val TABLE_LABELS = "label"
const val COLUMN_LABEL_ID = "ID"
const val COLUMN_LABEL_NAME = "Name"
const val COLUMN_LABEL_COLOR = "Color"
const val COLUMN_LABEL_DISPLAY = "Display"
const val COLUMN_LABEL_ORDER = "LabelOrder"
const val COLUMN_LABEL_EXCLUSIVE = "Exclusive"
const val COLUMN_LABEL_TYPE = "Type"
// endregion

@Entity(tableName=TABLE_LABELS,indices=[Index(value=[COLUMN_LABEL_ID],unique=true)])
data class Label constructor(
		@PrimaryKey
		@ColumnInfo(name = COLUMN_LABEL_ID)
		val id: String,
		@ColumnInfo(name = COLUMN_LABEL_NAME, index = true)
		val name: String,
		@ColumnInfo(name = COLUMN_LABEL_COLOR, index = true)
		val color: String,
		@ColumnInfo(name = COLUMN_LABEL_DISPLAY)
		val display: Int,
		@ColumnInfo(name = COLUMN_LABEL_ORDER)
		val order: Int,
		@ColumnInfo(name = COLUMN_LABEL_EXCLUSIVE)
		val exclusive: Boolean,
		@ColumnInfo(name = COLUMN_LABEL_TYPE)
		val type: Int
) {
	@Ignore
	@JvmOverloads
	constructor(id:String,
				name:String,
				color:String,
				display:Int=0,
				order:Int=0,
				exclusive:Boolean=false):this(id,name,color,display,order,exclusive,1)


}

