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

package ch.protonmail.android.attachments.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import ch.protonmail.android.attachments.domain.model.UriPair
import ch.protonmail.android.di.AppCacheDirectory
import ch.protonmail.android.utils.DateUtil
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import java.io.File
import javax.inject.Inject

/**
 * Create an [Uri] for a new photo to be taken from Camera
 */
class GetNewPhotoUri @Inject constructor(
    private val context: Context,
    @AppCacheDirectory private val cacheDirectory: File,
    private val dispatchers: DispatcherProvider
) {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend operator fun invoke(): UriPair = withContext(dispatchers.Io) {
        val file = File.createTempFile(DateUtil.generateTimestamp(), ".jpg", cacheDirectory)
        val secureUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        return@withContext UriPair(secure = secureUri, insecure = file.toUri())
    }
}
