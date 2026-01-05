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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.util.InternetUtils
import org.json.JSONObject

class EndurainSetupViewModel : ViewModel() {

    enum class Step { SERVER, LOGIN_TYPE, LOCAL_LOGIN, SSO_LOGIN, DONE }

    var step: Step = Step.SERVER
    var server: String = ""
    var localLoginEnabled: Boolean = false
    var ssoEnabled: Boolean = false

    fun fetchServerCapabilities(
        server: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val uri = "$server/api/v1/public/server_settings"
                val json: JSONObject? =
                    InternetUtils.doJsonRequest(uri.toUri())
                if (json == null) return@withContext false
                localLoginEnabled = json.optBoolean("local_login_enabled")
                ssoEnabled = json.optBoolean("sso_enabled")
                true
            }
            onResult(ok)
        }
    }

    fun performLocalLogin(
        server: String,
        user: String,
        pass: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                // TODO real API call
                user.isNotBlank() && pass.isNotBlank()
            }
            onResult(success)
        }
    }
}
