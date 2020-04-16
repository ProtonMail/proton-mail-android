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

import ch.protonmail.android.api.models.Subscription;

/**
 * Created by dkadrikj on 7/8/16.
 */
public class PaymentMethodEvent {
    private final Status status;
    private final String error;
    private final String errorDescription;
    private final Subscription subscription;

    public PaymentMethodEvent(Status status, String error, String errorDescription) {
        this.status = status;
        this.error = error;
        this.errorDescription = errorDescription;
        this.subscription = null;
    }

    public PaymentMethodEvent(Status status, Subscription subscription) {
        this.status = status;
        this.subscription = subscription;
        this.error = null;
        this.errorDescription = null;
    }

    public Status getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public Subscription getSubscription() {
        return subscription;
    }
}
