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
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.GeneralSecurityException

// FIXME MasterKey/EncryptedSharedPreferences is deprecated - we should move to Tink
@Suppress("DEPRECATION")
class WandererTokenManager(context: Context) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(WandererTokenManager::class.java)
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val tokenPreferences = createPreferences(context)

    private fun createPreferences(context: Context): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                "wanderer_tokens",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            LOG.warn("Unable to decrypt wanderer token preferences, resetting them instead\n", e)
            context.getSharedPreferences("wanderer_tokens", Context.MODE_PRIVATE)
                .edit(commit = true) { clear() }
            EncryptedSharedPreferences.create(
                context,
                "wanderer_tokens",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun saveToken(apiToken: String) {
        tokenPreferences.edit {
            putString("api_token", apiToken)
        }
    }

    fun clearTokens() {
        tokenPreferences.edit { clear() }
    }

    fun getAPIToken(): String? = tokenPreferences.getString("api_token", null)

    fun isLoggedIn(): Boolean {
        return getAPIToken() != null && getAPIToken()?.startsWith("wanderer_key_") == true
    }
}