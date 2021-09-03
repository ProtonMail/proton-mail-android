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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class LoadMoreFlow<T> internal constructor(
    underlying: Flow<T>,
    internal val trigger: MutableSharedFlow<Unit>
) : Flow<T> by underlying {

    fun loadMore() {
        trigger.tryEmit(Unit)
    }
}

/**
 * @return a [Flow] that can independently load next page by calling [LoadMoreFlow.loadMore]
 *
 * @param B the type of the bookmark, for example [Long] if we're using a timestamp
 * @param T the type of the expected elements, for example [List] of `Item`
 *
 * @param initialBookmark the initial bookmark used for fetch the first page
 * @param createNextBookmark a function that, given the last page [T] ( like a [List] ) fetched, creates a new bookmark
 * @param load a function that, given the current bookmark, fetches the next page [T]
 */
fun <B, T> loadMoreFlow(
    initialBookmark: B,
    createNextBookmark: (result: T, previousBookmark: B) -> B,
    load: suspend (bookmark: B) -> T
): LoadMoreFlow<T> {
    var bookmark = initialBookmark
    val trigger = MutableSharedFlow<Unit>(replay = 1)
    val underlying = trigger.onStart {
        // Load the first page
        trigger.emit(Unit)
    }.map {
        // For each 'loadMore' invocation, load next page and prepare next bookmark
        val page = load(bookmark)
        bookmark = createNextBookmark(page, bookmark)
        page
    }

    return LoadMoreFlow(underlying, trigger)
}
