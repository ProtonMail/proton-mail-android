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
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.jobs.FetchMessageDetailJob
import ch.protonmail.android.labels.data.LabelRepository

internal class RegisterReloadTask(
    private val message: Message,
    private val labelRepository: LabelRepository
) : AsyncTask<Void, Void, Void>() {

    override fun doInBackground(vararg voids: Void): Void? {
        val app = ProtonMailApplication.getApplication()
        val messagesDatabase = MessageDatabase.getInstance(app, app.userManager.requireCurrentUserId()).getDao()
        val jobManager = app.jobManager
        if (message.checkIfAttHeadersArePresent(messagesDatabase)) {
            jobManager.addJobInBackground(FetchMessageDetailJob(message.messageId, labelRepository))
        }
        return null
    }
}
