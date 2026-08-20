/*  Copyright (C) 2015-2024 115ek, Andreas Böhler, Andreas Shimokawa, Andrew
    Watkins, angelpup, Carsten Pfeiffer, Cre3per, criogenic, DanialHanif, Daniel
    Dakhno, Daniele Gobbetti, Daniel Thompson, Da Pa, Dmytro Bielik, Frank Ertl,
    GeekosaurusR3x, Gordon Williams, Jean-François Greffier, jfgreffier, jhey,
    João Paulo Barraca, Jochen S, Johannes Krude, José Rebelo, ladbsoft,
    Lesur Frederic, mamucho, Manuel Ruß, maxirnilian, mkusnierz, narektor,
    Noodlez, odavo32nof, opavlov, pangwalla, Pavel Elagin, Petr Kadlec, Petr
    Vaněk, protomors, Quallenauge, Quang Ngô, Raghd Hamzeh, Sami Alaoui,
    Sebastian Kranz, sedy89, Sergey Trofimov, Sophanimus, Stefan Bora, Taavi
    Eomäe, thermatk, tiparega, Vadim Kaushan, x29a, xaos, Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleSupport;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

import java.lang.reflect.Constructor;
import java.util.EnumSet;

public class DeviceSupportFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceSupportFactory.class);

    private final BluetoothAdapter mBtAdapter;
    private final Context mContext;

    DeviceSupportFactory(Context context) {
        mContext = context;
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();
    }

    @Nullable
    public synchronized DeviceSupport createDeviceSupport(final @NonNull GBDevice device) throws GBException {
        if (device.getDeviceCoordinator().getConnectionType() == DeviceCoordinator.ConnectionType.USB) {
            return createUsbDeviceSupport(device);
        }

        final String deviceAddress = device.getAddress();
        final int indexFirstColon = deviceAddress.indexOf(":");
        if (indexFirstColon > 0) {
            if (indexFirstColon == deviceAddress.lastIndexOf(":")) { // only one colon
                return createTCPDeviceSupport(device);
            } else {
                // multiple colons -- bt?
                return createBTDeviceSupport(device);
            }
        } else {
            // no colon at all, maybe a class name?
            return createClassNameDeviceSupport(device);
        }
    }

    @Nullable
    private DeviceSupport createClassNameDeviceSupport(GBDevice device) throws GBException {
        final String className = device.getAddress();
        try {
            final Class<?> deviceSupportClass = Class.forName(className);
            final Constructor<?> constructor = deviceSupportClass.getConstructor();
            final DeviceSupport support = (DeviceSupport) constructor.newInstance();
            // has to create the device itself
            support.setContext(device, mBtAdapter, mContext);
            return support;
        } catch (ClassNotFoundException e) {
            LOG.warn("Unable to find DeviceSupport class {} for {}", className, device.getAddress());
            return null; // not a class, or not known at least
        } catch (Exception e) {
            throw new GBException("Error creating DeviceSupport instance for " + className, e);
        }
    }

    private ServiceDeviceSupport createServiceDeviceSupport(GBDevice device) throws GBException {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final Class<?> supportClass = coordinator.getDeviceSupportClass(device);

        try {
            final Constructor<?> constructor = supportClass.getConstructor();
            final DeviceSupport supportInstance = (DeviceSupport) constructor.newInstance();
            final EnumSet<ServiceDeviceSupport.Flags> initialFlags = coordinator.getInitialFlags();
            if (BuildConfig.DEBUG && initialFlags.contains(ServiceDeviceSupport.Flags.BUSY_CHECKING)) {
                final DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);
                if (devicePrefs.getBoolean("pref_device_debug_remove_busy_checking", false)) {
                    LOG.warn("Removing BUSY_CHECKING flag from {}", device.getAddress());
                    initialFlags.remove(ServiceDeviceSupport.Flags.BUSY_CHECKING);
                }
            }
            return new ServiceDeviceSupport(supportInstance, initialFlags);
        } catch (ReflectiveOperationException e) {
            LOG.error("error calling DeviceSupport constructor for {} with zero arguments", device.getAddress());
            throw new GBException(e);
        }
    }

    @Nullable
    private DeviceSupport createBTDeviceSupport(final GBDevice gbDevice) throws GBException {
        if (mBtAdapter == null) {
            LOG.warn("Unable to create bt device support for {} - no bt adapter", gbDevice.getAddress());
            return null;
        }
        if (!mBtAdapter.isEnabled()) {
            LOG.warn("Unable to create bt device support for {} - bt is disabled", gbDevice.getAddress());
            return null;
        }

        try {
            final DeviceSupport deviceSupport = createServiceDeviceSupport(gbDevice);
            deviceSupport.setContext(gbDevice, mBtAdapter, mContext);
            return deviceSupport;
        } catch (final Exception e) {
            throw new GBException(mContext.getString(R.string.cannot_connect_bt_address_invalid_), e);
        }
    }

    private DeviceSupport createTCPDeviceSupport(final GBDevice gbDevice) throws GBException {
        try {
            final DeviceSupport deviceSupport = new ServiceDeviceSupport(new PebbleSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
            deviceSupport.setContext(gbDevice, mBtAdapter, mContext);
            return deviceSupport;
        } catch (Exception e) {
            throw new GBException("cannot connect to " + gbDevice, e); // FIXME: localize
        }
    }

    @Nullable
    private DeviceSupport createUsbDeviceSupport(final GBDevice gbDevice) throws GBException {
        final UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            LOG.warn("No UsbManager available on this device");
            return null;
        }

        final UsbAccessory accessory = findUsbAccessory(manager, gbDevice);
        if (accessory == null) {
            LOG.warn("No compatible USB accessory attached for {}", gbDevice.getAddress());
            return null;
        }

        try {
            final DeviceSupport deviceSupport = createServiceDeviceSupport(gbDevice);
            deviceSupport.setContext(gbDevice, accessory, mContext);
            return deviceSupport;
        } catch (final Exception e) {
            throw new GBException("cannot connect to " + gbDevice, e);
        }
    }

    @Nullable
    private UsbAccessory findUsbAccessory(final UsbManager usbManager, final GBDevice gbDevice) {
        final UsbAccessory[] accessoryList = usbManager.getAccessoryList();
        if (accessoryList == null) {
            LOG.warn("USB accessory list is null");
            return null;
        }

        for (final UsbAccessory candidate : accessoryList) {
            LOG.debug("Checking compatibility with {}", candidate);
            if (!usbManager.hasPermission(candidate)) {
                // when we get here, we must already have permission. If not, it's not this device
                continue;
            }
            // We don't check if the coordinator supports it, since it might be an unsupported device
            // that was added as a test device. Once we reach here, we must be able to match by
            // serial number.
            if (gbDevice.getAddress().equals("usb:" + candidate.getSerial())) {
                return candidate;
            }
        }

        return null;
    }
}
