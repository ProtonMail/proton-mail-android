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
package ch.protonmail.android.api.models.room.contacts

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_COLOR
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_DISPLAY
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_EXCLUSIVE
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_ID
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_NAME
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_ORDER
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_TYPE
import ch.protonmail.android.contacts.details.ContactEmailGroupSelectionState
import me.proton.core.util.kotlin.EMPTY_STRING
import java.io.Serializable

// region constants
const val TABLE_CONTACT_LABEL = "ContactLabel"
// endregion

@Entity(
    tableName = TABLE_CONTACT_LABEL,
    indices = [(Index(COLUMN_LABEL_ID, unique = true))]
)
data class ContactLabel @JvmOverloads constructor(
    @ColumnInfo(name = COLUMN_LABEL_ID)
    @PrimaryKey
    var ID: String = "",
    @ColumnInfo(name = COLUMN_LABEL_NAME)
    var name: String = "",
    @ColumnInfo(name = COLUMN_LABEL_COLOR)
    var color: String = "",
    @ColumnInfo(name = COLUMN_LABEL_DISPLAY)
    var display: Int = 0,
    @ColumnInfo(name = COLUMN_LABEL_ORDER)
    var order: Int = 0,
    @ColumnInfo(name = COLUMN_LABEL_EXCLUSIVE)
    var exclusive: Boolean = false,
    @ColumnInfo(name = COLUMN_LABEL_TYPE)
    var type: Int = 1
) : Parcelable, Serializable {
    var contactEmailsCount: Int = 0
    var contactDataCount: Int = 0
    @Ignore
    var isSelected: ContactEmailGroupSelectionState = ContactEmailGroupSelectionState.DEFAULT

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: EMPTY_STRING,
        parcel.readString() ?: EMPTY_STRING,
        parcel.readString() ?: EMPTY_STRING,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt()
    ) {
        contactEmailsCount = parcel.readInt()
        contactDataCount = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(ID)
        parcel.writeString(name)
        parcel.writeString(color)
        parcel.writeInt(display)
        parcel.writeInt(order)
        parcel.writeByte(if (exclusive) 1 else 0)
        parcel.writeInt(type)
        parcel.writeInt(contactEmailsCount)
        parcel.writeInt(contactDataCount)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ContactLabel> {
        override fun createFromParcel(parcel: Parcel): ContactLabel {
            return ContactLabel(parcel)
        }

        override fun newArray(size: Int): Array<ContactLabel?> {
            return arrayOfNulls(size)
        }
    }

}
