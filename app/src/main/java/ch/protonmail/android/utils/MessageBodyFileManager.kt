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
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.Constants.DIR_MESSAGE_BODY_DOWNLOADS
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * A class responsible for saving and reading message bodies from file.
 */

class MessageBodyFileManager @Inject constructor(
    private val applicationContext: Context,
    private val dispatcherProvider: DispatcherProvider
) {

    suspend fun readMessageBodyFromFile(message: Message): String? =
        withContext(dispatcherProvider.Io) {
            val messageId = message.messageId
            if (messageId != null) {
                val messageBodyFile = File(
                    applicationContext.filesDir.toString() + DIR_MESSAGE_BODY_DOWNLOADS,
                    messageId.replace(" ", "_").replace("/", ":")
                )
                return@withContext runCatching {
                    FileInputStream(messageBodyFile)
                        .bufferedReader()
                        .use { it.readText() }
                }.fold(
                    onSuccess = { it },
                    onFailure = { null }
                )
            }
            return@withContext null
        }

    suspend fun saveMessageBodyToFile(message: Message, shouldOverwrite: Boolean = true): String? =
        withContext(dispatcherProvider.Io) {
            val messageId = message.messageId
            val messageBody = message.messageBody
            val messageBodyDirectory = File(applicationContext.filesDir.toString() + DIR_MESSAGE_BODY_DOWNLOADS)
            messageBodyDirectory.mkdirs()

            if (messageId != null && messageBody != null) {
                val messageBodyFile = File(
                    messageBodyDirectory,
                    messageId.replace(" ", "_").replace("/", ":")
                )
                if (shouldOverwrite || !messageBodyFile.exists()) {
                    return@withContext runCatching {
                        FileOutputStream(messageBodyFile)
                            .use { it.write(messageBody.toByteArray()) }
                    }.fold(
                        onSuccess = { "file://${messageBodyFile.absolutePath}" },
                        onFailure = { null }
                    )
                }
            }
            return@withContext null
        }
}
