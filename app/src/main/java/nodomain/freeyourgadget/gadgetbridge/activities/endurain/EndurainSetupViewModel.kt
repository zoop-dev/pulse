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
import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.util.InternetUtils
import org.slf4j.LoggerFactory

class EndurainSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val LOG = LoggerFactory.getLogger(EndurainSetupViewModel::class.java)
    private lateinit var apiClient: EndurainApiClient
    private val pkceHelper = PkceHelper()

    enum class Step {
        SERVER,
        LOGIN_TYPE,
        LOCAL_LOGIN,
        MFA_VERIFY,
        SSO_PROVIDERS
    }

    val tokenManager = EndurainTokenManager(application)
    var step = Step.SERVER
    var server = ""
    var localLoginEnabled = false
    var ssoEnabled = false
    var pendingMfaUsername: String? = null
    var availableProviders: List<IdentityProvider> = emptyList()
    var serverVersion: String? = null

    /**
     * Fetch server version
     */
    fun fetchServerVersion(
        serverUrl: String,
        callback: (Boolean) -> Unit
    ) {
        Thread {
            try {
                apiClient = EndurainApiClient(serverUrl, tokenManager)
                serverVersion = apiClient.fetchVersion()
                callback(true)
            } catch (e: Exception) {
                LOG.error("Fetching server version error", e)
                callback(false)
            }
        }.start()
    }

    /**
     * Fetch server capabilities to determine available login methods
     */
    fun fetchServerCapabilities(serverUrl: String, callback: (Boolean) -> Unit) {
        Thread {
            try {
                // Fetch server settings from the public endpoint
                val settingsUri = "$serverUrl/api/v1/public/server_settings".toUri()
                val headers = mutableMapOf("X-Client-Type" to "mobile")
                val settingsResponse = InternetUtils.doJsonRequest(
                    uri = settingsUri,
                    requestHeaders = headers
                )

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
     * Fetch available SSO providers
     */
    fun fetchSsoProviders(callback: (Boolean) -> Unit) {
        Thread {
            try {
                apiClient = EndurainApiClient(server, tokenManager)
                val providers = apiClient.getIdentityProviders()

                if (providers != null && providers.isNotEmpty()) {
                    availableProviders = providers
                    callback(true)
                } else {
                    LOG.error("No SSO providers available")
                    callback(false)
                }
            } catch (e: Exception) {
                LOG.error("Error fetching SSO providers", e)
                callback(false)
            }
        }.start()
    }

    /**
     * Generate SSO URL with PKCE for Custom Tabs
     * Returns the URL and also stores the verifier for later
     */
    fun generateSsoUrl(idpSlug: String): String {
        val codeVerifier = pkceHelper.generateCodeVerifier()
        val codeChallenge = pkceHelper.generateCodeChallenge(codeVerifier)

        LOG.debug("Initiating PKCE with verifier=$codeVerifier and challenge=$codeChallenge")

        // Store verifier in shared preferences for the callback activity
        getApplication<GBApplication>().getSharedPreferences("endurain_oauth_temp", Context.MODE_PRIVATE)
            .edit {
                putString("code_verifier", codeVerifier)
            }

        // Build OAuth URL with custom redirect URI for deep linking
        return "$server/api/v1/public/idp/login/$idpSlug".toUri()
            .buildUpon()
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("redirect", BuildConfig.APPLICATION_ID + "://endurain/oauth/callback")
            .build()
            .toString()
    }

    /**
     * Exchange OAuth session for tokens
     */
    fun exchangeSsoSession(sessionId: String, callback: (Boolean) -> Unit) {
        Thread {
            try {
                // Retrieve the stored code verifier
                val prefs = getApplication<GBApplication>()
                    .getSharedPreferences("endurain_oauth_temp", Context.MODE_PRIVATE)

                val verifier = prefs.getString("code_verifier", null)
                if (verifier == null) {
                    LOG.error("No pending code verifier")
                    callback(false)
                    return@Thread
                }

                // Clear the verifier
                prefs.edit { remove("code_verifier") }

                if (!::apiClient.isInitialized) {
                    apiClient = EndurainApiClient(server, tokenManager)
                }

                val response = apiClient.exchangeOAuthSession(sessionId, verifier)

                if (response?.access_token != null) {
                    LOG.info("SSO login successful")
                    tokenManager.saveTokens(
                        response.access_token,
                        response.refresh_token!!,
                        response.expires_in!!,
                        response.refresh_token_expires_in!!
                    )
                    callback(true)
                } else {
                    LOG.error("SSO login failed")
                    callback(false)
                }
            } catch (e: Exception) {
                LOG.error("SSO session exchange error", e)
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
                            response.expires_in!!,
                            response.refresh_token_expires_in!!
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

                LOG.debug("MFA verification response: {}", response)

                if (response?.access_token != null) {
                    LOG.info("MFA verification successful")
                    tokenManager.saveTokens(
                        response.access_token,
                        response.refresh_token!!,
                        response.expires_in!!,
                        response.refresh_token_expires_in!!
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