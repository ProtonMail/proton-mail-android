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
import ch.protonmail.android.api.models.enumerations.MessageEncryption;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.core.Constants;

/**
 * Created by kaylukas on 21/06/2018.
 */
public class SenderLockIcon implements LockIcon {

    private Message mMessage;
    private boolean mHasValidSignature;
    private boolean mHasInvalidSignature;

    public SenderLockIcon(Message message, boolean hasValidSignature, boolean hasInvalidSignature) {
        mMessage = message;
        mHasValidSignature = hasValidSignature;
        mHasInvalidSignature = hasInvalidSignature;
    }

    @Override
    public int getIcon() {
        if (!mMessage.getMessageEncryption().isStoredEncrypted()) {
            return R.string.pgp_lock_open;
        }
        if (mHasInvalidSignature) {
            return R.string.pgp_lock_warning;
        }
        if (mMessage.isSent()) {
            if (!mHasValidSignature && mMessage.getTime() > Constants.PM_SIGNATURES_START) {
                return R.string.pgp_lock_warning;
            }
            return R.string.lock_default;
        }
        if (mHasValidSignature) {
            return R.string.pgp_lock_check;
        }
        return R.string.lock_default;
    }

    @Override
    public int getColor() {
        MessageEncryption messageEncryption = mMessage.getMessageEncryption();
        if (messageEncryption.isPGPEncrypted()) {
            return R.color.icon_green;
        }
        if (messageEncryption.isEndToEndEncrypted() || messageEncryption.isInternalEncrypted()) {
            return R.color.icon_purple;
        }
        return R.color.icon_gray;
    }

    @Override
    public int getTooltip() {
        if (mHasInvalidSignature) {
            return R.string.sender_lock_verification_failed;
        }
        MessageEncryption messageEncryption = mMessage.getMessageEncryption();
        if (messageEncryption == MessageEncryption.AUTO_RESPONSE) {
            return R.string.sender_lock_sent_autoresponder;
        }
        if (mMessage.isSent()) {
            return getSentTooltip();
        }
        if (messageEncryption.isInternalEncrypted()) {
            return getInternalTooltip();
        }
        if (messageEncryption.isPGPEncrypted()) {
            return getPGPTooltip();
        }
        // We only support EO (handled in SentTooltip), PGP and Internal for e2e-encryption
        // So this should never happen:
        if (messageEncryption.isEndToEndEncrypted()) {
            Log.wtf("SenderLockIcon", "Unhandled EndToEndEncryption tooltip!");
            return R.string.sender_lock_unknown_scheme;
        }
        if (messageEncryption.isStoredEncrypted()) {
            return getZeroAccessTooltip();
        }
        return R.string.sender_lock_unencrypted;
    }

    /**
     * Special case, we don't show when signatures are actually valid, but signatures should always be here
     * so instead, when a signature is not available we display an error.
     * This is a special case, because address verification happens always for sent messages
     * So we don't want to show the regular verified sender case if the user does not know about
     * address verification.
     */
    private int getSentTooltip() {
        MessageEncryption messageEncryption = mMessage.getMessageEncryption();
        if (!mHasValidSignature && mMessage.getTime() > Constants.PM_SIGNATURES_START) {
            return R.string.sender_lock_verification_failed;
        }
        if (messageEncryption.isEndToEndEncrypted()) {
            return R.string.sender_lock_sent_end_to_end;
        }
        if (messageEncryption == MessageEncryption.INTERNAL) {
            return R.string.sender_lock_zero_access;
        }
        return R.string.sender_lock_zero_access;
    }

    private int getInternalTooltip() {
        if (mHasValidSignature) {
            return R.string.sender_lock_internal_verified;
        }
        return R.string.sender_lock_internal;
    }

    private int getPGPTooltip() {
        if (mHasValidSignature) {
            return R.string.sender_lock_pgp_encrypted_verified;
        }
        return R.string.sender_lock_pgp_encrypted;
    }

    private int getZeroAccessTooltip() {
        if (mHasValidSignature) {
            return R.string.sender_lock_pgp_signed_verified_sender;
        }
        return R.string.sender_lock_zero_access;
    }

}
