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
package ch.protonmail.android.uitests.testsHelper.testRail

import org.json.simple.JSONObject
import java.io.IOException
import java.util.*

object TestRailService {

    var PROJECT_ID = "23" //enter your project_id or pass as property;
    var TESTRAIL_USERNAME = "filip.gudjevski@protonmail.com"//enter testrail username
    var TESTRAIL_PASSWORD = "Grofche0412!"//enter testrail password
    var RAILS_ENGINE_URL = "https://proton.testrail.io/"

    fun createTestRun(): String {
        val client = APIClient(RAILS_ENGINE_URL)
        client.user = TESTRAIL_USERNAME
        client.password = TESTRAIL_PASSWORD
        val data = HashMap<Any?, Any?>()
        data["name"] = "Test Run"
        var newRun: JSONObject? = null
        try {
            newRun = client.sendPost("add_run/$PROJECT_ID", data) as JSONObject
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: APIException) {
            e.printStackTrace()
        }
        val id = newRun!!["id"] as Long
        return id.toString()
    }

    @Throws(APIException::class, IOException::class)
    fun addResultForTestCase(testCaseId: String, status: Int,
                             error: String, testRunId: String) {
        val client = APIClient(RAILS_ENGINE_URL)
        client.user = TESTRAIL_USERNAME
        client.password = TESTRAIL_PASSWORD
        val data = HashMap<Any?, Any?>()
        data["status_id"] = status
        if(status == 5) {
            data["comment"] = error;
        } else {
            data["comment"] = "Test Passed.";
        }
        client.sendPost("add_result_for_case/$testRunId/$testCaseId", data)
    }

//    @get:Throws(APIException::class, IOException::class)
//    val testRun: String
//        get() {
//            val client = APIClient(RAILS_ENGINE_URL)
//            client.user = TESTRAIL_USERNAME
//            client.password = TESTRAIL_PASSWORD
//            val resp = client.sendGet("get_runs/" + 23) as JSONArray
//            val obj = resp[0] as JSONObject
//            val id = obj["id"] as Long
//            return java.lang.Long.toString(id)
//        }

}