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
package ch.protonmail.android.contacts.list

import ch.protonmail.android.contacts.list.listView.ContactItem
import ch.protonmail.android.contacts.list.viewModel.IContactsListViewModel
import ch.protonmail.android.jobs.ConvertLocalContactsJob
import com.birbit.android.jobqueue.JobManager

class LocalContactsConverter(
    private val jobManager: JobManager,
    private val viewModel: IContactsListViewModel
) {
    fun startConversion(contacts: List<ContactItem>) {
        viewModel.setProgress(0)
        viewModel.setProgressMax(contacts.size)
        jobManager.addJobInBackground(ConvertLocalContactsJob(contacts))
    }
}
