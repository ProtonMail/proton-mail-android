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
package ch.protonmail.android.api.models;

import com.google.gson.annotations.SerializedName;

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 12/6/16.
 */

public class Domain {
    @SerializedName(Fields.Domain.ID)
    private String id;
    @SerializedName(Fields.Domain.DOMAIN_NAME)
    private String domainName;
    @SerializedName(Fields.Domain.VERIFY_CODE)
    private String verifyCode;
    @SerializedName(Fields.Domain.DKIM_PUBLIC_KEY)
    private String dkimPublicKey;
    @SerializedName(Fields.Domain.STATE)
    private int state;
    @SerializedName(Fields.Domain.CHECK_TIME)
    private long checkTime;
    @SerializedName(Fields.Domain.VERIFY_STATE)
    private int verifyState;
    @SerializedName(Fields.Domain.MX_STATE)
    private int mxState;
    @SerializedName(Fields.Domain.SPF_STATE)
    private int spfState;
    @SerializedName(Fields.Domain.DKIM_STATE)
    private int dkimState;
    @SerializedName(Fields.Domain.DMARC_STATE)
    private int dmarcState;

    public String getId() {
        return id;
    }

    public String getDomainName() {
        return domainName;
    }
}
