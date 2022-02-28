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
package ch.protonmail.android.storage

import android.content.Intent
import androidx.core.app.JobIntentService
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.utils.Logger
import java.io.File

// region constants
private const val TAG_MESSAGE_BODY_CLEARING_SERVICE = "MessageBodyClearing"
private const val ACTION_CLEAR_CACHE = "ACTION_CLEAR_CACHE"
// endregion

class MessageBodyClearingService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        when (intent.action) {
            ACTION_CLEAR_CACHE -> {
                File(applicationContext.filesDir.toString() + Constants.DIR_MESSAGE_BODY_DOWNLOADS).listFiles()?.forEach {
                    Logger.doLog(TAG_MESSAGE_BODY_CLEARING_SERVICE, "deleting message body ${it.name}")
                    it.delete()
                }
            }
        }
    }

    companion object {

        fun startClearUpService() {
            val context = ProtonMailApplication.getApplication()
            val intent = Intent(context, MessageBodyClearingService::class.java)
            intent.action = ACTION_CLEAR_CACHE
            JobIntentService.enqueueWork(context, MessageBodyClearingService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGE_BODY_CLEARING, intent)
        }
    }
}
