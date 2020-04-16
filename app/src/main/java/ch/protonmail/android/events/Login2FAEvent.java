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
package ch.protonmail.android.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ch.protonmail.android.api.models.LoginInfoResponse;
import ch.protonmail.android.api.models.LoginResponse;
import ch.protonmail.android.api.models.ResponseBody;

public class Login2FAEvent extends ResponseBody {
    public final @NonNull AuthStatus status;
    public final @Nullable LoginInfoResponse infoResponse;
    public final @Nullable LoginResponse loginResponse;
    public final String username;
    public final byte[] password;
    public final int fallbackAuthVersion;

    public Login2FAEvent(final @NonNull AuthStatus status, final @Nullable LoginInfoResponse infoResponse,
                         final String username, final byte[] password, final @Nullable LoginResponse loginResponse,
                         final int fallbackAuthVersion) {
        this.status = status;
        this.infoResponse = infoResponse;
        this.username = username;
        this.password = password;
        this.loginResponse = loginResponse;
        this.fallbackAuthVersion = fallbackAuthVersion;
    }
}
