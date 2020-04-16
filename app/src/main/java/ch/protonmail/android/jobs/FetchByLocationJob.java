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
package ch.protonmail.android.jobs;

import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Params;

import ch.protonmail.android.api.services.MessagesService;
import ch.protonmail.android.core.Constants;

/**
 * Persistent job to process certain jobs after first login and which requires network, so they can be executed after we get network access if were offline before
 */
public class FetchByLocationJob extends ProtonMailBaseJob {

    private final Constants.MessageLocationType location;
    @Nullable
    private final String labelId;
    private final boolean includeLabels;
    private final String uuid;

    public FetchByLocationJob(Constants.MessageLocationType location,
                              @Nullable String labelId, boolean includeLabels, String uuid) {
        super(new Params(Priority.MEDIUM).groupBy(Constants.JOB_GROUP_MESSAGE));
        this.location = location;
        this.labelId = labelId;
        this.includeLabels = includeLabels;
        this.uuid = uuid;
    }

    @Override
    public void onRun() throws Throwable {
        switch (location) {
            case LABEL:
            case LABEL_OFFLINE:
            case LABEL_FOLDER:
                MessagesService.Companion.startFetchFirstPageByLabel(location, labelId);
                break;
            default:
                MessagesService.Companion.startFetchFirstPage(location, false, uuid);
                if (includeLabels) {
                    MessagesService.Companion.startFetchLabels();
                    MessagesService.Companion.startFetchContactGroups();
                }
                break;
        }
    }
}
