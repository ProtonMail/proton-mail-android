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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import ezvcard.VCard;
import ezvcard.property.Email;
import ezvcard.property.RawProperty;

/**
 * Created by dino on 11/8/17.
 */

public class VCardUtil {

    public static final String CUSTOM_TYPE_PREFIX = "x-";

    public static boolean isCustomType(String type) {
        if (TextUtils.isEmpty(type)) {
            return false;
        }
        String theType = new String(type).toLowerCase();
        return theType.startsWith(CUSTOM_TYPE_PREFIX);
    }

    public static String removeCustomPrefixForCustomType(String type) {
        if (isCustomType(type)) {
            return type.substring(2);
        }
        return type;
    }

    public static String capitalizeType(String type) {
        return StringUtils.capitalize(type);
    }

    public static String getGroup(VCard clear, VCard signed, String email) throws Exception {
        List<Email> emails = combine(clear.getEmails(), signed.getEmails());
        for (Email emailProp : emails) {
            if (emailProp.getValue().equalsIgnoreCase(email)) {
                return emailProp.getGroup();
            }
        }
        throw new Exception("Email not found!");
    }

    private static <T> List<T> combine(List<T> list1, List<T> list2) {
        List<T> result = new ArrayList<>();
        result.addAll(list1);
        result.addAll(list2);
        return result;
    }


    public static RawProperty findProperty(VCard vCard, String name, String group) {
        for (RawProperty raw : vCard.getExtendedProperties()) {
            if (raw.getPropertyName().equalsIgnoreCase(name) && raw.getGroup().equalsIgnoreCase(group)) {
                return raw;
            }
        }
        return null;
    }
}
