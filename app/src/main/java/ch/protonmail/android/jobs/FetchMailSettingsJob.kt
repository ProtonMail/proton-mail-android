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

import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint

import ch.protonmail.android.core.Constants

/**
 * Created by dino on 2/4/18.
 */

class FetchMailSettingsJob : ProtonMailBaseJob(Params(Priority.MEDIUM).groupBy(Constants.JOB_GROUP_MISC)) {

    @Throws(Throwable::class)
    override fun onRun() {
        try {
            val mailSettingsResponse = mApi.fetchMailSettings()
            val mailSettings = mailSettingsResponse.mailSettings
            if (mailSettings != null) {
                mUserManager.mailSettings = mailSettings
            } else {
                throw NullPointerException()
            }
        } catch (e: Exception) {
            // noop
        }

    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint? {
        return if (throwable.cause is java.lang.NullPointerException)
            RetryConstraint.RETRY
        else
            RetryConstraint.CANCEL
    }
}
