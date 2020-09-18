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
package ch.protonmail.android.contacts.details

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import ch.protonmail.android.exceptions.BadImageUrlError
import ch.protonmail.android.exceptions.ImageNotFoundError
import ch.protonmail.android.exceptions.errorStateGenerator
import ch.protonmail.android.testAndroid.ViewStateStoreTest
import ch.protonmail.android.testAndroid.viewStateStoreTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.assertIs
import studio.forface.viewstatestore.ViewState
import java.io.FileNotFoundException
import kotlin.test.Test

/**
 * __Unit__ test suite for [ContactDetailsViewModel]
 *
 * Verifies:
 * * handling of error loading profile pictures
 *
 * @author Davide Farella
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ContactDetailsViewModelTest :
    ArchTest,
    CoroutinesTest,
    ViewStateStoreTest by viewStateStoreTest(errorStateGenerator) {

    @Test
    fun `getBitmapFromURL handles timeout`() = coroutinesTest {

        // GIVEN
        val viewModel = ContactDetailsViewModel(
            dispatchers,
            downloadFile = mockk {
                coEvery { invoke(url = any()) } coAnswers {
                    withTimeout(1) {
                        delay(2)
                        mockk()
                    }
                }
            },
            contactDetailsRepository = mockk(),
        )

        // WHEN
        viewModel.getBitmapFromURL("http://hello.world")
        advanceTimeBy(5)

        // THEN
        assertIs<ImageNotFoundError>(viewModel.profilePicture.await())
    }

    @Test
    fun `getBitmapFromURL handles malformed url`() = coroutinesTest {

        // GIVEN
        val viewModel = ContactDetailsViewModel(
            dispatchers,
            downloadFile = mockk(),
            contactDetailsRepository = mockk(),
        )

        // WHEN
        viewModel.getBitmapFromURL("malformed_url")

        // THEN
        assertIs<BadImageUrlError>(viewModel.profilePicture.await())
    }

    @Test
    fun `getBitmapFromURL handles 404`() = coroutinesTest {
        // GIVEN
        val viewModel = ContactDetailsViewModel(
            dispatchers,
            downloadFile = mockk {
                coEvery { invoke(url = any()) } answers  { throw FileNotFoundException() }
            },
            contactDetailsRepository = mockk(),
        )

        // WHEN
        viewModel.getBitmapFromURL("http://hello.world")

        // THEN
        assertIs<ImageNotFoundError>(viewModel.profilePicture.await())
    }

    @Test
    fun `getBitmapFromURL load image correctly`() = coroutinesTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns mockk()

        // GIVEN
        val viewModel = ContactDetailsViewModel(
            dispatchers,
            downloadFile = mockk {
                coEvery { invoke(url = any()) } returns mockk()
            },
            contactDetailsRepository = mockk(),
        )

        // WHEN
        viewModel.getBitmapFromURL("http://hello.world")

        // THEN
        assertIs<ViewState.Success<Bitmap>>(viewModel.profilePicture.await())

        unmockkStatic(BitmapFactory::class)
    }
}
