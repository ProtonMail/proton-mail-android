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
package ch.protonmail.android.api.models.enumerations;

/**
 * Created by kaylukas on 01/05/2018.
 */
public enum MIMEType {
    MIME("multipart/mixed"), PLAINTEXT("text/plain"), HTML("text/html");

    private final String mime;

    MIMEType(String mime) {
        this.mime = mime;
    }

    public String toString() {
        return mime;
    }


    public static MIMEType fromString(String val) {
        for (MIMEType type : MIMEType.values()) {
            if (type.toString().equals(val)) {
                return type;
            }
        }
        return null;
    }
}
