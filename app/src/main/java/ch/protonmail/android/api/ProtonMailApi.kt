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

import android.os.Build
import ch.protonmail.android.api.Tls12SocketFactory.Companion.enableTls12
import ch.protonmail.android.api.interceptors.ProtonMailAttachmentRequestInterceptor
import ch.protonmail.android.api.interceptors.ProtonMailRequestInterceptor
import ch.protonmail.android.api.models.AttachmentHeaders
import ch.protonmail.android.api.models.BugsBody
import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.NewMessage
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.segments.ONE_MINUTE
import ch.protonmail.android.api.segments.THREE_SECONDS
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
import ch.protonmail.android.api.utils.StringConverterFactory
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.crypto.ServerTimeInterceptor
import com.birbit.android.jobqueue.JobManager
import com.datatheorem.android.trustkit.TrustKit
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Modifier
import java.net.URL
import java.util.Arrays
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


/**
 * TODO: split this api into it's natural parts and rewrite it using delegation in kotlin:
 * https://kotlinlang.org/docs/reference/delegation.html
 */
@Singleton
open class ProtonMailApi private constructor(
        private val addressApi : AddressApiSpec,
        private val attachmentApi : AttachmentApiSpec,
        private val authenticationApi : AuthenticationApiSpec,
        private val connectivityApi : ConnectivityApiSpec,
        private val contactApi : ContactApiSpec,
        private val deviceApi : DeviceApiSpec,
        private var keyApi : KeyApiSpec,
        private val messageApi: MessageApiSpec,
        private var labelApi : LabelApiSpec,
        private var organizationApi : OrganizationApiSpec,
        private var paymentApi : PaymentApiSpec,
        private var reportApi : ReportApiSpec,
        private val resetApi : ResetApiSpec,
        private var mailSettingsApi : MailSettingsApiSpec,
        private var userSettingsApi : UserSettingsApiSpec,
        private val userApi : UserApiSpec,
        private val domainApi: DomainApiSpec,
        val securedServices : SecuredServices) : BaseApi(),
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
        DomainApiSpec by domainApi {

    // hack to insert our parameters in the constructor instead of via init: otherwise delegation doesn't work
    @Inject
    constructor(userManager: UserManager, jobManager: JobManager, networkUtil: QueueNetworkUtil) : this(createConstructionParams(userManager, jobManager, networkUtil))

    constructor(params : Array<Any>) : this(
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
            params[18] as SecuredServices)

    init {
        ProtonMailApplication.getApplication().appComponent.inject(this)
    }

    companion object {
        private fun createConstructionParams(userManager: UserManager, jobManager: JobManager, networkUtil: QueueNetworkUtil) : Array<Any> {
            val spec: List<ConnectionSpec?> = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
                listOf(ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .cipherSuites(
                                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                                CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                        .build())
            } else {
                Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS)
            }
            val serverTimeInterceptor = ServerTimeInterceptor()

            val gsonUcc = GsonBuilder()
                    .setFieldNamingStrategy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                    .registerTypeAdapter(NewMessage::class.java, NewMessage.NewMessageSerializer())
                    .registerTypeAdapter(MessageRecipient::class.java, MessageRecipient.MessageRecipientSerializer())// Android 6 bug fix
                    .registerTypeAdapter(MessageRecipient::class.java, MessageRecipient.MessageRecipientDeserializer())// Android 6 bug fix
                    .registerTypeAdapter(LabelBody::class.java, LabelBody.LabelBodySerializer())// Android 6 bug fix
                    .registerTypeAdapter(LabelBody::class.java, LabelBody.LabelBodyDeserializer())// Android 6 bug fix
                    .registerTypeAdapter(BugsBody::class.java, BugsBody.BugsBodySerializer())// Android 6 bug fix
                    .registerTypeAdapter(BugsBody::class.java, BugsBody.BugsBodyDeserializer())// Android 6 bug fix
                    .registerTypeAdapter(AttachmentHeaders::class.java, AttachmentHeaders.AttachmentHeadersDeserializer())// Android 6 bug fix
                    .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)// Android 6 bug fix
                    .create()

            val interceptor = ProtonMailRequestInterceptor.getInstance(userManager, jobManager, networkUtil)

            //region adapters setup
            val restAdapterPub = Retrofit.Builder()
                    .baseUrl(Constants.ENDPOINT_URI)
                    .client(getOkHttpClient(ONE_MINUTE, interceptor, HttpLoggingInterceptor.Level.BODY, spec, serverTimeInterceptor))
                    .addConverterFactory(GsonConverterFactory.create(gsonUcc))
                    .build()

            //Just for ping fetchContactGroups
            val restAdapterPing = Retrofit.Builder()
                    .baseUrl(Constants.ENDPOINT_URI)
                    .client(getOkHttpClient(THREE_SECONDS, interceptor, HttpLoggingInterceptor.Level.BODY, spec, serverTimeInterceptor))
                    .addConverterFactory(GsonConverterFactory.create(gsonUcc))
                    .build()

            val restAdapterExtendedTimeoutUcc = Retrofit.Builder()
                    .baseUrl(Constants.ENDPOINT_URI)
                    .client(getOkHttpClient(ONE_MINUTE * 2, interceptor, HttpLoggingInterceptor.Level.BODY, spec, serverTimeInterceptor))
                    .addConverterFactory(StringConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gsonUcc))
                    .build()
            //endregion

            val okHttpClientInt = getOkHttpClient(ONE_MINUTE, interceptor, HttpLoggingInterceptor.Level.BODY, spec, serverTimeInterceptor)

            val services = SecuredServices(okHttpClientInt)
            val publicService = restAdapterPub.create(ProtonMailPublicService::class.java)
            val authPubService = restAdapterPub.create(AuthenticationPubService::class.java)
            val paymentPubService = restAdapterPub.create(PaymentPubService::class.java)
            val userPubService = restAdapterPub.create(UserPubService::class.java)
            val domainPubService = restAdapterPub.create(DomainPubService::class.java)
            val servicePing = restAdapterPing.create(PingService::class.java)
            val mUploadService = restAdapterExtendedTimeoutUcc.create(AttachmentUploadService::class.java)

            interceptor.publicService = publicService

            val attachReqInter = ProtonMailAttachmentRequestInterceptor.getInstance(publicService, userManager, jobManager, networkUtil)
            val okHttpClientAttachments = getOkHttpClient(3 * ONE_MINUTE, attachReqInter, HttpLoggingInterceptor.Level.HEADERS, spec, null)
            val restAdapterUccAttachments = Retrofit.Builder()
                    .baseUrl(Constants.ENDPOINT_URI)
                    .client(okHttpClientAttachments)
                    .addConverterFactory(GsonConverterFactory.create(gsonUcc))
                    .build()
            val mAttachmentsService = restAdapterUccAttachments.create(AttachmentDownloadService::class.java)

            val addressApi = AddressApi(services.address)
            val attachmentApi = AttachmentApi(services.attachment, mAttachmentsService, attachReqInter, mUploadService)
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

            return arrayOf(addressApi, attachmentApi, authenticationApi, connectivityApi, contactApi,
                    deviceApi, keyApi, messageApi, labelApi, organizationApi, paymentApi,  reportApi,
                    resetApi, mailSettingsApi, userSettingsApi, userApi, domainApi, attachReqInter,
                    services)
        }

        private fun getOkHttpClient(timeout: Long, interceptor: Interceptor, loggingLevel: HttpLoggingInterceptor.Level, connectionSpecs: List<ConnectionSpec?>,
                                    serverTimeInterceptor: ServerTimeInterceptor?): OkHttpClient {
            val okClientBuilder = OkHttpClient.Builder()

            if (Constants.FeatureFlags.TLS_12_UPGRADE) {
                okClientBuilder.enableTls12()
            }

            okClientBuilder.connectTimeout(timeout, TimeUnit.SECONDS)
            okClientBuilder.readTimeout(timeout, TimeUnit.SECONDS)
            okClientBuilder.writeTimeout(timeout, TimeUnit.SECONDS)
            okClientBuilder.addInterceptor(interceptor)
            if (AppUtil.isDebug()) {
                val httpLoggingInterceptor = HttpLoggingInterceptor()
                httpLoggingInterceptor.level = loggingLevel
                okClientBuilder.addInterceptor(httpLoggingInterceptor)
            }
            if (serverTimeInterceptor != null) {
                okClientBuilder.addInterceptor(serverTimeInterceptor)
            }
            okClientBuilder.connectionSpecs(connectionSpecs)

            // TLS Certificate Pinning
            val trustKit = TrustKit.getInstance()
            val serverHostname = URL(Constants.ENDPOINT_URI).host
            okClientBuilder.sslSocketFactory(
                    trustKit.getSSLSocketFactory(serverHostname),
                    trustKit.getTrustManager(serverHostname)
            )
            return okClientBuilder.build()
        }
    }
}
