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
package ch.protonmail.tokenautocomplete;


import androidx.annotation.NonNull;

import java.util.Locale;

class Range {
    public final int start;
    public final int end;

    Range(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH,
                    "Start (%d) cannot be greater than end (%d)", start, end));
        }
        this.start = start;
        this.end = end;
    }

    public int length() {
        return end - start;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Range)) {
            return false;
        }

        Range other = (Range) obj;
        return other.start == start && other.end == end;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US, "[%d..%d]", start, end);
    }
}
