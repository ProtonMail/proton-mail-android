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

package ch.protonmail.android.labels.domain.usecase

import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.data.local.model.LabelId
import ch.protonmail.android.labels.data.local.model.LabelType
import ch.protonmail.android.labels.data.mapper.LabelEntityDomainMapper
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.presentation.model.StandardFolderLocation
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAllLabelsTest {

    private val labelsMapper = LabelEntityDomainMapper()

    @MockK
    private lateinit var accountManager: AccountManager

    @MockK
    private lateinit var repository: LabelRepository

    private lateinit var useCase: GetAllLabels

    private val testUserId = UserId("TestUser")

    private val testColor = "blue"
    private val testPath = "testPath"
    private val testParentId = "testParentId"
    private val sheetType = LabelType.MESSAGE_LABEL
    private val emailsCount = 0
    private val testLabelId1 = LabelId("labelId1")
    private val testLabelId2 = LabelId("labelId2")

    private val testLabelName1 = "labelName1"
    private val testLabelName2 = "labelName2"

    private val label1 = Label(
        testLabelId1,
        testLabelName1,
        testColor,
        sheetType,
        testPath,
        testParentId,
        contactEmailsCount = emailsCount
    )
    private val label2 = Label(
        testLabelId2,
        testLabelName2,
        testColor,
        sheetType,
        testPath,
        testParentId,
        contactEmailsCount = emailsCount
    )

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        useCase = GetAllLabels(labelsMapper, accountManager, repository)
    }

    @Test
    fun verifyThatLabelsTypeAreReceivedAndProcessedCorrectly() = runBlocking {
        // given
        val labelEntity1 = LabelEntity(
            testLabelId1,
            testUserId,
            testLabelName1,
            testColor,
            1,
            LabelType.MESSAGE_LABEL,
            testPath,
            testParentId,
            0,
            0,
            0
        )
        val labelEntity2 = LabelEntity(
            testLabelId2,
            testUserId,
            testLabelName2,
            testColor,
            1,
            LabelType.MESSAGE_LABEL,
            testPath,
            testParentId,
            0,
            0,
            0
        )

        val expected = listOf(label1, label2)
        coEvery { repository.findAllLabels(testUserId, false) } returns listOf(labelEntity1, labelEntity2)

        // when
        val result = useCase.invoke()

        // then
        assertEquals(expected, result)
    }

    private fun getAllStandardFolders(): List<LabelActonItemUiModel> =
        StandardFolderLocation.values()
            .map { location ->
                LabelActonItemUiModel(
                    labelId = location.id,
                    iconRes = location.iconRes,
                    titleRes = location.title,
                    labelType = LabelType.FOLDER.typeInt
                )
            }


}
