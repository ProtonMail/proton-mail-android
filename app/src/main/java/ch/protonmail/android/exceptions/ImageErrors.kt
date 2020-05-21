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
package ch.protonmail.android.exceptions

import ch.protonmail.android.R
import studio.forface.viewstatestore.ViewState

internal class BadImageUrlException(url: String) : Exception("Malformed url: $url")

internal class BadImageUrlError(throwable: BadImageUrlException) :
    ViewState.Error(throwable, R.string.error_image_bad_url)

internal class ImageNotFoundException(cause: Throwable, url: String) :
    Exception("Cannot resolve image at url: $url", cause)

internal class ImageNotFoundError(throwable: ImageNotFoundException) :
    ViewState.Error(throwable, R.string.error_image_not_found)
