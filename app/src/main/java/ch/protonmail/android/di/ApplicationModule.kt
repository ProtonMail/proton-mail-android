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

package ch.protonmail.android.di

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.api.DnsOverHttpsProviderRFC8484
import ch.protonmail.android.api.OkHttpProvider
import ch.protonmail.android.api.ProtonRetrofitBuilder
import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.api.models.messages.receive.AttachmentFactory
import ch.protonmail.android.api.models.messages.receive.IAttachmentFactory
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
import ch.protonmail.android.domain.usecase.DownloadFile
import ch.protonmail.android.notifications.presentation.utils.NotificationServer
import ch.protonmail.android.utils.BuildInfo
import ch.protonmail.android.utils.base64.AndroidBase64Encoder
import ch.protonmail.android.utils.base64.Base64Encoder
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ProtonCookieStore
import me.proton.core.network.domain.server.ServerTimeListener
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
    fun androidContactsLoaderCallbacksFactory(context: Context): AndroidContactsLoaderCallbacksFactory =
        AndroidContactsLoaderCallbacksFactory(context, ContactItemListFactory()::convert)

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
    @CurrentUserId
    fun currentUserId(userManager: UserManager): UserId =
        userManager.requireCurrentUserId()

    @Provides
    @Singleton
    @DefaultSharedPreferences
    fun defaultSharedPreferences(app: ProtonMailApplication): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(app)

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
        userManager: UserManager,
        jobManager: JobManager,
        serverTimeListener: ServerTimeListener,
        networkUtil: QueueNetworkUtil,
        cookieStore: ProtonCookieStore,
        okHttpProvider: OkHttpProvider,
        @DefaultSharedPreferences prefs: SharedPreferences,
        userNotifier: UserNotifier,
        sessionManager: SessionManager,
        @BaseUrl baseUrl: String
    ): ProtonRetrofitBuilder {

        // userManager.user.allowSecureConnectionsViaThirdParties)
        val user = userManager.currentLegacyUser
        val dnsOverHttpsHost =
            if (user != null && !user.usingDefaultApi)
                Proxies.getInstance(null, prefs).getCurrentWorkingProxyDomain()
            else baseUrl

        return ProtonRetrofitBuilder(
            userManager,
            jobManager,
            serverTimeListener,
            networkUtil,
            cookieStore,
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

    // Coroutine scope to use e.g. in EventHandler, something that has no access to viewModel, following ideas from:
    // https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
    @Provides
    @Singleton
    fun applicationCoroutineScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())
}

@Module
@AssistedModule
@InstallIn(SingletonComponent::class)
interface AssistedApplicationModule
