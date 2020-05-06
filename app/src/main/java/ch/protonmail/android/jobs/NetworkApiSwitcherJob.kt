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
import com.birbit.android.jobqueue.Params

class NetworkApiSwitcherJob(requestUrl : String) : ProtonMailBaseJob(Params(Priority.MEDIUM).groupBy(Constants.JOB_GROUP_MISC)) {

    private val mRequestUrl = requestUrl

    override fun onRun() {
        if(mRequestUrl != Constants.ENDPOINT_URI) {
            // means that we are using the alternative api, and we got a 503
            // so try to ping the regular api, and switch to it
            // TODO: should we ping at all, or just switch apis instead?
            // val pingSuccess = DnsOverHttpsUtil.pingRegularApi()
            // if(pingSuccess) {
            //     ProtonMailApplication.getApplication().changeApiProviders(Constants.ENDPOINT_URI)
            // } else {
            //     // not needed to do anything here
            // }
        }
    }
}