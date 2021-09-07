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

package ch.protonmail.android.contacts.details.presentation.model

import ch.protonmail.android.contacts.details.ContactEmailGroupSelectionState
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.data.local.model.LabelType

data class ContactLabelUiModel(
    val id: LabelId,
    val name: String,
    val color: String,
    val type: LabelType,
    val path: String,
    val parentId: String,
    val expanded: Int,
    val sticky: Int,
    val contactEmailsCount: Int,
    val contactDataCount: Int = 0,
    val isSelected: ContactEmailGroupSelectionState = ContactEmailGroupSelectionState.DEFAULT
)
