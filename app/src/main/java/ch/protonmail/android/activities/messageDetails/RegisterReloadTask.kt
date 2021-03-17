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
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.jobs.FetchMessageDetailJob
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Kamil Rajtar on 30.07.18.
 */
internal class RegisterReloadTask(private val message:Message,
								  private val requestPending:AtomicBoolean):AsyncTask<Void,Void,Void>() {

	override fun doInBackground(vararg voids:Void):Void? {
		val messagesDatabase = MessageDatabase.getInstance(ProtonMailApplication.getApplication()).getDao()
		val jobManager = ProtonMailApplication.getApplication().jobManager
		if(message.checkIfAttHeadersArePresent(messagesDatabase)) {
			requestPending.set(true)
			jobManager.addJobInBackground(FetchMessageDetailJob(message.messageId))
		}
		return null
	}
}
