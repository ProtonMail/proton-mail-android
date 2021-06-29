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

import android.net.Uri
import androidx.core.net.toUri
import ch.protonmail.android.di.AppDataDirectory
import ch.protonmail.android.utils.DateUtil
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import java.io.File
import javax.inject.Inject

/**
 * Create an [Uri] for a new photo to be taken from Camera
 */
class GetNewPhotoUri @Inject constructor(
    @AppDataDirectory private val dataDirectory: File,
    private val dispatchers: DispatcherProvider
) {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend operator fun invoke(): Uri = withContext(dispatchers.Io) {
        File.createTempFile(DateUtil.generateTimestamp(), "jpg", dataDirectory).toUri()
    }
}
