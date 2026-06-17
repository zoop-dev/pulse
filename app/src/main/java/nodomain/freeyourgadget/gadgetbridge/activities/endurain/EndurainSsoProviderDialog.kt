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

import android.app.AlertDialog
import android.content.Context
import nodomain.freeyourgadget.gadgetbridge.R

class EndurainSsoProviderDialog(
    private val context: Context,
    private val providers: List<EndurainIdentityProvider>,
    private val onProviderSelected: (EndurainIdentityProvider) -> Unit
) {

    fun show() {
        val providerNames = providers.map { it.name }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.endurain_select_identity_provider))
            .setItems(providerNames) { dialog, which ->
                onProviderSelected(providers[which])
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }
}