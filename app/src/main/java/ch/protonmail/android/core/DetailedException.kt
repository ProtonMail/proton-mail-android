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

package ch.protonmail.android.core

import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult

private const val EXTRA_API_ERROR_CODE = "API error code"
private const val EXTRA_API_ERROR_MESSAGE = "API error message"
private const val EXTRA_MESSAGE_ID = "Message id"
private const val EXTRA_USER_ID = "User id"

/**
 * A [Throwable] that enables us to bring more details to Logs.
 *  Particularly it has a set of Extra that can be added easily with extension functions present in this file.
 *
 * @property message the message of the source [Throwable]
 * @property cause the source [Throwable] itself
 */
data class DetailedException(
    override val message: String? = null,
    override val cause: Throwable? = null,
    val extras: Map<String, Any?> = emptyMap()
) : Throwable()

fun Throwable?.apiError(code: Int, message: String): DetailedException =
    apiErrorCode(code).apiErrorMessage(message)

fun Throwable?.apiErrorCode(code: Int): DetailedException =
    copyWithExtra(EXTRA_API_ERROR_CODE to code)

fun Throwable?.apiErrorMessage(message: String): DetailedException =
    copyWithExtra(EXTRA_API_ERROR_MESSAGE to message)

fun Throwable?.messageId(id: String?): DetailedException =
    copyWithExtra(EXTRA_MESSAGE_ID to id)

fun Throwable?.userId(userId: UserId): DetailedException =
    copyWithExtra(EXTRA_USER_ID to userId.id)

private fun Throwable?.copyWithExtra(newExtra: Pair<String, Any?>): DetailedException {
    val extras = (this as? DetailedException)?.extras ?: emptyMap()
    return toDetailedException().copy(extras = extras + newExtra)
}

fun Throwable?.toDetailedException(): DetailedException =
    when (this) {
        is DetailedException -> this
        is ApiException -> {
            val apiProtonData = (error as? ApiResult.Error.Http)?.proton
            apiProtonData?.let { apiError(it.code, it.error) } ?: DetailedException(message, cause = this)
        }
        is ch.protonmail.android.api.exceptions.ApiException -> {
            apiError(response.code, response.error)
        }
        else -> DetailedException(this?.message, cause = this)
    }
