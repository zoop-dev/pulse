/*  Copyright (C) 2015-2025 Andreas Shimokawa, Carsten Pfeiffer, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.bluetooth.BluetoothDevice;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * Unified source of truth for Pebble hardware definitions.
 * Contains platform types, hardware revisions, codenames, and BLE-only status.
 */
public class PebbleHardware {
    private static final Logger LOG = LoggerFactory.getLogger(PebbleHardware.class);

    // ======================= Vendor IDs =======================
    public static final int PEBBLE_VENDOR_ID = 0x0154;  // 340 - Standard Pebble devices
    public static final int CORE_VENDOR_ID = 0x0EEA;    // 3818 - Core devices (Pebble 2 Duo)

    // ======================= Platform Enum =======================
    /**
     * Pebble platform types, corresponding to the SDK platform names.
     * Each platform defines its capabilities: BLE-only, health tracking, heart rate monitor.
     * Codename prefixes are used for model string matching (e.g., "snowy_dvt" matches "snowy" → BASALT).
     */
    public enum Platform {
        //     platformName, displayName,       bleOnly, hasHealth, hasHRM, codenamePrefixes
        APLITE("aplite", "Pebble Classic",      false,   false,     false),
        BASALT("basalt", "Pebble Time",         false,   true,      false,  "snowy"),
        CHALK("chalk", "Pebble Time Round",     false,   true,      false,  "spalding"),
        DIORITE("diorite", "Pebble 2",          true,    true,      true,   "silk"),
        EMERY("emery", "Pebble Time 2",         true,    true,      true,   "robert", "obelix"),
        FLINT("flint", "Pebble 2 Duo",          true,    true,      false,  "asterix"),
        GABBRO("gabbro", "Pebble Time Round 2", true,    true,      true,   "getafix");

        private final String platformName;
        private final String displayName;
        private final boolean bleOnly;
        private final boolean hasHealth;
        private final boolean hasHRM;
        private final String[] codenamePrefixes;

        Platform(String platformName, String displayName, boolean bleOnly, boolean hasHealth, boolean hasHRM, String... codenamePrefixes) {
            this.platformName = platformName;
            this.displayName = displayName;
            this.bleOnly = bleOnly;
            this.hasHealth = hasHealth;
            this.hasHRM = hasHRM;
            this.codenamePrefixes = codenamePrefixes;
        }

        public String getPlatformName() {
            return platformName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isBleOnly() {
            return bleOnly;
        }

        public boolean hasHealth() {
            return hasHealth;
        }

        public boolean hasHRM() {
            return hasHRM;
        }

        public String[] getCodenamePrefixes() {
            return codenamePrefixes;
        }

        /**
         * Find platform by codename prefix.
         * @param model Model string to match
         * @return Platform or null if no prefix matches
         */
        public static Platform findByCodenamePrefix(String model) {
            if (model == null || model.isEmpty()) {
                return null;
            }
            for (Platform platform : values()) {
                for (String prefix : platform.codenamePrefixes) {
                    if (model.startsWith(prefix)) {
                        return platform;
                    }
                }
            }
            return null;
        }
    }

    // ======================= Hardware Revision =======================
    /**
     * Represents a specific Pebble hardware revision.
     */
    public static class HardwareRevision {
        private final int hardwareId;
        private final String codename;
        private final Platform platform;

        public HardwareRevision(int hardwareId, String codename, Platform platform) {
            this.hardwareId = hardwareId;
            this.codename = codename;
            this.platform = platform;
        }

        public int getHardwareId() {
            return hardwareId;
        }

        public String getCodename() {
            return codename;
        }

        public Platform getPlatform() {
            return platform;
        }

        public boolean isBleOnly() {
            return platform.isBleOnly();
        }

        public boolean hasHealth() {
            return platform.hasHealth();
        }

        public boolean hasHRM() {
            return platform.hasHRM();
        }

        public String getPlatformName() {
            return platform.getPlatformName();
        }

        public String getDisplayName() {
            return platform.getDisplayName();
        }
    }

    // ======================= Hardware Registry =======================
    // Maps hardware ID (from advertisement data) to HardwareRevision
    private static final SparseArray<HardwareRevision> BY_HARDWARE_ID = new SparseArray<>();
    // Maps codename to HardwareRevision
    private static final Map<String, HardwareRevision> BY_CODENAME = new HashMap<>();
    // Array indexed by hardware ID for PebbleProtocol compatibility
    private static final String[] HW_REVISIONS_BY_ID;

    static {
        // Register all hardware revisions
        // Format: register(hardwareId, codename, platform)

        // Unknown / Default
        register(0, "unknown", Platform.BASALT);

        // Pebble Classic (APLITE)
        register(1, "ev1", Platform.APLITE);
        register(2, "ev2", Platform.APLITE);
        register(3, "ev2_3", Platform.APLITE);
        register(4, "ev2_4", Platform.APLITE);
        register(5, "v1_5", Platform.APLITE);
        register(6, "v2_0", Platform.APLITE);
        register(254, "bb2", Platform.APLITE);
        register(255, "bigboard", Platform.APLITE);

        // Pebble Time (BASALT)
        register(7, "snowy_evt2", Platform.BASALT);
        register(8, "snowy_dvt", Platform.BASALT);
        register(10, "snowy_s3", Platform.BASALT);
        register(252, "snowy_bb2", Platform.BASALT);
        register(253, "snowy_bb", Platform.BASALT);

        // Pebble Time Round (CHALK)
        register(9, "spalding_evt", Platform.CHALK);
        register(11, "spalding", Platform.CHALK);
        register(251, "spalding_bb2", Platform.CHALK);

        // Pebble 2 (DIORITE) - BLE only
        register(12, "silk_evt", Platform.DIORITE);
        register(14, "silk", Platform.DIORITE);
        register(248, "silk_bb2", Platform.DIORITE);
        register(250, "silk_bb", Platform.DIORITE);

        // Pebble Time 2 (EMERY) - BLE only
        register(13, "robert_evt", Platform.EMERY);
        register(16, "obelix_evt", Platform.EMERY);
        register(17, "obelix_dvt", Platform.EMERY);
        register(18, "obelix_pvt", Platform.EMERY);
        register(243, "obelix_bb2", Platform.EMERY);
        register(244, "obelix_bb", Platform.EMERY);
        register(247, "robert_bb2", Platform.EMERY);
        register(249, "robert_bb", Platform.EMERY);

        // Pebble 2 Duo / Core (FLINT) - BLE only
        register(15, "asterix", Platform.FLINT);

        // Pebble Time Round 2 (GABBRO) - unreleased, BLE only
        register(19, "getafix_evt", Platform.GABBRO);
        register(20, "getafix_dvt", Platform.GABBRO);

        // Build the array for PebbleProtocol compatibility
        int maxId = 0;
        for (int i = 0; i < BY_HARDWARE_ID.size(); i++) {
            int id = BY_HARDWARE_ID.keyAt(i);
            if (id > maxId) maxId = id;
        }
        HW_REVISIONS_BY_ID = new String[maxId + 1];
        for (int i = 0; i < BY_HARDWARE_ID.size(); i++) {
            int id = BY_HARDWARE_ID.keyAt(i);
            HW_REVISIONS_BY_ID[id] = BY_HARDWARE_ID.valueAt(i).getCodename();
        }
    }

    private static void register(int hardwareId, String codename, Platform platform) {
        HardwareRevision hw = new HardwareRevision(hardwareId, codename, platform);
        BY_HARDWARE_ID.put(hardwareId, hw);
        BY_CODENAME.put(codename, hw);
    }

    // ======================= Lookup Methods =======================

    /**
     * Get hardware revision by hardware ID (from advertisement data).
     *
     * @param hardwareId The hardware ID from BLE advertisement
     * @return HardwareRevision or null if unknown
     */
    @Nullable
    public static HardwareRevision getByHardwareId(int hardwareId) {
        return BY_HARDWARE_ID.get(hardwareId);
    }

    /**
     * Get hardware revision by exact codename.
     *
     * @param codename The exact codename (e.g., "silk", "snowy_dvt")
     * @return HardwareRevision or null if unknown
     */
    @Nullable
    public static HardwareRevision getByCodename(String codename) {
        return BY_CODENAME.get(codename);
    }

    /**
     * Get hardware revision by codename prefix (for model string matching).
     * The model string may have additional suffixes (e.g., "silk_evt" matches "silk").
     *
     * @param model The model/hardware revision string from device
     * @return HardwareRevision or null if unknown
     */
    @Nullable
    public static HardwareRevision getByModelString(String model) {
        if (model == null || model.isEmpty()) {
            return null;
        }

        // First try exact match in registry
        HardwareRevision exact = BY_CODENAME.get(model);
        if (exact != null) {
            return exact;
        }

        // Fall back to platform codename prefix matching
        Platform platform = Platform.findByCodenamePrefix(model);
        if (platform != null) {
            // Return a synthetic HardwareRevision for the platform
            return new HardwareRevision(-1, model, platform);
        }

        return null;
    }

    /**
     * Get codename string by hardware ID for PebbleProtocol compatibility.
     *
     * @param hardwareId The hardware ID
     * @return Codename string or null if out of range
     */
    @Nullable
    public static String getCodenameByHardwareId(int hardwareId) {
        if (hardwareId >= 0 && hardwareId < HW_REVISIONS_BY_ID.length) {
            return HW_REVISIONS_BY_ID[hardwareId];
        }
        return null;
    }

    // ======================= BLE-Only Detection =======================

    /**
     * Check if hardware ID indicates a BLE-only device.
     *
     * @param hardwareId Hardware ID from advertisement data
     * @return true if this is a BLE-only device
     */
    public static boolean isBleOnlyByHardwareId(int hardwareId) {
        HardwareRevision hw = BY_HARDWARE_ID.get(hardwareId);
        return hw != null && hw.isBleOnly();
    }

    /**
     * Check if model/codename string indicates a BLE-only device.
     *
     * @param model Model string from GBDevice.getModel()
     * @return true if this is a BLE-only device
     */
    public static boolean isBleOnlyByModel(@Nullable String model) {
        if (model == null || model.isEmpty()) {
            return false;
        }
        HardwareRevision hw = getByModelString(model);
        return hw != null && hw.isBleOnly();
    }

    /**
     * Check if device is a BLE-only Pebble using GBDevice model info.
     * Falls back to name pattern matching if model is not available.
     *
     * @param gbDevice The GBDevice with model info
     * @param btDevice The BluetoothDevice (used for type and name check)
     * @return true if this is a BLE-only Pebble device
     */
    public static boolean isBleOnly(@Nullable GBDevice gbDevice, @Nullable BluetoothDevice btDevice) {
        if (btDevice == null) {
            return false;
        }

        int type = btDevice.getType();
        if (type != BluetoothDevice.DEVICE_TYPE_LE && type != BluetoothDevice.DEVICE_TYPE_DUAL) {
            return false;
        }

        // First check hardware revision (model) if available
        if (gbDevice != null) {
            String model = gbDevice.getModel();
            if (model != null && !model.isEmpty()) {
                if (isBleOnlyByModel(model)) {
                    LOG.debug("isBleOnly: {} identified via hardware revision '{}'",
                            gbDevice.getName(), model);
                    return true;
                }
            }
        }

        // Fall back to name pattern matching
        return isBleOnlyByName(btDevice.getName());
    }

    /**
     * Check if device is a BLE-only Pebble using BluetoothDevice name.
     *
     * @param btDevice The BluetoothDevice to check
     * @return true if this is a BLE-only Pebble device
     */
    public static boolean isBleOnly(BluetoothDevice btDevice) {
        if (btDevice == null) {
            return false;
        }

        int type = btDevice.getType();
        if (type != BluetoothDevice.DEVICE_TYPE_LE && type != BluetoothDevice.DEVICE_TYPE_DUAL) {
            return false;
        }

        return isBleOnlyByName(btDevice.getName());
    }

    /**
     * Check if device is a BLE-only Pebble using manufacturer data.
     *
     * @param manufacturerData SparseArray from BLE scan
     * @return true if this is a BLE-only Pebble device
     */
    public static boolean isBleOnly(SparseArray<byte[]> manufacturerData) {
        if (manufacturerData == null) {
            return false;
        }

        byte[] data = manufacturerData.get(PEBBLE_VENDOR_ID);
        if (data == null) {
            data = manufacturerData.get(CORE_VENDOR_ID);
        }
        if (data == null || data.length < 14) {
            return false;
        }

        int hardwareId = data[13] & 0xFF;
        return isBleOnlyByHardwareId(hardwareId);
    }

    /**
     * Check if device name matches BLE-only Pebble pattern.
     * Pebble 2, Time 2, and 2 Duo advertise as "Pebble XXXX" (4 hex digits).
     *
     * @param name Device name to check
     * @return true if name matches BLE-only Pebble pattern
     */
    public static boolean isBleOnlyByName(@Nullable String name) {
        if (name == null) {
            return false;
        }

        // Must start with "Pebble "
        if (!name.startsWith("Pebble ")) {
            return false;
        }

        // Exclude old-style LE companions
        if (name.startsWith("Pebble-LE ") || name.startsWith("Pebble Time LE ")) {
            return false;
        }

        // Check for exact "Pebble XXXX" pattern (11 chars, 4 hex digits)
        if (name.length() == 11) {
            String suffix = name.substring(7);
            if (suffix.matches("[0-9A-Fa-f]{4}")) {
                LOG.debug("isBleOnlyByName: {} matches Pebble 2/Time 2/2 Duo pattern", name);
                return true;
            }
        }

        return false;
    }

    /**
     * Check if device is an old-style LE Pebble companion (not BLE-only).
     *
     * @param btDevice The BluetoothDevice to check
     * @return true if this is an LE companion device
     */
    public static boolean isLePebbleCompanion(BluetoothDevice btDevice) {
        if (btDevice == null) {
            return false;
        }

        int type = btDevice.getType();
        if (type != BluetoothDevice.DEVICE_TYPE_LE && type != BluetoothDevice.DEVICE_TYPE_DUAL) {
            return false;
        }

        String name = btDevice.getName();
        return name != null && (name.startsWith("Pebble-LE ") || name.startsWith("Pebble Time LE "));
    }

    // ======================= Platform Utilities =======================

    /**
     * Get platform name for a model/codename string.
     *
     * @param model The model string
     * @return Platform name (aplite, basalt, etc.) or "aplite" as default
     */
    public static String getPlatformName(@Nullable String model) {
        if (model == null || model.isEmpty()) {
            return Platform.APLITE.getPlatformName();
        }
        HardwareRevision hw = getByModelString(model);
        return hw != null ? hw.getPlatformName() : Platform.APLITE.getPlatformName();
    }

    /**
     * Get display model name for a hardware revision string.
     *
     * @param hwRev Hardware revision string
     * @return Display model name (e.g., "pebble_time_black")
     */
    public static String getModelDisplayName(@Nullable String hwRev) {
        if (hwRev == null || hwRev.isEmpty()) {
            return "pebble_black";
        }

        HardwareRevision hw = getByModelString(hwRev);
        if (hw == null) {
            return "pebble_black";
        }

        return switch (hw.getPlatform()) {
            case BASALT -> "pebble_time_black";
            case CHALK -> "pebble_time_round_black_20mm";
            case DIORITE -> "pebble2_black";
            case EMERY -> hwRev.startsWith("obelix") ? "coredevices_pt2_black_grey" : "pebble_time2_black";
            case FLINT -> "coredevices_p2d_black";
            case GABBRO -> "coredevices_ptr2_black";
            default -> "pebble_black";
        };
    }

    /**
     * Check if hardware revision supports heart rate monitor.
     *
     * @param hwRev Hardware revision string
     * @return true if device has HRM
     */
    public static boolean hasHRM(@Nullable String hwRev) {
        HardwareRevision hw = getByModelString(hwRev);
        if (hw == null) {
            return false;
        }
        return hw.getPlatform().hasHRM();
    }

    /**
     * Check if hardware revision supports health tracking.
     *
     * @param hwRev Hardware revision string
     * @return true if device supports health
     */
    public static boolean hasHealth(@Nullable String hwRev) {
        HardwareRevision hw = getByModelString(hwRev);
        if (hw == null) {
            return true; // Assume yes for unknown
        }
        return hw.getPlatform().hasHealth();
    }

    /**
     * Check if manufacturer data indicates any Pebble device.
     *
     * @param manufacturerData SparseArray from BLE scan
     * @return true if vendor ID matches Pebble or Core
     */
    public static boolean isPebbleFromManufacturerData(SparseArray<byte[]> manufacturerData) {
        if (manufacturerData == null) {
            return false;
        }
        return manufacturerData.get(PEBBLE_VENDOR_ID) != null ||
               manufacturerData.get(CORE_VENDOR_ID) != null;
    }

    /**
     * Parse hardware platform from manufacturer data.
     *
     * @param manufacturerData SparseArray from BLE scan
     * @return HardwareRevision or null if not a Pebble or data is insufficient
     */
    @Nullable
    public static HardwareRevision parseManufacturerData(SparseArray<byte[]> manufacturerData) {
        if (manufacturerData == null) {
            return null;
        }

        byte[] data = manufacturerData.get(PEBBLE_VENDOR_ID);
        if (data == null) {
            data = manufacturerData.get(CORE_VENDOR_ID);
        }
        if (data == null || data.length < 14) {
            return null;
        }

        int hardwareId = data[13] & 0xFF;
        return getByHardwareId(hardwareId);
    }
}
