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

import android.util.Log;

import ch.protonmail.android.R;

/**
 * Created by kaylukas on 21/06/2018.
 */
public class RecipientLockIcon implements LockIcon {

    private String mRecipEnc;
    private String mRecipAuth;

    public RecipientLockIcon(String recipEnc, String recipAuth) {
        mRecipEnc = recipEnc;
        mRecipAuth = recipAuth;
    }

    @Override
    public int getTooltip() {
        if (mRecipEnc.startsWith("pgp-pm") || mRecipEnc.startsWith("pgp-eo")) {
            if(mRecipEnc.endsWith("-pinned")) {
                return R.string.composer_lock_internal_pinned;
            }
            return R.string.composer_lock_internal;
        }
        if (mRecipEnc.startsWith("pgp-mime") || mRecipEnc.startsWith("pgp-inline")) {
            return R.string.composer_lock_pgp_encrypted_pinned;
        }
        // This should never happen
        if (!mRecipEnc.equals("none")) {
            Log.wtf("RecipientLockIcon", "Recipient encryption value");
            return R.string.composer_lock_unknown_scheme;
        }
        if (mRecipAuth.startsWith("pgp-mime") || mRecipAuth.startsWith("pgp-inline")) {
            return R.string.composer_lock_pgp_signed;
        }
        if (!mRecipAuth.equals("none")) {
            Log.wtf("RecipientLockIcon", "Unknown recipient encryption value");
            return R.string.composer_lock_unknown_scheme;
        }
        return R.string.composer_lock_no_scheme;
    }

    @Override
    public int getIcon() {
        if (mRecipEnc.equals("none")) {
            if (!mRecipAuth.equals("none")) {
                return R.string.pgp_lock_pen;
            }
            return R.string.no_icon;
        }
        if (mRecipEnc.endsWith("-pinned")) {
            return R.string.pgp_lock_check;
        }
        return R.string.lock_default;
    }

    @Override
    public int getColor() {
        if (mRecipEnc.startsWith("pgp-mime") || mRecipEnc.startsWith("pgp-inline") ||
                mRecipEnc.equals("none")) {
            return R.color.icon_green;
        }
        return R.color.icon_purple;
    }
}
