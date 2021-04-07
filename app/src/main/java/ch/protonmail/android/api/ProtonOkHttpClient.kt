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
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.crypto.ServerTimeInterceptor
import com.datatheorem.android.trustkit.TrustKit
import com.datatheorem.android.trustkit.config.PublicKeyPin
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URL
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

// region constants

const val TLS = "TLS"

// endregion

/**
 *
 * @param cookieStore The cookie store. If set to null, a default InMemory cookie store will be used. Otherwise, for
 * permanent Cookie Store please use instance of [ProtonCookieStore].
 */
sealed class ProtonOkHttpClient(
    timeout: Long,
    interceptor: Interceptor,
    loggingLevel: HttpLoggingInterceptor.Level,
    connectionSpecs: List<ConnectionSpec>,
    serverTimeInterceptor: ServerTimeInterceptor?,
    endpointUri: String,
    cookieStore: ProtonCookieStore? = null
) {

    // the OkHttp builder instance
    val okClientBuilder = OkHttpClient.Builder()
    // TLS Certificate Pinning
    val trustKit = TrustKit.getInstance()
    // val serverHostname = URL(Constants.ENDPOINT_URI).host
    val serverHostname: String = URL(endpointUri).host

    init {
        if (cookieStore != null) {
            val cookieManager = CookieManager(
                cookieStore,
                CookiePolicy.ACCEPT_ALL
            )
            CookieManager.setDefault(cookieManager)
            okClientBuilder.cookieJar(JavaNetCookieJar(cookieManager))
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
    }
}

/**
 * This class defines and configures the OkHttpClient that is used by default.
 */
class DefaultOkHttpClient(
    timeout: Long,
    interceptor: Interceptor,
    loggingLevel: HttpLoggingInterceptor.Level,
    connectionSpecs: List<ConnectionSpec>,
    serverTimeInterceptor: ServerTimeInterceptor?,
    cookieStore: ProtonCookieStore?
) : ProtonOkHttpClient(
    timeout,
    interceptor,
    loggingLevel,
    connectionSpecs,
    serverTimeInterceptor,
    Constants.ENDPOINT_URI,
    cookieStore
) {

    init {
        okClientBuilder.sslSocketFactory(
            trustKit.getSSLSocketFactory(serverHostname),
            trustKit.getTrustManager(serverHostname)
        )
    }
}

/**
 * This class defines and configures the OkHttpClient that is used with proxies (when the default
 * is not available (api not accessible or banned).
 */
class ProxyOkHttpClient(
    timeout: Long,
    interceptor: Interceptor,
    loggingLevel: HttpLoggingInterceptor.Level,
    connectionSpecs: List<ConnectionSpec>,
    serverTimeInterceptor: ServerTimeInterceptor?,
    endpointUri: String,
    pinnedKeyHashes: List<String>,
    cookieStore: ProtonCookieStore?
) : ProtonOkHttpClient(
    timeout,
    interceptor,
    loggingLevel,
    connectionSpecs,
    serverTimeInterceptor,
    endpointUri,
    cookieStore
) {

    init {
        val trustManager = PinningTrustManager(pinnedKeyHashes)
        val sslContext = SSLContext.getInstance(TLS)
        sslContext.init(null, arrayOf(trustManager), null)
        okClientBuilder.sslSocketFactory(sslContext.socketFactory, trustManager)
        okClientBuilder.hostnameVerifier(
            // Verification is based solely on SPKI pinning of leaf certificate
            HostnameVerifier { _, _ -> true }
        )
    }

    class PinningTrustManager(pinnedKeyHashes: List<String>) : X509TrustManager {

        private val pins: List<PublicKeyPin> = pinnedKeyHashes.map { PublicKeyPin(it) }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            // TODO: is that enough? need security review
            if (PublicKeyPin(chain.first()) !in pins)
                throw CertificateException("Pin verification failed")
        }

        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
            throw CertificateException("Client certificates not supported!")
        }

        override fun getAcceptedIssuers(): Array<X509Certificate?>? = arrayOfNulls(0)
    }
}
