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
package ch.protonmail.android.testAndroidInstrumented

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.hamcrest.SelfDescribing

typealias MatcherFactory = (Any?) -> Matcher<*>

fun Matcher<*>.reportMismatch(
		name:String,
		item:Any?,
		mismatchDescription:Description,
		firstMismatch:Boolean
) {
	if (!firstMismatch)
		mismatchDescription.appendText(", ")
	mismatchDescription.appendText(name).appendText(" ")
	describeMismatch(item, mismatchDescription)
}

fun Description.appendField(field: Pair<String, SelfDescribing>) {
	appendText(field.first)
	appendDescriptionOf(field.second)
}

fun Description.build(vararg fields: Pair<String, SelfDescribing>) {
	appendText("{")
	val first = fields.first()
	appendField(first)
	fields.drop(1).forEach {
		appendText(", ")
		appendField(it)
	}
	appendText("}")
	return
}

inline fun <reified T: Any> Iterable<T?>.getMatchers(
		noinline factory: MatcherFactory = { `is`(Matchers.equalTo(it)) }
) : List<Matcher<T>> {
	return map { element->
		element?.let { ReflectivePropertiesMatcher(it, factory) } ?: `is`(nullValue()) as Matcher<T>
	}
}

inline val <reified T: Any> Iterable<T?>.matchers
	get() = getMatchers()

