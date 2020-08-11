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
package ch.protonmail.android.utils.extensions

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.utils.MessageUtils
import com.google.gson.Gson
import retrofit2.HttpException
import java.util.regex.Pattern

fun HttpException.toPMResponseBody(): ResponseBody? {
    return response()?.errorBody()?.toPMResponseBody()
}

fun okhttp3.ResponseBody.toPMResponseBody(): ResponseBody {
    return Gson().fromJson(string(), ResponseBody::class.java)
}

/**
 * Performs mapping like [Transformations.map][androidx.lifecycle.Transformations.map] but executes
 * the mapping function in background thread.
 */
fun <X, Y> LiveData<X>.asyncMap(func: (X?) -> Y?): LiveData<Y> {
    val result = MediatorLiveData<Y>()
    result.addSource(this) { x ->
        AsyncTask.execute {
            result.postValue(func.invoke(x))
        }
    }
    return result
}

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

fun String.isValidEmail(): Boolean = Pattern.compile(EmailAddress.VALIDATION_REGEX).matcher(this).matches()

val Context.app get() = applicationContext as ProtonMailApplication
