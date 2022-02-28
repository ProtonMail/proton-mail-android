/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.api.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 7/5/16.
 */
public class CardDetails implements Serializable {
    @SerializedName(Fields.Payment.NUMBER)
    private String number;
    @SerializedName(Fields.Payment.EXPIRATION_MONTH)
    private String expirationMonth;
    @SerializedName(Fields.Payment.EXPIRATION_YEAR)
    private String expirationYear;
    @SerializedName(Fields.Payment.CVC)
    private String cvc;
    @SerializedName(Fields.Payment.NAME)
    private String name;
    @SerializedName(Fields.Payment.COUNTRY)
    private String country;
    @SerializedName(Fields.Payment.ZIP)
    private String zip;
    @SerializedName(Fields.Payment.BRAND)
    private String brand;
    @SerializedName(Fields.Payment.LAST_4)
    private String last4;

    // TODO temporary field added to CardDetails instead of refactoring
    @SerializedName(Fields.Payment.BILLING_AGREEMENT_ID)
    private String billingAgreementId;
    // TODO temporary field added to CardDetails instead of refactoring
    @SerializedName(Fields.Payment.PAYER)
    private String payer;

    public CardDetails(String number, String expirationMonth, String expirationYear, String cvc,
                       String name, String country, String zip, String brand, String last4, String billingAgreementId, String payer) {
        this.number = number;
        this.expirationMonth = expirationMonth;
        this.expirationYear = expirationYear;
        this.cvc = cvc;
        this.name = name;
        this.country = country;
        this.zip = zip;
        this.brand = null;
        this.last4 = null;
        this.billingAgreementId = billingAgreementId;
        this.payer = payer;
    }

    public String getExpirationMonth() {
        return expirationMonth;
    }

    public String getExpirationYear() {
        return expirationYear;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getZip() {
        return zip;
    }

    public String getBrand() {
        return brand;
    }

    public String getLast4() {
        return last4;
    }

    public String getBillingAgreementId() {
        return billingAgreementId;
    }

    public String getPayer() {
        return payer;
    }
}
