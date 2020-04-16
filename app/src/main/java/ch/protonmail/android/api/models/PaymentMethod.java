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

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 7/5/16.
 */
public class PaymentMethod implements Serializable {
    @SerializedName(Fields.Payment.ID)
    private String id;
    @SerializedName(Fields.Payment.TYPE)
    private String type;
    @SerializedName(Fields.Payment.DETAILS)
    private CardDetails cardDetails;

    public PaymentMethod(String id, String type, String expirationMonth, String expirationYear,
                         String name, String country, String zip, String brand, String last4, String billingAgreementId, String payer) {
        this.id = id;
        this.type = type;
        this.cardDetails = new CardDetails(null, expirationMonth, expirationYear, null, name, country, zip, brand, last4, billingAgreementId, payer);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public CardDetails getCardDetails() {
        return cardDetails;
    }
}
