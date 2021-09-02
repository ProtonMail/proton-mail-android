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

package ch.protonmail.android.labels.data

import app.cash.turbine.test
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.labels.data.local.LabelDao
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.data.local.model.LabelId
import ch.protonmail.android.labels.data.local.model.LabelType
import ch.protonmail.android.labels.data.mapper.LabelEntityApiMapper
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.data.remote.model.LabelsResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class LabelRepositoryImplTest : CoroutinesTest {

    private val labelDao = mockk<LabelDao>()
    private val api = mockk<ProtonMailApi>()
    private val labelMapper = LabelEntityApiMapper()
    private val networkConnectivityManager = mockk<NetworkConnectivityManager>()

    private val repository = LabelRepositoryImpl(labelDao, api, labelMapper, networkConnectivityManager)

    private val dbFlow = MutableSharedFlow<List<LabelEntity>>(replay = 2, onBufferOverflow = BufferOverflow.SUSPEND)

    @BeforeTest
    fun setup() {
        coEvery {
            labelDao.observeAllLabels(
                testUserId
            )
        } returns dbFlow
    }

    @Test
    fun verifyThatObserveAllLabelsStartsWithFetchingDataFromRemoteApiAndSavingInDb() = runBlockingTest {
        // given
        coEvery { api.fetchLabels(testUserId) } returns ApiResult.Success(LabelsResponse(listOf(testLabel1)))
        coEvery { api.fetchContactGroups(testUserId) } returns ApiResult.Success(LabelsResponse(listOf(testLabel2)))
        coEvery { api.fetchFolders(testUserId) } returns ApiResult.Success(LabelsResponse(listOf(testLabel3)))
        coEvery { networkConnectivityManager.isInternetConnectionPossible() } returns true

        val dataToSaveInDb = listOf(testLabelEntity1, testLabelEntity2, testLabelEntity3)
        coEvery { labelDao.insertOrUpdate(*anyVararg()) } answers {
            dbFlow.tryEmit(dataToSaveInDb)
        }

        val subsequentDbReply = listOf(testLabelEntity1)

        // when
        repository.observeAllLabels(testUserId, true).test {

            // then
            coVerify { labelDao.insertOrUpdate(*anyVararg()) }
            assertEquals(dataToSaveInDb, expectItem())
            dbFlow.tryEmit(subsequentDbReply)
            assertEquals(subsequentDbReply, expectItem())
        }
    }

    @Test
    fun verifyThatFetchLabelsDoesNotRefreshTheDataWhenThereIsNoConnectivity() = runBlocking {
        // given
        coEvery { networkConnectivityManager.isInternetConnectionPossible() } returns false
        val dbReply = listOf(testLabelEntity1)
        val shallRefresh = true

        // when
        repository.observeAllLabels(testUserId, shallRefresh).test {

            // then
            dbFlow.emit(dbReply)
            assertEquals(dbReply, expectItem())
            coVerify(exactly = 0) { labelDao.insertOrUpdate(*anyVararg()) }
        }
    }


    @Test
    fun verifyThatFetchLabelsDoesNotRefreshTheDataWhenThereIsConnectivityButItWasNotRequested() = runBlocking {
        // given
        coEvery { networkConnectivityManager.isInternetConnectionPossible() } returns true
        val dbReply = listOf(testLabelEntity1)
        val shallRefresh = false

        // when
        repository.observeAllLabels(testUserId, shallRefresh).test {

            // then
            dbFlow.emit(dbReply)
            assertEquals(dbReply, expectItem())
            coVerify(exactly = 0) { labelDao.insertOrUpdate(*anyVararg()) }
        }
    }

    companion object {

        private const val labelId1 = "labelId1"
        private const val labelId2 = "labelId2"
        private const val labelId3 = "labelId3"
        private const val labelName1 = "labelName1"
        private const val labelName2 = "labelName2"
        private const val labelName3 = "labelName3"
        private const val labelColor = "labelColor11"
        private const val testPath = "a/bcPath"
        private const val testParentId = "parentIdForTests"
        val testUserId = UserId("testUser")

        val testLabel1 = LabelApiModel(
            id = labelId1,
            name = labelName1,
            path = testPath,
            color = labelColor,
            type = LabelType.MESSAGE_LABEL,
            notify = 0,
            order = 0,
            expanded = null,
            sticky = null,
            parentId = testParentId
        )
        val testLabel2 = LabelApiModel(
            id = labelId2,
            name = labelName2,
            path = testPath,
            color = labelColor,
            type = LabelType.CONTACT_GROUP,
            notify = 0,
            order = 0,
            expanded = null,
            sticky = null,
            parentId = testParentId
        )
        val testLabel3 = LabelApiModel(
            id = labelId3,
            name = labelName3,
            path = testPath,
            color = labelColor,
            type = LabelType.FOLDER,
            notify = 0,
            order = 0,
            expanded = null,
            sticky = null,
            parentId = testParentId
        )

        val testLabelEntity1 = LabelEntity(
            id = LabelId(labelId1),
            userId = testUserId,
            name = labelName1,
            path = testPath,
            color = labelColor,
            type = LabelType.MESSAGE_LABEL,
            notify = 0,
            order = 0,
            expanded = 0,
            sticky = 0,
            parentId = testParentId
        )
        val testLabelEntity2 = LabelEntity(
            id = LabelId(labelId2),
            userId = testUserId,
            name = labelName1,
            path = testPath,
            color = labelColor,
            type = LabelType.CONTACT_GROUP,
            notify = 0,
            order = 0,
            expanded = 0,
            sticky = 0,
            parentId = testParentId
        )
        val testLabelEntity3 = LabelEntity(
            id = LabelId(labelId3),
            userId = testUserId,
            name = labelName1,
            path = testPath,
            color = labelColor,
            type = LabelType.FOLDER,
            notify = 0,
            order = 0,
            expanded = 0,
            sticky = 0,
            parentId = testParentId
        )


    }
}
