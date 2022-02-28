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

package ch.protonmail.android.utils

import me.proton.core.network.data.LogTag
import me.proton.core.util.kotlin.Logger
import me.proton.core.util.kotlin.LoggerLogTag
import org.jetbrains.annotations.NonNls
import timber.log.Timber

class CoreTimberLogger : Logger {

    override fun e(tag: String, e: Throwable) =
        Timber.tag(tag).e(e)

    override fun e(tag: String, e: Throwable, @NonNls message: String) =
        Timber.tag(tag).e(e, message)

    override fun i(tag: String, @NonNls message: String) =
        Timber.tag(tag).i(message)

    override fun i(tag: String, e: Throwable, message: String) =
        Timber.tag(tag).i(e, message)

    override fun d(tag: String, message: String) =
        Timber.tag(tag).d(message)

    override fun d(tag: String, e: Throwable, message: String) =
        Timber.tag(tag).d(e, message)

    override fun v(tag: String, message: String) =
        Timber.tag(tag).v(message)

    override fun v(tag: String, e: Throwable, message: String) =
        Timber.tag(tag).v(e, message)

    override fun log(tag: LoggerLogTag, message: String) = when (tag) {
        LogTag.API_REQUEST,
        LogTag.API_RESPONSE,
        LogTag.API_ERROR,
        LogTag.REFRESH_TOKEN -> Timber.tag(tag.name).d(message)
        LogTag.SERVER_TIME_PARSE_ERROR -> Timber.tag(tag.name).e(message)
        else -> Timber.tag(tag.name).d(message)
    }
}
