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

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Ignore
import kotlin.test.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

internal class CollectionExtensionsTest {

    @Test
    fun `forEachAsync executes concurrently`() {
        val task = suspend { delay(100) }
        val tasks = (1..10).map { task }

        val asyncTime = runBlocking {
            measureTimeMillis { tasks.forEachAsync { it() } }
        }

        assert(asyncTime in 100..300)
    }

    @Test
    fun `forEachAsync works correctly`() {
        val i = AtomicInteger()
        val increment = { i.incrementAndGet() }
        val increments = (1..10).map { increment }

        runBlocking { increments.forEachAsync { it() } }

        assertEquals(10, i.get())
    }

    @Test
    fun `mapAsync executes concurrently`() {
        val task = suspend { delay(100) }
        val tasks = (1..10).map { task }

        val asyncTime = runBlocking {
            measureTimeMillis { tasks.mapAsync { it() } }
        }

        assert(asyncTime in 100..300)
    }

    @Test
    fun `mapAsync works correctly`() {
        val input = (1..10)
        // Multiply the input * 10
        val mapper: suspend (Int) -> Int = { it * 10 }

        val output = runBlocking { input.mapAsync(mapper) }

        // toList casting is required for match the assertion
        assertEquals((10..100 step 10).toList(), output.toList())
    }

    @Test
    fun `Map filterValues by java class`() {
        val mixedMap = mapOf(
            "1" to 1,
            "2" to 2,
            "3" to false,
            "4" to "hello"
        )
        val result = mixedMap.filterValues(String::class.java)
        assertEquals(1, result.size)
        assertEquals("hello", result.values.first())
    }

    @Test
    fun `Map filterValues by kotlin class`() {
        val mixedMap = mapOf(
            "1" to 1,
            "2" to 2,
            "3" to false,
            "4" to "hello"
        )
        val expected = mapOf("1" to 1, "2" to 2)
        val result = mixedMap.filterValues(Int::class)
        assertEquals(expected, result)
    }

    @Ignore("Not working after android gradle plugin and kotlin update")
    @Test
    fun `Map filterValues by reified`() {
        val mixedMap = mapOf(
            "1" to 1,
            "2" to 2,
            "3" to false,
            "4" to "hello"
        )
        val expected = mapOf("1" to 1, "2" to 2)
        val result = mixedMap.filterValues<String, Int>()
        assertEquals(expected, result)
    }
}
