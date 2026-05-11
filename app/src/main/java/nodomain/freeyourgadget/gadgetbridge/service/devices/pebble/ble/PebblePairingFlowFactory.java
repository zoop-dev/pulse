/*  Copyright (C) 2024 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleHardware;

/**
 * Factory for creating the appropriate Pebble pairing flow based on device hardware and characteristics.
 * <p>
 * Device detection strategy:
 * 1. Primary: Use hardware platform (via PebbleHardware.isBleOnlyByModel) to determine expected flow
 * 2. Validation: Verify device has expected GATT characteristics
 * 3. Defensive: If mismatch, adapt based on actual characteristics (e.g., firmware upgrade scenario)
 * <p>
 * Expected characteristics by hardware:
 * - BLE-only Pebbles (Pebble 2, Time 2, 2 Duo): CONNECTIVITY_CHARACTERISTIC, no CONNECTION_PARAMETERS → ModernPebblePairingFlow
 * - Dual-mode Pebbles (Pebble Time, Time Round): CONNECTION_PARAMETERS_CHARACTERISTIC → LegacyPebblePairingFlow
 * - Classic Pebbles: No BLE pairing needed
 */
class PebblePairingFlowFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PebblePairingFlowFactory.class);

    /**
     * Create the appropriate pairing flow for the given device.
     *
     * @param context        Android context for bonding operations
     * @param deviceModel    Device model string from GBDevice.getModel() (may be null)
     * @param pairingService The Pebble pairing service (0000fed9)
     * @param clientOnly     Whether to use clientOnly mode (affects pairing trigger value)
     * @param callback       Callback to invoke when pairing is complete
     * @return The appropriate pairing flow implementation
     */
    static PebblePairingFlow createPairingFlow(
            Context context,
            @Nullable String deviceModel,
            BluetoothGattService pairingService,
            boolean clientOnly,
            PairingCallback callback) {

        // Detect GATT characteristics
        BluetoothGattCharacteristic connectivityChar =
                pairingService.getCharacteristic(PebbleGATTConstants.CONNECTIVITY_CHARACTERISTIC);
        BluetoothGattCharacteristic connectionParamChar =
                pairingService.getCharacteristic(PebbleGATTConstants.CONNECTION_PARAMETERS_CHARACTERISTIC);

        boolean hasConnectivityChar = (connectivityChar != null);
        boolean hasConnectionParamChar = (connectionParamChar != null);

        // Hardware-based expectation (if model is known)
        Boolean hardwareExpectsModern = null;  // null = unknown
        if (deviceModel != null && !deviceModel.isEmpty()) {
            hardwareExpectsModern = PebbleHardware.isBleOnlyByModel(deviceModel);
            LOG.info("Hardware detection: model='{}', BLE-only={}", deviceModel, hardwareExpectsModern);
        }

        if (hardwareExpectsModern != null) {
            if (hardwareExpectsModern && !hasConnectivityChar) {
                LOG.warn("Mismatch: Hardware expects modern flow but CONNECTIVITY_CHARACTERISTIC missing (model={})", deviceModel);
            }

            if (!hardwareExpectsModern && hasConnectivityChar && !hasConnectionParamChar) {
                LOG.warn("Mismatch: Hardware expects legacy flow but has modern characteristics (model={}) - possible firmware upgrade", deviceModel);
            }
        }

        boolean useModernFlow;
        if (hasConnectivityChar) {
            // Modern characteristic present - use V2 flow
            useModernFlow = true;
            LOG.info("Using PebblePairingFlowV2 (CONNECTIVITY_CHARACTERISTIC present)");
        } else if (hasConnectionParamChar) {
            // Legacy characteristic present - use V1 flow
            useModernFlow = false;
            LOG.info("Using PebblePairingFlowV1 (CONNECTION_PARAMETERS_CHARACTERISTIC present)");
        } else {
            // No characteristics - fall back to hardware expectation or default to V1
            useModernFlow = (hardwareExpectsModern != null && hardwareExpectsModern);
            LOG.warn("No pairing characteristics found - using {} flow based on {}",
                    useModernFlow ? "V2" : "V1",
                    hardwareExpectsModern != null ? "hardware expectation" : "default");
        }

        if (useModernFlow) {
            return new PebblePairingFlowV2(context, clientOnly, callback);
        } else {
            return new PebblePairingFlowV1(clientOnly, callback);
        }
    }

    private PebblePairingFlowFactory() {
        // Utility class
    }
}

