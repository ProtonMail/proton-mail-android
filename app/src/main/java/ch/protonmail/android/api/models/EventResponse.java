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
package ch.protonmail.android.api.models;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.messages.receive.ServerLabel;
import ch.protonmail.android.api.models.messages.receive.ServerMessage;
import ch.protonmail.android.api.models.room.contacts.ContactEmail;
import ch.protonmail.android.api.models.room.contacts.FullContactDetails;
import ch.protonmail.android.api.models.room.contacts.server.FullContactDetailsFactory;
import ch.protonmail.android.api.models.room.contacts.server.ServerFullContactDetails;
import ch.protonmail.android.api.utils.Fields;

public class EventResponse extends ResponseBody {

    @SerializedName(Fields.Events.EVENT_ID)
    private String eventID;
    @SerializedName(Fields.Events.MORE)
    private int more;
    @SerializedName(Fields.Events.REFRESH)
    private int refresh;
    @SerializedName(Fields.Events.MESSAGES)
    private List<MessageEventBody> messages;
    @SerializedName(Fields.Events.CONTACTS)
    private List<ContactEventBody> contacts;
    @SerializedName(Fields.Events.CONTACT_EMAILS)
    private List<ContactEmailEventBody> contactEmails;
    @SerializedName(Fields.Events.LABELS)
    private List<LabelsEventBody> labels;
    @SerializedName(Fields.Events.USER)
    private User user;
    @SerializedName(Fields.Events.MAIL_SETTINGS)
    private MailSettings mailSettings;
    @SerializedName(Fields.Events.USER_SETTINGS)
    private UserSettings userSettings;
    @SerializedName(Fields.Events.MESSAGE_COUNTS)
    private List<MessageCount> messageCounts;
    @SerializedName(Fields.Events.USED_SPACE)
    private long usedSpace;
    @SerializedName(Fields.Events.ADDRESSES)
    private List<AddressEventBody> addresses;

    @Nullable
    public List<MessageEventBody> getMessageUpdates() {
        return messages;
    }

    public List<ContactEventBody> getContactUpdates() { return contacts; }

    public List<ContactEmailEventBody> getContactEmailsUpdates() { return contactEmails; }

    public List<MessageCount> getMessageCounts() {
        return messageCounts;
    }

    public List<LabelsEventBody> getLabelUpdates() { return labels; }

    public User getUserUpdates(){ return user; }

    public MailSettings getMailSettingsUpdates(){ return mailSettings; }

    public UserSettings getUserSettingsUpdates(){ return userSettings; }

    public String getEventID(){ return eventID;}

    public boolean refreshContacts() {
        return (refresh & RefreshStatus.CONTACTS.getStatus()) == RefreshStatus.CONTACTS.getStatus();
    }

    public boolean refresh() {
        return
                (refresh & RefreshStatus.MAIL.getStatus()) == RefreshStatus.MAIL.getStatus() ||
                (refresh & RefreshStatus.ALL.getStatus()) == RefreshStatus.ALL.getStatus();
    }

    public long getUsedSpace() {
        return usedSpace;
    }

    public boolean hasMore() {
        return more > 0;
    }

    public List<AddressEventBody> getAddresses() {
        return addresses;
    }

    public class AddressEventBody {
        @SerializedName(Fields.Events.ID)
        private String id;
        @SerializedName(Fields.Events.ACTION)
        private int action;
        @SerializedName(Fields.Events.ADDRESS)
        private Address address;

        public String getId() {
            return id;
        }

        public int getType() {
            return action;
        }

        public Address getAddress() {
            return address;
        }
    }

    public class MessageEventBody {
        @SerializedName(Fields.Events.ID)
        private String id;
        @SerializedName(Fields.Events.ACTION)
        private int action;
        @SerializedName(Fields.Events.MESSAGE)
        private ServerMessage message;

        public String getMessageID() {
            return id;
        }

        public ServerMessage getMessage() {
            return message;
        }

        public int getType() {
            return action;
        }
    }

    public class ContactEventBody {
        @SerializedName(Fields.Events.ID)
        private String id;
        @SerializedName(Fields.Events.ACTION)
        private int action;
        @SerializedName(Fields.Events.CONTACT)
        private ServerFullContactDetails contact;

        public String getContactID() {
            return id;
        }

        public FullContactDetails getContact() {
            FullContactDetailsFactory fullContactDetailsFactory=new FullContactDetailsFactory();
            return fullContactDetailsFactory.createFullContactDetails(contact);
        }

        public int getType() {
            return action;
        }
    }

    public class ContactEmailEventBody {
        @SerializedName(Fields.Events.ID)
        private String id;
        @SerializedName(Fields.Events.ACTION)
        private int action;
        @SerializedName(Fields.Events.CONTACT_EMAIL)
        private ContactEmail contactEmail;

        public String getContactID() {
            return id;
        }

        public ContactEmail getContactEmail() {
            return contactEmail;
        }

        public int getType() {
            return action;
        }
    }

    public class LabelsEventBody {
        @SerializedName(Fields.Events.ID)
        private String id;
        @SerializedName(Fields.Events.ACTION)
        private int action;
        @SerializedName(Fields.Events.LABEL)
        private ServerLabel label;

        public String getID() {
            return id;
        }

        public int getType() {
            return action;
        }

        public ServerLabel getLabel() {
            return label;
        }
    }

    enum RefreshStatus {
        OK(0), MAIL(1), CONTACTS(1 << 1), ALL(0xFF);

        private final int status;

        RefreshStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
