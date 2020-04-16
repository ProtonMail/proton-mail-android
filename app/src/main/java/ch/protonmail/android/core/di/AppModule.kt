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

import android.content.Context
import android.content.SharedPreferences
import ch.protonmail.android.activities.messageDetails.MessageRenderer
import ch.protonmail.android.adapters.swipe.SwipeProcessor
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabase
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabaseFactory
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import ch.protonmail.android.api.segments.contact.ContactEmailsManager
import ch.protonmail.android.api.segments.event.EventManager
import ch.protonmail.android.bl.HtmlProcessor
import ch.protonmail.android.core.*
import ch.protonmail.android.jobs.ProtonMailBaseJob
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.Logger
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.libs.core.preferences.get
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.config.Configuration
import com.birbit.android.jobqueue.log.CustomLogger
import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
class AppModule(val app: ProtonMailApplication) {

    @Provides
    @Singleton
    fun provideApplication(): ProtonMailApplication = app

    @Provides
    @Singleton
    fun provideApplicationContext(): Context = app.applicationContext

    @Provides
    @Singleton
    fun provideNetworkUtil(): QueueNetworkUtil = QueueNetworkUtil(app)

    @Provides
    @Singleton
    fun provideHtmlProcessor(): HtmlProcessor = HtmlProcessor()

    @Provides
    fun provideSwipeProcessor(): SwipeProcessor = SwipeProcessor()

    @Provides
    @Singleton
    fun provideOpenPGP(): OpenPGP = OpenPGP()

    @Provides
    @Singleton
    fun provideEventManager(): EventManager = EventManager()

    @Provides
    @Singleton
    fun provideContactEmailsManager(protonMailApi: ProtonMailApi, databaseProvider: DatabaseProvider): ContactEmailsManager
            = ContactEmailsManager(protonMailApi, databaseProvider)

    @Provides
    @Singleton
    fun provideDatabaseProvider(context: Context): DatabaseProvider {
        return DatabaseProvider(context)
    }

    @Provides
    @Singleton
    @Named("messages")
    fun provideMessagesDatabase(@Named("messages_factory") messagesDatabaseFactory: MessagesDatabaseFactory): MessagesDatabase {
        return messagesDatabaseFactory.getDatabase()
    }

    @Provides
    @Singleton
    @Named("messages_factory")
    fun provideMessagesDatabaseFactory(app: ProtonMailApplication): MessagesDatabaseFactory {
        return MessagesDatabaseFactory.getInstance(app)
    }

    @Provides
    fun provideMessageRendererFactory(app: ProtonMailApplication) = MessageRenderer.Factory(
            File(app.filesDir, Constants.DIR_EMB_ATTACHMENT_DOWNLOADS)
    )

    @Provides
    @Singleton
    @Named("messages_search")
    fun provideSearchMessagesDatabase(@Named("messages_search_factory") messagesDatabaseFactory: MessagesDatabaseFactory): MessagesDatabase
            = messagesDatabaseFactory.getDatabase()

    @Provides
    @Singleton
    @Named("messages_search_factory")
    fun provideSearchMessagesDatabaseFactory(app: ProtonMailApplication): MessagesDatabaseFactory {
        return MessagesDatabaseFactory.getSearchDatabase(app)
    }

    @Provides
    @Singleton
    fun providePendingActionsDatabase(pendingActionsDatabaseFactory: PendingActionsDatabaseFactory): PendingActionsDatabase
        = pendingActionsDatabaseFactory.getDatabase()

    @Provides
    @Singleton
    fun providePendingActionsDatabaseFactory(app: ProtonMailApplication): PendingActionsDatabaseFactory {
        return PendingActionsDatabaseFactory.getInstance(app)
    }

    @Provides
    @Singleton
    fun provideAttachmentMetadataDatabase(app: ProtonMailApplication): AttachmentMetadataDatabase {
        return AttachmentMetadataDatabaseFactory.getInstance(app).getDatabase()
    }

    @Inject
    @Provides
    @Singleton
    fun provideApi(userManager: UserManager, jobManager: JobManager, networkUtil: QueueNetworkUtil)
            : ProtonMailApi = ProtonMailApi(userManager, jobManager, networkUtil)

    @Provides
    @Singleton
    fun provideUserManager(@Named(Constants.PrefsType.DEFAULT) prefs: SharedPreferences,
                           @Named(Constants.PrefsType.BACKUP) backupPrefs: SharedPreferences,
                           app: ProtonMailApplication): UserManager = UserManager(prefs, backupPrefs, app.applicationContext)

    @Provides
    @Singleton
    fun provideNetworkResults(): NetworkResults = NetworkResults()

    @Provides
    @Singleton
    fun provideBigContentHolder(): BigContentHolder = BigContentHolder()

    @Provides
    @Singleton
    @Named(Constants.PrefsType.DEFAULT)
    fun provideDefaultSharedPreferences(): SharedPreferences = app.defaultSharedPreferences

    @Provides
    @Singleton
    @Named(Constants.PrefsType.BACKUP)
    fun provideBackupSharedPreferences(): SharedPreferences = app.getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideJobManager(queueNetworkUtil: QueueNetworkUtil): JobManager {
        val config = Configuration.Builder(app)
                .minConsumerCount(1)
                .consumerKeepAlive(120) // 2 minutes
                .networkUtil(queueNetworkUtil)
                .injector { job ->
                    if (job is ProtonMailBaseJob) {
                        app.appComponent.inject(job)
                    }
                }
                .customLogger(object : CustomLogger {
                    override fun v(text: String, vararg args: Any?) {
                        Logger.doLog(TAG, String.format(text, *args))
                    }

                    private val TAG = "JOBS"

                    override fun isDebugEnabled(): Boolean {
                        return AppUtil.isDebug()
                    }

                    override fun d(text: String, vararg args: Any) {
                        Logger.doLog(TAG, String.format(text, *args))
                    }

                    override fun e(t: Throwable, text: String, vararg args: Any) {
                        Logger.doLogException(TAG, String.format(text, *args), t)
                    }

                    override fun e(text: String, vararg args: Any) {
                        Logger.doLogException(TAG, String.format(text, *args), null)
                    }
                })
                .build()

        return JobManager(config)
    }


    /* * * * * SORTED ALPHABETICALLY BELOW!! * * * * */

    @Provides
    @Named(CURRENT_USERNAME)
    fun provideCurrentUserUsername(
            @Named(Constants.PrefsType.DEFAULT) prefs: SharedPreferences
    ): String = prefs[PREF_USERNAME]!!

    @Provides
    fun provideMailSettings(
            userManager: UserManager,
            @Named(CURRENT_USERNAME) username: String
    ) = userManager.getMailSettings(username)

    /* * * * * SORTED ALPHABETICALLY ABOVE!! * * * * */
}

// Dependency names
// region User
private const val CURRENT_USERNAME = "username"
// endregion
