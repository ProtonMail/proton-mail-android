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
package ch.protonmail.android.uitests

import ch.protonmail.android.uitests.tests.composer.ComposerTests
import ch.protonmail.android.uitests.tests.contacts.ContactsTests
import ch.protonmail.android.uitests.tests.inbox.InboxTests
import ch.protonmail.android.uitests.tests.login.LoginTests
import ch.protonmail.android.uitests.tests.menu.MenuTests
import ch.protonmail.android.uitests.tests.settings.AccountSettingsTests
import ch.protonmail.android.uitests.tests.settings.SettingsTests
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    AccountSettingsTests::class,
    ComposerTests::class,
    ContactsTests::class,
    InboxTests::class,
    LoginTests::class,
    ContactsTests::class,
    MenuTests::class,
    SettingsTests::class
)
class TestSuite
