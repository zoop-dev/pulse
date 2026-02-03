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

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EndurainTokenManager(context: Context) {
    private val LOG: Logger = LoggerFactory.getLogger(EndurainTokenManager::class.java)
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "endurain_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accessToken: String, refreshToken: String, accessTokenExpiresAt: Int) {
        sharedPreferences.edit {
            putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putInt("access_token_expires_at", accessTokenExpiresAt)
                .putInt("refresh_token_expires_at",
                    ((System.currentTimeMillis() / 1000) + (7 * 24 * 60 * 60)).toInt()
                )  // FIXME: 7 days is the Endurain default for refresh token expiry
                   // https://github.com/endurain-project/endurain/issues/514
        }
    }

    fun clearTokens() {
        sharedPreferences.edit { clear() }
    }

    fun getAccessToken(): String? = sharedPreferences.getString("access_token", null)
    fun getAccessTokenExpiresAt(): Int = sharedPreferences.getInt("access_token_expires_at", 0)
    fun getRefreshToken(): String? = sharedPreferences.getString("refresh_token", null)
    fun getRefreshTokenExpiresAt(): Int = sharedPreferences.getInt("refresh_token_expires_at", 0)

    fun isLoggedIn(): Boolean {
        return getRefreshToken() != null && !isRefreshTokenExpired()
    }
    fun isAccessTokenExpired(): Boolean {
        return (System.currentTimeMillis() / 1000) >= getAccessTokenExpiresAt()
    }
    fun isRefreshTokenExpired(): Boolean {
        return (System.currentTimeMillis() / 1000) >= getRefreshTokenExpiresAt()
    }

    fun performTokenRefresh(
        serverUrl: String,
        callback: (Boolean) -> Unit
    ) {
        Thread {
            try {
                val apiClient = EndurainApiClient(serverUrl, this)
                val response = apiClient.refreshToken()

                when {
                    response == null -> {
                        LOG.error("Token refresh failed: null response")
                        callback(false)
                    }
                    response.access_token != null -> {
                        LOG.info("Token refresh successful")
                        saveTokens(
                            response.access_token,
                            response.refresh_token!!,
                            response.expires_in!!
                        )
                        callback(true)
                    }
                    else -> {
                        LOG.error("Token refresh failed: ${response.detail}")
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Token refresh error", e)
                callback(false)
            }
        }.start()
    }
}