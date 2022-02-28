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
package ch.protonmail.android.utils.extensions

import android.content.Context
import android.net.Uri
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.domain.entity.EmailAddress
import com.google.gson.Gson
import retrofit2.HttpException

fun Throwable.toPmResponseBodyOrNull(): ResponseBody? =
    (this as? HttpException)?.response()?.errorBody()?.toPmResponseBody()

fun okhttp3.ResponseBody.toPmResponseBody(): ResponseBody =
    Gson().fromJson(string(), ResponseBody::class.java)

/** @return [Boolean] `true` if the receiver [Uri] is [Uri.EMPTY] */
fun Uri.isEmpty() = this == Uri.EMPTY

/**
 * This parametrized extension on [Any] is useful for assert that a statement is exhaustive.
 *
 * I.E. `when` can used as expression ( `val something = when { ... }` ) or as a statement.
 * When used as expression, it's required to be exhaustive, so all the possible branches are
 * required; when it's used as statement, this is not true so, with this extension, `when` is
 * required to be exhaustive
 *
 * @return the receiver as is
 */
val <T : Any?> T.exhaustive get() = this

fun String.isValidEmail(): Boolean = EmailAddress.VALIDATION_REGEX.matches(this)

val Context.app get() = applicationContext as ProtonMailApplication
