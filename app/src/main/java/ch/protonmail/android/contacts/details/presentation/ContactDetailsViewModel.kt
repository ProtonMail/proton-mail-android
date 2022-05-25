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

package ch.protonmail.android.contacts.details.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import ch.protonmail.android.contacts.details.domain.FetchContactDetails
import ch.protonmail.android.contacts.details.domain.FetchContactGroups
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsViewState
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.utils.FileHelper
import ch.protonmail.android.worker.DeleteContactWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactDetailsViewModel @Inject constructor(
    private val fetchContactDetails: FetchContactDetails,
    private val fetchContactGroups: FetchContactGroups,
    private val mapper: ContactDetailsMapper,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val workManager: WorkManager,
    private val fileHelper: FileHelper,
    private val userManager: UserManager
) : ViewModel() {

    private val mutableContactsResultFlow = MutableStateFlow<ContactDetailsViewState>(ContactDetailsViewState.Loading)
    val contactsViewState: StateFlow<ContactDetailsViewState>
        get() = mutableContactsResultFlow

    private val mutableFlowVcard = MutableSharedFlow<Uri>()
    val vCardSharedFlow: SharedFlow<Uri>
        get() = mutableFlowVcard

    fun getContactDetails(contactId: String) {
        val userId = userManager.currentUserId
            ?: return
        viewModelScope.launch {
            fetchContactDetails(contactId)
                .combine(fetchContactGroups(userId, contactId)) { contacts, groups ->
                    Timber.v("Details $contactId, emails: ${contacts.emails.size}, groups: ${groups.groupsList.size}")
                    mapper.mapToContactViewData(contacts, groups)
                }
                .catch {
                    mutableContactsResultFlow.value = ContactDetailsViewState.Error(it)
                }
                .collect { fetchResult ->
                    mutableContactsResultFlow.value = fetchResult
                }
        }
    }

    fun deleteContact(contactId: String) = DeleteContactWorker.Enqueuer(workManager).enqueue(listOf(contactId))

    fun saveVcard(
        vCardToShare: String,
        contactName: String,
        context: Context
    ) {
        viewModelScope.launch {
            val vCardFileName = "$contactName.vcf"
            val uri = fileHelper.saveStringToFileProvider(vCardFileName, vCardToShare, context)
            mutableFlowVcard.emit(uri)
        }
    }

    fun moveDraftToTrash(messageId: String) {
        viewModelScope.launch {
            moveMessagesToFolder(
                listOf(messageId),
                Constants.MessageLocationType.TRASH.asLabelIdString(),
                Constants.MessageLocationType.DRAFT.asLabelIdString(),
                userManager.requireCurrentUserId()
            )
        }
    }
}
