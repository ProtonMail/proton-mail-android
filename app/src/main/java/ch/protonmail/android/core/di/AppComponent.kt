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
package ch.protonmail.android.core.di

import javax.inject.Singleton

import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.activities.SearchActivity
import ch.protonmail.android.activities.dialogs.ManageLabelsDialogFragment
import ch.protonmail.android.activities.mailbox.MailboxActivity
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.ProtonRetrofit
import ch.protonmail.android.api.interceptors.ProtonMailRequestInterceptor
import ch.protonmail.android.api.SecuredServices
import ch.protonmail.android.api.models.address.AddressKeyActivationWorker
import ch.protonmail.android.api.segments.contact.ContactEmailsManager
import ch.protonmail.android.api.segments.event.EventHandler
import ch.protonmail.android.api.segments.event.EventManager
import ch.protonmail.android.api.segments.event.EventUpdaterService
import ch.protonmail.android.api.services.*
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.gcm.GcmIntentService
import ch.protonmail.android.gcm.PMRegistrationIntentService
import ch.protonmail.android.jobs.FetchContactsEmailsJob
import ch.protonmail.android.jobs.ProtonMailBaseJob
import ch.protonmail.android.receivers.ConnectivityBroadcastReceiver
import ch.protonmail.android.receivers.NotificationReceiver
import ch.protonmail.android.receivers.VerificationOnSendReceiver
import ch.protonmail.android.storage.AttachmentClearingService
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.crypto.ServerTimeInterceptor
import dagger.Component

@Singleton
@Component(modules = [AppModule::class, ActivityModule::class, RepositoryModule::class])
interface AppComponent {
    fun inject(application: ProtonMailApplication)
    fun inject(api: ProtonMailApiManager)
    fun inject(api: ProtonMailApi)
    fun inject(userManager: UserManager)
    fun inject(eventManager: EventManager)
    fun inject(eventHandler: EventHandler)
    fun inject(contactEmailsManager: ContactEmailsManager)
    fun inject(openPGP: OpenPGP)
    fun inject(protonRetrofit: ProtonRetrofit)

    fun inject(job: ProtonMailBaseJob)
    fun inject(job: FetchContactsEmailsJob)
    fun inject(activity: BaseActivity)
    fun inject(activity: MailboxActivity)
    fun inject(activity: SearchActivity)
    fun inject(manageLabelsDialogFragment: ManageLabelsDialogFragment)

    fun inject(service: LoginService)
    fun inject(service: MessagesService)
    fun inject(service: EventUpdaterService)
    fun inject(service: PostMessageServiceFactory)
    fun inject(service: GcmIntentService)
    fun inject(service: PMRegistrationIntentService)
    fun inject(service: AttachmentClearingService)
    fun inject(service: SecuredServices)
    fun inject(service: LogoutService)

    fun inject(receiver: NotificationReceiver)
    fun inject(receiver: ConnectivityBroadcastReceiver)
    fun inject(receiver: VerificationOnSendReceiver)
    fun inject(interceptor: ProtonMailRequestInterceptor)
    fun inject(interceptor: ServerTimeInterceptor)

    fun inject(worker: DownloadEmbeddedAttachmentsWorker)
    fun inject(worker: AddressKeyActivationWorker)
}
