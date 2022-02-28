/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.tokenautocomplete;

import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;

public interface Tokenizer extends Parcelable {
    /**
     * Find all ranges that can be tokenized. This system should detect possible tokens
     * both with and without having had wrapTokenValue called on the token string representation
     *
     * @param charSequence the string to search in
     * @param start where the tokenizer should start looking for tokens
     * @param end where the tokenizer should stop looking for tokens
     * @return all ranges of characters that are valid tokens
     */
    @NonNull
    List<Range> findTokenRanges(CharSequence charSequence, int start, int end);

    /**
     * Return a complete string representation of the token. Often used to add commas after email
     * addresses when creating tokens
     *
     * This value must NOT include any leading or trailing whitespace
     *
     * @param unwrappedTokenValue the value to wrap
     * @return the token value with any expected delimiter characters
     */
    @NonNull
    CharSequence wrapTokenValue(CharSequence unwrappedTokenValue);

    /**
     * Return true if there is a character in the charSequence that should trigger token detection
     * @param charSequence source text to look at
     * @return true if charSequence contains a value that should end a token
     */
    boolean containsTokenTerminator(CharSequence charSequence);
}
