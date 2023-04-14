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

import android.app.Activity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class StartRateAppFlowTest {

    private val activity: Activity = mockk(relaxed = true)
    private val reviewInfo: ReviewInfo = mockk()
    private val requestMock: Task<ReviewInfo> = mockk {
        every { result } returns reviewInfo
        every { addOnSuccessListener(any()) } answers {
            if (this@mockk.isSuccessful) {
                val successListener = firstArg<OnSuccessListener<ReviewInfo>>()
                successListener.onSuccess(reviewInfo)
            }
            Tasks.forResult(reviewInfo)
        }
        every { addOnFailureListener(any()) } answers {
            val exception = Exception("Failed")
            if (!this@mockk.isSuccessful) {
                val failureListener = firstArg<OnFailureListener>()
                failureListener.onFailure(exception)
            }
            Tasks.forException(exception)
        }
    }
    private val reviewManagerMock: ReviewManager = mockk {
        every { requestReviewFlow() } returns requestMock
    }

    private val startRateAppFlow = StartRateAppFlow(reviewManagerMock)

    @Test
    fun `succeeds when review flow request is successful`() {
        // given
        every { requestMock.isSuccessful } returns true
        every { reviewManagerMock.launchReviewFlow(activity, reviewInfo) } returns mockk()

        // when
        startRateAppFlow(activity)

        // then
        verify { reviewManagerMock.launchReviewFlow(activity, reviewInfo) }
    }

    @Test
    fun `fails when review flow request is not successful`() {
        // given
        every { requestMock.isSuccessful } returns false
        every { reviewManagerMock.launchReviewFlow(activity, reviewInfo) } returns mockk()

        // when
        startRateAppFlow(activity)

        // then
        verify(exactly = 0) { reviewManagerMock.launchReviewFlow(any(), any()) }
    }
}