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
    private val mSendPreference: SendPreference,
    private val mEOEnabled: Boolean
) : LockIcon {

    override fun getIcon(): Int {
        if (mSendPreference.isEncryptionEnabled) {
            return if (mSendPreference.hasPinnedKeys()) {
                R.string.pgp_lock_check
            } else {
                R.string.lock_default
            }
        }
        if (mEOEnabled) {
            return R.string.lock_default
        }
        return if (mSendPreference.isSignatureEnabled) {
            R.string.pgp_lock_pen
        } else {
            R.string.no_icon
        }
    }

    override fun getColor(): Int {
        if (mSendPreference.encryptionScheme == PackageType.PM ||
            !mSendPreference.isEncryptionEnabled && mEOEnabled
        ) {
            return R.color.icon_purple
        }
        return if (mSendPreference.isPGP) {
            R.color.icon_green
        } else {
            R.color.icon_warning
        }
    }

    override fun getTooltip(): Int {
        if (mSendPreference.encryptionScheme == PackageType.PM) {
            return if (mSendPreference.hasPinnedKeys()) {
                R.string.composer_lock_internal_pinned
            } else {
                R.string.composer_lock_internal
            }
        }
        if (!mSendPreference.isEncryptionEnabled && mEOEnabled) {
            return R.string.composer_lock_internal
        }
        return if (mSendPreference.isPGP) {
            if (mSendPreference.isEncryptionEnabled) {
                R.string.composer_lock_pgp_encrypted_pinned
            } else {
                R.string.composer_lock_pgp_signed
            }
        } else {
            0
        }
    }
}
