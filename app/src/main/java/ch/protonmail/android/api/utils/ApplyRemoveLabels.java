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
package ch.protonmail.android.api.utils;

import java.util.List;

/**
 * Created by dino on 11/20/17.
 */

public class ApplyRemoveLabels {

    private List<String> labelsToApply;
    private List<String> labelsToRemove;

    public ApplyRemoveLabels(List<String> labelsToApply, List<String> labelsToRemove) {
        this.labelsToApply = labelsToApply;
        this.labelsToRemove = labelsToRemove;
    }

    public List<String> getLabelsToApply() {
        return labelsToApply;
    }

    public void setLabelsToApply(List<String> labelsToApply) {
        this.labelsToApply = labelsToApply;
    }

    public List<String> getLabelsToRemove() {
        return labelsToRemove;
    }

    public void setLabelsToRemove(List<String> labelsToRemove) {
        this.labelsToRemove = labelsToRemove;
    }
}
