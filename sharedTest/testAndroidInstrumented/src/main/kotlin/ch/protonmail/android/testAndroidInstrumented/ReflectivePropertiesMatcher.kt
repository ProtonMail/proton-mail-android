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
package ch.protonmail.android.testAndroidInstrumented

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.TypeSafeDiagnosingMatcher
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Created by Kamil Rajtar on 07.09.18.  */
class ReflectivePropertiesMatcher<T:Any>(
		target: T,
		matcherFactory: (Any?) -> Matcher<*> = {`is`(equalTo(it)) },
		includeNonPublicFields: Boolean = true
):TypeSafeDiagnosingMatcher<T> () {

	private val declaredMemberProperties = target::class.declaredMemberProperties
	private val propertiesMatchers = declaredMemberProperties.mapNotNull { property ->
		if(property.visibility == KVisibility.PUBLIC || includeNonPublicFields) {
			property.isAccessible = true
			val name = property.name
			val getter = property.getter
			val value = getter.call(target)
			val matcher = matcherFactory(value)
			PropertyMatcher(name, matcher, getter)
		}
		else {
			null
		}

	}

	override fun describeTo(description:Description) {
		return description.build(*propertiesMatchers.map { it.name to it.matcher }.toTypedArray())
	}

	override fun matchesSafely(item: T, mismatchDescription: Description):Boolean {
		val builder = HamcrestMismatchBuilder(mismatchDescription)
		propertiesMatchers.forEach {
			builder.match(it.name, it.matcher, it.getter.call(item))
		}
		return builder.build()
	}

	private data class PropertyMatcher(
			val name:String,
			val matcher:Matcher<*>,
			val getter:KProperty1.Getter<*,*>
	)
}
