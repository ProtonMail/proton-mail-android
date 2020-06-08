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

import ch.protonmail.android.api.services.MessagesService.Companion.startFetchContactGroups
import ch.protonmail.android.api.services.MessagesService.Companion.startFetchFirstPage
import ch.protonmail.android.api.services.MessagesService.Companion.startFetchFirstPageByLabel
import ch.protonmail.android.api.services.MessagesService.Companion.startFetchLabels
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType
import com.birbit.android.jobqueue.Params

/**
 * Persistent job to process certain jobs after first login and which requires network, so they can
 * be executed after we get network access if were offline before
 */
class FetchByLocationJob(
    private val location: MessageLocationType,
    private val labelId: String?,
    private val includeLabels: Boolean,
    private val uuid: String?
) : ProtonMailBaseJob(Params(Priority.MEDIUM).groupBy(Constants.JOB_GROUP_MESSAGE)) {
    @Throws(Throwable::class)
    override fun onRun() {
        when (location) {
            MessageLocationType.LABEL, MessageLocationType.LABEL_OFFLINE, MessageLocationType.LABEL_FOLDER -> {
                messageDetailsRepository.deleteMessagesByLabel(labelId!!)
                startFetchFirstPageByLabel(location, labelId)
            }
            else -> {
                messageDetailsRepository.deleteMessagesByLocation(location)
                startFetchFirstPage(location, false, uuid)
                if (includeLabels) {
                    startFetchLabels()
                    startFetchContactGroups()
                }
            }
        }
    }
}
