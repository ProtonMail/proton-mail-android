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
package ch.protonmail.android.toasts

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import ch.protonmail.android.utils.Event

@Deprecated("This should not be used! Toast's should be handled from the UI! No replacement provided")
interface IToastViewModel {
    val toast: LiveData<Event<ToastStatus>>
    fun postToast(@StringRes textId: Int, duration: Int = Toast.LENGTH_LONG)
    fun postToast(text: String, duration: Int = Toast.LENGTH_LONG)
}
