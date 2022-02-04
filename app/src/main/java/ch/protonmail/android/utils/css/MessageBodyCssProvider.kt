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

package ch.protonmail.android.utils.css

import android.content.Context
import ch.protonmail.android.R
import ch.protonmail.android.usecase.IsAppInDarkMode
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.webview.GetViewInDarkModeMessagePreference
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class MessageBodyCssProvider @Inject constructor(
    private val context: Context,
    private val isAppInDarkMode: IsAppInDarkMode,
    private val getViewInDarkModeMessagePreference: GetViewInDarkModeMessagePreference
) {

    fun getMessageBodyCss(): String = AppUtil.readTxt(context, R.raw.css_reset_with_custom_props)

    suspend fun getMessageBodyDarkModeCss(userId: UserId, messageId: String): String =
        if (isAppInDarkMode(context) && getViewInDarkModeMessagePreference(context, userId, messageId)) {
            AppUtil.readTxt(context, R.raw.css_reset_dark_mode_only)
        } else {
            EMPTY_STRING
        }
}
