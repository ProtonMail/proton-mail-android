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
package ch.protonmail.android.utils.ui.locks;

import ch.protonmail.android.R;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.api.models.enumerations.PackageType;

/**
 * Created by kaylukas on 22/06/2018.
 */

public class ComposerLockIcon implements LockIcon {

    private SendPreference mSendPreference;
    private boolean mEOEnabled;

    public ComposerLockIcon(SendPreference sendPreference, boolean eoEnabled) {
        mSendPreference = sendPreference;
        mEOEnabled = eoEnabled;
    }

    @Override
    public int getIcon() {
        if (mSendPreference.isEncryptionEnabled()) {
            if (mSendPreference.hasPinnedKeys()) {
                return R.string.pgp_lock_check;
            }
            return R.string.lock_default;
        }
        if (mEOEnabled) {
            return R.string.lock_default;
        }
        if (mSendPreference.isSignatureEnabled()) {
            return R.string.pgp_lock_pen;
        }
        return R.string.no_icon;
    }

    @Override
    public int getColor() {
        if (mSendPreference.getEncryptionScheme() == PackageType.PM ||
                (!mSendPreference.isEncryptionEnabled() && mEOEnabled)) {
            return R.color.icon_purple;
        }
        if (mSendPreference.isPGP()) {
            return R.color.icon_green;
        }
        return R.color.icon_warning;
    }

    @Override
    public int getTooltip() {
        if (mSendPreference.getEncryptionScheme() == PackageType.PM) {
            if (mSendPreference.hasPinnedKeys()) {
                return R.string.composer_lock_internal_pinned;
            }
            return R.string.composer_lock_internal;
        }
        if (!mSendPreference.isEncryptionEnabled() && mEOEnabled) {
            return R.string.composer_lock_internal;
        }
        if (mSendPreference.isPGP()) {
            if (mSendPreference.isEncryptionEnabled()) {
                return R.string.composer_lock_pgp_encrypted_pinned;
            }
            return R.string.composer_lock_pgp_signed;
        }
        return 0;
    }
}
