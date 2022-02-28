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
package ch.protonmail.android.contacts.groups.list

import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class ContactGroupsRepository @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val labelRepository: LabelRepository,
    private val accountsManager: AccountManager,
    private val contactRepository: ContactsRepository
) {

    fun observeContactGroups(userId: UserId, filter: String): Flow<List<ContactLabelUiModel>> =
        accountsManager.getPrimaryUserId().filterNotNull()
            .flatMapLatest {
                labelRepository.observeSearchContactGroups(filter, it)
            }
            .map { labels ->
                labels.map { entity ->
                    ContactLabelUiModel(
                        id = entity.id,
                        name = entity.name,
                        color = entity.color,
                        type = entity.type,
                        path = entity.path,
                        parentId = entity.parentId,
                        contactEmailsCount = contactRepository.countContactEmailsByLabelId(userId, entity.id)
                    )
                }
            }
            .flowOn(dispatchers.Io)

    suspend fun getContactGroupEmails(userId: UserId, id: String): List<ContactEmail> =
        contactRepository.findAllContactEmailsByContactGroupId(userId, id)

    suspend fun saveContactGroup(contactLabel: Label, userId: UserId) {
        labelRepository.saveLabel(contactLabel, userId)
    }

}
