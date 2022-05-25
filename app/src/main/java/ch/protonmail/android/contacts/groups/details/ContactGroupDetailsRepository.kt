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
package ch.protonmail.android.contacts.groups.details

import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ContactGroupDetailsRepository @Inject constructor(
    private val labelRepository: LabelRepository,
    private val contactRepository: ContactsRepository
) {

    fun observeContactGroupDetails(id: String): Flow<Label?> =
        labelRepository.observeLabel(LabelId(id))

    fun observeContactGroupEmails(userId: UserId, groupLabelId: String): Flow<List<ContactEmail>> =
        contactRepository.observeAllContactEmailsByContactGroupId(userId, groupLabelId)

    fun filterContactGroupEmails(userId: UserId, groupLabelId: String, filter: String): Flow<List<ContactEmail>> =
        contactRepository.observeFilterContactEmailsByContactGroup(userId, groupLabelId, filter)

}
