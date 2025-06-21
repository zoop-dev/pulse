/*  Copyright (C) 2020-2025 Andreas Böhler, Arjan Schrijver, Daniel Dakhno,
    José Rebelo, Taavi Eomäe, Thomas Kuehne

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

import static androidx.core.app.ActivityCompat.startIntentSenderForResult;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.companion.DeviceFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.MacAddress;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BleNamesResolver;

@SuppressLint("MissingPermission")
public class BondingUtil {
    public static final String STATE_DEVICE_CANDIDATE = "stateDeviceCandidate";

    private static final int REQUEST_CODE = 1;
    private static final Logger LOG = LoggerFactory.getLogger(BondingUtil.class);
    private static final long DELAY_AFTER_BONDING = 1000; // 1s

    /**
     * Returns a BroadcastReceiver that handles Gadgetbridge's device changed broadcasts
     */
    public static BroadcastReceiver getPairingReceiver(final BondingInterface activity) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                    GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    LOG.debug("Pairing receiver: device changed: " + device);
                    if (device != null && device.getAddress().equals(activity.getMacAddress())) {
                        if (device.isInitialized()) {
                            LOG.info("Device is initialized, finish things up");
                            activity.onBondingComplete(true);
                        } else if (device.isConnecting() || device.isInitializing()) {
                            LOG.info("Still connecting/initializing device...");
                        }
                    }
                }
            }
        };
    }

    /**
     * Returns a BroadcastReceiver that handles Bluetooth bonding broadcasts
     * @see BluetoothDevice#ACTION_BOND_STATE_CHANGED
     */
    public static BroadcastReceiver getBondingReceiver(final BondingInterface bondingInterface) {
        return new BroadcastReceiver() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String bondingMacAddress = bondingInterface.getMacAddress();
                    if (device == null) {
                        LOG.error("invalid {} intent: {} is null", BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                                BluetoothDevice.EXTRA_DEVICE);
                        return;
                    }

                    final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,  BluetoothDevice.ERROR);
                    final int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                    LOG.info("Bond state changed for {} from {} to {}, target address {}",
                            device,
                            BleNamesResolver.getBondStateString(bondState),
                            BleNamesResolver.getBondStateString(prevBondState),
                            bondingMacAddress
                    );


                    if (bondingMacAddress == null || !bondingMacAddress.equalsIgnoreCase(device.getAddress())) {
                        LOG.debug("ignore due to MAC address: got {} but expected {}", device.getAddress(), bondingMacAddress);
                    } else {
                        switch (bondState) {
                            case BluetoothDevice.BOND_BONDED: {
                                LOG.info("Bonded with {}", device.getAddress());
                                //noinspection StatementWithEmptyBody
                                if (isLePebble(device) || isPebble2(device) || !bondingInterface.getAttemptToConnect()) {
                                    // Do not initiate connection to LE Pebble and some others!
                                } else {
                                    attemptToFirstConnect(device);
                                }
                                return;
                            }
                            case BluetoothDevice.BOND_NONE: {
                                LOG.info("Not bonded with {}, attempting to connect anyway.",
                                        device.getAddress());
                                if(bondingInterface.getAttemptToConnect())
                                    attemptToFirstConnect(device);
                                return;
                            }
                            case BluetoothDevice.BOND_BONDING: {
                                LOG.info("Bonding in progress with {}", device.getAddress());
                                return;
                            }
                            default: {
                                LOG.warn("Unhandled bond state for device {}: {}", device.getAddress(),
                                        BleNamesResolver.getBondStateString(bondState));
                                bondingInterface.onBondingComplete(false);
                            }
                        }
                    }
                }
            }
        };
    }

    /**
     * Connect to candidate after a certain delay
     *
     * @param candidate the device to connect to
     */
    public static void attemptToFirstConnect(final BluetoothDevice candidate) {
        Looper mainLooper = Looper.getMainLooper();
        new Handler(mainLooper).postDelayed(new Runnable() {
            @Override
            public void run() {
                GBDevice device = DeviceHelper.getInstance().toSupportedDevice(candidate);
                GBApplication.deviceService(device).disconnect();
                connectToGBDevice(device);
            }
        }, DELAY_AFTER_BONDING);
    }

    /**
     * Just calls DeviceService connect with the "first time" flag
     */
    private static void connectToGBDevice(GBDevice device) {
        if (device != null) {
            GBApplication.deviceService(device).connect(true);
        } else {
            GB.toast("Unable to connect, can't recognize the device type", Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    /**
     * Connects to the device and calls callback
     */
    public static void connectThenComplete(BondingInterface bondingInterface, GBDeviceCandidate deviceCandidate) {
        GBDevice device = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate);
        connectThenComplete(bondingInterface, device);
    }

    /**
     * Connects to the device and calls callback
     */
    public static void connectThenComplete(BondingInterface bondingInterface, GBDevice device) {
        toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.discovery_trying_to_connect_to, device.getName()), Toast.LENGTH_SHORT, GB.INFO);
        // Disconnect when LE Pebble so that the user can manually initiate a connection
        GBApplication.deviceService(device).disconnect();
        GBApplication.deviceService(device).connect(true);
        bondingInterface.onBondingComplete(true);
    }

    /**
     * Checks the type of bonding needed for the device and continues accordingly
     */
    public static void initiateCorrectBonding(final BondingInterface bondingInterface, final GBDeviceCandidate deviceCandidate, DeviceCoordinator coordinator) {
        int bondingStyle = coordinator.getBondingStyle();
        if (bondingStyle == DeviceCoordinator.BONDING_STYLE_NONE ||
            bondingStyle == DeviceCoordinator.BONDING_STYLE_LAZY ) {
            // Do nothing
            return;
        } else if (bondingStyle == DeviceCoordinator.BONDING_STYLE_ASK) {
            new MaterialAlertDialogBuilder(bondingInterface.getContext())
                    .setCancelable(true)
                    .setTitle(bondingInterface.getContext().getString(R.string.discovery_pair_title, deviceCandidate.getName()))
                    .setMessage(bondingInterface.getContext().getString(R.string.discovery_pair_question))
                    .setPositiveButton(bondingInterface.getContext().getString(R.string.discovery_yes_pair), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BondingUtil.tryBondThenComplete(bondingInterface, deviceCandidate.getDevice());
                        }
                    })
                    .setNegativeButton(R.string.discovery_dont_pair, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BondingUtil.connectThenComplete(bondingInterface, deviceCandidate);
                        }
                    })
                    .show();
        } else {
            BondingUtil.tryBondThenComplete(bondingInterface, deviceCandidate.getDevice());
        }
        LOG.debug("Bonding initiated");
    }

    /**
     * Tries to create a BluetoothDevice bond
     * Do not call directly, use createBond(Activity, GBDeviceCandidate) instead!
     */
    private static void bluetoothBond(BondingInterface context, BluetoothDevice device) {
        if (device.createBond()) {
            // Async, results will be delivered via a broadcast
            LOG.info("Bonding in progress...");
        } else {
            LOG.error(String.format(Locale.getDefault(),
                    "Bonding failed immediately! %1$s (%2$s) %3$d",
                    device.getName(),
                    device.getAddress(),
                    device.getType())
            );

            BluetoothClass bluetoothClass = device.getBluetoothClass();
            if (bluetoothClass != null) {
                LOG.error(String.format(Locale.getDefault(),
                        "BluetoothClass: %1$s",
                        bluetoothClass.toString()));
            }

            // Theoretically we shouldn't be doing this
            // because this function shouldn't've been called
            // with an already bonded device
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                LOG.warn("For some reason the device is already bonded, but let's try first connect");
                attemptToFirstConnect(context.getCurrentTarget().getDevice());
            } else if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                LOG.warn("Device is still bonding after an error");
                // TODO: Not sure we can handle this better, it's weird already.
            } else {
                LOG.warn("Bonding failed immediately and no bond was made");
                toast(context.getContext(), context.getContext().getString(R.string.discovery_bonding_failed_immediately, device.getName()), Toast.LENGTH_SHORT, GB.ERROR);
            }
        }
    }

    /**
     * Handles the activity result and checks if there's anything CompanionDeviceManager-related going on
     */
    public static void handleActivityResult(BondingInterface bondingInterface, int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && requestCode == REQUEST_CODE) {
            if (resultCode != CompanionDeviceManager.RESULT_OK) {
                final String reason = switch (resultCode) {
                    case CompanionDeviceManager.RESULT_CANCELED -> "RESULT_CANCELED";
                    case CompanionDeviceManager.RESULT_USER_REJECTED -> "RESULT_USER_REJECTED";
                    case CompanionDeviceManager.RESULT_DISCOVERY_TIMEOUT -> "RESULT_DISCOVERY_TIMEOUT";
                    case CompanionDeviceManager.RESULT_INTERNAL_ERROR -> "RESULT_INTERNAL_ERROR";
                    case CompanionDeviceManager.RESULT_SECURITY_ERROR -> "RESULT_SECURITY_ERROR";
                    default -> Integer.toString(resultCode);
                };
                LOG.warn("associating CompanionDevice {} failed: {}",
                        bondingInterface.getMacAddress(), reason);
                return;
            }

            final BluetoothDevice deviceToPair;
            final Object extra = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
            if (extra instanceof BluetoothDevice) {
                deviceToPair = (BluetoothDevice) extra;
            } else if (extra instanceof final ScanResult scanResult) {
                deviceToPair = scanResult.getDevice();
            } else {
                LOG.error("handleActivityResult unexpected EXTRA_DEVICE {}", extra);
                deviceToPair = null;
            }

            if (deviceToPair != null) {
                if (deviceToPair.getAddress().equals(bondingInterface.getMacAddress())) {
                    StartObserving(bondingInterface.getContext(), deviceToPair.getAddress());
                    if (deviceToPair.getBondState() != BluetoothDevice.BOND_BONDED) {
                        BondingUtil.bluetoothBond(bondingInterface, bondingInterface.getCurrentTarget().getDevice());
                    } else {
                        bondingInterface.onBondingComplete(true);
                    }
                } else {
                    LOG.debug("handleActivityResult unexpected device {}", deviceToPair);
                    bondingInterface.onBondingComplete(false);
                }
            }
        }
    }

    /**
     * Checks if device is LE Pebble
     */
    public static boolean isLePebble(BluetoothDevice device) {
        return (device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL || device.getType() == BluetoothDevice.DEVICE_TYPE_LE) &&
                (device.getName().startsWith("Pebble-LE ") || device.getName().startsWith("Pebble Time LE "));
    }

    /**
     * Checks if device is Pebble 2
     */
    public static boolean isPebble2(BluetoothDevice device) {
        return device.getType() == BluetoothDevice.DEVICE_TYPE_LE &&
                device.getName().startsWith("Pebble ") &&
                !device.getName().startsWith("Pebble Time LE ");
    }

    /**
     * Uses the CompanionDeviceManager bonding method
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private static void companionDeviceManagerBond(BondingInterface bondingInterface,
                                                   BluetoothDevice device) {
        final String macAddress = device.getAddress();
        final int type = device.getType();
        final DeviceFilter<?> deviceFilter;

        if (type == BluetoothDevice.DEVICE_TYPE_LE || type == BluetoothDevice.DEVICE_TYPE_DUAL) {
            LOG.debug("companionDeviceManagerBond {} type {} - treat as LE",
                    macAddress, type);
            ScanFilter scan = new ScanFilter.Builder()
                    .setDeviceAddress(macAddress)
                    .build();

            deviceFilter = new BluetoothLeDeviceFilter.Builder()
                    .setScanFilter(scan)
                    .build();
        } else {
            LOG.debug("companionDeviceManagerBond {} type {} - treat as classic BT",
                    macAddress, type);
            deviceFilter = new BluetoothDeviceFilter.Builder()
                    .setAddress(macAddress)
                    .build();
        }

        AssociationRequest pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(true)
                .build();

        CompanionDeviceManager manager = (CompanionDeviceManager) bondingInterface.getContext().getSystemService(Context.COMPANION_DEVICE_SERVICE);
        LOG.debug(String.format("Searching for %s associations", macAddress));
        for (String association : manager.getAssociations()) {
            LOG.debug(String.format("Already associated with: %s", association));
            if (association.equals(macAddress)) {
                StartObserving(bondingInterface.getContext(), macAddress);
                LOG.info("The device has already been bonded through CompanionDeviceManager, using regular");
                // If it's already "associated", we should immediately pair
                // because the callback is never called (AFAIK?)
                BondingUtil.bluetoothBond(bondingInterface, device);
                return;
            }
        }

        LOG.debug("Starting association request");
        manager.associate(pairingRequest,
                getCompanionDeviceManagerCallback(bondingInterface),
                null);
    }

    /**
     * This is a bit hacky, but it does stop a bonding that might be otherwise stuck,
     * use with some caution
     */
    public static void stopBluetoothBonding(BluetoothDevice device) {
        try {
            //noinspection JavaReflectionMemberAccess
            device.getClass().getMethod("cancelBondProcess").invoke(device);
        } catch (Throwable ignore) {
        }
    }

    /**
     * Finalizes bonded device
     */
    public static void handleDeviceBonded(BondingInterface bondingInterface, GBDeviceCandidate deviceCandidate) {
        if (deviceCandidate == null) {
            LOG.error("deviceCandidate was null! Can't handle bonded device!");
            return;
        }

        toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.discovery_successfully_bonded, deviceCandidate.getName()), Toast.LENGTH_SHORT, GB.INFO);
        connectThenComplete(bondingInterface, deviceCandidate);
    }

    /**
     * Use this function to initiate bonding to a GBDeviceCandidate
     */
    public static void tryBondThenComplete(final BondingInterface bondingInterface, final BluetoothDevice device) {
        bondingInterface.registerBroadcastReceivers();

        final int bondState = device.getBondState();

        if (bondState == BluetoothDevice.BOND_BONDING) {
            GB.toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.pairing_in_progress, device.getName(), device.getAddress()), Toast.LENGTH_LONG, GB.INFO);
            return;
        }

        // FIXME: We can only attempt companion pairing if the context of the bondingInterface is an activity
        // however, when called from the BondAction it will always be the global Application context
        // See https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/3784#issuecomment-2121792
        final boolean contextIsActivity = bondingInterface.getContext() instanceof Activity;

        if (bondState == BluetoothDevice.BOND_BONDED) {
            GB.toast(bondingInterface.getContext().getString(R.string.pairing_already_bonded, device.getName(), device.getAddress()), Toast.LENGTH_SHORT, GB.INFO);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isPebble2(device) && contextIsActivity) {
                // If CompanionDeviceManager is available, skip connection and go bond
                // TODO: It would theoretically be nice to check if it's already been granted,
                //  but re-bond works
                askCompanionPairing(bondingInterface, device);
            } else {
                attemptToFirstConnect(bondingInterface.getCurrentTarget().getDevice());
            }
            return;
        }

        GB.toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.pairing_creating_bond_with, device.getName(), device.getAddress()), Toast.LENGTH_LONG, GB.INFO);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isPebble2(device) && contextIsActivity) {
            askCompanionPairing(bondingInterface, device);
        } else if (isPebble2(device)) {
            // TODO: start companionDevicePairing after connecting to Pebble 2 but before writing to pairing trigger
            attemptToFirstConnect(device);
        } else {
            bluetoothBond(bondingInterface, device);
        }

        GB.toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.pairing_bonding_under_way), Toast.LENGTH_LONG, GB.INFO);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static void askCompanionPairing(BondingInterface bondingInterface, BluetoothDevice device) {
        new MaterialAlertDialogBuilder(bondingInterface.getContext())
                .setTitle(R.string.companion_pairing_request_title)
                .setMessage(R.string.companion_pairing_request_description)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    companionDeviceManagerBond(bondingInterface, device);
                })
                .setNegativeButton(R.string.no, (dialog, whichButton) -> {
                    bluetoothBond(bondingInterface, device);
                })
                .show();
    }

    /**
     * Returns a callback for CompanionDeviceManager
     *
     * @param bondingInterface the activity that started the CDM bonding process
     * @return CompanionDeviceManager.Callback that handles the CompanionDeviceManager bonding process results
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private static CompanionDeviceManager.Callback getCompanionDeviceManagerCallback(final BondingInterface bondingInterface) {
        return new CompanionDeviceManager.Callback() {
            @Override
            public void onFailure(CharSequence error) {
                Context context = bondingInterface.getContext();
                String message = context.getString(R.string.discovery_bonding_error, error);
                toast(context, message, Toast.LENGTH_SHORT, GB.ERROR);
            }

            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                try {
                    startIntentSenderForResult((Activity) bondingInterface.getContext(),
                            chooserLauncher,
                            REQUEST_CODE,
                            null,
                            0,
                            0,
                            0,
                            null);
                } catch (IntentSender.SendIntentException e) {
                    LOG.error(e.toString());
                }
            }
        };
    }

    public static boolean StartObservingAll(Context context) {
        return StartObserving(context, null);
    }

    public static boolean StartObserving(Context context, String mac) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            LOG.info("StartObserving - API {} < {} is too old",
                    Build.VERSION.SDK_INT, Build.VERSION_CODES.S);
            return false;
        }

        if (mac != null && !BluetoothAdapter.checkBluetoothAddress(mac)) {
            LOG.warn("StartObserving - mac '{}' is invalid", mac);
            return false;
        }

        final CompanionDeviceManager manager = getCompanionDeviceManager(context);
        if (manager == null) {
            LOG.warn("StartObserving - CompanionDeviceManager is null");
            return false;
        }

        boolean success = false;
        final List<String> addresses;
        if (mac == null) {
            addresses = manager.getAssociations();
            LOG.debug("StartObserving - {} associations", addresses.size());
        } else {
            addresses = Collections.singletonList(mac);
        }

        for (final String address : addresses) {
            try {
                LOG.debug("StartObserving - {}", address);
                manager.startObservingDevicePresence(address);
                success = true;
            } catch (Exception e) {
                LOG.warn("StartObserving - exception", e);
            }
        }
        return success;
    }


    public static boolean StopObservingAll(Context context) {
        return StopObserving(context, null);
    }

    public static boolean StopObserving(final Context context, final String mac) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            LOG.info("StopObserving - API {} < {} is too old",
                    Build.VERSION.SDK_INT, Build.VERSION_CODES.S);
            return false;
        }

        if (mac != null && !BluetoothAdapter.checkBluetoothAddress(mac)) {
            LOG.warn("StopObserving - mac '{}' is invalid", mac);
            return false;
        }

        final CompanionDeviceManager manager = getCompanionDeviceManager(context);
        if (manager == null) {
            LOG.warn("StopObserving - CompanionDeviceManager is null");
            return false;
        }

        boolean success = false;
        final List<String> addresses;
        if (mac == null) {
            addresses = manager.getAssociations();
            LOG.debug("StopObserving - {} associations", addresses.size());
        } else {
            addresses = Collections.singletonList(mac);
        }

        for (final String address : addresses) {
            try {
                LOG.debug("StopObserving - {}", address);
                manager.stopObservingDevicePresence(address);
                success = true;
            } catch (Exception e) {
                LOG.warn("StopObserving - exception {}", e.getMessage());
            }
        }
        return success;
    }

    public static boolean Disassociate(Context context, String mac) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            LOG.info("Disassociate - API {} < {} is too old",
                    Build.VERSION.SDK_INT, Build.VERSION_CODES.O);
            return false;
        }

        if (!BluetoothAdapter.checkBluetoothAddress(mac)) {
            LOG.warn("Disassociate - mac '{}' is invalid", mac);
            return false;
        }

        final CompanionDeviceManager manager = getCompanionDeviceManager(context);
        if (manager == null) {
            LOG.warn("Disassociate - CompanionDeviceManager is null");
        } else {
            try {
                StopObserving(context, mac);
                LOG.debug("Disassociate - {}", mac);
                manager.disassociate(mac);
                return true;
            } catch (Exception e) {
                LOG.info("Disassociate - exception {}", e.getMessage());
            }
        }

        return false;
    }

    public static boolean Unpair(Context context, String mac) {
        if (!BluetoothAdapter.checkBluetoothAddress(mac)) {
            LOG.warn("Unpair - mac '{}' is invalid", mac);
            return false;
        }

        StopObserving(context, mac);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            LOG.debug("Unpair - API {} < {} is too old for modern bond removal",
                    Build.VERSION.SDK_INT, Build.VERSION_CODES.BAKLAVA);
        } else {
            final CompanionDeviceManager manager = getCompanionDeviceManager(context);
            if (manager == null) {
                LOG.warn("Unpair - CompanionDeviceManager is null");
            } else {
                List<AssociationInfo> associations = manager.getMyAssociations();
                LOG.debug("Unpair - {} associations", associations.size());
                for (AssociationInfo association : associations) {
                    MacAddress devAddress = association.getDeviceMacAddress();
                    String devMac = (devAddress == null) ? null : devAddress.toString();

                    if (mac.equalsIgnoreCase(devMac)) {
                        boolean removed = false;
                        try {
                            removed = manager.removeBond(association.getId());
                            LOG.debug("Unpair - Modern removeBond({}) => {}",
                                    association.getDeviceMacAddress(), removed);
                        } catch (Exception e) {
                            LOG.info("Unpair - Modern removeBond exception:{}", e.getMessage());
                        }

                        try {
                            // TODO use BluetoothDevice.ACTION_BOND_STATE_CHANGED instead of sleep
                            Thread.sleep(DELAY_AFTER_BONDING);
                        } catch (InterruptedException ignore) {
                        }

                        if (removed) {
                            try {
                                manager.disassociate(association.getId());
                            } catch (Exception e) {
                                LOG.info("Unpair - Modern disassociate exception:{}", e.getMessage());
                            }
                            return true;
                        } else {
                            // fall back to classic
                            return UnpairClassic(context, mac);
                        }
                    }
                }
                LOG.debug("Unpair - no matching association found");
            }
        }
        return UnpairClassic(context, mac);
    }

    private static boolean UnpairClassic(Context context, String mac) {
        final BluetoothAdapter adapter = getBluetoothAdapter(context);
        boolean removed = false;
        if (adapter == null) {
            LOG.warn("UnpairClassic - BluetoothAdapter is null");
        } else {
            if (!adapter.isEnabled()) {
                LOG.warn("UnpairClassic - BluetoothAdapter is disabled");
            }

            final BluetoothDevice device = adapter.getRemoteDevice(mac);
            if (device == null) {
                LOG.warn("UnpairClassic - BluetoothDevice is null");
            } else {
                int bondState = device.getBondState();

                switch (bondState) {
                    case BluetoothDevice.BOND_NONE:
                        LOG.debug("UnpairClassic - nothing todo for {} with {}",
                                device.getAddress(),
                                BleNamesResolver.getBondStateString(bondState));
                        removed = true;
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        LOG.debug("UnpairClassic - removeBond for {} with {}",
                                device.getAddress(),
                                BleNamesResolver.getBondStateString(bondState));
                        try {
                            Method method = device.getClass().getMethod("removeBond");
                            Object result = method.invoke(device);
                            removed = Boolean.TRUE.equals(result);
                        } catch (Exception e) {
                            LOG.warn("UnpairClassic - exception for removeBond", e);
                        }
                        try {
                            // TODO use BluetoothDevice.ACTION_BOND_STATE_CHANGED instead of sleep
                            Thread.sleep(DELAY_AFTER_BONDING);
                        } catch (InterruptedException ignore) {
                        }
                        LOG.debug("UnpairClassic - result:{} success:{}",
                                BleNamesResolver.getBondStateString(device.getBondState()), removed);
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        LOG.debug("UnpairClassic - cancelBondProcess for {} with {}",
                                device.getAddress(),
                                BleNamesResolver.getBondStateString(bondState));

                        try {
                            Method method = device.getClass().getMethod("cancelBondProcess");
                            Object result = method.invoke(device);
                            removed = Boolean.TRUE.equals(result);
                        } catch (Exception e) {
                            LOG.warn("UnpairClassic - exception for cancelBondProcess", e);
                        }
                        try {
                            // TODO use BluetoothDevice.ACTION_BOND_STATE_CHANGED instead of sleep
                            Thread.sleep(DELAY_AFTER_BONDING);
                        } catch (InterruptedException ignore) {
                        }
                        break;
                    default:
                        LOG.debug("UnpairClassic - unhandled {} for {}",
                                BleNamesResolver.getBondStateString(bondState),
                                device.getAddress()
                        );
                }
            }
        }

        Disassociate(context, mac);

        return removed;
    }

    @Nullable
    public static BluetoothAdapter getBluetoothAdapter(Context context) {
        if (context == null) {
            LOG.error("getBluetoothAdapter - context is null");
        } else {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager == null) {
                LOG.warn("getBluetoothAdapter - BluetoothManager is null");
            } else {
                BluetoothAdapter adapter = manager.getAdapter();
                if (adapter == null) {
                    LOG.warn("getBluetoothAdapter - BluetoothAdapter is null");
                } else {
                    return adapter;
                }
            }

            final PackageManager pm = context.getPackageManager();
            if (pm == null) {
                LOG.warn("getBluetoothAdapter - PackageManager is null");
            } else {
                LOG.warn("getBluetoothAdapter - FEATURE_BLUETOOTH available: {} ",
                        pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH));
                LOG.warn("getBluetoothAdapter - FEATURE_BLUETOOTH_LE available: {} ",
                        pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
            }
        }
        return null;
    }

    @Nullable
    public static CompanionDeviceManager getCompanionDeviceManager(Context context) {
        if (context == null) {
            LOG.error("getCompanionDeviceManager - context is null");
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            LOG.debug("getCompanionDeviceManager - API {} < {} is too old",
                    Build.VERSION.SDK_INT, Build.VERSION_CODES.O);
        } else {
            final CompanionDeviceManager manager = (CompanionDeviceManager) context.getSystemService(Context.COMPANION_DEVICE_SERVICE);
            if (manager != null) {
                return manager;
            }

            LOG.warn("getCompanionDeviceManager - CompanionDeviceManager is null");
            final PackageManager pm = context.getPackageManager();
            if (pm == null) {
                LOG.warn("getCompanionDeviceManager - PackageManager is null");
            } else {
                LOG.warn("getCompanionDeviceManager - FEATURE_COMPANION_DEVICE_SETUP is available: {}",
                        pm.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP));
            }
        }
        return null;
    }
}
