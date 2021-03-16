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

package ch.protonmail.android.utils.notifier

import ch.protonmail.android.core.UserManager
import ch.protonmail.android.servers.notification.INotificationServer
import javax.inject.Inject

class AndroidErrorNotifier @Inject constructor(
    private val notificationServer: INotificationServer,
    private val userManager: UserManager
) : ErrorNotifier {

    override fun showPersistentError(errorMessage: String, messageSubject: String?) {
        notificationServer.notifySaveDraftError(errorMessage, messageSubject, userManager.username)
    }

}
