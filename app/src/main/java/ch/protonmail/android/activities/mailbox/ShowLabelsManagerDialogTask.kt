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
package ch.protonmail.android.activities.mailbox

import android.os.AsyncTask
import androidx.fragment.app.FragmentManager
import ch.protonmail.android.activities.dialogs.ManageLabelsDialogFragment
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Message
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

internal class ShowLabelsManagerDialogTask(
	private val fragmentManager: FragmentManager,
	private val messageDetailsRepository: MessageDetailsRepository,
	private val messageIds: List<String>
) : AsyncTask<Void, Void, List<Message>>() {

	override fun doInBackground(vararg voids: Void): List<Message> {
		return messageIds.filter { it.isNotEmpty() }.mapNotNull(messageDetailsRepository::findMessageByIdBlocking)
	}

	override fun onPostExecute(messages: List<Message>) {
		val attachedLabels = HashSet<String>()
		val numberOfSelectedMessages = HashMap<String, Int>()
		messages.forEach { message ->
			val messageLabelIds = message.labelIDsNotIncludingLocations
			messageLabelIds.forEach { labelId ->
				numberOfSelectedMessages[labelId] = numberOfSelectedMessages[labelId]?.let { it + 1 } ?: 1
			}
			attachedLabels.addAll(messageLabelIds)
		}
		val manageLabelsDialogFragment = ManageLabelsDialogFragment.newInstance(
			attachedLabels, numberOfSelectedMessages, ArrayList(messageIds))
		val transaction = fragmentManager.beginTransaction()
		transaction.add(manageLabelsDialogFragment, manageLabelsDialogFragment.fragmentKey)
		transaction.commitAllowingStateLoss()
	}
}
