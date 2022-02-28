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
package ch.protonmail.android.testAndroidInstrumented;

import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Created by Kamil Rajtar on 06.09.18.  */
class HamcrestMismatchBuilder(val mismatchDescription: Description){
	private var matches = true

	init {
		mismatchDescription.appendText("{")
	}

	fun <T> match(name: String, expected: Matcher<out T>, actual: T): HamcrestMismatchBuilder {
		System.err.println("Name: $name, expected: $expected, actual: $actual.")
		if(!expected.matches(actual)) {
			expected.reportMismatch(name, actual, mismatchDescription, matches)
			matches = false
		}
		return this
	}

	fun build():Boolean {
		mismatchDescription.appendText("}")
		return matches
	}
}
