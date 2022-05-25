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
package ch.protonmail.android.utils

import java.util.LinkedList
import kotlin.reflect.KProperty

class ResettableLazyManager {
    // we synchronize to make sure the timing of a reset() call and new inits do not collide
    private val managedDelegates = LinkedList<Resettable>()

    fun register(managed: Resettable) {
        synchronized(managedDelegates) {
            managedDelegates.add(managed)
        }
    }

    fun reset() {
        synchronized(managedDelegates) {
            managedDelegates.forEach { it.reset() }
            managedDelegates.clear()
        }
    }
}

interface Resettable {
    fun reset()
}

class ResettableLazy<TYPE>(val manager: ResettableLazyManager, val init: () -> TYPE) : Resettable {
    @Volatile
    var lazyHolder = makeInitBlock()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): TYPE {
        return lazyHolder.value
    }

    override fun reset() {
        lazyHolder = makeInitBlock()
    }

    fun makeInitBlock(): Lazy<TYPE> {
        return lazy {
            manager.register(this)
            init()
        }
    }
}

fun <TYPE> resettableLazy(manager: ResettableLazyManager, init: () -> TYPE): ResettableLazy<TYPE> {
    return ResettableLazy(manager, init)
}

fun resettableManager(): ResettableLazyManager = ResettableLazyManager()
