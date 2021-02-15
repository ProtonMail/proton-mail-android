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

import ch.protonmail.android.api.cookie.ProtonCookieStore
import ch.protonmail.android.api.interceptors.ProtonMailRequestInterceptor
import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.segments.ATTACH_PATH
import ch.protonmail.android.api.segments.ONE_MINUTE
import ch.protonmail.android.api.segments.THIRTY_SECONDS
import ch.protonmail.android.api.utils.StringConverterFactory
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.AttachmentHeaders
import ch.protonmail.android.utils.crypto.ServerTimeInterceptor
import ch.protonmail.android.utils.notifier.UserNotifier
import com.birbit.android.jobqueue.JobManager
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.network.domain.server.ServerTimeListener
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Modifier
import javax.inject.Inject
import javax.inject.Singleton

enum class RetrofitType {
    PUBLIC, PING, EXTENDED_TIMEOUT, ATTACHMENTS, SECURE
}

@Singleton
class ProtonRetrofitBuilder @Inject constructor(
    val userManager: UserManager,
    val jobManager: JobManager,
    private val serverTimeListener: ServerTimeListener,
    private val networkUtil: QueueNetworkUtil,
    private val cookieStore: ProtonCookieStore?,
    private val userNotifier: UserNotifier,
    private val sessionManager: SessionManager,
) {
    private val cache = HashMap<RetrofitType, Retrofit>()
    private lateinit var endpointUri: String

    fun rebuildMapFor(okHttpProvider: OkHttpProvider, endpointUri: String) {
        this.endpointUri = endpointUri

        enumValues<RetrofitType>().forEach { type ->
            cache[type] = when (type) {
                RetrofitType.PUBLIC -> {
                    ProtonRetrofitPublic(
                        okHttpProvider,
                        userManager,
                        jobManager,
                        serverTimeListener,
                        networkUtil,
                        cookieStore,
                        userNotifier,
                        sessionManager
                    ).build(endpointUri).build()
                }
                RetrofitType.PING -> {
                    ProtonRetrofitPing(
                        okHttpProvider,
                        userManager,
                        jobManager,
                        serverTimeListener,
                        networkUtil,
                        cookieStore,
                        userNotifier,
                        sessionManager
                    ).build(endpointUri).build()
                }
                RetrofitType.EXTENDED_TIMEOUT -> {
                    ProtonRetrofitExtended(
                        okHttpProvider,
                        userManager,
                        jobManager,
                        serverTimeListener,
                        networkUtil,
                        cookieStore,
                        userNotifier,
                        sessionManager
                    ).buildExtended(endpointUri).build()
                }
                RetrofitType.ATTACHMENTS -> {
                    ProtonRetrofitAttachments(
                        okHttpProvider,
                        userManager,
                        jobManager,
                        serverTimeListener,
                        networkUtil,
                        cookieStore,
                        userNotifier,
                        sessionManager
                    ).build(endpointUri).build()
                }
                else -> { // secure is default
                    ProtonRetrofitSecure(
                        okHttpProvider,
                        userManager,
                        jobManager,
                        serverTimeListener,
                        networkUtil,
                        cookieStore,
                        userNotifier,
                        sessionManager
                    ).build(endpointUri).build()
                }
            }
        }
    }

    fun provideRetrofit(type: RetrofitType): Retrofit {
        if (cache[type] == null) {
            throw RuntimeException("Retrofit Cache could not be empty")
        }
        return cache[type]!!
    }
}

/**
 * We have to receive a preconfigured okHttpClient, which is already configured to work with our normal
 * API or with some of the proxy APIs.
 */
sealed class ProtonRetrofit(
    val userManager: UserManager,
    val jobManager: JobManager,
    serverTimeListener: ServerTimeListener,
    networkUtil: QueueNetworkUtil,
    userNotifier: UserNotifier,
    sessionManager: SessionManager,
) {
    private val defaultInterceptor =
        ProtonMailRequestInterceptor.getInstance(userManager, jobManager, networkUtil, userNotifier, sessionManager)

    val spec: List<ConnectionSpec> = listOf(
        ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .cipherSuites(
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
            ).build()
    )
    val serverTimeInterceptor = ServerTimeInterceptor(serverTimeListener, networkUtil)

    // region gson specs
    private val gsonUcc: Gson = GsonBuilder()
        .setFieldNamingStrategy(FieldNamingPolicy.UPPER_CAMEL_CASE)
        .registerTypeAdapter(MessageRecipient::class.java, MessageRecipient.MessageRecipientSerializer())
        .registerTypeAdapter(MessageRecipient::class.java, MessageRecipient.MessageRecipientDeserializer())
        .registerTypeAdapter(LabelBody::class.java, LabelBody.LabelBodySerializer())
        .registerTypeAdapter(LabelBody::class.java, LabelBody.LabelBodyDeserializer())
        .registerTypeAdapter(AttachmentHeaders::class.java, AttachmentHeaders.AttachmentHeadersDeserializer())
        .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
        .create()
    // endregion

    protected abstract fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor
    ): OkHttpClient

    fun build(endpointUri: String): Retrofit.Builder {
        return Retrofit.Builder()
            .baseUrl(endpointUri)
            .client(configureOkHttp(endpointUri, defaultInterceptor))
            .addConverterFactory(GsonConverterFactory.create(gsonUcc))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    }

    fun buildExtended(endpointUri: String): Retrofit.Builder {
        return Retrofit.Builder()
            .baseUrl(endpointUri)
            .client(configureOkHttp(endpointUri, defaultInterceptor))
            .addConverterFactory(StringConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gsonUcc))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    }
}

class ProtonRetrofitPublic(
    private val okHttpProvider: OkHttpProvider,
    userManager: UserManager,
    jobManager: JobManager,
    serverTimeListener: ServerTimeListener,
    networkUtil: QueueNetworkUtil,
    private val cookieStore: ProtonCookieStore?,
    userNotifier: UserNotifier,
    sessionManager: SessionManager,
) : ProtonRetrofit(userManager, jobManager, serverTimeListener, networkUtil, userNotifier, sessionManager) {
    override fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor
    ): OkHttpClient {
        val okHttpClient = okHttpProvider.provideOkHttpClient(
            endpointUri,
            endpointUri,
            THIRTY_SECONDS,
            interceptor,
            HttpLoggingInterceptor.Level.BODY,
            spec,
            serverTimeInterceptor,
            cookieStore
        )
        return okHttpClient.okClientBuilder.build()
    }
}

class ProtonRetrofitPing(
    private val okHttpProvider: OkHttpProvider,
    userManager: UserManager,
    jobManager: JobManager,
    serverTimeListener: ServerTimeListener,
    networkUtil: QueueNetworkUtil,
    private val cookieStore: ProtonCookieStore?,
    userNotifier: UserNotifier,
    sessionManager: SessionManager,
) : ProtonRetrofit(userManager, jobManager, serverTimeListener, networkUtil, userNotifier, sessionManager) {
    override fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor
    ): OkHttpClient {
        val okHttpClient = okHttpProvider.provideOkHttpClient(
            endpointUri,
            endpointUri,
            THIRTY_SECONDS,
            interceptor,
            HttpLoggingInterceptor.Level.BODY,
            spec,
            serverTimeInterceptor,
            cookieStore
        )
        return okHttpClient.okClientBuilder.build()
    }
}

class ProtonRetrofitExtended(
    private val okHttpProvider: OkHttpProvider,
    userManager: UserManager,
    jobManager: JobManager,
    serverTimeListener: ServerTimeListener,
    networkUtil: QueueNetworkUtil,
    private val cookieStore: ProtonCookieStore?,
    userNotifier: UserNotifier,
    sessionManager: SessionManager,
) : ProtonRetrofit(userManager, jobManager, serverTimeListener, networkUtil, userNotifier, sessionManager) {
    override fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor
    ): OkHttpClient {
        val okHttpClient = okHttpProvider.provideOkHttpClient(
            endpointUri,
            endpointUri,
            ONE_MINUTE,
            interceptor,
            HttpLoggingInterceptor.Level.BODY,
            spec,
            serverTimeInterceptor,
            cookieStore
        )
        return okHttpClient.okClientBuilder.build()
    }
}

class ProtonRetrofitAttachments(
    private val okHttpProvider: OkHttpProvider,
    userManager: UserManager,
    jobManager: JobManager,
    serverTimeListener: ServerTimeListener,
    networkUtil: QueueNetworkUtil,
    private val cookieStore: ProtonCookieStore?,
    userNotifier: UserNotifier,
    sessionManager: SessionManager
) : ProtonRetrofit(userManager, jobManager, serverTimeListener, networkUtil, userNotifier, sessionManager) {
    override fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor
    ): OkHttpClient {
        val okHttpClient = okHttpProvider.provideOkHttpClient(
            endpointUri,
            endpointUri + ATTACH_PATH,
            THIRTY_SECONDS,
            interceptor,
            HttpLoggingInterceptor.Level.BODY,
            spec,
            serverTimeInterceptor,
            cookieStore
        )
        return okHttpClient.okClientBuilder.build()
    }
}

class ProtonRetrofitSecure(
    private val okHttpProvider: OkHttpProvider,
    userManager: UserManager,
    jobManager: JobManager,
    serverTimeListener: ServerTimeListener,
    networkUtil: QueueNetworkUtil,
    private val cookieStore: ProtonCookieStore?,
    userNotifier: UserNotifier,
    sessionManager: SessionManager,
) : ProtonRetrofit(userManager, jobManager, serverTimeListener, networkUtil, userNotifier, sessionManager) {
    override fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor
    ): OkHttpClient {
        val okHttpClient = okHttpProvider.provideOkHttpClient(
            endpointUri,
            endpointUri,
            THIRTY_SECONDS,
            interceptor,
            HttpLoggingInterceptor.Level.BODY,
            spec,
            serverTimeInterceptor,
            cookieStore
        )
        return okHttpClient.okClientBuilder.build()
    }
}
