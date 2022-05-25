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
interface ProtonStoreMapper<Key, in In, out Out> : Mapper<In, Out> {

    fun In.toOut(key: Key): Out
}

/**
 * A [ProtonStoreMapper] that has the same type as In and Out
 */
fun <Key, T> NoProtonStoreMapper() = object : ProtonStoreMapper<Key, T, T> {

    override fun T.toOut(key: Key) = this
}

fun <K : Any, T : Any, V : Any> Flow<DataResult<List<T>>>.map(
    key: K,
    mapper: ProtonStoreMapper<K, T, V>
): Flow<DataResult<List<V>>> =
    map { result ->
        result.mapSuccess { success ->
            DataResult.Success(result.source, success.value.map(mapper) { it.toOut(key) })
        }
    }
