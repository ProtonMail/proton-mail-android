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
package ch.protonmail.android.uitests.tests.suites

import ch.protonmail.android.uitests.tests.composer.AttachmentsTests
import ch.protonmail.android.uitests.tests.composer.ForwardMessageTests
import ch.protonmail.android.uitests.tests.composer.ReplyToMessageTests
import ch.protonmail.android.uitests.tests.composer.SendNewMessageTests
import ch.protonmail.android.uitests.tests.contacts.ContactsTests
import ch.protonmail.android.uitests.tests.drafts.DraftsTests
import ch.protonmail.android.uitests.tests.inbox.InboxTests
import ch.protonmail.android.uitests.tests.inbox.SearchTests
import ch.protonmail.android.uitests.tests.labelsfolders.LabelsFoldersTests
import ch.protonmail.android.uitests.tests.login.LoginTests
import ch.protonmail.android.uitests.tests.manageaccounts.MultiuserManagementTests
import ch.protonmail.android.uitests.tests.menu.MenuTests
import ch.protonmail.android.uitests.tests.messagedetail.MessageDetailTests
import ch.protonmail.android.uitests.tests.settings.AccountSettingsTests
import ch.protonmail.android.uitests.tests.settings.PrivacyAccountSettingsTests
import ch.protonmail.android.uitests.tests.settings.SettingsTests
import ch.protonmail.android.uitests.tests.settings.SwipeGesturesTests
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Composer tests
    AttachmentsTests::class,
    SendNewMessageTests::class,
    ForwardMessageTests::class,
    ReplyToMessageTests::class,
    // Contacts tests
    ContactsTests::class,
    // Drafts tests
    DraftsTests::class,
    // Inbox tests
    InboxTests::class,
    // Labels and folders tests
    LabelsFoldersTests::class,
    // Login tests
    LoginTests::class,
    // Multi-user management tests
    MultiuserManagementTests::class,
    // Menu tests
    MenuTests::class,
    // Message detail tests
    MessageDetailTests::class,
    // Search tests
    SearchTests::class,
    // Settings tests
    AccountSettingsTests::class,
    PrivacyAccountSettingsTests::class,
    SettingsTests::class,
    SwipeGesturesTests::class
)
class RegressionSuite
