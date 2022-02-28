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
package ch.protonmail.android.events;

import java.util.List;

import ch.protonmail.android.api.models.AllCurrencyPlans;
import ch.protonmail.android.api.models.AvailablePlansResponse;

/**
 * Created by dkadrikj on 7/6/16.
 */
public class AvailablePlansEvent {

    private final Status status;
    private AllCurrencyPlans allCurrencyPlans;

    public AvailablePlansEvent(Status status) {
        this.status = status;
    }

    public AvailablePlansEvent(Status status, AllCurrencyPlans allCurrencyPlans) {
        this.status = status;
        this.allCurrencyPlans = allCurrencyPlans;
    }

    public Status getStatus() {
        return status;
    }

    public AllCurrencyPlans getAllPlans() {
        return allCurrencyPlans;
    }

    public List<AvailablePlansResponse> getAllPlansList() {
        return allCurrencyPlans.getPlans();
    }
}
