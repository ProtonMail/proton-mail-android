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
package ch.protonmail.android.events.payment;

import ch.protonmail.android.events.Status;

/**
 * Created by dkadrikj on 7/11/16.
 */
public class PaymentsStatusEvent {
    private final Status status;
    private boolean stripe;
    private boolean paypal;

    public PaymentsStatusEvent(Status status) {
        this.status = status;
    }

    public PaymentsStatusEvent(Status status, boolean stripe, boolean paypal) {
        this.status = status;
        this.stripe = stripe;
        this.paypal = paypal;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isStripeActive() {
        return stripe;
    }

    public boolean isPaypalActive() {
        return paypal;
    }
}
