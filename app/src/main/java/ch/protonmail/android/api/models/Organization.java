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

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 7/10/16.
 */
public class Organization {
    @SerializedName(Fields.Organization.DISPLAY_NAME)
    private String displayName;
    @SerializedName(Fields.Organization.PLAN_NAME)
    private String planName;
    @SerializedName(Fields.Organization.VPN_PLAN_NAME)
    private String vpnPlanName;
    @SerializedName(Fields.Organization.MAX_DOMAINS)
    private int maxDomains;
    @SerializedName(Fields.Organization.MAX_ADDRESSES)
    private int maxAddresses;
    @SerializedName(Fields.Organization.MAX_SPACE)
    private long maxSpace;
    @SerializedName(Fields.Organization.MAX_MEMBERS)
    private int maxMembers;
    @SerializedName(Fields.Organization.MAX_VPN)
    private int maxVPN;
    @SerializedName(Fields.Organization.TWO_FACTOR)
    private int twoFactor;
    @SerializedName(Fields.Organization.USED_DOMAINS)
    private int usedDomains;
    @SerializedName(Fields.Organization.USED_MEMBERS)
    private int usedMembers;
    @SerializedName(Fields.Organization.USED_ADDRESSES)
    private int usedAddresses;
    @SerializedName(Fields.Organization.USED_SPACE)
    private long usedSpace;
    @SerializedName(Fields.Organization.ASSIGNED_SPACE)
    private long assignedSpace;

    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public String getPlanName() {
        return planName;
    }

    public String getVpnPlanName() {
        return vpnPlanName;
    }

    public int getMaxDomains() {
        return maxDomains;
    }

    public int getMaxAddresses() {
        return maxAddresses;
    }

    public long getMaxSpace() {
        return maxSpace;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public int getMaxVPN() {
        return maxVPN;
    }

    public int getTwoFactor() {
        return twoFactor;
    }

    public int getUsedDomains() {
        return usedDomains;
    }

    public int getUsedMembers() {
        return usedMembers;
    }

    public int getUsedAddresses() {
        return usedAddresses;
    }

    public long getUsedSpace() {
        return usedSpace;
    }

    public long getAssignedSpace() {
        return assignedSpace;
    }
}
