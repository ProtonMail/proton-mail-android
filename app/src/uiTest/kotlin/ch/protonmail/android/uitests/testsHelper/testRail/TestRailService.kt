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
package ch.protonmail.android.uitests.testsHelper.testRail

import android.util.Log
import ch.protonmail.android.BuildConfig
import me.proton.core.test.android.instrumented.ProtonTest.Companion.testTag
import org.json.simple.JSONObject
import java.io.IOException
import java.util.HashMap
import kotlin.jvm.Throws

object TestRailService {

    private const val RAILS_ENGINE_URL = "https://proton.testrail.io/"
    private const val androidSuiteId = "23"
    private const val failedStatus = 5

    fun createTestRun(): String {
        val client = APIClient(RAILS_ENGINE_URL)
        client.user = BuildConfig.TESTRAIL_USERNAME
        client.password = BuildConfig.TESTRAIL_PASSWORD
        val data = HashMap<Any?, Any?>()
        data["name"] = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        data["suite_id"] = androidSuiteId
        var newRun: JSONObject? = null
        try {
            newRun = client.sendPost("add_run/${BuildConfig.TESTRAIL_PROJECT_ID}", data) as JSONObject
        } catch (t: Throwable) {
            Log.d(testTag, "Error while sending a request to TestRail: ${t.message}")
        }
        val id = newRun!!["id"] as Long
        return id.toString()
    }

    @Throws(APIException::class, IOException::class)
    fun addResultForTestCase(testCaseId: String, status: Int, error: String, testRunId: String) {
        val client = APIClient(RAILS_ENGINE_URL)
        val data = HashMap<Any?, Any?>()
        client.user = BuildConfig.TESTRAIL_USERNAME
        client.password = BuildConfig.TESTRAIL_PASSWORD
        data["status_id"] = status
        if (status == failedStatus) {
            data["comment"] = error
        } else {
            data["comment"] = "Test Passed."
        }
        try {
            client.sendPost("add_result_for_case/$testRunId/$testCaseId", data)
        } catch (t: Throwable) {
            Log.d(testTag, "Error while sending a request to TestRail: ${t.message}")
        }
    }
}
