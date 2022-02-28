/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.api

import ch.protonmail.android.api.segments.attachment.AttachmentService
import ch.protonmail.android.api.segments.contact.ContactService
import ch.protonmail.android.api.segments.device.DeviceService
import ch.protonmail.android.api.segments.event.EventService
import ch.protonmail.android.api.segments.key.KeyService
import ch.protonmail.android.api.segments.message.MessageService
import ch.protonmail.android.api.segments.organization.OrganizationService
import ch.protonmail.android.api.segments.report.ReportService
import ch.protonmail.android.api.segments.settings.mail.MailSettingsService
import ch.protonmail.android.labels.data.remote.LabelService
import ch.protonmail.android.mailbox.data.remote.ConversationService
import retrofit2.Retrofit

class SecuredServices(private val retrofit: Retrofit) {

    val contact: ContactService by createService(ContactService::class.java)

    val message: MessageService by createService(MessageService::class.java)

    val device: DeviceService by createService(DeviceService::class.java)

    val report: ReportService by createService(ReportService::class.java)

    val event: EventService by createService(EventService::class.java)

    val mailSettings: MailSettingsService by createService(MailSettingsService::class.java)

    val key: KeyService by createService(KeyService::class.java)

    val label: LabelService by createService(LabelService::class.java)

    val attachment: AttachmentService by createService(AttachmentService::class.java)

    val organization: OrganizationService by createService(OrganizationService::class.java)

    val conversation: ConversationService by createService(ConversationService::class.java)

    // Every service gets the same Retrofit instance (lazy loaded)
    private fun <T> createService(serviceInterface: Class<T>): Lazy<T> =
        lazy { retrofit.create(serviceInterface) }
}
