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

package ch.protonmail.android.feature.rating

import ch.protonmail.android.testdata.UserTestData
import kotlinx.coroutines.test.runTest
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.featureflag.domain.entity.FeatureId
import me.proton.core.featureflag.domain.entity.Scope
import org.junit.Test
import kotlin.test.assertEquals

class ShowReviewAppInMemoryRepositoryTest {

    private val showReviewAppRepository = ShowReviewAppInMemoryRepository()

    @Test
    fun saveSkipsFeatureFlagWithInvalidUserId() = runTest {
        // given
        val invalidFlag = FeatureFlag(null, FeatureId("any"), Scope.Unknown, false, false)
        val validFlag = FeatureFlag(UserTestData.userId, FeatureId("any"), Scope.Unknown, false, true)

        // when
        showReviewAppRepository.save(invalidFlag)
        showReviewAppRepository.save(validFlag)

        // then
        val expected = mapOf(UserTestData.userId to true)
        assertEquals(expected, showReviewAppRepository.featureFlags)
    }

}