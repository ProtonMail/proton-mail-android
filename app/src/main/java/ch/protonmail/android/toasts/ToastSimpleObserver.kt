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

import android.content.Context
import androidx.lifecycle.Observer
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.extensions.showToast

class ToastSimpleObserver(val context: Context) : Observer<Event<ToastStatus>> {
    override fun onChanged(event: Event<ToastStatus>?) {
        val status = event?.getContentIfNotHandled() ?: return
        when (status) {
            is ToastStatus.Id -> context.showToast(status.textId, status.toastLength)
            is ToastStatus.Text -> context.showToast(status.text, status.toastLength)
        }
    }
}
