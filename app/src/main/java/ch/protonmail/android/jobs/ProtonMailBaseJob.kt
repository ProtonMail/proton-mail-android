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
package ch.protonmail.android.jobs

import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.di.JobEntryPoint
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import dagger.hilt.EntryPoints
import me.proton.core.domain.entity.UserId
import timber.log.Timber

abstract class ProtonMailBaseJob @JvmOverloads protected constructor(
    params: Params?,
    userId: UserId? = null
) : Job(params) {

    protected val entryPoint
        get() = EntryPoints.get(ProtonMailApplication.getApplication(), JobEntryPoint::class.java)

    // This property is serialized by JobQueue library.
    private var userIdString: String? = userId?.id ?: getUserManager().currentUserId?.id

    protected val userId: UserId?
        get() = userIdString?.let(::UserId)
            ?: getUserManager().currentUserId

    protected fun requireUserId(): UserId =
        requireNotNull(userId)

    protected fun getAccountManager() = entryPoint.accountManager()
    protected fun getApi() = entryPoint.apiManager()
    protected fun getJobManager() = entryPoint.jobManager()
    protected fun getMessageDetailsRepository() = entryPoint.messageDetailsRepository()
    protected fun getQueueNetworkUtil() = entryPoint.queueNetworkUtil()
    protected fun getUserManager() = entryPoint.userManager()
    protected fun getUserAddressManager() = entryPoint.userAddressManager()

    override fun onAdded() {}

    override fun getRetryLimit() = Constants.JOB_RETRY_LIMIT_DEFAULT

    /**
     * Use [.onProtonCancel] for custom cancellation logic.
     */
    override fun onCancel(cancelReason: Int, throwable: Throwable?) {
        Timber.d(throwable, "${javaClass.name} cancelled, reason = $cancelReason, retryLimit = $retryLimit")
        try {
            onProtonCancel(cancelReason, throwable)

        } catch (ignored: Exception) {
            Timber.e(ignored, "%s threw exception in onProtonCancel", this.javaClass.name)
        }
    }

    /**
     * Use this method for custom logic when Jobs get cancelled.
     */
    protected open fun onProtonCancel(cancelReason: Int, throwable: Throwable?) {}

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint? =
        null
}
