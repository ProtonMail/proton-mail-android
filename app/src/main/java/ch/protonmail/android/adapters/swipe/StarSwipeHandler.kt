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
package ch.protonmail.android.adapters.swipe

import ch.protonmail.android.api.models.SimpleMessage
import ch.protonmail.android.core.Constants
import ch.protonmail.android.jobs.PostStarJob
import ch.protonmail.android.jobs.PostUnstarJob
import com.birbit.android.jobqueue.Job

/**
 * Created by dkadrikj on 9.7.15.
 */
class StarSwipeHandler : ISwipeHandler {

    override fun handleSwipe(message: SimpleMessage, currentLocation: String?): Job {
        return if (!message.isStarred) {
            PostStarJob(listOf(message.messageId))
        } else {
            PostUnstarJob(listOf(message.messageId))
        }
    }

    override fun handleUndo(message: SimpleMessage, messageLocation: Constants.MessageLocationType, currentLocation: String?): Job {
        return if (message.isStarred) {
            PostStarJob(listOf(message.messageId))
        } else {
            PostUnstarJob(listOf(message.messageId))
        }
    }
}
