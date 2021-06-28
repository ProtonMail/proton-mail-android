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

package ch.protonmail.android.utils

import android.content.Context
import ch.protonmail.android.core.Constants.DIR_MESSAGE_BODY_DOWNLOADS
import ch.protonmail.android.data.local.model.Message
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

/**
 * A class responsible for saving and reading message bodies from file.
 */

class MessageBodyFileManager @Inject constructor(
    private val applicationContext: Context,
    private val fileHelper: FileHelper,
    private val dispatcherProvider: DispatcherProvider
) {

    fun readMessageBodyFromFile(message: Message): String? {
        val messageId = message.messageId
        if (messageId != null) {
            val messageBodyFile = fileHelper.createFile(
                applicationContext.filesDir.toString() + DIR_MESSAGE_BODY_DOWNLOADS,
                messageId.replace(" ", "_").replace("/", ":")
            )
            return fileHelper.readFromFile(messageBodyFile)
        }
        return null
    }

    suspend fun saveMessageBodyToFile(message: Message, shouldOverwrite: Boolean = true): String? =
        withContext(dispatcherProvider.Io) {
            val messageId = message.messageId
            val messageBody = message.messageBody

            if (messageId != null && messageBody != null) {
                val messageBodyFile = fileHelper.createFile(
                    applicationContext.filesDir.toString() + DIR_MESSAGE_BODY_DOWNLOADS,
                    messageId.replace(" ", "_").replace("/", ":")
                )
                if (shouldOverwrite || !messageBodyFile.exists()) {
                    if (fileHelper.writeToFile(messageBodyFile, messageBody)) {
                        return@withContext "file://${messageBodyFile.absolutePath}"
                    }
                }
            }
            return@withContext null
        }
}
