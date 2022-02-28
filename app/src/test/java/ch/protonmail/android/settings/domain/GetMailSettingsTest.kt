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

package ch.protonmail.android.settings.domain

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountDetails
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.domain.type.StringEnum
import me.proton.core.mailsettings.domain.entity.ComposerMode
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.MessageButtons
import me.proton.core.mailsettings.domain.entity.MimeType
import me.proton.core.mailsettings.domain.entity.PMSignature
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.mailsettings.domain.entity.ShowImage
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.mailsettings.domain.entity.ViewLayout
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetMailSettingsTest {

    private var accountManager: AccountManager = mockk(relaxed = true)

    private var repository: MailSettingsRepository = mockk()

    private lateinit var getMailSettings: GetMailSettings

    private val userId = UserId("userId")
    private val account = Account(
        userId,
        "username",
        "email",
        AccountState.Ready,
        null,
        null,
        AccountDetails(null, null)
    )

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        getMailSettings = GetMailSettings(accountManager, repository, TestDispatcherProvider)
    }

    @Test
    fun verifyMailSettingsAreFetched() = runBlockingTest {

        // given
        coEvery { accountManager.getPrimaryAccount() } returns flowOf(account)
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(userId)
        coEvery { accountManager.getAccount(userId) } returns flowOf(account)

        coEvery { repository.getMailSettingsFlow(userId) } returns flowOf(
            DataResult.Processing(ResponseSource.Remote), DataResult.Success(ResponseSource.Remote, mailSettings())
        )

        // when
        val result = getMailSettings().take(2).toList()

        // then
        assertEquals(GetMailSettings.MailSettingsState.Success(mailSettings()), result[1])
    }

    private fun mailSettings() = MailSettings(
        userId = UserId("userId"),
        displayName = null,
        signature = null,
        autoSaveContacts = true,
        composerMode = IntEnum(1, ComposerMode.Maximized),
        messageButtons = IntEnum(1, MessageButtons.UnreadFirst),
        showImages = IntEnum(1, ShowImage.Remote),
        showMoved = IntEnum(1, ShowMoved.Drafts),
        viewMode = IntEnum(1, ViewMode.NoConversationGrouping),
        viewLayout = IntEnum(1, ViewLayout.Row),
        swipeLeft = IntEnum(1, SwipeAction.Spam),
        swipeRight = IntEnum(1, SwipeAction.Spam),
        shortcuts = true,
        pmSignature = IntEnum(1, PMSignature.Disabled),
        numMessagePerPage = 1,
        draftMimeType = StringEnum("text/plain", MimeType.PlainText),
        receiveMimeType = StringEnum("text/plain", MimeType.PlainText),
        showMimeType = StringEnum("text/plain", MimeType.PlainText),
        enableFolderColor = true,
        inheritParentFolderColor = true,
        rightToLeft = true,
        attachPublicKey = true,
        sign = true,
        pgpScheme = IntEnum(1, PackageType.ProtonMail),
        promptPin = true,
        stickyLabels = true,
        confirmLink = true
    )
}
