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

package ch.protonmail.android.domain

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * @return [LoadMoreFlow] that emits only from the receiver [Flow], while it collects from [LoadMoreFlow] and execute
 *  [onLoad] every time this emits.
 *
 * A common use case is that the receiver [Flow] is a Flow from database and [loader] is a [LoadMoreFlow] from API and
 *  we want to observe only the database, and fetch more data by calling [LoadMoreFlow.loadMore]
 */
@OptIn(FlowPreview::class)
fun <A, B> Flow<A>.withLoadMore(loader: LoadMoreFlow<B>, onLoad: suspend (B) -> Unit): LoadMoreFlow<A> {
    val underlying = combine(this, loader.emitInitialNull()) { fromFlow, fromLoadMore ->
        if (fromLoadMore != null) onLoad(fromLoadMore)
        fromFlow
    }.distinctUntilChanged()
    return LoadMoreFlow(underlying, loader.trigger)
}

@Suppress("USELESS_CAST") // Cast as nullable is needed in order to emit `null`
private fun <T> Flow<T>.emitInitialNull(): Flow<T?> =
    map { it as T? }.onStart { emit(null) }
