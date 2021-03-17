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
package ch.protonmail.android.jobs.contacts;

import com.birbit.android.jobqueue.Params;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.protonmail.android.api.exceptions.ApiException;
import ch.protonmail.android.api.models.MailSettings;
import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.api.models.factories.SendPreferencesFactory;
import ch.protonmail.android.data.local.ContactDao;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.contacts.SendPreferencesEvent;
import ch.protonmail.android.jobs.Priority;
import ch.protonmail.android.jobs.ProtonMailBaseJob;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;

import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_RECIPIENT_NOT_FOUND;

/**
 * Created by protonlabs on 5/26/18.
 */

public class GetSendPreferenceJob extends ProtonMailBaseJob {

	private final ContactDao contactDao;
    private final List<String> mEmails;
    private final Destination mDestination;

    public enum Destination {
        TO, CC, BCC
    }

    public GetSendPreferenceJob(ContactDao contactDao, List<String> emails, Destination destination) {
        super(new Params(Priority.HIGH).requireNetwork());
		this.contactDao = contactDao;
		mEmails = emails;
        mDestination = destination;
    }

    @Override
    public void onRun() throws Throwable {
        MailSettings mailSettings = getUserManager().getMailSettings();
        SendPreferencesFactory factory = new SendPreferencesFactory(getApi(), getUserManager(), getUserManager().getUsername(), mailSettings, contactDao);
        Map<String, SendPreference> sendPreferenceMap = new HashMap<>();
        sendPreferenceMap.put(mEmails.get(0), null);
        try {
            sendPreferenceMap = factory.fetch(mEmails);
        } catch (ApiException apiException) {
            ResponseBody body = apiException.getResponse();
            if (body.getCode() == RESPONSE_CODE_RECIPIENT_NOT_FOUND) {
                AppUtil.postEventOnUi(new SendPreferencesEvent(Status.SUCCESS, sendPreferenceMap, mDestination, false));
            } else {
                AppUtil.postEventOnUi(new SendPreferencesEvent(Status.FAILED, sendPreferenceMap, mDestination, true));
            }
            return;
        } catch (Exception e) {
            Logger.doLogException(e);
            AppUtil.postEventOnUi(new SendPreferencesEvent(Status.FAILED, sendPreferenceMap, mDestination, false));
            return;
        }
        AppUtil.postEventOnUi(new SendPreferencesEvent(Status.SUCCESS, sendPreferenceMap, mDestination, true));
    }
}
