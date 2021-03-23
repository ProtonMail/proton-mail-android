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

import android.text.TextUtils
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.jobs.Priority
import ch.protonmail.android.jobs.ProtonMailBaseJob
import com.birbit.android.jobqueue.Params

class SetMembersForContactGroupJob(
    private val contactGroupId: String,
    private val contactGroupName: String,
    private val membersList: List<String>
) : ProtonMailBaseJob(Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL)) {

    override fun onRun() {
        val contactsDatabase = ContactDatabase
            .getInstance(applicationContext, getUserManager().requireCurrentUserId())
            .getDao()
        var id = contactGroupId
        if (TextUtils.isEmpty(id)) {
            val contactLabel = contactsDatabase.findContactGroupByName(contactGroupName)
            id = contactLabel?.ID ?: ""
        }
        val labelContactsBody = LabelContactsBody(id, membersList)
        getApi().labelContacts(labelContactsBody)
                .subscribeOn(ThreadSchedulers.io())
                .observeOn(ThreadSchedulers.io())
                .blockingAwait()
    }

}
