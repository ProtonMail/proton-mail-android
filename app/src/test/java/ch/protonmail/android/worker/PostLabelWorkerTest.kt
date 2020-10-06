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

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.data.LabelRepository
import ch.protonmail.android.data.RoomLabelRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Assert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PostLabelWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var repository: LabelRepository

    @RelaxedMockK
    private lateinit var labelApiResponse: LabelResponse

    private lateinit var worker: PostLabelWorker

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        worker = PostLabelWorker(
            context,
            parameters,
            apiManager,
            repository
        )
    }

    @Test
    fun `worker does not fail when labelId parameter is not passed`() {
        every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_ID) } returns null

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `worker fails when labelName parameter is not passed`() {
        every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_NAME) } returns null

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `worker fails when color parameter is not passed`() {
        every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_COLOR) } returns null

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `workers saves label in repository when creation succeeds`() {
        every { labelApiResponse.label } returns Label("ID", "name", "color")
        every { labelApiResponse.hasError() } returns false
        every { apiManager.createLabel(any()) } returns labelApiResponse

        val result = worker.doWork()

        verify { repository.saveLabel(any()) }
        assertEquals(ListenableWorker.Result.success(), result)
    }
}