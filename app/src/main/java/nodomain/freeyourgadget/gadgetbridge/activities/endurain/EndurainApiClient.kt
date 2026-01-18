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
import nodomain.freeyourgadget.gadgetbridge.util.InternetUtils
import org.slf4j.LoggerFactory

data class LoginResponse(
    val session_id: String? = null,
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Long? = null,
    val token_type: String? = null,
    val mfa_required: Boolean? = null,
    val username: String? = null,
    val detail: String? = null
)

data class MfaVerifyRequest(
    val username: String,
    val mfa_code: String
)

data class TokenRefreshRequest(
    val refresh_token: String
)

data class TokenExchangeRequest(
    val code_verifier: String
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
    private fun buildHeaders(): Map<String, String> {
        val headers = mutableMapOf("X-Client-Type" to "mobile")

        tokenManager.getAccessToken()?.let { token ->
            headers["Authorization"] = "Bearer $token"
        }

        return headers
    }

    /**
     * Username/Password Login
     */
    fun login(username: String, password: String): LoginResponse? {
        try {
            val uri = "$baseUrl/api/v1/auth/login".toUri()

            // Form-encoded body
            val body = "username=${Uri.encode(username)}&password=${Uri.encode(password)}"

            val headers = mapOf("X-Client-Type" to "mobile")

            val responseText = InternetUtils.doStringRequest(
                uri = uri,
                method = "POST",
                requestHeaders = headers,
                body = body,
                bodyContentType = "application/x-www-form-urlencoded",
                allowInsecure = false
            )

            return if (responseText != null) {
                gson.fromJson(responseText, LoginResponse::class.java)
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
    fun verifyMfa(username: String, mfaCode: String): LoginResponse? {
        try {
            val uri = "$baseUrl/api/v1/auth/mfa/verify".toUri()

            val request = MfaVerifyRequest(username, mfaCode)
            val body = gson.toJson(request)

            val headers = mapOf("X-Client-Type" to "mobile")

            val responseText = InternetUtils.doStringRequest(
                uri = uri,
                method = "POST",
                requestHeaders = headers,
                body = body,
                bodyContentType = "application/json",
                allowInsecure = false
            )

            return if (responseText != null) {
                gson.fromJson(responseText, LoginResponse::class.java)
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
    fun refreshToken(): LoginResponse? {
        try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                LOG.error("No refresh token available")
                return null
            }

            val uri = "$baseUrl/api/v1/auth/refresh".toUri()

            val request = TokenRefreshRequest(refreshToken)
            val body = gson.toJson(request)

            val headers = buildHeaders()

            val responseText = InternetUtils.doStringRequest(
                uri = uri,
                method = "POST",
                requestHeaders = headers,
                body = body,
                bodyContentType = "application/json",
                allowInsecure = false
            )

            return if (responseText != null) {
                gson.fromJson(responseText, LoginResponse::class.java)
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

            val headers = buildHeaders()

            InternetUtils.doStringRequest(
                uri = uri,
                method = "POST",
                requestHeaders = headers,
                body = null,
                bodyContentType = "application/json",
                allowInsecure = false
            )

            tokenManager.clearTokens()
            return true
        } catch (e: Exception) {
            LOG.error("Logout error", e)
            return false
        }
    }

    /**
     * Exchange OAuth session for tokens (PKCE flow)
     */
    fun exchangeOAuthSession(sessionId: String, codeVerifier: String): LoginResponse? {
        try {
            val uri = "$baseUrl/api/v1/public/idp/session/$sessionId/tokens".toUri()

            val request = TokenExchangeRequest(codeVerifier)
            val body = gson.toJson(request)

            val headers = mapOf("X-Client-Type" to "mobile")

            val responseText = InternetUtils.doStringRequest(
                uri = uri,
                method = "POST",
                requestHeaders = headers,
                body = body,
                bodyContentType = "application/json",
                allowInsecure = false
            )

            return if (responseText != null) {
                gson.fromJson(responseText, LoginResponse::class.java)
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

            val headers = buildHeaders()

            return InternetUtils.doStringRequest(
                uri = uri,
                method = method,
                requestHeaders = headers,
                body = body,
                bodyContentType = "application/json",
                allowInsecure = false
            )
        } catch (e: Exception) {
            LOG.error("Authenticated request error", e)
            return null
        }
    }
}