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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.LoggerFactory

class WandererSetupBottomSheet : BottomSheetDialogFragment() {
    private val LOG = LoggerFactory.getLogger(WandererSetupBottomSheet::class.java)
    private val prefs get() = GBApplication.getPrefs().preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(
        R.layout.wanderer_bottomsheet_setup_wizard,
        container,
        false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val serverNameInput = view.findViewById<EditText>(R.id.wanderer_server_name)
        val apiTokenInput = view.findViewById<EditText>(R.id.wanderer_api_token)
        val saveButton = view.findViewById<Button>(R.id.save_button)

        serverNameInput.setText(prefs.getString("wanderer_server", ""))

        saveButton.setOnClickListener {
            val serverName = serverNameInput.text.toString().trim()
            val apiToken = apiTokenInput.text.toString().trim()

            if (serverName.isNotEmpty() && apiToken.startsWith("wanderer_key_")) {
                LOG.info("Saving Wanderer server ({}) and API token", serverName)
                prefs.edit { putString("wanderer_server", serverName) }
                WandererTokenManager(requireContext()).saveToken(apiToken)
                parentFragmentManager.setFragmentResult(
                    "wanderer_login_result",
                    Bundle().apply { putBoolean("success", true) }
                )
                dismiss()
            } else if (serverName.isNotEmpty() && apiToken.isNotEmpty()) {
                GB.toast(getString(R.string.wanderer_setup_api_token_error), Toast.LENGTH_SHORT, GB.WARN)
            } else {
                GB.toast(getString(R.string.wanderer_setup_missing_information), Toast.LENGTH_SHORT, GB.WARN)
            }
        }
    }
}