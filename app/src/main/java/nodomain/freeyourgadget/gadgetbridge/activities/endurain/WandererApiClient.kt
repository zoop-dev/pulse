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
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
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
     * Verifies both that the server is reachable and that [apiToken] is valid, by calling an
     * authenticated endpoint. Wanderer's auth layer rejects a bad/revoked key on any /api request
     * (the response carries an error object instead of a resource list), so a successful list
     * response means the key is good. [apiToken] is passed in explicitly because during setup it
     * is not persisted yet. [callback] fires with (reachable, reason): reason is null on success,
     * otherwise a localized, user-facing explanation (server unreachable / invalid API key).
     */
    fun checkServerReachable(apiToken: String, callback: (reachable: Boolean, reason: String?) -> Unit) {
        Thread {
            val context = GBApplication.getContext()
            try {
                val headers = mutableMapOf("Authorization" to "Bearer $apiToken")
                // networkFailureReason() (via onError) is more specific than connectFailureReason:
                // it distinguishes could-not-resolve-host / refused / timed-out / TLS.
                var networkReason: String? = null
                val response = InternetUtils.doJsonRequest(
                    uri = "$baseUrl/api/v1/api-token".toUri(),
                    requestHeaders = headers,
                    onError = { reason -> networkReason = reason }
                )
                when {
                    // No usable response at all: offline / helper unavailable / server down.
                    response == null ->
                        callback(false, networkReason ?: InternetUtils.connectFailureReason(context, baseUrl))
                    // Authenticated list came back → server reachable and key accepted.
                    response.has("items") ->
                        callback(true, null)
                    // Server responded but rejected the credentials (bad or revoked key).
                    else ->
                        callback(false, context.getString(R.string.toast_error_invalid_api_key))
                }
            } catch (e: Exception) {
                LOG.error("Wanderer reachability check failed", e)
                callback(false, InternetUtils.connectFailureReason(context, baseUrl))
            }
        }.start()
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
                ) { success, statusCode, responseText, reason ->
                    if (success && statusCode != null && statusCode >= 200 && statusCode < 300 && responseText != null) {
                        LOG.debug("Response $statusCode from Wanderer: $responseText")
                        val jsonObject = JSONObject(responseText)
                        callback(jsonObject.getString("id"), null)
                    } else {
                        // Prefer the server's own error message; fall back to the network reason.
                        val message = try {
                            if (responseText != null) JSONObject(responseText).getString("message") else null
                        } catch (e: Exception) {
                            null
                        } ?: reason ?: statusCode?.let { "HTTP $it" }
                        LOG.error("Activity upload failed: {}", message)
                        callback(null, message)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Activity upload error", e)
                callback(null, null)
            }
        }.start()
    }
}
