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
package ch.protonmail.android.uitests.testRail

import org.json.simple.JSONObject
import org.json.simple.JSONValue
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class APIClient(base_url: String) {
    /**
     * Get/Set User
     *
     * Returns/sets the user used for authenticating the API requests.
     */
    var user: String? = null

    /**
     * Get/Set Password
     *
     * Returns/sets the password used for authenticating the API requests.
     */
    var password: String? = null
    private val m_url: String

    /**
     * Send Get
     *
     * Issues a GET request (read) against the API and returns the result
     * (as Object, see below).
     *
     * Arguments:
     *
     * uri                  The API method to call including parameters
     * (e.g. get_case/1)
     *
     * Returns the parsed JSON response as standard object which can
     * either be an instance of JSONObject or JSONArray (depending on the
     * API method). In most cases, this returns a JSONObject instance which
     * is basically the same as java.util.Map.
     *
     * If 'get_attachment/:attachment_id', returns a String
     */
    @Throws(MalformedURLException::class, IOException::class, APIException::class)
    fun sendGet(uri: String, data: String?): Any? {
        return sendRequest("GET", uri, data)
    }

    @Throws(MalformedURLException::class, IOException::class, APIException::class)
    fun sendGet(uri: String): Any? {
        return sendRequest("GET", uri, null)
    }

    /**
     * Send POST
     *
     * Issues a POST request (write) against the API and returns the result
     * (as Object, see below).
     *
     * Arguments:
     *
     * uri                  The API method to call including parameters
     * (e.g. add_case/1)
     * data                 The data to submit as part of the request (e.g.,
     * a map)
     * If adding an attachment, must be the path
     * to the file
     *
     * Returns the parsed JSON response as standard object which can
     * either be an instance of JSONObject or JSONArray (depending on the
     * API method). In most cases, this returns a JSONObject instance which
     * is basically the same as java.util.Map.
     */
    @Throws(MalformedURLException::class, IOException::class, APIException::class)
    fun sendPost(uri: String, data: Any?): Any? {
        return sendRequest("POST", uri, data)
    }

    @Throws(MalformedURLException::class, IOException::class, APIException::class)
    private fun sendRequest(method: String, uri: String, data: Any?): Any? {
        val url = URL(m_url + uri)
        // Create the connection object and set the required HTTP method
        // (GET/POST) and headers (content type and basic auth).
        val conn = url.openConnection() as HttpURLConnection
        val auth = getAuthorization(user, password)
        conn.addRequestProperty("Authorization", "Basic $auth")
        if (method == "POST") {
            conn.requestMethod = "POST"
            // Add the POST arguments, if any. We just serialize the passed
            // data object (i.e. a dictionary) and then add it to the
            // request body.
            if (data != null) {
                if (uri.startsWith("add_attachment")) // add_attachment API requests
                {
                    val boundary = "TestRailAPIAttachmentBoundary" //Can be any random string
                    val uploadFile = File(data as String?)
                    conn.doOutput = true
                    conn.addRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    val ostreamBody = conn.outputStream
                    val bodyWriter = BufferedWriter(OutputStreamWriter(ostreamBody))
                    bodyWriter.write("\n\n--$boundary\r\n")
                    bodyWriter.write("Content-Disposition: form-data; name=\"attachment\"; filename=\""
                            + uploadFile.name + "\"")
                    bodyWriter.write("\r\n\r\n")
                    bodyWriter.flush()

                    //Read file into request
                    val istreamFile: InputStream = FileInputStream(uploadFile)
                    var bytesRead: Int
                    val dataBuffer = ByteArray(1024)
                    while (istreamFile.read(dataBuffer).also { bytesRead = it } != -1) {
                        ostreamBody.write(dataBuffer, 0, bytesRead)
                    }
                    ostreamBody.flush()

                    //end of attachment, add boundary
                    bodyWriter.write("\r\n--$boundary--\r\n")
                    bodyWriter.flush()

                    //Close streams
                    istreamFile.close()
                    ostreamBody.close()
                    bodyWriter.close()
                } else  // Not an attachment
                {
                    conn.addRequestProperty("Content-Type", "application/json")
                    val block = JSONValue.toJSONString(data).toByteArray(charset("UTF-8"))
                    conn.doOutput = true
                    val ostream = conn.outputStream
                    ostream.write(block)
                    ostream.close()
                }
            }
        } else  // GET request
        {
            conn.addRequestProperty("Content-Type", "application/json")
        }

        // Execute the actual web request (if it wasn't already initiated
        // by getOutputStream above) and record any occurred errors (we use
        // the error stream in this case).
        val status = conn.responseCode
        val istream: InputStream?
        if (status != 200) {
            istream = conn.errorStream
            if (istream == null) {
                throw APIException(
                        "TestRail API return HTTP " + status +
                                " (No additional error message received)"
                )
            }
        } else {
            istream = conn.inputStream
        }

        // If 'get_attachment' (not 'get_attachments') returned valid status code, save the file
        if (istream != null
                && uri.startsWith("get_attachment/")) {
            val outputStream = FileOutputStream(data as String?)
            var bytesRead = 0
            val buffer = ByteArray(1024)
            while (istream.read(buffer).also { bytesRead = it } > 0) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.close()
            istream.close()
            return data
        }

        // Not an attachment received
        // Read the response body, if any, and deserialize it from JSON.
        var text = ""
        if (istream != null) {
            val reader = BufferedReader(
                    InputStreamReader(
                            istream,
                            "UTF-8"
                    )
            )
            var line: String? = null;

            while ({ line = reader.readLine(); line }() != null) {
                text += line
                text += System.getProperty("line.separator")
            }
            reader.close()
        }
        val result: Any?
        result = if (text != "") {
            JSONValue.parse(text)
        } else {
            JSONObject()
        }

        // Check for any occurred errors and add additional details to
        // the exception message, if any (e.g. the error message returned
        // by TestRail).
        if (status != 200) {
            var error = "No additional error message received"
            if (result != null && result is JSONObject) {
                val obj = result
                if (obj.containsKey("error")) {
                    error = '"'.toString() + obj["error"] as String + '"'
                }
            }
            throw APIException(
                    "TestRail API returned HTTP " + status +
                            "(" + error + ")"
            )
        }
        return result
    }

    companion object {
        private fun getAuthorization(user: String?, password: String?): String {
            try {
                return String(Base64.getEncoder().encode("$user:$password".toByteArray()))
            } catch (e: IllegalArgumentException) {
                // Not thrown
            }
            return ""
        }
    }

    init {
        var base_url = base_url
        if (!base_url.endsWith("/")) {
            base_url += "/"
        }
        m_url = base_url + "index.php?/api/v2/"
    }
}