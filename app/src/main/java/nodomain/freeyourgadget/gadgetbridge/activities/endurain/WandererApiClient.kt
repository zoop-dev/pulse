/*  Copyright (C) 2026 Arjan Schrijver

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.endurain

import androidx.core.net.toUri
import nodomain.freeyourgadget.gadgetbridge.util.InternetUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File

class WandererApiClient(
    private val baseUrl: String,
    private val tokenManager: WandererTokenManager
) {
    private val LOG = LoggerFactory.getLogger(WandererApiClient::class.java)

    /**
     * Build headers with authentication tokens
     */
    private fun buildHeaders(): MutableMap<String, String> {
        val headers: MutableMap<String, String> = mutableMapOf()

        tokenManager.getAPIToken()?.let { token ->
            headers["Authorization"] = "Bearer $token"
        }

        return headers
    }

    /**
     * Upload activity file (GPX)
     */
    fun uploadActivity(file: File, callback: (String?, String?) -> Unit) {
        Thread {
            try {
                val uri = "$baseUrl/api/v1/trail/upload".toUri()
                val headers = buildHeaders()

                InternetUtils.uploadBinaryFile(
                    uri = uri,
                    file = file,
                    requestHeaders = headers,
                    method = "PUT"
                ) { success, statusCode, responseText ->
                    if (success && statusCode != null && statusCode >= 200 && statusCode < 300 && responseText != null) {
                        LOG.debug("Response $statusCode from Wanderer: $responseText")
                        val jsonObject = JSONObject(responseText)
                        callback(jsonObject.getString("id"), null)
                    } else {
                        if (responseText != null) {
                            val jsonObject = JSONObject(responseText)
                            val message = jsonObject.getString("message")
                            LOG.error("Activity upload failed: $message")
                            callback(null, message)
                        } else {
                            LOG.error("Activity upload failed")
                            callback(null, null)
                        }
                    }
                }
            } catch (e: Exception) {
                LOG.error("Activity upload error", e)
                callback(null, null)
            }
        }.start()
    }
}
