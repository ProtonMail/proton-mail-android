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

import app.cash.turbine.test
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.data.remote.OfflineDataResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.assertIs
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [ProtonStore]
 */
class ProtonStoreTest : CoroutinesTest by CoroutinesTest() {

    private val api = FakePagedApi()
    private val database = FakeDatabase()
    private val noMapper = object : ProtonStoreMapper<Int, Item, Item> {
        override fun Item.toOut(key: Int) = this
    }
    private val fromApiMapper = object : ProtonStoreMapper<Int, ApiResponse, List<Item>> {
        override fun ApiResponse.toOut(key: Int) = this.items
    }
    private val store = ProtonStore(
        fetcher = api::getItems,
        reader = { database.findAll() },
        writer = { _, items -> database.save(items) },
        createBookmarkKey = { _, data -> data.items.maxOfOrNull { it.position } },
        apiToDomainMapper = fromApiMapper,
        databaseToDomainMapper = noMapper,
        apiToDatabaseMapper = fromApiMapper
    )

    @Test
    fun flowEmitsFromDatabase() = runTest {
        // given
        database.save(item1, item2, item3)
        val expected = listOf(item1, item2, item3).local()

        // when
        store.flow(0, refresh = false).test {

            // then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun flowEmitsFromDatabaseAndApi() = runTest {
        // given
        database.save(item1, item2, item3)
        val expectedFromDatabase = listOf(item1, item2, item3).local()
        val expectedFromApi = listOf(item1, item2).remote()

        // when
        store.flow(0, refresh = true).test {

            // then
            assertEquals(expectedFromApi, awaitItem())
            assertEquals(expectedFromDatabase, awaitItem())
        }
    }

    @Test
    fun flowEmitsFetcherErrorIfNoConnectivityManager() = runTest {
        // given
        val connectivityManager: NetworkConnectivityManager = mockk {
            every { isInternetConnectionPossible() } returns true
        }
        val expectedMessage = "Ouch!"
        val expectedException = IllegalStateException(expectedMessage)
        fun throwException(): ApiResponse {
            throw expectedException
        }

        val store = ProtonStore(
            fetcher = { throwException() },
            reader = { database.findAll() },
            writer = { _: Int, items -> database.save(items) },
            createBookmarkKey = { _, data -> data.items.maxOfOrNull { it.position } },
            apiToDomainMapper = fromApiMapper,
            databaseToDomainMapper = noMapper,
            apiToDatabaseMapper = fromApiMapper,
            connectivityManager = null
        )

        // when
        store.flow(0, refresh = true).test {

            // then
            val error = awaitItem() as DataResult.Error.Remote
            assertEquals(expectedException, error.cause)
            assertEquals(expectedMessage, error.message)
            assertEquals(emptyList<Item>().local(), awaitItem())
        }
    }

    @Test
    fun flowEmitsFetcherErrorIfOnline() = runTest {
        // given
        val connectivityManager: NetworkConnectivityManager = mockk {
            every { isInternetConnectionPossible() } returns true
        }
        val expectedMessage = "Ouch!"
        val expectedException = IllegalStateException(expectedMessage)
        fun throwException(): ApiResponse {
            throw expectedException
        }

        val store = ProtonStore(
            fetcher = { throwException() },
            reader = { database.findAll() },
            writer = { _: Int, items -> database.save(items) },
            createBookmarkKey = { _, data -> data.items.maxOfOrNull { it.position } },
            apiToDomainMapper = fromApiMapper,
            databaseToDomainMapper = noMapper,
            apiToDatabaseMapper = fromApiMapper,
            connectivityManager = connectivityManager
        )

        // when
        store.flow(0, refresh = true).test {

            // then
            val error = awaitItem() as DataResult.Error.Remote
            assertEquals(expectedException, error.cause)
            assertEquals(expectedMessage, error.message)
            assertEquals(emptyList<Item>().local(), awaitItem())
        }
    }

    @Test
    fun flowEmitsFetcherOfflineDataResultIfOffline() = runTest {
        // given
        val connectivityManager: NetworkConnectivityManager = mockk {
            every { isInternetConnectionPossible() } returns false
        }
        val expectedMessage = "Ouch!"
        val expectedException = IllegalStateException(expectedMessage)
        fun throwException(): ApiResponse {
            throw expectedException
        }

        val store = ProtonStore(
            fetcher = { throwException() },
            reader = { database.findAll() },
            writer = { _: Int, items -> database.save(items) },
            createBookmarkKey = { _, data -> data.items.maxOfOrNull { it.position } },
            apiToDomainMapper = fromApiMapper,
            databaseToDomainMapper = noMapper,
            apiToDatabaseMapper = fromApiMapper,
            connectivityManager = connectivityManager
        )

        // when
        store.flow(0, refresh = true).test {

            // then
            assertEquals(OfflineDataResult, awaitItem())
            assertEquals(emptyList<Item>().local(), awaitItem())
        }
    }

    @Test
    fun loadMoreFlowEmitsFromDatabase() = runTest {
        // given
        database.save(item1, item2, item3)
        val expected = listOf(item1, item2, item3).local()

        // when
        store.loadMoreFlow(0, refreshAtStart = false).test {

            // then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun loadMoreFlowEmitsFromDatabaseAndApi() = runTest {
        // given
        database.save(item1, item2, item3)
        val expectedFromDatabase = listOf(item1, item2, item3).local()
        val expectedFromApi = listOf(item1, item2).remote()

        // when
        val flow = store.loadMoreFlow(0, refreshAtStart = true)
        flow.test {

            // then
            assertEquals(expectedFromDatabase, awaitItem())
            assertEquals(expectedFromApi, awaitItem())
        }
    }

    @Test
    fun loadMoreFlowEmitsFromDatabaseAndApiOnLoadMore() = runTest {
        // given
        database.save(item1, item2, item3)

        // when
        val flow = store.loadMoreFlow(0, refreshAtStart = true)
        flow.test {

            // then
            assertEquals(allItems.take(3).local(), awaitItem())
            assertEquals(allItems.slice(0..1).remote(), awaitItem())

            flow.loadMore()
            assertEquals(allItems.slice(2..3).remote(), awaitItem())
            assertEquals(allItems.take(4).local(), awaitItem())

            flow.loadMore()
            assertEquals(allItems.slice(4..5).remote(), awaitItem())
            assertEquals(allItems.take(6).local(), awaitItem())
        }
    }

    @Test
    fun loadMoreFlowEmitsFetcherErrorIfOnline() = runTest {
        // given
        val connectivityManager: NetworkConnectivityManager = mockk {
            every { isInternetConnectionPossible() } returns true
        }
        val expectedMessage = "Ouch!"
        val expectedException = IllegalStateException(expectedMessage)
        fun throwException(): ApiResponse {
            throw expectedException
        }

        val store = ProtonStore(
            fetcher = { throwException() },
            reader = { database.findAll() },
            writer = { _: Int, items -> database.save(items) },
            createBookmarkKey = { _, data -> data.items.maxOfOrNull { it.position } },
            apiToDomainMapper = fromApiMapper,
            databaseToDomainMapper = noMapper,
            apiToDatabaseMapper = fromApiMapper,
            connectivityManager = connectivityManager
        )

        // when
        store.loadMoreFlow(0, refreshAtStart = true).test {

            // then
            assertEquals(emptyList<Item>().local(), awaitItem())

            val error = awaitItem() as DataResult.Error.Remote
            assertEquals(expectedException, error.cause)
            assertEquals(expectedMessage, error.message)
        }
    }

    @Test
    fun loadMoreFlowEmitsFromReaderIfFetcherThrowsError() = runTest {
        // given
        val items = listOf(
            Item(14, "hello"),
            Item(17, "world"),
        )

        fun throwException(): ApiResponse {
            throw IllegalStateException("Ouch!")
        }

        val store = ProtonStore(
            fetcher = { throwException() },
            reader = { flowOf(items) },
            writer = { _: Int, _ -> },
            createBookmarkKey = { _, data -> data.items.maxOfOrNull { it.position } },
            apiToDomainMapper = fromApiMapper,
            databaseToDomainMapper = noMapper,
            apiToDatabaseMapper = fromApiMapper
        )

        // when
        store.loadMoreFlow(0, refreshAtStart = true).test {

            // then
            assertEquals(items.local(), awaitItem())
            assertIs<DataResult.Error.Remote>(awaitItem())
        }
    }

    private fun List<Item>.local() = DataResult.Success(ResponseSource.Local, this)
    private fun List<Item>.remote() = DataResult.Success(ResponseSource.Remote, this)

    private data class Item(val position: Int, val content: String)
    private data class ApiResponse(val items: List<Item>)

    private class FakePagedApi {
        // return at most first 2 items after the given position
        fun getItems(position: Int): ApiResponse {
            val items = allItems.asSequence()
                .filter { it.position > position }
                .take(2)
                .toList()
            return ApiResponse(items)
        }
    }
    private class FakeDatabase {
        private val savedItems = MutableStateFlow(emptySet<Item>())
        suspend fun save(items: List<Item>) {
            savedItems.emit(savedItems.value + items)
        }
        suspend fun save(vararg items: Item) {
            savedItems.emit(savedItems.value + items)
        }
        fun findAll(): Flow<List<Item>> = savedItems.map { it.toList() }
    }

    private companion object {

        val item1 = Item(13, "hello")
        val item2 = Item(17, "world")
        val item3 = Item(20, "how")
        val item4 = Item(24, "are")
        val item5 = Item(29, "you")
        val item6 = Item(32, "today")
        val item7 = Item(99, "never")
        val allItems = listOf(
            item1,
            item2,
            item3,
            item4,
            item5,
            item6,
            item7
        )
    }
}
