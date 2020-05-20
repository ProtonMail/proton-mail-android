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
