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
package ch.protonmail.android.contacts.groups.jobs

import com.birbit.android.jobqueue.Params

import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory
import ch.protonmail.android.core.Constants
import ch.protonmail.android.events.LabelAddedEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.Priority
import ch.protonmail.android.jobs.ProtonMailBaseJob
import ch.protonmail.android.utils.AppUtil

/**
 * Created by dkadrikj on 17.7.15.
 */
class CreateContactGroupJob(private val labelName: String, private val color: String, private val display: Int,
                            private val exclusive: Int, private val update: Boolean, private val labelId: String)
    : ProtonMailBaseJob(Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL)) {

    @Throws(Throwable::class)
    override fun onRun() {
        val contactsDatabase = ContactsDatabaseFactory.getInstance(applicationContext).getDatabase()
        val labelResponse: LabelResponse = if (update) {
            // edit the label
            mApi.updateLabel(labelId, LabelBody(labelName, color, display, exclusive, Constants.LABEL_TYPE_CONTACT_GROUPS))
        } else {
            mApi.createLabel(LabelBody(labelName, color, display, exclusive, Constants.LABEL_TYPE_CONTACT_GROUPS))
        }

        if (labelResponse.hasError()) {
            AppUtil.postEventOnUi(LabelAddedEvent(Status.FAILED, labelResponse.error))
            return
        }

        val labelBody = labelResponse.contactGroup
        val labelId = labelBody.ID
        // update local label
        if (labelId != "") {
            contactsDatabase.saveContactGroupLabel(labelBody)
            AppUtil.postEventOnUi(LabelAddedEvent(Status.SUCCESS, null))
        }
    }
}