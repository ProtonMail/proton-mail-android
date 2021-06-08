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

package ch.protonmail.android.attachments

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.AddAttachmentsActivity.EXTRA_DRAFT_ID
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.utils.MessageUtils
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.DispatcherProvider

class AttachmentsViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val dispatchers: DispatcherProvider,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val networkConnectivityManager: NetworkConnectivityManager
) : ViewModel() {

    val viewState: MutableLiveData<AttachmentsViewState> = MutableLiveData()

    fun init() {
        viewModelScope.launch(dispatchers.Io) {
            val messageId = savedStateHandle.get<String>(EXTRA_DRAFT_ID) ?: return@launch
            val message = messageDetailsRepository.findMessageById(messageId)

            message?.let { existingMessage ->
                val messageDbId = requireNotNull(existingMessage.dbId)
                val messageFlow = messageDetailsRepository.findMessageByDbId(messageDbId)

                if (!networkConnectivityManager.isInternetConnectionPossible()) {
                    viewState.postValue(AttachmentsViewState.MissingConnectivity)
                }

                messageFlow.collect { updatedMessage ->
                    if (updatedMessage == null) {
                        return@collect
                    }
                    if (!this.isActive) {
                        return@collect
                    }
                    if (draftCreationHappened(existingMessage, updatedMessage)) {
                        viewState.postValue(AttachmentsViewState.UpdateAttachments(updatedMessage.Attachments))
                        this.cancel()
                    }
                }
            }
        }
    }

    private fun draftCreationHappened(existingMessage: Message, updatedMessage: Message) =
        !isRemoteMessage(existingMessage) && isRemoteMessage(updatedMessage)

    private fun isRemoteMessage(message: Message) = !MessageUtils.isLocalMessageId(message.messageId)

}
