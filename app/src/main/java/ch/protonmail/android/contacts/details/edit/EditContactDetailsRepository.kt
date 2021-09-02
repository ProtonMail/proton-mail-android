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
package ch.protonmail.android.contacts.details.edit

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.jobs.UpdateContactJob
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.mapper.LabelEntityApiMapper
import ch.protonmail.android.labels.data.mapper.LabelEntityDomainMapper
import com.birbit.android.jobqueue.JobManager
import ezvcard.VCard
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class EditContactDetailsRepository @Inject constructor(
    jobManager: JobManager,
    api: ProtonMailApiManager,
    dispatcherProvider: DispatcherProvider,
    contactDao: ContactDao,
    private val labelRepository: LabelRepository,
    val contactRepository: ContactsRepository,
    labelEntityApiMapper: LabelEntityApiMapper,
    labelEntityDomainMapper: LabelEntityDomainMapper
) : ContactDetailsRepository(
    jobManager, api, contactDao, dispatcherProvider, labelRepository, contactRepository, labelEntityApiMapper,
    labelEntityDomainMapper
) {

    suspend fun clearEmail(email: String) {
        contactDao.clearByEmail(email)
    }

    fun updateContact(
        contactId: String,
        contactName: String,
        emails: List<ContactEmail>,
        vCardEncrypted: VCard,
        vCardSigned: VCard,
        mapEmailGroupsIds: Map<ContactEmail, List<ContactLabelUiModel>>,
    ) {
        jobManager.addJobInBackground(
            UpdateContactJob(
                contactId, contactName, emails, vCardEncrypted.write(),
                vCardSigned.write(), mapEmailGroupsIds, labelRepository
            )
        )
    }

}
