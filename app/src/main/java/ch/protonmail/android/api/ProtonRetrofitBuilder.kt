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
import ch.protonmail.android.api.cookie.ProtonCookieStore
import ch.protonmail.android.api.interceptors.ProtonMailAttachmentRequestInterceptor
import ch.protonmail.android.api.interceptors.ProtonMailAuthenticator
import ch.protonmail.android.api.interceptors.ProtonMailRequestInterceptor
import ch.protonmail.android.api.models.AttachmentHeaders
import ch.protonmail.android.api.models.BugsBody
import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.segments.ATTACH_PATH
import ch.protonmail.android.api.segments.ONE_MINUTE
import ch.protonmail.android.api.segments.THIRTY_SECONDS
import ch.protonmail.android.api.utils.StringConverterFactory
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.crypto.ServerTimeInterceptor
import ch.protonmail.android.utils.notifier.UserNotifier
import com.birbit.android.jobqueue.JobManager
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
    private val networkUtil: QueueNetworkUtil,
    private val authenticator: ProtonMailAuthenticator,
    private val cookieStore: ProtonCookieStore?,
    private val userNotifier: UserNotifier
) {
    private val cache = HashMap<RetrofitType, Retrofit>()
    private lateinit var endpointUri: String
    lateinit var attachReqInter: ProtonMailAttachmentRequestInterceptor

    fun rebuildMapFor(okHttpProvider: OkHttpProvider, endpointUri: String) {
        this.endpointUri = endpointUri

        enumValues<RetrofitType>().forEach { type ->
            cache[type] = when (type) {
                RetrofitType.PUBLIC -> {
                    ProtonRetrofitPublic(
                        okHttpProvider,
                        userManager,
                        jobManager,
                        networkUtil,
                        authenticator,
                        cookieStore,
                        userNotifier
                    ).build(endpointUri).build()
                }
                RetrofitType.PING -> {
                    val retrofit = ProtonRetrofitPing(
                        okHttpProvider,
                        userManager,
                        jobManager,
                        networkUtil,
                        authenticator,
                        cookieStore,
                        userNotifier
                    )
                    retrofit.build(endpointUri).build()
                }
                RetrofitType.EXTENDED_TIMEOUT -> {
                    val retrofit = ProtonRetrofitExtended(
                        okHttpProvider,
                        userManager,
                        jobManager,
                        networkUtil,
                        authenticator,
                        cookieStore,
                        userNotifier
                    )
                    retrofit.buildExtended(endpointUri).build()
                }
                RetrofitType.ATTACHMENTS -> {
                    attachReqInter =
                        ProtonMailAttachmentRequestInterceptor.getInstance(
                            userManager,
                            jobManager,
                            networkUtil,
                            userNotifier
                        )
                    val retrofit = ProtonRetrofitAttachments(
                        okHttpProvider,
                        attachReqInter,
                        userManager,
                        jobManager,
                        networkUtil,
                        authenticator,
                        cookieStore,
                        userNotifier
                    )
                    retrofit.build(endpointUri).build()
                }
                else -> { // secure is default

                    val retrofit = ProtonRetrofitSecure(
                        okHttpProvider,
                        userManager,
                        jobManager,
                        networkUtil,
                        authenticator,
                        cookieStore,
                        userNotifier
                    )
                    retrofit.build(endpointUri).build()
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
    networkUtil: QueueNetworkUtil,
    userNotifier: UserNotifier
) {
    val defaultInterceptor =
        ProtonMailRequestInterceptor.getInstance(userManager, jobManager, networkUtil, userNotifier)
    val spec: List<ConnectionSpec?> = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
        listOf(
            ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .cipherSuites(
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
                ).build()
        )
    } else {
        listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS)
    }
    val serverTimeInterceptor = ServerTimeInterceptor(userManager.openPgp, networkUtil)

    // region gson specs
    private val gsonUcc: Gson = GsonBuilder()
        .setFieldNamingStrategy(FieldNamingPolicy.UPPER_CAMEL_CASE)
        .registerTypeAdapter(MessageRecipient::class.java, MessageRecipient.MessageRecipientSerializer())
        .registerTypeAdapter(MessageRecipient::class.java, MessageRecipient.MessageRecipientDeserializer())
        .registerTypeAdapter(LabelBody::class.java, LabelBody.LabelBodySerializer())
        .registerTypeAdapter(LabelBody::class.java, LabelBody.LabelBodyDeserializer())
        .registerTypeAdapter(BugsBody::class.java, BugsBody.BugsBodySerializer())
        .registerTypeAdapter(BugsBody::class.java, BugsBody.BugsBodyDeserializer())
        .registerTypeAdapter(AttachmentHeaders::class.java, AttachmentHeaders.AttachmentHeadersDeserializer())
        .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
        .create()
    // endregion

    protected abstract fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor?
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
    networkUtil: QueueNetworkUtil,
    private val authenticator: ProtonMailAuthenticator,
    private val cookieStore: ProtonCookieStore?,
    userNotifier: UserNotifier
) : ProtonRetrofit(userManager, jobManager, networkUtil, userNotifier) {
    override fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor?
    ): OkHttpClient {
        // for the public retrofit client it is not needed to have the interceptor, so we tolerate null
        val okHttpClient = okHttpProvider.provideOkHttpClient(
            endpointUri,
            endpointUri,
            THIRTY_SECONDS,
            interceptor,
            authenticator,
            HttpLoggingInterceptor.Level.HEADERS,
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
    networkUtil: QueueNetworkUtil,
    private val authenticator: ProtonMailAuthenticator,
    private val cookieStore: ProtonCookieStore?,
    userNotifier: UserNotifier
) : ProtonRetrofit(userManager, jobManager, networkUtil, userNotifier) {
    override fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor?
    ): OkHttpClient {
        if (interceptor == null) {
            throw RuntimeException("Private OkHttp client is mandatory to be provided with public request interceptor")
        }
        val okHttpClient = okHttpProvider.provideOkHttpClient(
            endpointUri,
            endpointUri,
            THIRTY_SECONDS,
            interceptor,
            authenticator,
            HttpLoggingInterceptor.Level.HEADERS,
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
    networkUtil: QueueNetworkUtil,
    private val authenticator: ProtonMailAuthenticator,
    private val cookieStore: ProtonCookieStore?,
    userNotifier: UserNotifier
) : ProtonRetrofit(userManager, jobManager, networkUtil, userNotifier) {
    override fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor?
    ): OkHttpClient {
        if (interceptor == null) {
            throw RuntimeException("Private OkHttp client is mandatory to be provided with public request interceptor")
        }
        val okHttpClient = okHttpProvider.provideOkHttpClient(
            endpointUri,
            endpointUri,
            ONE_MINUTE,
            interceptor,
            authenticator,
            HttpLoggingInterceptor.Level.HEADERS,
            spec,
            serverTimeInterceptor,
            cookieStore
        )
        return okHttpClient.okClientBuilder.build()
    }
}

class ProtonRetrofitAttachments(
    private val okHttpProvider: OkHttpProvider,
    private val attachReqInter: ProtonMailAttachmentRequestInterceptor,
    userManager: UserManager,
    jobManager: JobManager,
    networkUtil: QueueNetworkUtil,
    private val authenticator: ProtonMailAuthenticator,
    private val cookieStore: ProtonCookieStore?,
    userNotifier: UserNotifier
) : ProtonRetrofit(userManager, jobManager, networkUtil, userNotifier) {
    override fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor?
    ): OkHttpClient {
        if (interceptor == null) {
            throw RuntimeException("Private OkHttp client is mandatory to be provided with public request interceptor")
        }
        val okHttpClient = okHttpProvider.provideOkHttpClient(
            endpointUri,
            endpointUri + ATTACH_PATH,
            THIRTY_SECONDS,
            attachReqInter,
            authenticator,
            HttpLoggingInterceptor.Level.BASIC,
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
    networkUtil: QueueNetworkUtil,
    private val authenticator: ProtonMailAuthenticator,
    private val cookieStore: ProtonCookieStore?,
    userNotifier: UserNotifier
) : ProtonRetrofit(userManager, jobManager, networkUtil, userNotifier) {
    override fun configureOkHttp(
        endpointUri: String,
        interceptor: ProtonMailRequestInterceptor?
    ): OkHttpClient {
        if (interceptor == null) {
            throw RuntimeException("Private OkHttp client is mandatory to be provided with public request interceptor")
        }
        val okHttpClient = okHttpProvider.provideOkHttpClient(
            endpointUri,
            endpointUri,
            THIRTY_SECONDS,
            interceptor,
            authenticator,
            HttpLoggingInterceptor.Level.BASIC,
            spec,
            serverTimeInterceptor,
            cookieStore
        )
        return okHttpClient.okClientBuilder.build()
    }
}

