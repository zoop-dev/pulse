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

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import nodomain.freeyourgadget.gadgetbridge.util.InternetUtils
import org.slf4j.LoggerFactory
import java.util.Date

class EndurainSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val LOG = LoggerFactory.getLogger(EndurainSetupViewModel::class.java)
    private val tokenManager = EndurainTokenManager(application)
    private lateinit var apiClient: EndurainApiClient

    enum class Step {
        SERVER,
        LOGIN_TYPE,
        LOCAL_LOGIN,
        MFA_VERIFY,
        SSO_LOGIN
    }

    var step = Step.SERVER
    var server = ""
    var localLoginEnabled = false
    var ssoEnabled = false
    var pendingMfaUsername: String? = null

    /**
     * Fetch server capabilities to determine available login methods
     */
    fun fetchServerCapabilities(serverUrl: String, callback: (Boolean) -> Unit) {
        Thread {
            try {
                // Fetch server settings from the public endpoint
                val settingsUri = "$serverUrl/api/v1/public/server_settings".toUri()
                val settingsResponse = InternetUtils.doJsonRequest(settingsUri)

                if (settingsResponse != null) {
                    // Parse server settings to determine available authentication methods
                    localLoginEnabled = settingsResponse.optBoolean("local_login_enabled", true)
                    ssoEnabled = settingsResponse.optBoolean("sso_enabled", false)

                    LOG.info("Server capabilities - Local login: $localLoginEnabled, SSO: $ssoEnabled")

                    // Validate that at least one auth method is available
                    if (!localLoginEnabled && !ssoEnabled) {
                        LOG.warn("Server has no authentication methods enabled, defaulting to local login")
                        localLoginEnabled = true
                    }

                    callback(true)
                } else {
                    LOG.error("Failed to fetch server settings")
                    // Default to local login on failure
                    localLoginEnabled = true
                    ssoEnabled = false
                    callback(false)
                }
            } catch (e: Exception) {
                LOG.error("Error fetching server capabilities", e)
                // Default to local login on error
                localLoginEnabled = true
                ssoEnabled = false
                callback(false)
            }
        }.start()
    }

    /**
     * Perform token refresh
     */
    fun performTokenRefresh(
        serverUrl: String,
        callback: (Boolean) -> Unit
    ) {
        Thread {
            try {
                apiClient = EndurainApiClient(serverUrl, tokenManager)
                val response = apiClient.refreshToken()

                when {
                    response == null -> {
                        LOG.error("Token refresh failed: null response")
                        callback(false)
                    }
                    response.access_token != null -> {
                        LOG.info("Token refresh successful")
                        tokenManager.saveTokens(
                            response.access_token,
                            response.refresh_token!!,
                            response.expires_in!!
                        )
                        callback(true)
                    }
                    else -> {
                        LOG.error("Token refresh failed: ${response.message}")
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Token refresh error", e)
                callback(false)
            }
        }.start()
    }

    /**
     * Perform local username/password login
     */
    fun performLocalLogin(
        serverUrl: String,
        username: String,
        password: String,
        callback: (Boolean) -> Unit
    ) {
        Thread {
            try {
                apiClient = EndurainApiClient(serverUrl, tokenManager)
                val response = apiClient.login(username, password)

                when {
                    response == null -> {
                        LOG.error("Login failed: null response")
                        callback(false)
                    }
                    response.mfa_required == true -> {
                        LOG.info("MFA required for user: ${response.username}")
                        pendingMfaUsername = response.username ?: username
                        step = Step.MFA_VERIFY
                        callback(true) // Return true to indicate MFA step is needed
                    }
                    response.access_token != null -> {
                        LOG.info("Login successful")
                        tokenManager.saveTokens(
                            response.access_token,
                            response.refresh_token!!,
                            response.expires_in!!
                        )
                        callback(true)
                    }
                    else -> {
                        LOG.error("Login failed: ${response.detail}")
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Login error", e)
                callback(false)
            }
        }.start()
    }

    /**
     * Verify MFA code
     */
    fun verifyMfa(mfaCode: String, callback: (Boolean) -> Unit) {
        Thread {
            try {
                val username = pendingMfaUsername
                if (username == null) {
                    LOG.error("No pending MFA username")
                    callback(false)
                    return@Thread
                }

                val response = apiClient.verifyMfa(username, mfaCode)

                if (response?.access_token != null) {
                    LOG.info("MFA verification successful")
                    tokenManager.saveTokens(
                        response.access_token,
                        response.refresh_token!!,
                        response.expires_in!!
                    )
                    pendingMfaUsername = null
                    callback(true)
                } else {
                    LOG.error("MFA verification failed")
                    callback(false)
                }
            } catch (e: Exception) {
                LOG.error("MFA verification error", e)
                callback(false)
            }
        }.start()
    }

    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(): Boolean {
        return tokenManager.getAccessToken() != null && !tokenManager.isTokenExpired()
    }

    /**
     * Get token expiry date
     */
    fun getTokenExpiresAt(): Date {
        return DateTimeUtils.parseTimeStamp(tokenManager.getAccessTokenExpiresAt())
    }

    /**
     * Logout and clear tokens
     */
    fun logout(callback: (Boolean) -> Unit) {
        Thread {
            try {
                if (::apiClient.isInitialized) {
                    apiClient.logout()
                } else {
                    tokenManager.clearTokens()
                }
                callback(true)
            } catch (e: Exception) {
                LOG.error("Logout error", e)
                tokenManager.clearTokens() // Clear tokens anyway
                callback(true)
            }
        }.start()
    }
}