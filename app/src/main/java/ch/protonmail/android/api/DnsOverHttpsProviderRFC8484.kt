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

import android.util.Base64
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.api.cookie.ProtonCookieStore
import ch.protonmail.android.api.segments.DnsOverHttpsRetrofitApi
import ch.protonmail.android.api.utils.Json
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
// import org.apache.commons.codec.binary.Base32
import org.minidns.dnsmessage.DnsMessage
import org.minidns.dnsmessage.Question
import org.minidns.record.Record
import org.minidns.record.TXT
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

class DnsOverHttpsProviderRFC8484(
    private val baseUrl: String,
    cookieStore: ProtonCookieStore? = null
) {

    private val api: DnsOverHttpsRetrofitApi

    init {
        require(baseUrl.endsWith('/'))

        val converterFactory = JacksonConverterFactory.create(Json.MAPPER)
        val httpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_S, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            httpClientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
            )
        }
        if (cookieStore != null) {
            val cookieManager = CookieManager(
                cookieStore,
                CookiePolicy.ACCEPT_ALL
            )
            CookieManager.setDefault(cookieManager)
            httpClientBuilder.cookieJar(JavaNetCookieJar(cookieManager))
        }

        val okClient = httpClientBuilder.build()
        api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okClient)
            .addConverterFactory(converterFactory)
            .build()
            .create(DnsOverHttpsRetrofitApi::class.java)
    }

    suspend fun getAlternativeBaseUrls(): List<String>? {
        val question = Question("dMFYGSLTQOJXXI33ONVQWS3BOMNUA.protonpro.xyz", Record.TYPE.TXT)
        val queryMessage = DnsMessage.builder()
            .setRecursionDesired(true)
            .setQuestion(question)
            .build()
        val queryMessageBase64 = Base64.encodeToString(
            queryMessage.toArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

        val response = api.getServers(baseUrl.removeSuffix("/"), queryMessageBase64)
        if (response.isSuccessful) {
            val answerData = response.body()?.bytes()
            val answerMessage = DnsMessage(answerData)
            val answers = answerMessage.answerSection
            return answers
                .mapNotNull { (it.payload as? TXT)?.text }
                .map { "https://${it}/" }
                .takeIf { it.isNotEmpty() }
        }
        return null
    }

    companion object {
        private const val TIMEOUT_S = 10L
    }
}
