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

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * @return [LoadMoreFlow] that emits only from the receiver [Flow], while it collects from [LoadMoreFlow] and execute
 *  [onLoad] every time this emits.
 *
 * A common use case is that the receiver [Flow] is a Flow from database and [loader] is a [LoadMoreFlow] from API and
 *  we want to observe only the database, and fetch more data by calling [LoadMoreFlow.loadMore]
 */
fun <A, B> Flow<A>.withLoadMore(
    loader: LoadMoreFlow<B>,
    onLoad: suspend (B) -> Unit
): LoadMoreFlow<A> {
    val underlying = combine(this, loader.emitInitialNull()) { fromFlow, fromLoadMore ->
        if (fromLoadMore != null) onLoad(fromLoadMore)
        fromFlow
    }.distinctUntilChanged()
    return LoadMoreFlow(underlying, loader.trigger)
}

/**
 * Convert the receiver [Flow] to a [LoadMoreFlow] without any action executed when [LoadMoreFlow.loadMore] is called
 */
fun <T> Flow<T>.asLoadMoreFlow(): LoadMoreFlow<T> =
    withLoadMore(loadMoreFlowOf<T>()) {}

/**
 * Convert the receiver [Flow] to a [LoadMoreFlow]
 *
 * @param loadAtStart if `true`, executes [onLoadMore] at start, without calling [LoadMoreFlow.loadMore]
 * @param onLoadMore is executed when [LoadMoreFlow.loadMore] is called.
 *  it must return the next bookmark [B]
 */
fun <B, T> Flow<T>.asLoadMoreFlow(
    initialBookmark: B,
    loadAtStart: Boolean = false,
    onLoadMore: suspend FlowCollector<T>.(previousBookmark: B) -> B,
): LoadMoreFlow<T> {
    var bookmark = initialBookmark
    val trigger = MutableSharedFlow<Unit>(replay = 1)

    val fixedFlow = this
    val fixedTrigger = trigger.emitInitialNull()

    var lastFromFlow: T? = null
    var shouldLoadOnNonTriggerEvent = loadAtStart
    val underlying = combineTransform(fixedFlow, fixedTrigger) { fromFlow, _ ->
        val isTriggerEvent = fromFlow === lastFromFlow
        if (isTriggerEvent) {
            bookmark = onLoadMore(bookmark)
        } else {
            lastFromFlow = fromFlow
            emit(fromFlow)
        }

        if (shouldLoadOnNonTriggerEvent) {
            bookmark = onLoadMore(bookmark)
            shouldLoadOnNonTriggerEvent = false
        }
    }

    return LoadMoreFlow(underlying, trigger)
}

/**
 * Same as [Flow.catch], but returns a [LoadMoreFlow] instead
 */
fun <T> LoadMoreFlow<T>.loadMoreCatch(action: suspend FlowCollector<T>.(Throwable) -> Unit): LoadMoreFlow<T> =
    LoadMoreFlow(catch(action), trigger)

fun <T> LoadMoreFlow<T>.loadMoreBuffer(
    capacity: Int = Channel.BUFFERED,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
): LoadMoreFlow<T> =
    LoadMoreFlow(buffer(capacity, onBufferOverflow), trigger)

/**
 * Same as [combineTransform], but returns a [LoadMoreFlow] instead
 */
public fun <T1, T2, R> loadMoreCombineTransform(
    flow: Flow<T1>,
    loadMoreFlow: LoadMoreFlow<T2>,
    transform: suspend FlowCollector<R>.(a: T1, b: T2) -> Unit
): LoadMoreFlow<R> = LoadMoreFlow(combineTransform(flow, loadMoreFlow, transform), loadMoreFlow.trigger)

public fun <T1, T2, R> loadMoreCombine(
    flow: Flow<T1>,
    loadMoreFlow: LoadMoreFlow<T2>,
    transform: suspend (a: T1, b: T2) -> R
): LoadMoreFlow<R> = LoadMoreFlow(combine(flow, loadMoreFlow, transform), loadMoreFlow.trigger)

/**
 * @return [LoadMoreFlow] with nullable [T], emitting one `null` when Flow is started
 *  @see Flow.onStart
 *
 * A common scenario is when combining a Flow from Database, with a Flow from Api, where we want to call this on
 *  the Flow from API, in order to emit whenever the Database emits, without waiting for first emission from Api
 */
fun <T> LoadMoreFlow<T>.loadMoreEmitInitialNull(): LoadMoreFlow<T?> =
    LoadMoreFlow(emitInitialNull(), trigger)

/**
 * Same as [flowOf], but returns a [LoadMoreFlow] instead
 */
fun <T> loadMoreFlowOf(vararg elements: T): LoadMoreFlow<T> =
    LoadMoreFlow(flowOf(*elements), MutableSharedFlow())

/**
 * Same as [Flow.flatMapLatest], but returns a [LoadMoreFlow] instead
 */
fun <A, B> LoadMoreFlow<A>.loadMoreFlatMapLatest(transform: suspend (A) -> Flow<B>): LoadMoreFlow<B> =
    LoadMoreFlow(flatMapLatest(transform), trigger)

/**
 * Same as [Flow.map], but returns a [LoadMoreFlow] instead
 */
fun <A, B> LoadMoreFlow<A>.loadMoreMap(mapper: suspend (A) -> B): LoadMoreFlow<B> =
    LoadMoreFlow(map(mapper), trigger)

@Suppress("USELESS_CAST") // Cast as nullable is needed in order to emit `null`
private fun <T> Flow<T>.emitInitialNull(): Flow<T?> =
    map { it as T? }.onStart { emit(null) }
