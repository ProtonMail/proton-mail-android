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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_ID

// region constants
const val TABLE_CONTACT_EMAILS_LABELS_JOIN = "ContactEmailsLabelJoin"
const val COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = "labelId"
const val COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID = "emailId"
// endregion

@Entity(
    tableName = TABLE_CONTACT_EMAILS_LABELS_JOIN,
    primaryKeys = [
        COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID,
        COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID
    ],
    foreignKeys = [
        ForeignKey(
            entity = ContactEmail::class,
            childColumns = [COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID],
            parentColumns = [COLUMN_CONTACT_EMAILS_ID], onDelete = CASCADE
        ),

        ForeignKey(
            entity = ContactLabel::class,
            childColumns = [COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID],
            parentColumns = [COLUMN_LABEL_ID], onDelete = CASCADE
        )
    ],
    indices = [
        Index(COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID),
        Index(COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID)
    ]
)
data class ContactEmailContactLabelJoin constructor(
    @ColumnInfo(name = COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID)
    var emailId: String,
    @ColumnInfo(name = COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID)
    var labelId: String
)
