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

package ch.protonmail.android.domain

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [LoadMoreFlow]
 */
class LoadMoreFlowTest : CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher())}) {

    private val fakePagedApi = FakePagedApi()
    private val fakeDatabase = FakeDatabase()
    private val fakeOnlineOnlyRepository = FakeOnlineOnlyRepository(fakePagedApi)
    private val fakeOfflineEnabledRepository = FakeOfflineEnabledRepository(fakePagedApi, fakeDatabase)

    @Test
    fun emitsFirstPageFromOnlineOnly() = coroutinesTest {
        // given
        val flow = fakeOnlineOnlyRepository.getAllItems()

        // when - then
        flow.test {
            assertEquals(allItems.take(2), awaitItem())
        }
    }

    @Test
    fun emitsAllPagesFromOnlineOnly() = coroutinesTest {
        // given
        val flow = fakeOnlineOnlyRepository.getAllItems()

        // when - then
        flow.test {
            assertEquals(allItems.subList(0, 2), awaitItem())
            flow.loadMore()
            assertEquals(allItems.subList(2, 4), awaitItem())
            flow.loadMore()
            assertEquals(allItems.subList(4, 6), awaitItem())
            flow.loadMore()
            assertEquals(allItems.subList(6, 7), awaitItem())
        }
    }

    @Test
    fun emitsFirstPageFromOfflineEnabledIfNoCacheAndHasNet() = coroutinesTest {
        // given
        val flow = fakeOfflineEnabledRepository.withNet().getAllItems()

        // when - then
        flow.test {
            assertEquals(emptyList(), awaitItem())
            assertEquals(allItems.take(2), awaitItem())
        }
    }

    @Test
    fun emitsAllPagesFromOfflineEnabledIfNoCacheAndHasNet() = coroutinesTest {
        // given
        val flow = fakeOfflineEnabledRepository.withNet().getAllItems()

        // when - then
        flow.test {
            assertEquals(emptyList(), awaitItem())
            assertEquals(allItems.take(2), awaitItem())
            flow.loadMore()
            assertEquals(allItems.take(4), awaitItem())
            flow.loadMore()
            assertEquals(allItems.take(6), awaitItem())
            flow.loadMore()
            assertEquals(allItems.take(7), awaitItem())
        }
    }

    @Test
    fun emitsNothingOnFirstPageFromOfflineEnabledIfNoCacheAndHasNoNet() = coroutinesTest {
        // given
        val flow = fakeOfflineEnabledRepository.withoutNet().getAllItems()

        // when - then
        flow.test {
            assertEquals(emptyList(), awaitItem())
        }
    }

    @Test
    fun emitsNothingOnAllPagesFromOfflineEnabledIfNoCacheAndHasNoNet() = coroutinesTest {
        // given
        val flow = fakeOfflineEnabledRepository.withoutNet().getAllItems()

        // when - then
        flow.test {
            assertEquals(emptyList(), awaitItem())
            flow.loadMore()
            expectNoEvents()
        }
    }

    @Test
    fun emitsFirstPageFromOfflineEnabledIfNoNetButHasCache() = coroutinesTest {
        // given
        fakeDatabase.save(item1, item2, item3)
        val flow = fakeOfflineEnabledRepository.withoutNet().getAllItems()

        // when - then
        flow.test {
            assertEquals(allItems.take(3), awaitItem())
        }
    }

    @Test
    fun emitsAllPagesFromOfflineEnabledIfNoNetButHasCache() = coroutinesTest {
        // given
        fakeDatabase.save(item1, item2, item3)
        val flow = fakeOfflineEnabledRepository.withoutNet().getAllItems()

        // when - then
        flow.test {
            assertEquals(allItems.take(3), awaitItem())
            flow.loadMore()
            expectNoEvents()
        }
    }

    @Test
    fun emitsFirstPageFromOfflineEnabledIfHasNetAndCache() = coroutinesTest {
        // given
        fakeDatabase.save(item1, item2, item3)
        val flow = fakeOfflineEnabledRepository.withNet().getAllItems()

        // when - then
        flow.test {
            assertEquals(allItems.take(3), awaitItem())
        }
    }

    @Test
    fun emitsAllPagesFromOfflineEnabledIfHasNetAndCache() = coroutinesTest {
        // given
        fakeDatabase.save(item1, item2, item3)
        val flow = fakeOfflineEnabledRepository.withNet().getAllItems()

        // when - then
        flow.test {
            assertEquals(allItems.take(3), awaitItem())
            flow.loadMore()
            assertEquals(allItems.take(4), awaitItem())
        }
    }

    @Test
    fun emitsAllPagesFromOfflineEnabledWhenNetIsRestoredAndHasCache() = coroutinesTest {
        // given
        fakeDatabase.save(item1, item2, item3)
        val flow = fakeOfflineEnabledRepository.withoutNet().getAllItems()

        // when - then
        flow.test {
            assertEquals(allItems.take(3), awaitItem())
            // Try to load few times before restoring connection
            flow.loadMore()
            expectNoEvents()
            flow.loadMore()
            expectNoEvents()
            flow.loadMore()
            expectNoEvents()
            fakeOfflineEnabledRepository.withNet()
            flow.loadMore()
            flow.loadMore()
            assertEquals(allItems.take(4), awaitItem())
            flow.loadMore()
            assertEquals(allItems.take(6), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // region LoadMoreFlowUtils

    @Test
    fun asLoadMoreFlowEmitsOnlyFromTheOriginalFlow() = coroutinesTest {
        // given
        fakeDatabase.save(item1)
        val flow = fakeDatabase.findAll().asLoadMoreFlow(
            initialBookmark = 0,
        ) { bookmark ->
            val apiResult = fakePagedApi.getItems(bookmark)
            fakeDatabase.save(apiResult)
            apiResult.maxOfOrNull { it.position } ?: bookmark
        }

        // when - then
        flow.test {
            assertEquals(allItems.take(1), awaitItem())
            flow.loadMore()
            assertEquals(allItems.take(2), awaitItem())
            flow.loadMore()
            assertEquals(allItems.take(4), awaitItem())
        }
    }

    @Test
    fun asLoadMoreFlowLoadOnStartIfRequested() = coroutinesTest {
        // given
        fakeDatabase.save(item1)
        val flow = fakeDatabase.findAll().asLoadMoreFlow(
            initialBookmark = 0,
            loadAtStart = true
        ) { bookmark ->
            val apiResult = fakePagedApi.getItems(bookmark)
            fakeDatabase.save(apiResult)
            apiResult.maxOfOrNull { it.position } ?: bookmark
        }

        // when - then
        flow.test {
            assertEquals(allItems.take(1), awaitItem())
            assertEquals(allItems.take(2), awaitItem())
            flow.loadMore()
            assertEquals(allItems.take(4), awaitItem())
        }
    }

    // endregion

    private data class Item(val position: Int, val content: String)

    private class FakePagedApi {
        // return at most first 2 items after the given position
        fun getItems(position: Int): List<Item> =
            allItems.asSequence()
                .filter { it.position > position }
                .take(2)
                .toList()
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
    private class FakeOnlineOnlyRepository(
        private val api: FakePagedApi
    ) {
        fun getAllItems(): LoadMoreFlow<List<Item>> =
            loadMoreFlow(
                initialBookmark = 0,
                createNextBookmark = { list, previousBookmark -> list.maxOfOrNull { it.position } ?: previousBookmark },
                load = { api.getItems(it) }
            )
    }
    private class FakeOfflineEnabledRepository(
        private val api: FakePagedApi,
        private val database: FakeDatabase
    ) {

        private val hasNetworkFlow = MutableStateFlow(true)
        private val fromApiFlow = loadMoreFlow(
            initialBookmark = 0,
            createNextBookmark = { list, previousBookmark -> list?.maxOfOrNull { it.position } ?: previousBookmark },
            load = { if (hasNetworkFlow.value) api.getItems(it) else null }
        )

        private val fromDatabaseFlow = database.findAll()

        suspend fun withNet() = apply { hasNetworkFlow.emit(true) }
        suspend fun withoutNet() = apply { hasNetworkFlow.emit(false) }

        fun getAllItems(): LoadMoreFlow<List<Item>> =
            fromDatabaseFlow.withLoadMore(fromApiFlow) { fromApiListOrNull ->
                fromApiListOrNull?.let { database.save(it) }
            }
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
