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

package ch.protonmail.android.labels.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.utils.buildFolders
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveFoldersEligibleAsParentTest {

    private val labelRepository: LabelRepository = mockk()
    private val observeFolder = ObserveFoldersEligibleAsParent(labelRepository)

    @Test
    fun `filters 3rd level folders, as not eligible as parent`() = runBlockingTest {
        // given
        val input = buildFolders {
            folder("first")
            folder("second") {
                folder("second.first")
                folder("second.second") {
                    folder("second.second.first")
                }
            }
            folder("third") {
                folder("third.first") {
                    folder("third.first.first")
                    folder("third.first.second")
                }
                folder("third.second")
            }
            folder("fourth") {
                folder("fourth.first") {
                    folder("fourth.first.first")
                }
            }
        }
        val expected = buildFolders {
            folder("first")
            folder("second") {
                folder("second.first")
                folder("second.second")
            }
            folder("third") {
                folder("third.first")
                folder("third.second")
            }
            folder("fourth") {
                folder("fourth.first")
            }
        }

        every { labelRepository.observeAllLabelsOrFoldersWithChildren(USER_ID, LabelType.FOLDER, any()) } returns
            flowOf(input)

        // when
        observeFolder(USER_ID).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    companion object TestData {

        val USER_ID = UserId("test")
    }
}
