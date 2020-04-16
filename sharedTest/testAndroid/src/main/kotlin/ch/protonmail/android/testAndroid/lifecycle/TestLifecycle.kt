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
@file:Suppress("unused")

package ch.protonmail.android.testAndroid.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * A class for test within a [LifecycleOwner]
 *
 * @author Davide Giuseppe Farella
 */
class TestLifecycle : LifecycleOwner {

    /**
     * @return [Lifecycle.State]
     * @see LifecycleRegistry.getCurrentState
     */
    val currentState get() = registry.currentState

    /** A [LifecycleRegistry] for the [LifecycleOwner] */
    private val registry = LifecycleRegistry(this)


    /**
     * Call [Lifecycle.Event.ON_CREATE] on [Lifecycle]
     * @return this [TestLifecycle]
     */
    fun create() = handleLifecycleEvent(ON_CREATE)

    /**
     * Call [Lifecycle.Event.ON_START] on [Lifecycle]
     * @return this [TestLifecycle]
     */
    fun start() = handleLifecycleEvent(ON_START)

    /**
     * Call [Lifecycle.Event.ON_RESUME] on [Lifecycle]
     * @return this [TestLifecycle]
     */
    fun resume() = handleLifecycleEvent(ON_RESUME)

    /**
     * Call [Lifecycle.Event.ON_PAUSE] on [Lifecycle]
     * @return this [TestLifecycle]
     */
    fun pause() = handleLifecycleEvent(ON_PAUSE)

    /**
     * Call [Lifecycle.Event.ON_STOP] on [Lifecycle]
     * @return this [TestLifecycle]
     */
    fun stop() = handleLifecycleEvent(ON_STOP)

    /**
     * Call [Lifecycle.Event.ON_DESTROY] on [Lifecycle]
     * @return this [TestLifecycle]
     */
    fun destroy() = handleLifecycleEvent(ON_DESTROY)


    /** @see LifecycleOwner.getLifecycle */
    override fun getLifecycle() = registry

    /** @see LifecycleRegistry.handleLifecycleEvent */
    private fun handleLifecycleEvent(event: Lifecycle.Event) = apply {
        registry.handleLifecycleEvent(event)
    }
}
