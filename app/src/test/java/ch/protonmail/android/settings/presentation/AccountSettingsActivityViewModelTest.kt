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

package ch.protonmail.android.settings.presentation

import ch.protonmail.android.settings.domain.usecase.GetMailSettings
import ch.protonmail.android.settings.domain.usecase.ObserveUserSettings
import ch.protonmail.android.settings.domain.usecase.UpdateViewMode
import ch.protonmail.android.usecase.delete.ClearUserMessagesData
import ch.protonmail.android.utils.resources.StringResourceResolver
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.usersettings.domain.entity.Flags
import me.proton.core.usersettings.domain.entity.PasswordSetting
import me.proton.core.usersettings.domain.entity.RecoverySetting
import me.proton.core.usersettings.domain.entity.TwoFASetting
import me.proton.core.usersettings.domain.entity.UserSettings
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountSettingsActivityViewModelTest : ArchTest by ArchTest(),
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private var accountManager: AccountManager = mockk(relaxed = true)

    private var clearUserMessagesData: ClearUserMessagesData = mockk(relaxed = true)

    private var updateViewMode: UpdateViewMode = mockk(relaxed = true)

    private var getMailSettings: GetMailSettings = mockk(relaxed = true)

    private val stringResourceResolver: StringResourceResolver = mockk()

    private val observeUserSettings: ObserveUserSettings = mockk()

    private var viewModel: AccountSettingsActivityViewModel = mockk(relaxed = true)

    private val userId = UserId("userId")
    private val stringResource = "stringResource"

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel =
            AccountSettingsActivityViewModel(
                accountManager = accountManager,
                clearUserMessagesData = clearUserMessagesData,
                updateViewMode = updateViewMode,
                getMailSettings = getMailSettings,
                stringResourceResolver = stringResourceResolver,
                observeUserSettings = observeUserSettings
            )

        coEvery { accountManager.getPrimaryUserId() } returns flowOf(userId)
        every { stringResourceResolver(any()) } returns stringResource
    }

    @Test
    fun verifyThatWhenWeChangeViewModeUsersMessagesAndConversationsAreCleared() = runBlockingTest {

        // when
        viewModel.changeViewMode(ViewMode.NoConversationGrouping)

        // then
        coVerify { clearUserMessagesData(userId) }
    }

    @Test
    fun verifyThatWhenWeChangeViewModeUpdateViewModeUseCaseIsCalled() = runBlockingTest {

        // when
        viewModel.changeViewMode(ViewMode.ConversationGrouping)

        // then
        coVerify { updateViewMode(userId, ViewMode.ConversationGrouping) }
    }

    @Test
    fun `should emit recovery email when user settings are successfully fetched`() = runBlockingTest {
        // given
        val recoveryEmail = buildRecoverySetting(value = "test@protonmail.com")
        val userSettings = buildUserSettings(userId, email = recoveryEmail)
        coEvery { observeUserSettings(userId) } returns flowOf(userSettings)

        // when
        val result = viewModel.getRecoveryEmailFlow()

        // then
        assertEquals(recoveryEmail.value, result.first())
    }

    @Test
    fun `should emit placeholder string when recovery email from user settings is null`() = runBlockingTest {
        // given
        val userSettings = buildUserSettings(userId)
        coEvery { observeUserSettings(userId) } returns flowOf(userSettings)

        // when
        val result = viewModel.getRecoveryEmailFlow()

        // then
        assertEquals(stringResource, result.first())
    }

    @Test
    fun `should emit placeholder string when recovery email from user settings is an empty string`() = runBlockingTest {
        // given
        val recoveryEmail = buildRecoverySetting(value = EMPTY_STRING)
        val userSettings = buildUserSettings(userId, email = recoveryEmail)
        coEvery { observeUserSettings(userId) } returns flowOf(userSettings)

        // when
        val result = viewModel.getRecoveryEmailFlow()

        // then
        assertEquals(stringResource, result.first())
    }

    private fun buildRecoverySetting(
        value: String? = null,
        status: Int? = null,
        notify: Boolean? = null,
        reset: Boolean? = null
    ) = RecoverySetting(
        value = value,
        status = status,
        notify = notify,
        reset = reset
    )

    private fun buildUserSettings(
        userId: UserId,
        email: RecoverySetting? = null,
        phone: RecoverySetting? = null,
        password: PasswordSetting = PasswordSetting(null, null),
        twoFA: TwoFASetting? = null,
        news: Int? = null,
        locale: String? = null,
        logAuth: IntEnum<UserSettings.LogAuth>? = null,
        invoiceText: String? = null,
        density: IntEnum<UserSettings.Density>? = null,
        theme: String? = null,
        themeType: Int? = null,
        weekStart: IntEnum<UserSettings.WeekStart>? = null,
        dateFormat: IntEnum<UserSettings.DateFormat>? = null,
        timeFormat: IntEnum<UserSettings.TimeFormat>? = null,
        welcome: Boolean? = null,
        earlyAccess: Boolean? = null,
        flags: Flags? = null
    ) = UserSettings(
        userId = userId,
        email = email,
        phone = phone,
        password = password,
        twoFA = twoFA,
        news = news,
        locale = locale,
        logAuth = logAuth,
        invoiceText = invoiceText,
        density = density,
        theme = theme,
        themeType = themeType,
        weekStart = weekStart,
        dateFormat = dateFormat,
        timeFormat = timeFormat,
        welcome = welcome,
        earlyAccess = earlyAccess,
        flags = flags
    )
}
