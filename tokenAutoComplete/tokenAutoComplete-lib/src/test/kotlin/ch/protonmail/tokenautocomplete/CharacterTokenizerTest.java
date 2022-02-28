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

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Make sure the tokenizer finds the right boundaries
 *
 * Created by mgod on 8/24/17.
 */
public class CharacterTokenizerTest {

    @Test
    public void handleWhiteSpaceWithCommaTokens() {
        CharacterTokenizer tokenizer = new CharacterTokenizer(Collections.singletonList(','), ",");
        String text = "bears, ponies";

        assertTrue(tokenizer.containsTokenTerminator(text));

        assertEquals(2, tokenizer.findTokenRanges(text, 0, text.length()).size());

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Arrays.asList(new Range(0, 6), new Range(7, 13)), ranges);
        assertEquals("bears,", text.subSequence(ranges.get(0).start, ranges.get(0).end));
        assertEquals("ponies", text.subSequence(ranges.get(1).start, ranges.get(1).end));

        ranges = tokenizer.findTokenRanges(text, 5, text.length());
        assertEquals(", ponies", text.substring(5));
        assertEquals(Collections.singletonList(new Range(7, 13)), ranges);

        ranges = tokenizer.findTokenRanges(text, 1, text.length());
        assertEquals(Arrays.asList(new Range(1, 6), new Range(7, 13)), ranges);

        assertEquals(Collections.singletonList(new Range(7, 13)),
                tokenizer.findTokenRanges(text, 7, text.length()));
        assertEquals(Collections.singletonList(new Range(8, 13)),
                tokenizer.findTokenRanges(text, 8, text.length()));
        assertEquals(Collections.singletonList(new Range(11, 13)),
                tokenizer.findTokenRanges(text, 11, text.length()));
    }

    @Test
    public void handleWhiteSpaceWithWhitespaceTokens() {
        CharacterTokenizer tokenizer = new CharacterTokenizer(Collections.singletonList(' '), "");
        String text = "bears ponies";

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Arrays.asList(new Range(0, 6), new Range(6, 12)), ranges);

        ranges = tokenizer.findTokenRanges(text, 1, text.length());
        assertEquals(Arrays.asList(new Range(1, 6), new Range(6, 12)), ranges);

        ranges = tokenizer.findTokenRanges(text, 4, text.length());
        assertEquals(Arrays.asList(new Range(4, 6), new Range(6, 12)), ranges);

        ranges = tokenizer.findTokenRanges(text, 6, text.length());
        assertEquals(Collections.singletonList(new Range(6, 12)), ranges);

        ranges = tokenizer.findTokenRanges(text, 7, text.length());
        assertEquals(Collections.singletonList(new Range(7, 12)), ranges);

        ranges = tokenizer.findTokenRanges(text, 0, text.length() - 3);
        assertEquals(Arrays.asList(new Range(0, 6), new Range(6, 9)), ranges);
    }

    @Test
    public void handleLotsOfWhitespace() {
        CharacterTokenizer tokenizer = new CharacterTokenizer(Collections.singletonList(','), "");
        String text = "bears,      ponies     ,another";

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Arrays.asList(new Range(0, 6), new Range(12, 24), new Range(24, 31)), ranges);
    }

    @Test
    public void handleLotsOfWhitespaceWithWhitespaceTokenizer() {
        CharacterTokenizer tokenizer = new CharacterTokenizer(Collections.singletonList(' '), "");
        String text = "bears,  \t   ponies  \n  ,another";

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Arrays.asList(new Range(0, 7), new Range(12, 19), new Range(23, 31)), ranges);
    }

    @Test
    public void allowsOneCharacterCandidateRangeMatches() {
        CharacterTokenizer tokenizer = new CharacterTokenizer(Collections.singletonList(','), "");
        String text = "a";

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Collections.singletonList(new Range(0,1)), ranges);
    }

    @Test
    public void allowsOneCharacterCandidateRangeMatchesWithWhitespace() {
        CharacterTokenizer tokenizer = new CharacterTokenizer(Collections.singletonList(','), "");
        String text = " a";

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Collections.singletonList(new Range(1,2)), ranges);
    }

    @Test
    public void doesntMatchWhitespaceAsCandidateTokenRange() {
        CharacterTokenizer tokenizer = new CharacterTokenizer(Collections.singletonList(','), "");
        String text = "test, ";

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Collections.singletonList(new Range(0, 5)), ranges);
    }

    @Test
    public void matchesSingleLetterTokens() {
        CharacterTokenizer tokenizer = new CharacterTokenizer(Collections.singletonList(','), "");
        String text = "t,r, a,,b";

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Arrays.asList(new Range(0, 2), new Range(2,4), new Range(5,7), new Range(8,9)), ranges);
    }
}
