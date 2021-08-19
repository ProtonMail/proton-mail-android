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

package ch.protonmail.android.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.Mapper
import me.proton.core.domain.arch.map
import me.proton.core.domain.arch.mapSuccess

/**
 * Interface of [Mapper] that provides [toOut] function
 */
interface ProtonStoreMapper<in In, out Out> : Mapper<In, Out> {

    fun In.toOut(): Out
}

fun <T : Any, V : Any> Flow<DataResult<List<T>>>.map(mapper: ProtonStoreMapper<T, V>): Flow<DataResult<List<V>>> =
    map { result ->
        result.mapSuccess { success ->
            DataResult.Success(result.source, success.value.map(mapper) { it.toOut() })
        }
    }
