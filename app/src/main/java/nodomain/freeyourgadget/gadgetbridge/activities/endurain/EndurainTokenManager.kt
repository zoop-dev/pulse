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

class EndurainTokenManager(context: Context) {
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
                .putInt("expires_at", accessTokenExpiresAt)
        }
    }

    fun getAccessToken(): String? = sharedPreferences.getString("access_token", null)
    fun getRefreshToken(): String? = sharedPreferences.getString("refresh_token", null)
    fun getAccessTokenExpiresAt(): Int = sharedPreferences.getInt("expires_at", 0)
    fun isTokenExpired(): Boolean {
        return (System.currentTimeMillis() / 1000) >= getAccessTokenExpiresAt()
    }

    fun clearTokens() {
        sharedPreferences.edit { clear() }
    }
}