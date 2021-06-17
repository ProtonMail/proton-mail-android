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

package ch.protonmail.android.core

import ch.protonmail.android.core.DetailedException.Extra
import io.sentry.event.EventBuilder
import me.proton.core.domain.entity.UserId

private const val EXTRA_API_ERROR_CODE = "API error code"
private const val EXTRA_API_ERROR_MESSAGE = "API error message"
private const val EXTRA_MESSAGE_ID = "Message id"
private const val EXTRA_USER_ID = "User id"

/**
 * A [Throwable] that enables us to bring more details to Logs.
 *  Particularly it has a set of [DetailedException.Extra] that can be added easily with extension functions present
 *  in this file.
 *
 * @property message the message of the source [Throwable]
 * @property cause the source [Throwable] itself
 */
data class DetailedException(
    override val message: String? = null,
    override val cause: Throwable? = null,
    val extras: Set<Extra> = emptySet()
) : Throwable() {

    fun addToSentryEventBuilder(eventBuilder: EventBuilder) {
        for (extra in extras) eventBuilder.withExtra(extra.name, extra.value)
    }

    data class Extra(val name: String, val value: Any?)
}

fun Throwable?.apiError(code: Int, message: String): DetailedException =
    apiErrorCode(code).apiErrorMessage(message)

fun Throwable?.apiErrorCode(code: Int): DetailedException =
    copyWithExtra(Extra(EXTRA_API_ERROR_CODE, code))

fun Throwable?.apiErrorMessage(message: String): DetailedException =
    copyWithExtra(Extra(EXTRA_API_ERROR_MESSAGE, message))

fun Throwable?.messageId(id: String?): DetailedException =
    copyWithExtra(Extra(EXTRA_MESSAGE_ID, id))

fun Throwable?.userId(userId: UserId): DetailedException =
    copyWithExtra(Extra(EXTRA_USER_ID, userId.id))

// region private functions
private fun Throwable?.copyWithExtra(newExtra: Extra): DetailedException {
    val extras = (this as? DetailedException)?.extras ?: emptySet()
    return toDetailedException().copy(extras = extras + newExtra)
}

private fun Throwable?.toDetailedException(): DetailedException =
    if (this is DetailedException) this
    else DetailedException(this?.message, cause = this)
// endregion
