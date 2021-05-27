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
// Public APIs
@file:Suppress("unused")
@file:JvmName("CollectionExtensions")

package ch.protonmail.android.utils.extensions

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.reflect.KClass

/*
 * A file containing extensions for Collections
 * Author: Davide Farella
 */

/**
 * Execute concurrently a lambda [block] for each [T] element in the [Iterable]
 * @see forEach
 * @return [Unit]
 */
// TODO: Cannot inline due to compilation error: try with newer Kotlin version for trim allocations overhead
suspend fun <T> Iterable<T>.forEachAsync(block: suspend (T) -> Unit) = coroutineScope {
    map { async { block(it) } }.forEach { it.await() }
}

/**
 * Map concurrently each [T] element in the [Iterable]
 * @see map
 * @return [List] of [V]
 */
// TODO: Cannot inline due to compilation error: try with newer Kotlin version for trim allocations overhead
suspend fun <T, V> Iterable<T>.mapAsync(mapper: suspend (T) -> V) = coroutineScope {
    map { async { mapper(it) } }.map { it.await() }
}

/**
 * Change first the element matching the [predicate] with [newItem].
 * [newItem] will *NOT* be added if no item matches the [predicate]
 * [newItem] will be added at the end of the collection, if it is ordered
 *
 * @return `true` if element has been changed successfully
 *
 * @receiver [C] is [MutableCollection] of [T]
 */
inline fun <C : MutableCollection<T>, T> C.changeFirst(newItem: T, predicate: (T) -> Boolean) =
    removeFirst(predicate) && add(newItem)

/**
 * Remove first the element matching the [predicate]
 * @return `true` if element has been removed successfully
 *
 * @receiver [C] is [MutableCollection] of [T]
 */
inline fun <C : MutableCollection<T>, T> C.removeFirst(predicate: (T) -> Boolean) =
    find(predicate)?.let { remove(it) } ?: false

/**
 * Replace first the element matching the [predicate] with [newItem].
 * [newItem] will be added even if no item matches the [predicate]
 * [newItem] will be added at the end of the collection, if it is ordered
 *
 * @return `true` if element has been replaced or simply added successfully
 *
 * @receiver [C] is [MutableCollection] of [T]
 */
inline fun <C : MutableCollection<T>, T> C.replaceFirst(newItem: T, predicate: (T) -> Boolean) =
    removeFirst(predicate) or add(newItem)

/** @return [Map] of [K] and [V] by filtering by values which are instance of [javaClass] */
@Suppress("UNCHECKED_CAST")
fun <K, V : Any> Map<K, Any?>.filterValues(javaClass: Class<V>) =
    filterValues { it != null && it::class.java == javaClass } as Map<K, V>

/** @return [Map] of [K] and [V] by filtering by values which are instance of [kClass] */
@Suppress("UNCHECKED_CAST")
fun <K, V : Any> Map<K, Any?>.filterValues(kClass: KClass<V>) =
    filterValues { it != null && it::class == kClass } as Map<K, V>
