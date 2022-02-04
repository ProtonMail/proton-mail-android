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

package ch.protonmail.android.utils.webview

import android.content.Context
import android.content.res.Configuration
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class SetUpWebViewDarkModeHandlingIfSupported @Inject constructor(
    private val getViewInDarkModeMessagePreference: GetViewInDarkModeMessagePreference
) {

    suspend operator fun invoke(context: Context, userId: UserId, webView: WebView, messageId: String) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            val viewInDarkModeMessagePreference = getViewInDarkModeMessagePreference(context, userId, messageId)

            when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    if (viewInDarkModeMessagePreference) {
                        WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
                    } else {
                        WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
                    }
                }
                Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
                }
            }
        }
    }
}
