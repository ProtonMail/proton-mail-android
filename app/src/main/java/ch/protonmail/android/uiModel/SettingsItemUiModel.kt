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
package ch.protonmail.android.uiModel

import android.view.View
import com.google.gson.annotations.SerializedName

/**
 * @property settingDisabled is used if we don't wanna show the feature with [settingId] to user
 */
data class SettingsItemUiModel(
    @field:SerializedName("setting_id")
    var settingId: String = "",
    var settingHeader: String? = "",
    var settingValue: String? = "",
    @field:SerializedName("setting_hasValue")
    var settingHasValue: Boolean = false,
    @field:SerializedName("is_section")
    var isSection: Boolean = false,
    @field:SerializedName("setting_type")
    var settingType: SettingsItemTypeEnum? = SettingsItemTypeEnum.INFO,
    var settingsDescription: String? = "",
    var settingsHint: String? = "",
    var enabled: Boolean = false,
    var settingDisabled: Boolean = false,
    var toggleListener: ((View, Boolean) -> Unit)? = { _: View, _: Boolean -> },
    var editTextListener: (View) -> Unit = {},
    var editTextChangeListener: (String) -> Unit = {}
) {
    enum class SettingsItemTypeEnum {
        @SerializedName("info")
        INFO,
        @SerializedName("drill")
        DRILL_DOWN,
        @SerializedName("button")
        BUTTON,
        @SerializedName("toggle")
        TOGGLE,
        @SerializedName("spinner")
        SPINNER,
        @SerializedName("edit")
        EDIT_TEXT,
        @SerializedName("toggle_n_edit")
        TOGGLE_N_EDIT
    }
}
