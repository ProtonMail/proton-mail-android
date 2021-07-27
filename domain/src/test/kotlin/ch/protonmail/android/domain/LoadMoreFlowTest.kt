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

import app.cash.turbine.test
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [LoadMoreFlow]
 */
class LoadMoreFlowTest : CoroutinesTest {

    @Test
    fun emitsFirstPage() = coroutinesTest {
        // given
        val flow = FakePagedRepository.getAllItems()

        // when - then
        flow.test {
            assertEquals(allItems.take(2), expectItem())
        }
    }

    @Test
    fun emitsAllPages() = coroutinesTest {
        // given
        val flow = FakePagedRepository.getAllItems()

        // when - then
        flow.test {
            assertEquals(allItems.subList(0, 2), expectItem())
            flow.loadMore()
            assertEquals(allItems.subList(2, 4), expectItem())
            flow.loadMore()
            assertEquals(allItems.subList(4, 6), expectItem())
            flow.loadMore()
            assertEquals(allItems.subList(6, 7), expectItem())
        }
    }

    private data class Item(val position: Int, val content: String)

    private object FakePagedApi {
        // return at most first 2 items after the given position
        fun getItems(position: Int): List<Item> =
            allItems.asSequence()
                .filter { it.position > position }
                .take(2)
                .toList()
    }
    private object FakePagedRepository {
        fun getAllItems(): LoadMoreFlow<List<Item>> =
            loadMoreFlow(
                initialBookmark = 0,
                createNextBookmark = { list -> list.maxOfOrNull { it.position } ?: Int.MAX_VALUE },
                load = { FakePagedApi.getItems(it) }
            )
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
