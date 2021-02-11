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

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.Logger
import ch.protonmail.android.worker.FetchContactsDataWorker
import ch.protonmail.android.worker.FetchContactsEmailsWorker
import ch.protonmail.android.worker.FetchMailSettingsWorker
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.config.Configuration
import com.birbit.android.jobqueue.log.CustomLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JobModule {

    @Provides
    @Singleton
    fun jobManager(app: ProtonMailApplication, queueNetworkUtil: QueueNetworkUtil): JobManager {
        val config = Configuration.Builder(app)
            .minConsumerCount(1)
            .consumerKeepAlive(120) // 2 minutes
            .networkUtil(queueNetworkUtil)
            .customLogger(logger)
            .build()

        return JobManager(config)
    }

    private val logger = object : CustomLogger {
        override fun v(text: String, vararg args: Any?) {
            if (isDebugEnabled) {
                Logger.doLog(TAG, text.format(*args))
            }
        }

        private val TAG = "JOBS"

        override fun isDebugEnabled() = false

        override fun d(text: String, vararg args: Any) {
            if (isDebugEnabled) {
                Timber.i(text.format(*args))
            }
        }

        override fun e(t: Throwable, text: String, vararg args: Any) {
            Timber.e(t, text.format(*args))
        }

        override fun e(text: String, vararg args: Any) {
            Timber.e(text.format(*args))
        }
    }

}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface JobEntryPoint {

    fun accountManager(): AccountManager
    fun apiManager(): ProtonMailApiManager
    fun fetchContactsEmailsWorkerEnqueuer(): FetchContactsEmailsWorker.Enqueuer
    fun fetchContactsDataWorkerEnqueuer(): FetchContactsDataWorker.Enqueuer
    fun jobManager(): JobManager
    fun messageDetailsRepository(): MessageDetailsRepository
    fun queueNetworkUtil(): QueueNetworkUtil
    fun userManager(): UserManager
    fun fetchMailSettingsWorkerEnqueuer(): FetchMailSettingsWorker.Enqueuer
}
