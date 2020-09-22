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

import androidx.annotation.NonNull;

import com.birbit.android.jobqueue.Params;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.protonmail.android.api.models.PublicKeyBody;
import ch.protonmail.android.api.models.PublicKeyResponse;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.FetchEmailKeysEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dino on 1/14/17.
 */

public class FetchPublicKeysJob extends ProtonMailBaseJob {

    private List<PublicKeysBatchJob> mJobs;
    private boolean mRetry;

    public FetchPublicKeysJob(List<PublicKeysBatchJob> jobs, boolean retry) {
        super(new Params(Priority.HIGH).requireNetwork().groupBy(Constants.JOB_GROUP_MISC).removeTags().persist());
        mJobs = jobs;
        mRetry = retry;
    }

    @Override
    public void onRun() throws Throwable {
        List<FetchEmailKeysEvent.EmailKeyResponse> responses = new ArrayList<>();
        for (PublicKeysBatchJob job : mJobs) {
            Set<String> emailSet = new HashSet<>(job.getEmailList());
            Constants.RecipientLocationType location = job.getLocation();
            Map<String, String> response = new HashMap<>();
            try {
                response = getPublicKeys(emailSet);
            } catch (Exception e) {
                // noop
            }
            if (response.containsKey("Code")) {
                response.remove("Code");
            }
            responses.add(new FetchEmailKeysEvent.EmailKeyResponse(response, location, Status.SUCCESS));
        }
        AppUtil.postEventOnUi(new FetchEmailKeysEvent(Status.SUCCESS, responses, mRetry));
    }

    @NonNull
    private Map<String, String> getPublicKeys(@NonNull Set<String> emailSet) throws Exception {
        Map<String, PublicKeyResponse> response = getApi().getPublicKeys(emailSet);
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, PublicKeyResponse> entry : response.entrySet()) {
            PublicKeyResponse pubkey = entry.getValue();
//            if (pubkey.hasError()) {
//                throw new Exception(pubkey.getErrorDescription());
//            }
            PublicKeyBody[] keys = pubkey.getKeys();
            result.put(entry.getKey(), "");
            for (PublicKeyBody key : keys) {
                if (key.isAllowedForSending()) {
                    result.put(entry.getKey(), key.getPublicKey());
                    break;
                }
            }
        }
        return result;
    }

    public static class PublicKeysBatchJob implements Serializable {
        private List<String> mEmailList;
        private Constants.RecipientLocationType mLocation;

        public PublicKeysBatchJob(List<String> emailList, Constants.RecipientLocationType location) {
            this.mEmailList = emailList;
            this.mLocation = location;
        }

        List<String> getEmailList() {
            return mEmailList;
        }

        public Constants.RecipientLocationType getLocation() {
            return mLocation;
        }
    }
}
