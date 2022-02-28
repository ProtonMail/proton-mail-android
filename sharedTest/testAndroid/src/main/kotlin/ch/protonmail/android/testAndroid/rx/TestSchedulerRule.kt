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
package ch.protonmail.android.testAndroid.rx

import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Created by kadrikj on 8/25/18.
 */
class TestSchedulerRule : TestRule {

    companion object {
        val testScheduler: TestScheduler

        init {
            testScheduler = TestScheduler()
            RxAndroidPlugins.setInitMainThreadSchedulerHandler { testScheduler }
            RxJavaPlugins.setInitComputationSchedulerHandler { testScheduler }
            RxJavaPlugins.setInitIoSchedulerHandler { testScheduler }
        }
    }

    val schedulerTest get() = testScheduler

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                RxAndroidPlugins.reset()
                RxAndroidPlugins.setInitMainThreadSchedulerHandler{ testScheduler }

                RxJavaPlugins.reset()
                RxJavaPlugins.setIoSchedulerHandler{ testScheduler }
                RxJavaPlugins.setNewThreadSchedulerHandler{ testScheduler }

                base.evaluate()

                RxAndroidPlugins.reset()
                RxJavaPlugins.reset()
            }
        }
    }

}
