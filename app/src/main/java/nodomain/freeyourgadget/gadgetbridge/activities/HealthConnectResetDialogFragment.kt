/*  Copyright (C) 2025 Gideon Zenz

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
package nodomain.freeyourgadget.gadgetbridge.activities

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectPermissionManager
import org.slf4j.LoggerFactory

class HealthConnectResetDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "HealthConnectResetDialog"
        private val LOG = LoggerFactory.getLogger(HealthConnectResetDialogFragment::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.healthconnect_settings)
            .setMessage(R.string.health_connect_prompt_full_dao_reset)
            .setPositiveButton(R.string.health_connect_reset_sync_history) { _, _ ->
                LOG.info("User chose to reset Health Connect sync states.")
                HealthConnectPermissionManager.clearAllSyncStates(requireContext().applicationContext)
                // The flag is now cleared by clearAllSyncStates via setHealthConnectPermissionResetNeeded
            }
            .setNegativeButton(R.string.health_connect_keep_sync_history) { _, _ ->
                LOG.info("User cancelled Health Connect sync state reset.")
                clearPromptFlag() // Call to clear the flag
            }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        LOG.info("Health Connect sync state reset dialog was cancelled/dismissed.")
        clearPromptFlag() // Call to clear the flag
    }

    private fun clearPromptFlag() {
        // requireContext().applicationContext is used because the operation involves SharedPreferences
        // and should ideally use the application context if the original context's lifecycle is a concern,
        HealthConnectPermissionManager.setHealthConnectPermissionResetNeeded(requireContext().applicationContext, false)
    }
}
