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

import android.net.Uri
import androidx.core.net.toUri
import com.google.gson.Gson
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.util.InternetUtils
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File

enum class EndurainAuthType {
    NONE,
    AUTH_TOKEN,
    REFRESH_TOKEN
}

data class EndurainLoginResponse(
    val session_id: String? = null,
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Int? = null,
    val refresh_token_expires_in: Int? = null,
    val token_type: String? = null,
    val mfa_required: Boolean? = null,
    val username: String? = null,
    val detail: String? = null
)

data class EndurainMfaVerifyRequest(
    val username: String,
    val mfa_code: String
)

data class EndurainTokenExchangeRequest(
    val code_verifier: String
)

data class EndurainIdentityProvider(
    val id: String,
    val name: String,
    val slug: String
)

class EndurainApiClient(
    private val baseUrl: String,
    private val tokenManager: EndurainTokenManager
) {
    private val gson = Gson()
    private val LOG = LoggerFactory.getLogger(EndurainApiClient::class.java)

    /**
     * Build headers with authentication tokens
     */
    private fun buildHeaders(auth: EndurainAuthType): MutableMap<String, String> {
        val headers = mutableMapOf("X-Client-Type" to "mobile")

        when (auth) {
            EndurainAuthType.AUTH_TOKEN -> {
                tokenManager.getAccessToken()?.let { token ->
                    headers["Authorization"] = "Bearer $token"
                }
            }
            EndurainAuthType.REFRESH_TOKEN -> {
                tokenManager.getRefreshToken()?.let { token ->
                    headers["Authorization"] = "Bearer $token"
                }
            }
            else -> {}
        }

        return headers
    }

    /**
     * Username/Password Login
     */
    fun login(username: String, password: String): EndurainLoginResponse? {
        try {
            val uri = "$baseUrl/api/v1/auth/login".toUri()

            // Form-encoded body
            val body = "username=${Uri.encode(username)}&password=${Uri.encode(password)}"

            val headers = buildHeaders(EndurainAuthType.NONE)
            headers["Content-Type"] = "application/x-www-form-urlencoded"

            val responseText = InternetUtils.doStringRequest(
                uri = uri,
                method = "POST",
                requestHeaders = headers,
                body = body
            )

            return if (responseText != null) {
                gson.fromJson(responseText, EndurainLoginResponse::class.java)
            } else {
                LOG.error("Login failed: empty response")
                null
            }
        } catch (e: Exception) {
            LOG.error("Login error", e)
            return null
        }
    }

    /**
     * MFA Verification
     */
    fun verifyMfa(username: String, mfaCode: String): EndurainLoginResponse? {
        try {
            val uri = "$baseUrl/api/v1/auth/mfa/verify".toUri()

            val request = EndurainMfaVerifyRequest(username, mfaCode)
            val body = gson.toJson(request)

            val headers = buildHeaders(EndurainAuthType.NONE)
            headers["Content-Type"] = "application/json"

            val responseText = InternetUtils.doStringRequest(
                uri = uri,
                method = "POST",
                requestHeaders = headers,
                body = body
            )

            return if (responseText != null) {
                gson.fromJson(responseText, EndurainLoginResponse::class.java)
            } else {
                LOG.error("MFA verification failed: empty response")
                null
            }
        } catch (e: Exception) {
            LOG.error("MFA verification error", e)
            return null
        }
    }

    /**
     * Token Refresh
     */
    fun refreshToken(): EndurainLoginResponse? {
        try {
            val uri = "$baseUrl/api/v1/auth/refresh".toUri()

            val headers = buildHeaders(EndurainAuthType.REFRESH_TOKEN)
            headers["Content-Type"] = "application/json"

            val responseText = InternetUtils.doStringRequest(
                uri = uri,
                method = "POST",
                requestHeaders = headers,
                body = "{}"
            )

            return if (responseText != null) {
                gson.fromJson(responseText, EndurainLoginResponse::class.java)
            } else {
                LOG.error("Token refresh failed: empty response")
                null
            }
        } catch (e: Exception) {
            LOG.error("Token refresh error", e)
            return null
        }
    }

    /**
     * Logout
     */
    fun logout(): Boolean {
        try {
            val uri = "$baseUrl/api/v1/auth/logout".toUri()

            val headers = buildHeaders(EndurainAuthType.AUTH_TOKEN)
            headers["Content-Type"] = "application/json"

            InternetUtils.doStringRequest(
                uri = uri,
                method = "POST",
                requestHeaders = headers,
                body = "{}"
            )

            tokenManager.clearTokens()
            return true
        } catch (e: Exception) {
            LOG.error("Logout error", e)
            return false
        }
    }

    /**
     * Get list of available identity providers
     */
    fun getIdentityProviders(): List<EndurainIdentityProvider>? {
        try {
            val uri = "$baseUrl/api/v1/public/idp".toUri()

            val headers = buildHeaders(EndurainAuthType.NONE)
            headers["Content-Type"] = "application/json"

            val responseText = InternetUtils.doStringRequest(
                uri = uri,
                requestHeaders = headers
            )

            return if (responseText != null) {
                val type = object : com.google.gson.reflect.TypeToken<List<EndurainIdentityProvider>>() {}.type
                gson.fromJson(responseText, type)
            } else {
                LOG.error("Failed to fetch identity providers")
                null
            }
        } catch (e: Exception) {
            LOG.error("Error fetching identity providers", e)
            return null
        }
    }

    /**
     * Exchange OAuth session for tokens (PKCE flow)
     */
    fun exchangeOAuthSession(sessionId: String, codeVerifier: String): EndurainLoginResponse? {
        try {
            val uri = "$baseUrl/api/v1/public/idp/session/$sessionId/tokens".toUri()

            val request = EndurainTokenExchangeRequest(codeVerifier)
            val body = gson.toJson(request)

            val headers = buildHeaders(EndurainAuthType.NONE)
            headers["Content-Type"] = "application/json"

            val responseText = InternetUtils.doStringRequest(
                uri = uri,
                method = "POST",
                requestHeaders = headers,
                body = body
            )

            LOG.debug("OAuth token result: $responseText")

            return if (responseText != null) {
                gson.fromJson(responseText, EndurainLoginResponse::class.java)
            } else {
                LOG.error("OAuth token exchange failed: empty response")
                null
            }
        } catch (e: Exception) {
            LOG.error("OAuth token exchange error", e)
            return null
        }
    }

    /**
     * Generic authenticated API request
     */
    fun doAuthenticatedRequest(
        endpoint: String,
        method: String = "GET",
        body: String? = null
    ): String? {
        try {
            val uri = "$baseUrl$endpoint".toUri()

            val headers = buildHeaders(EndurainAuthType.AUTH_TOKEN)
            headers["Content-Type"] = "application/json"

            return InternetUtils.doStringRequest(
                uri = uri,
                method = method,
                requestHeaders = headers,
                body = body
            )
        } catch (e: Exception) {
            LOG.error("Authenticated request error", e)
            return null
        }
    }

    /**
     * Upload activity file (GPX)
     */
    fun uploadActivity(file: File, callback: (Int?) -> Unit) {
        Thread {
            try {
                val uri = "$baseUrl/api/v1/activities/create/upload".toUri()
                val headers = buildHeaders(EndurainAuthType.AUTH_TOKEN)

                InternetUtils.uploadBinaryFile(
                    uri = uri,
                    file = file,
                    requestHeaders = headers
                ) { success, statusCode, responseText ->
                    if (success && responseText != null) {
                        LOG.debug("Response $statusCode from Endurain: $responseText")
                        val jsonArray = JSONArray(responseText)
                        val firstObject = jsonArray.getJSONObject(0)
                        val id = firstObject.getInt("id")
                        callback(id)
                    } else {
                        LOG.error("Activity upload failed")
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Activity upload error", e)
                callback(null)
            }
        }.start()
    }

    /**
     * Upload activity photo
     */
    fun uploadActivityPhoto(activityId: Int, file: File) {
        Thread {
            try {
                val uri = "$baseUrl/api/v1/activities_media/upload/activity_id/$activityId".toUri()
                val headers = buildHeaders(EndurainAuthType.AUTH_TOKEN)

                InternetUtils.uploadBinaryFile(
                    uri = uri,
                    file = file,
                    requestHeaders = headers
                ) { success, statusCode, responseText ->
                    if (success && responseText != null) {
                        LOG.debug("Response ($statusCode) from Endurain: $responseText")
                    } else {
                        LOG.error("Activity photo upload to Endurain failed. Response ($statusCode) received: $responseText")
                    }
                }
            } catch (e: Exception) {
                LOG.error("Activity photo upload error", e)
            }
        }.start()
    }

    /**
     * Edit uploaded activity
     */
    fun editActivity(id: Int, activityKind: ActivityKind, name: String): Boolean {
        try {
            val uri = "$baseUrl/api/v1/activities/edit".toUri()
            val headers = buildHeaders(EndurainAuthType.AUTH_TOKEN)
            headers["Content-Type"] = "application/json"

            var activityType = 10  // Generic workout
            if (activityLookup.containsKey(activityKind.ordinal)) {
                activityType = activityLookup[activityKind.ordinal]!!
            }

            val bodyJson = JSONObject().apply {
                put("id", id)
                put("activity_type", activityType)
                put("name", name)
            }

            val result = InternetUtils.doStringRequest(
                method = "PUT",
                uri = uri,
                requestHeaders = headers,
                body = bodyJson.toString()
            )
            LOG.info("editActivity result: {}", result)
            return true
        } catch (e: Exception) {
            LOG.error("Activity edit error", e)
            return false
        }
    }

    /**
     * Fetch server version string
     */
    fun fetchVersion(): String? {
        try {
            val uri = "$baseUrl/api/v1/about".toUri()

            val headers = buildHeaders(EndurainAuthType.NONE)

            val result = InternetUtils.doJsonRequest(
                uri = uri,
                requestHeaders = headers,
            )
            return result?.getString("version")
        } catch (e: Exception) {
            LOG.error("Fetching server version error", e)
            return null
        }
    }

    /**
     * Lookup map to convert ActivityKind to the integer Endurain expects,
     * based on https://docs.endurain.com/developer-guide/supported-types/
     */
    val activityLookup = mapOf(
        ActivityKind.RUNNING.ordinal to 1,
        ActivityKind.TRAIL_RUN.ordinal to 2,
        ActivityKind.VIRTUAL_RUN.ordinal to 3,
        ActivityKind.CYCLING.ordinal to 4,
        ActivityKind.ROAD_BIKE.ordinal to 4,
        ActivityKind.GRAVEL_BIKE.ordinal to 5,
        ActivityKind.MOUNTAIN_BIKE.ordinal to 6,
        ActivityKind.POOL_SWIM.ordinal to 8,
        ActivityKind.SWIMMING_OPENWATER.ordinal to 9,
        ActivityKind.TRAINING.ordinal to 10,
        ActivityKind.WALKING.ordinal to 11,
        ActivityKind.HIKING.ordinal to 12,
        ActivityKind.ROWING.ordinal to 13,
        ActivityKind.YOGA.ordinal to 14,
        ActivityKind.SKIING.ordinal to 15,
        ActivityKind.SNOWBOARDING.ordinal to 17,
        ActivityKind.TRANSITION.ordinal to 18,
        ActivityKind.STRENGTH_TRAINING.ordinal to 19,
        ActivityKind.CROSSFIT.ordinal to 20,
        ActivityKind.TENNIS.ordinal to 21,
        ActivityKind.TABLE_TENNIS.ordinal to 22,
        ActivityKind.BADMINTON.ordinal to 23,
        ActivityKind.SQUASH.ordinal to 24,
        ActivityKind.RACQUETBALL.ordinal to 25,
        ActivityKind.PICKLEBALL.ordinal to 26,
        ActivityKind.BIKE_COMMUTE.ordinal to 27,
        ActivityKind.INDOOR_CYCLING.ordinal to 28,
        ActivityKind.WINDSURFING.ordinal to 30,
        ActivityKind.INDOOR_WALKING.ordinal to 31,
        ActivityKind.STAND_UP_PADDLEBOARDING.ordinal to 32,
        ActivityKind.SURFING.ordinal to 33,
        ActivityKind.TRACK_RUN.ordinal to 34,
        ActivityKind.E_BIKE.ordinal to 35,
        ActivityKind.E_MOUNTAIN_BIKE.ordinal to 36,
        ActivityKind.ICE_SKATING.ordinal to 37,
        ActivityKind.SOCCER.ordinal to 38,
        ActivityKind.PADEL.ordinal to 39,
        ActivityKind.TREADMILL.ordinal to 40,
        ActivityKind.CARDIO.ordinal to 41,
        ActivityKind.KAYAKING.ordinal to 42,
        ActivityKind.SAILING.ordinal to 43,
        ActivityKind.INLINE_SKATING.ordinal to 45,
        ActivityKind.HIIT.ordinal to 46
    )
}
