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
package ch.protonmail.android.uitests.testsHelper

import org.jboss.aerogear.security.otp.Totp
import org.jetbrains.annotations.Contract

/**
 * Created by Nikola Nolchevski on 31-Mar-20.
 */
class TestUser @Contract(pure = true) private constructor(var name: String, var email: String, var password: String, var mailboxPassword: String?, var twoFASecurityKey: String?) {

    val twoFACode: String get() = Totp(this.twoFASecurityKey).now()

    companion object {
        @Contract(value = " -> new", pure = true)
        fun twoPassUser(): TestUser {
            return TestUser("PMAutomationRobot1", "PMAutomationRobot1@protonmail.com", "auto123", "123", null)
        }

        @Contract(value = " -> new", pure = true)
        fun onePassUser(): TestUser {
            return TestUser("PMAutomationRobot2", "PMAutomationRobot2@protonmail.com", "auto123", null, null)
        }

        @Contract(value = " -> new", pure = true)
        fun onePassUserWith2FA(): TestUser {
            return TestUser("PMAutomationRobot5", "PMAutomationRobot5@protonmail.com", "auto123", null, "ATBG2FIRWHEFBLKS3ANCRPN4TCHQUP5B")
        }

        /*
    Important: Please make sure you saved the recovery codes.
    Otherwise you can permanently lose access to your account if you lose your 2FA device.
    If you lose your two-factor-enabled device, these codes can be used instead of the 6-digit
    2FA code to log into your account. Each code can only be used once.

    b3b24eac 1d5d9bd7 827eae87 7ca7f6a7 6dbcbd94 b3ec5672 8a5216c2 05754b32
    87adde60 b843ea5a 563d1844 f2b72b77 1b6b2021 f8eb2fc2 6aa74cf2 6ebf2c1f
     */
        @Contract(value = " -> new", pure = true)
        fun twoPassUserWith2FA(): TestUser {
            return TestUser("PMAutomationRobot6", "PMAutomationRobot6@protonmail.com", "auto123", "123", "37C2M62FFPI44ZV6IE6UIBBDRIADZL67")
        } /*
    Important: Please make sure you saved the recovery codes.
    Otherwise you can permanently lose access to your account if you lose your 2FA device.
    If you lose your two-factor-enabled device, these codes can be used instead of the 6-digit
    2FA code to log into your account. Each code can only be used once.

    4b338ac4 a14035de 48b13e89 9666f608 4e839997 6dee3836 5c77aa0f 8ec65a93
    08435e8a 867070ff bbf90720 5ec6f25d 2e9c1c6e eb702504 ed153cec 1bb22085
    */
    }

}