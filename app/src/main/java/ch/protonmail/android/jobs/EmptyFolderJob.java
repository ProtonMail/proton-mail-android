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

import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.api.segments.event.FetchUpdatesJob;
import ch.protonmail.android.core.Constants;

/**
 * Created by sunny on 8/26/15.
 */
public class EmptyFolderJob extends ProtonMailBaseJob {

	private Constants.MessageLocationType location;
	private String labelId;

    public EmptyFolderJob(Constants.MessageLocationType location, String labelId) {
        super(new Params(Priority.MEDIUM).requireNetwork());
		this.location = location;
		this.labelId = labelId;
    }

    @Override
    public void onAdded() {
        super.onAdded();
        MessagesDatabase messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(
                getApplicationContext()).getDatabase();

        if (labelId != null) {
            messagesDatabase.deleteMessagesByLabel(labelId);
        } else {
            messagesDatabase.deleteMessagesByLocation(location.getMessageLocationTypeValue());
        }
    }

    @Override
    public void onRun() throws Throwable {
        if (location == Constants.MessageLocationType.DRAFT){
            mApi.emptyDrafts();
        } else if (location == Constants.MessageLocationType.SPAM){
            mApi.emptySpam();
        } else if (location == Constants.MessageLocationType.TRASH){
            mApi.emptyTrash();
        } else if (labelId != null && (location == Constants.MessageLocationType.LABEL || location == Constants.MessageLocationType.LABEL_FOLDER)) {
            mApi.emptyCustomFolder(labelId);
        }
        mJobManager.addJobInBackground(new FetchUpdatesJob());


    }

}
