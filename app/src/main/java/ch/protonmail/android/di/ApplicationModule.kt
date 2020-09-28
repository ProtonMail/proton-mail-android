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

package ch.protonmail.android.di

import android.content.Context
import android.content.SharedPreferences
import androidx.work.WorkManager
import ch.protonmail.android.api.DnsOverHttpsProviderRFC8484
import ch.protonmail.android.api.OkHttpProvider
import ch.protonmail.android.api.ProtonRetrofitBuilder
import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.PREF_USERNAME
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.usecase.DownloadFile
import ch.protonmail.android.utils.extensions.app
import com.birbit.android.jobqueue.JobManager
import com.squareup.inject.assisted.dagger2.AssistedModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.kotlin.DispatcherProvider
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    fun context(@ApplicationContext context: Context) = context

    @Provides
    fun protonMailApplication(context: Context): ProtonMailApplication =
        context.app

    @Provides
    @AlternativeApiPins
    fun alternativeApiPins() = listOf(
        "EU6TS9MO0L/GsDHvVc9D5fChYLNy5JdGYpJw0ccgetM=",
        "iKPIHPnDNqdkvOnTClQ8zQAIKG0XavaPkcEo0LBAABA=",
        "MSlVrBCdL0hKyczvgYVSRNm88RicyY04Q2y5qrBt0xA=",
        "C2UxW0T1Ckl9s+8cXfjXxlEqwAfPM4HiW2y3UdtBeCw="
    )

    @Provides
    @AttachmentsDirectory
    fun attachmentsDirectory(context: Context) =
        File(context.filesDir, Constants.DIR_EMB_ATTACHMENT_DOWNLOADS)

    @Provides
    @Singleton
    @BackupSharedPreferences
    fun backupSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE)

    @Provides
    @CurrentUsername
    fun currentUserUsername(
        @DefaultSharedPreferences prefs: SharedPreferences
    ): String = prefs[PREF_USERNAME]!!

    @Provides
    @Singleton
    @DefaultSharedPreferences
    fun defaultSharedPreferences(app: ProtonMailApplication): SharedPreferences =
        app.defaultSharedPreferences

    @Provides
    fun dispatcherProvider() = object : DispatcherProvider {
        override val Io = Dispatchers.IO
        override val Comp = Dispatchers.Default
        override val Main = Dispatchers.Main
    }

    @Provides
    @Singleton
    @DohProviders
    fun dohProviders() = arrayOf(
        DnsOverHttpsProviderRFC8484("https://dns11.quad9.net/dns-query/"),
        DnsOverHttpsProviderRFC8484("https://dns.google/dns-query/")
    )

    // TODO move to data module into a proper class
    @Provides
    @Suppress("BlockingMethodInNonBlockingContext") // Network call launched in a Coroutines, which are not
    //                                                          recognised as non blocking scope
    fun downloadFile(dispatcherProvider: DispatcherProvider) = object : DownloadFile {
        override suspend fun invoke(url: String): InputStream {
            return withContext(dispatcherProvider.Io) {
                // hardcoded timeout - this network call should be implemented in the data layer anyway
                withTimeout(10_000) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.inputStream
                }
            }
        }
    }

    @Provides
    @AppCoroutineScope
    fun globalCoroutineScope(): CoroutineScope = GlobalScope

    @Provides
    fun mailSettings(
        userManager: UserManager,
        @CurrentUsername username: String
    ) = userManager.getMailSettings(username)

    @Provides
    @Singleton
    fun protonRetrofitBuilder(
        userManager: UserManager,
        jobManager: JobManager,
        networkUtil: QueueNetworkUtil,
        okHttpProvider: OkHttpProvider,
        @DefaultSharedPreferences prefs: SharedPreferences
    ): ProtonRetrofitBuilder {

        // userManager.user.allowSecureConnectionsViaThirdParties)
        val dnsOverHttpsHost =
            if (!userManager.user.usingDefaultApi)
                Proxies.getInstance(null, prefs).getCurrentWorkingProxyDomain()
            else Constants.ENDPOINT_URI

        // val dnsOverHttpsHost = Proxies.getInstance(null, prefs).getCurrentWorkingProxyDomain()

        return ProtonRetrofitBuilder(userManager, jobManager, networkUtil)
            .apply { rebuildMapFor(okHttpProvider, dnsOverHttpsHost) }
    }

    @Provides
    @Singleton
    fun proxies(@DefaultSharedPreferences prefs: SharedPreferences): Proxies =
        Proxies.getInstance(prefs = prefs)

    @Provides
    fun workManager(context: Context): WorkManager =
        WorkManager.getInstance(context)
}

@Module
@AssistedModule
@InstallIn(SingletonComponent::class)
interface AssistedApplicationModule
