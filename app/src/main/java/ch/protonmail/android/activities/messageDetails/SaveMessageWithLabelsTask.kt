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
package ch.protonmail.android.activities.messageDetails

import android.os.AsyncTask
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.jobs.ApplyLabelJob
import ch.protonmail.android.jobs.RemoveLabelJob
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import java.util.*

// TODO it looks like this is not used anymore
internal class SaveMessageWithLabelsTask(private val jobManager:JobManager,
										 private val messagesDatabase:MessagesDatabase,
										 private val message:Message,
										 private val checkedLabelIds:MutableList<String>):AsyncTask<Void,Void,Void>() {

	override fun doInBackground(vararg voids:Void):Void? {
		val labelsToRemove=ArrayList<String>()
		val jobList=ArrayList<Job>()
		val messageId=message.messageId!!
		val labelIDsWithoutLocations=message.labelIDsNotIncludingLocations
		val labels=messagesDatabase.findAllLabelsWithIds(labelIDsWithoutLocations)
		for(label in labels) {
			val labelId=label.id
			val exclusive=label.exclusive
			if(!checkedLabelIds.contains(labelId)&&!exclusive) {
				// this label should be removed
				labelsToRemove.add(labelId)
				jobList.add(RemoveLabelJob(listOf(messageId),labelId))
			} else if(checkedLabelIds.contains(labelId)) {
				// the label remains
				checkedLabelIds.remove(labelId)
			}
		}

		// what remains are the new labels
		val applyLabelsJobs=checkedLabelIds.map {ApplyLabelJob(listOf(messageId),it)}
		jobList.addAll(applyLabelsJobs)

		// update the message with the new labels
		message.addLabels(checkedLabelIds)
		message.removeLabels(labelsToRemove)

		messagesDatabase.saveMessage(message)
		jobList.forEach(jobManager::addJobInBackground)
		return null
	}
}
