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

package ch.protonmail.android.worker

import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result.failure
import kotlinx.serialization.Serializable
import me.proton.core.util.android.workmanager.deserialize
import me.proton.core.util.android.workmanager.toWorkData
import me.proton.core.util.kotlin.EMPTY_STRING

@Serializable
data class WorkerError(
    val message: String
)

/**
 * @return [ListenableWorker.Result] for a failed work, with [WorkerError] as data
 * @see androidx.work.ListenableWorker.Result.failure
 */
@Suppress("unused") // Receiver is unused, but we want it to be scoped to ListenableWorker
fun ListenableWorker.failure(throwable: Throwable) =
    failure(WorkerError(throwable.message ?: EMPTY_STRING).toWorkData())

/**
 * @return [WorkerError] from receiver [Data]
 * @throws IllegalStateException is [Data] does not contain String representation of [WorkerError]
 */
fun Data.error(): WorkerError =
    deserialize(WorkerError.serializer())
