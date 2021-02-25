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

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.utils.MessageUtils
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider


class AttachmentsViewModel @ViewModelInject constructor(
    private val dispatchers: DispatcherProvider,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val networkUtil: QueueNetworkUtil
) : ViewModel() {

    val viewState: MutableLiveData<ViewState> = MutableLiveData()

    fun init(messageId: String) {
        viewModelScope.launch(dispatchers.Io) {
            val message = messageDetailsRepository.findMessageById(messageId)

            message?.let { existingMessage ->
                val messageDbId = requireNotNull(existingMessage.dbId)
                val messageFlow = messageDetailsRepository.findMessageByDbId(messageDbId)

                if (!networkUtil.isConnected()) {
                    postViewState(ViewState.MissingConnectivity)
                }

                messageFlow.onEach { updatedMessage ->
                    if (isDraftCreationEvent(existingMessage, updatedMessage)) {
                        postViewState(ViewState.UpdateAttachments(updatedMessage.Attachments))
                        this.cancel()
                    }
                }.collect()
            }
        }
    }

    private fun isDraftCreationEvent(existingMessage: Message, updatedMessage: Message) =
        !isRemoteMessage(existingMessage) && isRemoteMessage(updatedMessage)

    private suspend fun postViewState(state: ViewState) = withContext(dispatchers.Main) {
        viewState.value = state
    }

    private fun isRemoteMessage(message: Message) = !MessageUtils.isLocalMessageId(message.messageId)

    sealed class ViewState {
        object MissingConnectivity : ViewState()
        data class UpdateAttachments(val attachments: List<Attachment>) : ViewState()
    }

}
