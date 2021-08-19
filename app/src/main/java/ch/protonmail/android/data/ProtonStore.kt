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

import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.domain.asLoadMoreFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.domain.arch.onSuccess
import me.proton.core.util.kotlin.invoke

/**
 * [ProtonStore] works in a similar manner as [com.dropbox.android.external.store4.Store] but provide us a more
 *  flexible solution for our needing.
 *
 * Core features are:
 *
 *  * Provide a [LoadMoreFlow], so we can load data progressively from BackEnd, on the same Flow
 *
 *  * Have a correct information about the [ResponseSource] of a given [DataResult] emitted
 *   ( [ResponseSource.Remote] for every [DataResult] emitted after the completion for the request triggered by
 *   [LoadMoreFlow.loadMore] and [ResponseSource.Local] only for initial [DataResult] emitted from Database and when
 *   the Database is updated from another place - e.g. by Event Manger )
 *
 *  * Being aware of the exact [DataResult] returned from an API call ( e.g. we're in the Mailbox and we want to call
 *   [LoadMoreFlow.loadMore] when we have on display the last Message returned from the *last API call*, and not the
 *   last Message emitted from Database, which can be different )
 *
 * @param createBookmarkKey a function that returns the next Bookmark [Key] given the current Bookmark and the result
 *  of the API call
 *  Defaults to the current Bookmark, that will work correctly for NON-paged API requests
 */
class ProtonStore<Key : Any, ApiModel : Any, DatabaseModel : Any, DomainModel : Any>(
    private val fetcher: suspend (Key) -> ApiModel,
    private val reader: (Key) -> Flow<List<DatabaseModel>>,
    private val writer: suspend (Key, List<DatabaseModel>) -> Unit,
    private val createBookmarkKey: (currentKey: Key, data: ApiModel) -> Key? = { currentKey, _ -> currentKey },
    private val apiToDomainMapper: ProtonStoreMapper<ApiModel, List<DomainModel>>,
    private val databaseToDomainMapper: ProtonStoreMapper<DatabaseModel, DomainModel>,
    private val apiToDatabaseMapper: ProtonStoreMapper<ApiModel, List<DatabaseModel>>
) {

    /**
     * Return a [Flow] that emits [ResponseSource.Local] and [ResponseSource.Remote] data.
     *  Note that [DataResult] with source [ResponseSource.Remote] should not be displayed in the UI, as they may
     *  contains less Data that the one present in Database
     *  @see filterRemoteSuccess for ignore that elements
     *
     * @param refresh if `true` call [fresh] and emit its result at start
     */
    fun flow(key: Key, refresh: Boolean): Flow<DataResult<List<DomainModel>>> =
        readerFlow(key)
            .map(databaseToDomainMapper)
            .onStart { if (refresh) emit(fresh(key)) }

    /**
     * Return a [LoadMoreFlow] that emits [ResponseSource.Local] and [ResponseSource.Remote] data.
     *  Note that [DataResult] with source [ResponseSource.Remote] should not be displayed in the UI, as they may
     *  contains less Data that the one present in Database
     *  @see filterRemoteSuccess for ignore that elements
     *
     * @param refreshAtStart if `true` call [fresh] and emit its result at start.
     *  This has the same effect as calling [LoadMoreFlow.loadMore]
     */
    fun loadMoreFlow(key: Key, refreshAtStart: Boolean): LoadMoreFlow<DataResult<List<DomainModel>>> =
        flow(key, refresh = false)
            .asLoadMoreFlow(initialBookmark = key, loadAtStart = refreshAtStart) { newKey ->
                val freshAsApiModelDataResult = freshAsApiModel(newKey)
                emit(freshAsApiModelDataResult.toDomainModelsDataResult())

                freshAsApiModelDataResult.valueOrNull()
                    ?.let { createBookmarkKey(newKey, it) } ?: newKey
            }

    /**
     * Return fresh data from [fetcher], also store to [writer]
     * @return [DataResult] of [DomainModel]
     */
    suspend fun fresh(key: Key): DataResult<List<DomainModel>> =
        freshAsApiModel(key).toDomainModelsDataResult()

    private suspend fun freshAsApiModel(key: Key): DataResult<ApiModel> {
        @Suppress("TooGenericExceptionCaught")
        val apiResult = try {
            val apiModels = fetcher(key)
            DataResult.Success(ResponseSource.Remote, apiModels)
        } catch (t: Throwable) {
            DataResult.Error.Remote(t.message, t)
        }

        return apiResult.onSuccess {
            val databaseModel = apiToDatabaseMapper { it.toOut() }
            writer(key, databaseModel)
        }
    }

    private fun readerFlow(key: Key) =
        reader(key).mapToLocalDataResult()

    private suspend fun DataResult<ApiModel>.toDomainModelsDataResult(): DataResult<List<DomainModel>> =
        mapSuccess { result ->
            val domainModels = apiToDomainMapper { result.value.toOut() }
            DataResult.Success(result.source, domainModels)
        }
}

/**
 * Filter elements that are [DataResult.Success] with [ResponseSource.Remote] source
 */
fun <T> Flow<DataResult<T>>.filterRemoteSuccess(): Flow<DataResult<T>> =
    filterNot { it is DataResult.Success && it.source == ResponseSource.Remote }

private fun <T> DataResult<T>.valueOrNull(): T? =
    if (this is DataResult.Success) value
    else null

private fun <T> Flow<List<T>>.mapToLocalDataResult(): Flow<DataResult<List<T>>> =
    map { DataResult.Success(ResponseSource.Local, it) }

