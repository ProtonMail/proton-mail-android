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
package ch.protonmail.android.utils;

import android.text.TextUtils;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

import ch.protonmail.android.R;
import ch.protonmail.android.api.models.Organization;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.UserManager;

/**
 * Created by dino on 10/17/17.
 */

public class UserUtils {

    public static int getMaxAllowedLabels(UserManager userManager) {
        List<String> accountTypes = Arrays.asList(ProtonMailApplication.getApplication().getResources().getStringArray(R.array.account_type_names));
        List<Integer> maxLabelsPerPlanArray = Arrays.asList(ArrayUtils.toObject(ProtonMailApplication.getApplication().getResources().getIntArray(R.array.max_labels_per_plan)));
        Organization organization = ProtonMailApplication.getApplication().getOrganization();

        boolean paidUser = false;
        String planName = accountTypes.get(0); // free
        int maxLabelsAllowed = maxLabelsPerPlanArray.get(0); // free

        User user = userManager.getUser();
        if (user != null && organization != null) {
            planName = organization.getPlanName();
            paidUser = user.isPaidUser() && !TextUtils.isEmpty(organization.getPlanName());
        }
        if (!paidUser) {
            return maxLabelsAllowed;
        }

        for (int i = 1; i < accountTypes.size(); i++) {
            String accountName = accountTypes.get(i);
            if (accountName.equalsIgnoreCase(planName)) {
                maxLabelsAllowed = maxLabelsPerPlanArray.get(i);
                break;
            }
        }

        return maxLabelsAllowed;
    }
}
