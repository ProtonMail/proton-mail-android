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

import ch.protonmail.android.core.Constants;

/**
 * Created by sunny on 7/28/15.
 */
public class MessageSentEvent {
    private Status status;

    public MessageSentEvent(int code){
        if (code == Constants.RESPONSE_CODE_OK){
            status = Status.SUCCESS;
        } else {
            status = Status.FAILED;
        }
    }

    public Status getStatus(){
        return status;
    }
}
