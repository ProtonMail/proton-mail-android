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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by kaylukas on 01/06/2018.
 */

public enum KeyFlag {
    VERIFICATION_ENABLED(1), ENCRYPTION_ENABLED(2);

    private final int value;

    KeyFlag(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Set<KeyFlag> fromInteger(int val) {
        Set<KeyFlag> flags = new HashSet<>();
        for (KeyFlag flag : KeyFlag.values()) {
            if ((val & flag.getValue()) == flag.getValue()) {
                flags.add(flag);
            }
        }
        return Collections.unmodifiableSet(flags);
    }

    public static int toInteger(Collection<KeyFlag> keyFlags) {
        int val = 0;
        for(KeyFlag flag : keyFlags) {
            val |= flag.getValue();
        }
        return val;
    }

    public static int toInteger(KeyFlag... keyFlags) {
        int val = 0;
        for(KeyFlag flag : keyFlags) {
            val |= flag.getValue();
        }
        return val;
    }
}
