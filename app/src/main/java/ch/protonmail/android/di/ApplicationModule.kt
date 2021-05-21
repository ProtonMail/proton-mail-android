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

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import androidx.work.WorkManager
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.api.DnsOverHttpsProviderRFC8484
import ch.protonmail.android.api.OkHttpProvider
import ch.protonmail.android.api.ProtonRetrofitBuilder
import ch.protonmail.android.api.cookie.ProtonCookieStore
import ch.protonmail.android.api.models.contacts.receive.ContactLabelFactory
import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.api.models.factories.IConverterFactory
import ch.protonmail.android.api.models.messages.receive.AttachmentFactory
import ch.protonmail.android.api.models.messages.receive.IAttachmentFactory
import ch.protonmail.android.api.models.messages.receive.ServerLabel
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.attachments.Armorer
import ch.protonmail.android.attachments.OpenPgpArmorer
import ch.protonmail.android.contacts.list.listView.ContactItemListFactory
import ch.protonmail.android.contacts.repositories.andorid.baseInfo.AndroidContactsLoaderCallbacksFactory
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.data.local.model.ContactLabel
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.usecase.DownloadFile
import ch.protonmail.android.servers.notification.NotificationServer
import ch.protonmail.android.utils.BuildInfo
import ch.protonmail.android.utils.base64.AndroidBase64Encoder
import ch.protonmail.android.utils.base64.Base64Encoder
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.notifier.AndroidUserNotifier
import ch.protonmail.android.utils.notifier.UserNotifier
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
import me.proton.core.accountmanager.domain.SessionManager
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
    fun alarmReceiver() = AlarmReceiver()

    @Provides
    @AlternativeApiPins
    fun alternativeApiPins() = listOf(
        "EU6TS9MO0L/GsDHvVc9D5fChYLNy5JdGYpJw0ccgetM=",
        "iKPIHPnDNqdkvOnTClQ8zQAIKG0XavaPkcEo0LBAABA=",
        "MSlVrBCdL0hKyczvgYVSRNm88RicyY04Q2y5qrBt0xA=",
        "C2UxW0T1Ckl9s+8cXfjXxlEqwAfPM4HiW2y3UdtBeCw="
    )

    @Provides
    fun androidContactsLoaderCallbacksFactory(context: Context): AndroidContactsLoaderCallbacksFactory =
        AndroidContactsLoaderCallbacksFactory(context, ContactItemListFactory()::convert)

    @Provides
    @AppCacheDirectory
    fun appCacheDirectory(context: Context): File =
        context.cacheDir

    @Provides
    @AppDataDirectory
    fun appDataDirectory(context: Context): File =
        File(context.applicationInfo.dataDir)

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
    fun contentResolver(context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @CurrentUserCrypto
    fun currentUserCrypto(
        userManager: UserManager,
        openPgp: OpenPGP,
        @CurrentUserId userId: Id
    ): UserCrypto = UserCrypto(userManager, openPgp, userId)

    @Provides
    @CurrentUserId
    fun currentUserId(userManager: UserManager): Id =
        userManager.requireCurrentUserId()

    @Provides
    @CurrentUserMailSettings
    fun currentUserMailSettings(
        userManager: UserManager
    ) = userManager.requireCurrentUserMailSettingsBlocking()

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
        userManager: UserManager
    ) = userManager.requireCurrentUserMailSettingsBlocking()

    @Provides
    @Singleton
    fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides
    @Singleton
    fun protonRetrofitBuilder(
        context: Context,
        userManager: UserManager,
        jobManager: JobManager,
        networkUtil: QueueNetworkUtil,
        okHttpProvider: OkHttpProvider,
        @DefaultSharedPreferences prefs: SharedPreferences,
        userNotifier: UserNotifier,
        sessionManager: SessionManager
    ): ProtonRetrofitBuilder {

        // userManager.user.allowSecureConnectionsViaThirdParties)
        val user = userManager.currentLegacyUser
        val dnsOverHttpsHost =
            if (user != null && !user.usingDefaultApi)
                Proxies.getInstance(null, prefs).getCurrentWorkingProxyDomain()
            else Constants.ENDPOINT_URI

        return ProtonRetrofitBuilder(
            userManager,
            jobManager,
            networkUtil,
            ProtonCookieStore(context),
            userNotifier,
            sessionManager
        ).apply { rebuildMapFor(okHttpProvider, dnsOverHttpsHost) }
    }

    @Provides
    @Singleton
    fun proxies(@DefaultSharedPreferences prefs: SharedPreferences): Proxies =
        Proxies.getInstance(prefs = prefs)

    @Provides
    fun workManager(context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    fun connectivityManager(context: Context): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    fun contactLabelFactory(): IConverterFactory<ServerLabel, ContactLabel> = ContactLabelFactory()

    @Provides
    fun buildInfo() = BuildInfo(
        Build.MODEL,
        Build.BRAND,
        BuildConfig.DEBUG,
        Build.VERSION.SDK_INT,
        BuildConfig.VERSION_NAME,
        Build.VERSION.RELEASE
    )

    @Provides
    fun provideUserCrypto(userManager: UserManager): UserCrypto =
        UserCrypto(userManager, userManager.openPgp, userManager.requireCurrentUserId())

    @Provides
    fun providesArmorer(): Armorer = OpenPgpArmorer()

    @Provides
    fun attachmentFactory(): IAttachmentFactory = AttachmentFactory()

    @Provides
    fun base64Encoder(): Base64Encoder = AndroidBase64Encoder()

    @Provides
    fun userNotifier(
        notificationServer: NotificationServer,
        userManager: UserManager,
        context: Context
    ): UserNotifier = AndroidUserNotifier(notificationServer, userManager, context, dispatcherProvider())
}

@Module
@AssistedModule
@InstallIn(SingletonComponent::class)
interface AssistedApplicationModule
