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
package ch.protonmail.android.api.models.enumerations;

/**
 * Created by kaylukas on 21/06/2018.
 *
 * Stores properties per MessageEncryption value
 */
public enum MessageEncryption {

    INTERNAL(/* IsE2E */ true,
             /* StoredEncrypted */ true,
             /* InternalEncryption */ true,
             /* PGPEncryption */ false), // all within ProtonMail

    EXTERNAL(/* IsE2E */ false,
            /* StoredEncrypted */ true,
            /* InternalEncryption */ false,
            /* PGPEncryption */ false),

    EXTERNAL_PGP(/* IsE2E */ false,
            /* StoredEncrypted */ true,
            /* InternalEncryption */ false,
            /* PGPEncryption */ true),

    MIME_PGP(/* IsE2E */ true,
             /* StoredEncrypted */ true,
             /* InternalEncryption */ false,
             /* PGPEncryption */ true),

    AUTO_RESPONSE(/* IsE2E */ false,
                 /* StoredEncrypted */ true,
                 /* InternalEncryption */ true,
                 /* PGPEncryption */ false);

    private boolean e2e;
    private boolean storedEnc;
    private boolean internalEncryption;
    private boolean pgpEnc;

    MessageEncryption(boolean e2e, boolean storedEnc, boolean internalEncryption,
                      boolean pgpEnc) {
        this.e2e = e2e;
        this.storedEnc = storedEnc;
        this.internalEncryption = internalEncryption;
        this.pgpEnc = pgpEnc;
    }

    /**
     * Drafts are also seen as end-to-end encrypted as drafts are never leaked to the server.
     */
    public boolean isEndToEndEncrypted() {
        return e2e;
    }

    public boolean isStoredEncrypted() {
        return storedEnc;
    }

    public boolean isInternalEncrypted() {
        return internalEncryption;
    }

    public boolean isPGPEncrypted() {
        return pgpEnc;
    }

}
