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

import java.io.Serializable;
import java.util.List;

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 18.8.15.
 */
public class UnreadTotalMessagesResponse extends ResponseBody implements Serializable {
    @SerializedName(Fields.General.COUNTS)
    private List<MessageCount> counts;

    public UnreadTotalMessagesResponse(List<MessageCount> counts){
        this.counts = counts;
    }

    public List<MessageCount> getCounts() {
        return counts;
    }
}
