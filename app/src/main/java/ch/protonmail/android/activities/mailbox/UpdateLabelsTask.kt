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
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.api.utils.ApplyRemoveLabels
import ch.protonmail.android.jobs.ApplyLabelJob
import ch.protonmail.android.jobs.RemoveLabelJob
import com.birbit.android.jobqueue.JobManager
import java.util.*

// TODO it looks like this is not used anymore
internal class UpdateLabelsTask(private val jobManager:JobManager,
								private val messagesDatabase:MessagesDatabase,
								private val messageIds:List<String>,
								private val checkedLabelIds:List<String>,
								private val unchangedLabels:List<String>,
								private val maxAllowedLabels:Int,
								private val onTaskFailedListener:OnTaskFailedListener):AsyncTask<Void,Void,RuntimeException>() {

	private fun resolveMessageLabels(message:Message,checkedLabelIds:MutableList<String>,
									 unchangedLabels:List<String>):ApplyRemoveLabels {
		val labelsToRemove=ArrayList<String>()

		//handle the case where too many lLabels exist for this message
		message.labelIDsNotIncludingLocations.forEach {labelId->
			val label=messagesDatabase.findLabelById(labelId)
			if(!checkedLabelIds.contains(labelId)&&!unchangedLabels.contains(
							labelId)&&label!=null&&!label.exclusive) {
				// this label should be removed
				labelsToRemove.add(labelId)
			} else if(checkedLabelIds.contains(labelId)) {
				// the label remains
				checkedLabelIds.remove(labelId)
			}
		}

		val labelList=ArrayList(message.labelIDsNotIncludingLocations)
		labelList.addAll(checkedLabelIds)
		labelList.removeAll(labelsToRemove)
		if(labelList.size>maxAllowedLabels) {
			throw MessageWithTooManyLabelsException(message.subject)
		}
		// update the message with the new labels
		message.addLabels(checkedLabelIds)
		message.removeLabels(labelsToRemove)
		messagesDatabase.saveMessage(message)


		return ApplyRemoveLabels(checkedLabelIds,labelsToRemove)
	}

	override fun doInBackground(vararg voids:Void):MessageWithTooManyLabelsException? {
		val messages=messageIds.mapNotNull(messagesDatabase::findMessageById)

		val applyRemoveLabelsList=messages.map {
			try {
				it.messageId!! to resolveMessageLabels(it,
						ArrayList(checkedLabelIds),ArrayList(unchangedLabels))
			} catch(e:MessageWithTooManyLabelsException) {
				return e
			}
		}

		applyRemoveLabelsList.map {(messageId,applyRemoveLabels)-> applyRemoveLabels.labelsToApply.map {it to messageId}}
				.flatten().
						groupBy(
				{(key,_)-> key},
				{(_,value)-> value}).map {(key,value)->
					ApplyLabelJob(value,
							key)
				}.forEach(jobManager::addJobInBackground)

		applyRemoveLabelsList.map {(messageId,applyRemoveLabels)-> applyRemoveLabels.labelsToRemove.map {it to messageId}}
				.flatten().groupBy(
						{(key,_)-> key},
						{(_,value)-> value}).map {(key,value)->
					RemoveLabelJob(value,
							key)
				}.forEach(jobManager::addJobInBackground)

		return null
	}

	internal inner class MessageWithTooManyLabelsException(message:String?):RuntimeException(message)

	override fun onPostExecute(e:RuntimeException?) {
		if(e!=null)
			onTaskFailedListener.onTaskFailed(e)
	}
}
