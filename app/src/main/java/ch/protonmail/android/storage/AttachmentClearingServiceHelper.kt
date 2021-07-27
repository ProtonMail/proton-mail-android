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

package ch.protonmail.android.storage

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import ch.protonmail.android.core.Constants.JOB_INTENT_SERVICE_ID_ATTACHMENT_CLEARING
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.servers.notification.EXTRA_USER_ID
import ch.protonmail.android.storage.AttachmentClearingService.ACTION_CLEAR_CACHE_IMMEDIATELY
import ch.protonmail.android.storage.AttachmentClearingService.ACTION_CLEAR_CACHE_IMMEDIATELY_DELETE_TABLES
import ch.protonmail.android.storage.AttachmentClearingService.ACTION_REGULAR_CHECK
import javax.inject.Inject

class AttachmentClearingServiceHelper @Inject constructor(
    private val context: Context
) {

    fun startRegularClearUpService() {
        val intent = Intent(context, AttachmentClearingService::class.java)
        intent.action = ACTION_REGULAR_CHECK
        JobIntentService.enqueueWork(
            context,
            AttachmentClearingService::class.java,
            JOB_INTENT_SERVICE_ID_ATTACHMENT_CLEARING,
            intent
        )
    }

    fun startClearUpImmediatelyService() {
        val intent = Intent(context, AttachmentClearingService::class.java)
        intent.action = ACTION_CLEAR_CACHE_IMMEDIATELY
        JobIntentService.enqueueWork(
            context,
            AttachmentClearingService::class.java,
            JOB_INTENT_SERVICE_ID_ATTACHMENT_CLEARING,
            intent
        )
    }

    fun startClearUpImmediatelyServiceAndDeleteTables(userId: UserId) {
        val intent = Intent(context, AttachmentClearingService::class.java)
        intent.action = ACTION_CLEAR_CACHE_IMMEDIATELY_DELETE_TABLES
        intent.putExtra(EXTRA_USER_ID, userId.id)
        JobIntentService.enqueueWork(
            context,
            AttachmentClearingService::class.java,
            JOB_INTENT_SERVICE_ID_ATTACHMENT_CLEARING,
            intent
        )
    }
}
