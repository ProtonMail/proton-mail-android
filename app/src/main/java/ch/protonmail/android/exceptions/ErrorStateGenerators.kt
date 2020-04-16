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
package ch.protonmail.android.exceptions

import studio.forface.viewstatestore.ErrorStateGenerator

/**
 * An instance of [ErrorStateGenerator] that maps [Throwable] to [ViewState.Error]
 * Here goes all the mapping logic for deliver [ViewState.Error] to the UI.
 * In case a **modularization** in expected in future plans, this won't be a problem, since
 * [ErrorStateGenerator] has a `plus` operator for concatenate them
 *
 * @author Davide Farella
 */
internal val errorStateGenerator: ErrorStateGenerator = { throwable ->
    when ( throwable ) {
        is InvalidRingtoneException -> InvalidRingtoneError( throwable )
        is NoDefaultRingtoneException -> NoDefaultRingtoneError( throwable )

        else -> default
    }
}