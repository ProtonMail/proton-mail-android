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

package ch.protonmail.android.usecase

import android.content.Context
import ch.protonmail.android.domain.entity.Id
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject
import ch.protonmail.android.api.models.User as LegacyUser

@Deprecated("Use new User entity", ReplaceWith("LoadUser"))
class LoadLegacyUser @Inject constructor(
    private val context: Context,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(userId: Id): LegacyUser = withContext(dispatchers.Io) {
        @Suppress("DEPRECATION")
        LegacyUser.load(userId, context)
    }

}
