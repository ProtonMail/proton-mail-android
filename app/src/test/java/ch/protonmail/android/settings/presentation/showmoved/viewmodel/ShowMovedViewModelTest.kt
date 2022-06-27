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

package ch.protonmail.android.settings.presentation.showmoved.viewmodel

import ch.protonmail.android.settings.domain.usecase.GetMailSettings
import ch.protonmail.android.settings.domain.usecase.UpdateShowMoved
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Test

private val TEST_USER_ID = UserId("userId")

class ShowMovedViewModelTest : ArchTest, CoroutinesTest {

    private var accountManager: AccountManager = mockk {
        every { getPrimaryUserId() } returns flowOf(TEST_USER_ID)
    }

    private var updateShowMoved: UpdateShowMoved = mockk(relaxUnitFun = true)

    private var getMailSettings: GetMailSettings = mockk(relaxed = true)

    private val viewModel by lazy {
        ShowMovedViewModel(
            accountManager = accountManager,
            updateShowMoved = updateShowMoved,
            getMailSettings = getMailSettings
        )
    }

    @Test
    fun `invokes use case with correct parameters when updating Show Moved`() = runBlockingTest {

        // given
        viewModel.setSettingCurrentValue()
        val value = ShowMoved.Both

        // when
        viewModel.onToggle(value)

        // then
        coVerify { updateShowMoved(TEST_USER_ID, value) }
    }

}
