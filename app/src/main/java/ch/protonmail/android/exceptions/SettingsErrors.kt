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

import android.net.Uri
import ch.protonmail.android.R
import studio.forface.viewstatestore.ViewState

/** An [Exception] that will be thrown when is it impossible to get a `Ringtone` for the user */
internal class InvalidRingtoneException( cause: Throwable, uri: Uri ) : Exception(
        "Error loading Ringtone from the given Uri: `$uri`", cause
)

/** A [ViewState.Error] that will be generated when ringtone is invalid */
internal class InvalidRingtoneError( throwable: InvalidRingtoneException ) :
        ViewState.Error( throwable, R.string.ringtone_invalid )

/** An [Exception] that will be thrown when it is impossible to get default `Ringtone` */
internal class NoDefaultRingtoneException : Exception(
        "Error loading default Ringtone"
)

/** A [ViewState.Error] that will be generated when it is impossible to get default `Ringtone` */
internal class NoDefaultRingtoneError( throwable: NoDefaultRingtoneException ) :
        ViewState.Error( throwable, R.string.ringtone_no_default )