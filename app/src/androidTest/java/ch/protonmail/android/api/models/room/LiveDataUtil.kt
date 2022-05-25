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
package ch.protonmail.android.api.models.room

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.junit.Assert
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Throws(InterruptedException::class)
inline fun <reified T> LiveData<T>.getTestValue(timeout:Long=2,timeUnit:TimeUnit=TimeUnit.SECONDS):T? {
	val data = arrayOfNulls<T?>(1)
	val latch = CountDownLatch(1)
	val observer = object : Observer<T> {
		override fun onChanged(value: T?) {
			data[0] = value
			latch.countDown()
			removeObserver(this)
		}
	}
	observeForever(observer)
	if (!latch.await(timeout, timeUnit)) {
		Assert.fail("Execution took more than $timeout $timeUnit")
	}
	return data[0]
}

val <reified T> LiveData<T>.testValue:T? inline get(){
	return getTestValue()
}
