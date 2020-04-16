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

import com.birbit.android.jobqueue.Params;

import ch.protonmail.android.api.services.MessagesService;
import ch.protonmail.android.core.Constants;

/**
 * Created by dkadrikj on 17.7.15.
 */
public class FetchLabelsJob extends ProtonMailBaseJob {

    public FetchLabelsJob() {
        this(0);
    }

    private FetchLabelsJob(long delayInMs) {
        super(new Params(Priority.LOW).delayInMs(delayInMs).requireNetwork().groupBy(Constants.JOB_GROUP_LABEL));
    }

    @Override
    public void onRun() throws Throwable {
        MessagesService.Companion.startFetchLabels();
        MessagesService.Companion.startFetchContactGroups();
    }
}
