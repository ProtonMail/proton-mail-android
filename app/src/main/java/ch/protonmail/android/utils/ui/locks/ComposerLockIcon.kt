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
package ch.protonmail.android.utils.ui.locks

import ch.protonmail.android.R
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.enumerations.PackageType

class ComposerLockIcon(
    private val sendPreference: SendPreference,
    private val isMessagePasswordEncrypted: Boolean
) : LockIcon {

    override fun getIcon(): Int {
        if (sendPreference.isEncryptionEnabled) {
            return if (sendPreference.hasPinnedKeys()) {
                R.string.pgp_lock_check
            } else {
                R.string.lock_default
            }
        }
        if (isMessagePasswordEncrypted) {
            return R.string.lock_default
        }
        return if (sendPreference.isSignatureEnabled) {
            R.string.pgp_lock_pen
        } else {
            R.string.no_icon
        }
    }

    override fun getColor(): Int {
        if (sendPreference.encryptionScheme == PackageType.PM ||
            !sendPreference.isEncryptionEnabled && isMessagePasswordEncrypted
        ) {
            return R.color.icon_purple
        }
        return if (sendPreference.isPGP) {
            R.color.icon_green
        } else {
            R.color.icon_warning
        }
    }

    override fun getTooltip(): Int {
        if (sendPreference.encryptionScheme == PackageType.PM) {
            return if (sendPreference.hasPinnedKeys()) {
                R.string.composer_lock_internal_pinned
            } else {
                R.string.composer_lock_internal
            }
        }
        if (!sendPreference.isEncryptionEnabled && isMessagePasswordEncrypted) {
            return R.string.composer_lock_internal
        }
        return if (sendPreference.isPGP) {
            if (sendPreference.isEncryptionEnabled) {
                R.string.composer_lock_pgp_encrypted_pinned
            } else {
                R.string.composer_lock_pgp_signed
            }
        } else {
            0
        }
    }
}
