/*  Copyright (C) 2015-2024 115ek, akasaka / Genjitsu Labs, Andreas Böhler,
    Andreas Shimokawa, Andrew Watkins, angelpup, Arjan Schrijver, Carsten Pfeiffer,
    Cre3per, DanialHanif, Daniel Dakhno, Daniele Gobbetti, Daniel Thompson, Da Pa,
    Dmytro Bielik, Frank Ertl, GeekosaurusR3x, Gordon Williams, Jean-François
    Greffier, jfgreffier, jhey, João Paulo Barraca, Jochen S, Johannes Krude,
    José Rebelo, ksiwczynski, ladbsoft, Lesur Frederic, Maciej Kuśnierz, mamucho,
    Manuel Ruß, maxirnilian, mkusnierz, narektor, Noodlez, odavo32nof, opavlov,
    pangwalla, Pavel Elagin, Petr Kadlec, Petr Vaněk, protomors, Quallenauge,
    Quang Ngô, Raghd Hamzeh, Sami Alaoui, Sebastian Kranz, sedy89, Sophanimus,
    Stefan Bora, Taavi Eomäe, thermatk, tiparega, Vadim Kaushan, x29a, xaos,
    Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class DeviceHelper {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceHelper.class);

    private static final DeviceHelper instance = new DeviceHelper();

    /**
     * A map from mac address to a forced device type this device will be recognized as. Allows unsupported
     * devices to be paired as a specified device type. This is a hack, required until we refactor the current
     * discovery and pairing process.
     */
    private final Map<String, DeviceType> forcedDeviceTypes = new HashMap<>();

    private DeviceType[] orderedDeviceTypes = null;

    public static DeviceHelper getInstance() {
        return instance;
    }

    private final HashMap<String, DeviceType> deviceTypeCache = new HashMap<>();

    @Nullable
    public GBDevice findAvailableDevice(String deviceAddress) {
        Set<GBDevice> availableDevices = getAvailableDevices();
        for (GBDevice availableDevice : availableDevices) {
            if (deviceAddress.equals(availableDevice.getAddress())) {
                return availableDevice;
            }
        }
        return null;
    }

    /**
     * Returns the list of all available devices that are supported by Gadgetbridge.
     * Note that no state is known about the returned devices. Even if one of those
     * devices is connected, it will report the default not-connected state.
     * <p>
     * Clients interested in the "live" devices being managed should use the class
     * DeviceManager.
     */
    public Set<GBDevice> getAvailableDevices() {
        return new LinkedHashSet<>(getDatabaseDevices());
    }

    public void setForcedDeviceType(final String address, final DeviceType deviceType) {
        LOG.debug("Forcing recognition of {} as {}", address, deviceType);
        synchronized (this) {
            forcedDeviceTypes.put(address.toLowerCase(), deviceType);
        }
    }

    public void clearForcedDeviceTypes() {
        synchronized (this) {
            if (!forcedDeviceTypes.isEmpty()) {
                LOG.debug("Clearing forced device types");
            }
            forcedDeviceTypes.clear();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public GBDevice toSupportedDevice(BluetoothDevice device) {
        GBDeviceCandidate candidate = new GBDeviceCandidate(device, GBDevice.RSSI_UNKNOWN, device.getUuids(), null);
        candidate.refreshNameIfUnknown();
        return toSupportedDevice(candidate);
    }

    public GBDevice toSupportedDevice(GBDeviceCandidate candidate) {
        DeviceType resolvedType = resolveDeviceType(candidate);
        return resolvedType.getDeviceCoordinator().createDevice(candidate, resolvedType);
    }

    private DeviceType[] getOrderedDeviceTypes() {
        if (orderedDeviceTypes == null) {
            ArrayList<DeviceType> orderedDevices = new ArrayList<>(Arrays.asList(DeviceType.values()));
            Collections.sort(orderedDevices, Comparator.comparingInt(dc -> dc.getDeviceCoordinator().getOrderPriority()));
            orderedDeviceTypes = orderedDevices.toArray(new DeviceType[0]);
        }

        return orderedDeviceTypes;
    }

    public DeviceType resolveDeviceType(@NonNull final GBDeviceCandidate deviceCandidate) {
        return resolveDeviceType(deviceCandidate, true);
    }

    public DeviceType resolveDeviceType(@NonNull final GBDeviceCandidate deviceCandidate, boolean useCache) {
        synchronized (this) {
            final String macAddress = deviceCandidate.getMacAddress().toLowerCase();

            if (forcedDeviceTypes.containsKey(macAddress)) {
                final DeviceType deviceType = forcedDeviceTypes.get(macAddress);
                LOG.debug("Resolving of {} is forced to {}", macAddress, deviceType);
                return deviceType;
            }

            if (useCache) {
                DeviceType cachedType =
                        deviceTypeCache.get(macAddress);
                if (cachedType != null) {
                    return cachedType;
                }
            }

            for (DeviceType type : getOrderedDeviceTypes()) {
                if (type.getDeviceCoordinator().supports(deviceCandidate)) {
                    deviceTypeCache.put(macAddress, type);
                    return type;
                }
            }
            deviceTypeCache.put(macAddress, DeviceType.UNKNOWN);
        }
        return DeviceType.UNKNOWN;
    }

    public DeviceCoordinator resolveCoordinator(GBDeviceCandidate device) {
        return resolveDeviceType(device).getDeviceCoordinator();
    }

    private List<GBDevice> getDatabaseDevices() {
        List<GBDevice> result = new ArrayList<>();
        try (DBHandler lockHandler = GBApplication.acquireDB()) {
            List<Device> activeDevices = DBHelper.getActiveDevices(lockHandler.getDaoSession());
            for (Device dbDevice : activeDevices) {
                GBDevice gbDevice = toGBDevice(dbDevice);
                if (gbDevice != null && gbDevice.getType().isSupported()) {
                    result.add(gbDevice);
                }
            }
            return result;

        } catch (Exception e) {
            GB.toast(GBApplication.getContext().getString(R.string.error_retrieving_devices_database), Toast.LENGTH_SHORT, GB.ERROR, e);
            return Collections.emptyList();
        }
    }

    /**
     * Converts a known device from the database to a GBDevice.
     * Note: The device might not be supported anymore, so callers should verify that.
     */
    public GBDevice toGBDevice(Device dbDevice) {
        DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());
        return deviceType.getDeviceCoordinator().createDevice(dbDevice, deviceType);
    }
}
