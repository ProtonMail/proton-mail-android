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
package ch.protonmail.tokenautocomplete.example;

import android.view.View;

import androidx.test.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Locale;

import static androidx.core.util.Preconditions.checkNotNull;

/** Convenience matchers to make it easier to check token view contents
 * Created by mgod on 8/25/17.
 */

class TokenMatchers {
    static Matcher<View> emailForPerson(final int position, final Matcher<String> stringMatcher) {
        checkNotNull(stringMatcher);
        return new BoundedMatcher<View, ContactsCompletionView>(ContactsCompletionView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText(String.format(Locale.US, "email for person %d: ", position));
                stringMatcher.describeTo(description);
            }
            @Override
            public boolean matchesSafely(ContactsCompletionView view) {
                if (view.getObjects().size() <= position) { return stringMatcher.matches(null); }
                return stringMatcher.matches(view.getObjects().get(position).getEmail());
            }
        };
    }

    static Matcher<View> tokenCount(final Matcher<Integer> intMatcher) {
        checkNotNull(intMatcher);
        return new BoundedMatcher<View, ContactsCompletionView>(ContactsCompletionView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("token count: ");
                intMatcher.describeTo(description);
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                description.appendText("token count: <" + item + ">");
                intMatcher.describeMismatch(item, description);
            }

            @Override
            public boolean matchesSafely(ContactsCompletionView view) {
                return intMatcher.matches(view.getObjects().size());
            }
        };
    }

    static Matcher<View> orderedTokenObjects(final List<?> objects) {
        checkNotNull(objects);
        return new BoundedMatcher<View, ContactsCompletionView>(ContactsCompletionView.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText(tokenObjectDescription(objects));
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                super.describeMismatch(item, description);

                String expected = tokenObjectDescription(objects);
                String actual = tokenObjectDescription(((ContactsCompletionView)item).getObjects());
                description.appendText(String.format("Expected %s\nGot %s", expected, actual));
            }

            @Override
            protected boolean matchesSafely(ContactsCompletionView view) {
                return objects.equals(view.getObjects());
            }

            private String tokenObjectDescription(List o) {
                return String.format(Locale.US, "token objects: %s", o.toString());
            }
        };
    }
}
