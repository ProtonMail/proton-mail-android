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
package ch.protonmail.android.domain.util

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Provides [CoroutineDispatcher]s in order to inject them in the constructor of a component allowing it to be tested
 *
 * @author Davide Farella
 */
@Suppress("PropertyName", "VariableNaming") // Non conventional naming starting with uppercase letter
interface DispatcherProvider {

    /** [CoroutineDispatcher] meant to run IO operations */
    val Io: CoroutineDispatcher

    /** [CoroutineDispatcher] meant to run computational operations */
    val Comp: CoroutineDispatcher

    /** [CoroutineDispatcher] meant to run on main thread */
    val Main: CoroutineDispatcher
}
