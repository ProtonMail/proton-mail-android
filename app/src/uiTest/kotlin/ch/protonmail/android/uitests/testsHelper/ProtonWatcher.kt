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
package ch.protonmail.android.uitests.testsHelper

class ProtonWatcher {

    private var timeout = DEFAULT_TIMEOUT
    private var watchInterval = DEFAULT_INTERVAL

    abstract class Condition {
        abstract fun getDescription(): String
        abstract fun checkCondition(): Boolean
    }

    companion object {
        const val CONDITION_NOT_MET = 0
        const val DEFAULT_TIMEOUT = 10_000L
        const val DEFAULT_INTERVAL = 250L
        const val TIMEOUT = 2
        var status = CONDITION_NOT_MET
        private const val CONDITION_MET = 1
        private val instance = ProtonWatcher()

        fun waitForCondition(condition: Condition) {
            var timeInterval = 0L
            while (status != CONDITION_MET) {
                if (condition.checkCondition()) {
                    status = CONDITION_MET
                    break
                } else {
                    if (timeInterval < instance.timeout) {
                        timeInterval += instance.watchInterval * 2
                        Thread.sleep(instance.watchInterval)
                    } else {
                        status = TIMEOUT
                        break
                    }
                }
            }
            // reset to initial state
            status = 0
        }

        fun setTimeout(ms: Long) {
            instance.timeout = ms
        }
    }
}
