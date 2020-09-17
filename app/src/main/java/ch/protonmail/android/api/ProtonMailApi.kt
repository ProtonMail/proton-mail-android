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
package ch.protonmail.android.api

import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.segments.address.AddressApi
import ch.protonmail.android.api.segments.address.AddressApiSpec
import ch.protonmail.android.api.segments.attachment.AttachmentApi
import ch.protonmail.android.api.segments.attachment.AttachmentApiSpec
import ch.protonmail.android.api.segments.attachment.AttachmentDownloadService
import ch.protonmail.android.api.segments.attachment.AttachmentUploadService
import ch.protonmail.android.api.segments.authentication.AuthenticationApi
import ch.protonmail.android.api.segments.authentication.AuthenticationApiSpec
import ch.protonmail.android.api.segments.authentication.AuthenticationPubService
import ch.protonmail.android.api.segments.connectivity.ConnectivityApi
import ch.protonmail.android.api.segments.connectivity.ConnectivityApiSpec
import ch.protonmail.android.api.segments.connectivity.PingService
import ch.protonmail.android.api.segments.contact.ContactApi
import ch.protonmail.android.api.segments.contact.ContactApiSpec
import ch.protonmail.android.api.segments.device.DeviceApi
import ch.protonmail.android.api.segments.device.DeviceApiSpec
import ch.protonmail.android.api.segments.domain.DomainApi
import ch.protonmail.android.api.segments.domain.DomainApiSpec
import ch.protonmail.android.api.segments.domain.DomainPubService
import ch.protonmail.android.api.segments.key.KeyApi
import ch.protonmail.android.api.segments.key.KeyApiSpec
import ch.protonmail.android.api.segments.label.LabelApi
import ch.protonmail.android.api.segments.label.LabelApiSpec
import ch.protonmail.android.api.segments.message.MessageApi
import ch.protonmail.android.api.segments.message.MessageApiSpec
import ch.protonmail.android.api.segments.organization.OrganizationApi
import ch.protonmail.android.api.segments.organization.OrganizationApiSpec
import ch.protonmail.android.api.segments.payment.PaymentApi
import ch.protonmail.android.api.segments.payment.PaymentApiSpec
import ch.protonmail.android.api.segments.payment.PaymentPubService
import ch.protonmail.android.api.segments.report.ReportApi
import ch.protonmail.android.api.segments.report.ReportApiSpec
import ch.protonmail.android.api.segments.reset.ResetApi
import ch.protonmail.android.api.segments.reset.ResetApiSpec
import ch.protonmail.android.api.segments.settings.mail.MailSettingsApi
import ch.protonmail.android.api.segments.settings.mail.MailSettingsApiSpec
import ch.protonmail.android.api.segments.settings.mail.UserSettingsApi
import ch.protonmail.android.api.segments.settings.mail.UserSettingsApiSpec
import ch.protonmail.android.api.segments.user.UserApi
import ch.protonmail.android.api.segments.user.UserApiSpec
import ch.protonmail.android.api.segments.user.UserPubService
import javax.inject.Inject

/**
 * Base API class that all API calls should go through.
 */
class ProtonMailApi private constructor(
    // region constructor params
    private val addressApi: AddressApiSpec,
    private val attachmentApi: AttachmentApiSpec,
    private val authenticationApi: AuthenticationApiSpec,
    val connectivityApi: ConnectivityApiSpec,
    private val contactApi: ContactApiSpec,
    private val deviceApi: DeviceApiSpec,
    private val keyApi: KeyApiSpec,
    private val messageApi: MessageApiSpec,
    private val labelApi: LabelApiSpec,
    private val organizationApi: OrganizationApiSpec,
    private val paymentApi: PaymentApiSpec,
    private val reportApi: ReportApiSpec,
    private val resetApi: ResetApiSpec,
    private val mailSettingsApi: MailSettingsApiSpec,
    private val userSettingsApi: UserSettingsApiSpec,
    private val userApi: UserApiSpec,
    private val domainApi: DomainApiSpec,
    var securedServices: SecuredServices
    // endregion
) :
    // region super classes and interfaces
    BaseApi(),
    AddressApiSpec by addressApi,
    AttachmentApiSpec by attachmentApi,
    AuthenticationApiSpec by authenticationApi,
    ConnectivityApiSpec by connectivityApi,
    ContactApiSpec by contactApi,
    DeviceApiSpec by deviceApi,
    KeyApiSpec by keyApi,
    LabelApiSpec by labelApi,
    MessageApiSpec by messageApi,
    OrganizationApiSpec by organizationApi,
    PaymentApiSpec by paymentApi,
    ReportApiSpec by reportApi,
    ResetApiSpec by resetApi,
    UserSettingsApiSpec by userSettingsApi,
    MailSettingsApiSpec by mailSettingsApi,
    UserApiSpec by userApi,
    DomainApiSpec by domainApi
    // endregion
{
    // region hack to insert parameters in the constructor instead of init, otherwise delegation doesn't work
    @Inject
    constructor(protonRetrofitBuilder: ProtonRetrofitBuilder) :
        this(createConstructionParams(protonRetrofitBuilder))

    constructor(params: Array<Any>) : this(
        // region params
        params[0] as AddressApiSpec,
        params[1] as AttachmentApiSpec,
        params[2] as AuthenticationApiSpec,
        params[3] as ConnectivityApiSpec,
        params[4] as ContactApiSpec,
        params[5] as DeviceApiSpec,
        params[6] as KeyApiSpec,
        params[7] as MessageApi,
        params[8] as LabelApiSpec,
        params[9] as OrganizationApiSpec,
        params[10] as PaymentApiSpec,
        params[11] as ReportApiSpec,
        params[12] as ResetApiSpec,
        params[13] as MailSettingsApiSpec,
        params[14] as UserSettingsApiSpec,
        params[15] as UserApiSpec,
        params[16] as DomainApiSpec,
        params[18] as SecuredServices
        // endregion
    )

    companion object {
        /**
         * We inject the base url, which is now becoming dynamic instead of previously kept in Constants.ENDPOINT_URI.
         * Retrofit builders should now depend on a dynamic base url and also we should not recreate
         * them on every API call.
         */
        private fun createConstructionParams(protonRetrofitBuilder: ProtonRetrofitBuilder): Array<Any> {

            // region config
            val services = SecuredServices(protonRetrofitBuilder.provideRetrofit(RetrofitType.SECURE))
            val authPubService = protonRetrofitBuilder.provideRetrofit(RetrofitType.PUBLIC).create(AuthenticationPubService::class.java)
            val paymentPubService = protonRetrofitBuilder.provideRetrofit(RetrofitType.PUBLIC).create(PaymentPubService::class.java)
            val userPubService = protonRetrofitBuilder.provideRetrofit(RetrofitType.PUBLIC).create(UserPubService::class.java)
            val domainPubService = protonRetrofitBuilder.provideRetrofit(RetrofitType.PUBLIC).create(DomainPubService::class.java)
            val servicePing = protonRetrofitBuilder.provideRetrofit(RetrofitType.PING).create(PingService::class.java)
            val mUploadService = protonRetrofitBuilder.provideRetrofit(RetrofitType.EXTENDED_TIMEOUT).create(AttachmentUploadService::class.java)
            val mAttachmentsService = protonRetrofitBuilder.provideRetrofit(RetrofitType.ATTACHMENTS).create(AttachmentDownloadService::class.java)

            val addressApi = AddressApi(services.address)
            val attachmentApi = AttachmentApi(services.attachment, mAttachmentsService, protonRetrofitBuilder.attachReqInter, mUploadService)
            val authenticationApi = AuthenticationApi(services.authentication, authPubService)
            val connectivityApi = ConnectivityApi(servicePing)
            val contactApi = ContactApi(services.contact)
            val deviceApi = DeviceApi(services.device)
            val keyApi = KeyApi(services.key)
            val messageApi = MessageApi(services.message)
            val labelApi = LabelApi(services.label)
            val organizationApi = OrganizationApi(services.organization)
            val paymentApi = PaymentApi(services.payment, paymentPubService)
            val reportApi = ReportApi(services.report)
            val resetApi = ResetApi(services.reset)
            val mailSettingsApi = MailSettingsApi(services.mailSettings)
            val userSettingsApi = UserSettingsApi(services.userSettings)
            val domainApi = DomainApi(domainPubService)
            val userApi = UserApi(services.user, userPubService)
            // endregion
            return arrayOf(addressApi, attachmentApi, authenticationApi, connectivityApi, contactApi,
                    deviceApi, keyApi, messageApi, labelApi, organizationApi, paymentApi, reportApi,
                    resetApi, mailSettingsApi, userSettingsApi, userApi, domainApi, protonRetrofitBuilder.attachReqInter,
                    services)
        }
    }
}
