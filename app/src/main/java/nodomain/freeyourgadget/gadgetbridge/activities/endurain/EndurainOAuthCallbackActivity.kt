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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import org.slf4j.LoggerFactory

/**
 * Handles OAuth callback deep links from the browser
 */
class EndurainOAuthCallbackActivity : AbstractGBActivity() {

    private val LOG = LoggerFactory.getLogger(EndurainOAuthCallbackActivity::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bring us back to front to dismiss the custom tab
        val dismissIntent = Intent(applicationContext, OnlineFitnessTrackersPreferencesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(dismissIntent)

        // Handle OAuth callback intent
        val uri = intent.data
        if (uri != null) {
            LOG.info("OAuth callback received: $uri")
            handleOAuthCallback(uri)
        } else {
            LOG.error("No URI in OAuth callback")
            Toast.makeText(this,
                getString(R.string.endurain_invalid_oauth_callback), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleOAuthCallback(uri: Uri) {
        // Expected format: nodomain.freeyourgadget.gadgetbridge://endurain/oauth/callback?session_id={uuid}

        val sessionId = uri.getQueryParameter("session_id")

        if (sessionId != null) {
            LOG.info("SSO successful, session_id: $sessionId")

            // Broadcast the session ID so the setup bottom sheet can handle it
            val resultIntent = Intent("nodomain.freeyourgadget.gadgetbridge.ENDURAIN_SSO_CALLBACK")
            resultIntent.putExtra("session_id", sessionId)
            resultIntent.putExtra("success", true)
            sendBroadcast(resultIntent)

            Toast.makeText(this,
                getString(R.string.endurain_oauth_completing_login), Toast.LENGTH_SHORT).show()
        } else {
            LOG.error("SSO failed or missing session_id")
            Toast.makeText(this, getString(R.string.endurain_sso_login_failed), Toast.LENGTH_SHORT).show()

            // Broadcast failure
            val resultIntent = Intent("nodomain.freeyourgadget.gadgetbridge.ENDURAIN_SSO_CALLBACK")
            resultIntent.putExtra("success", false)
            sendBroadcast(resultIntent)
        }

        finish()
    }
}