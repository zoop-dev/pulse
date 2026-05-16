/*  Copyright (C) 2016-2024 Andreas Böhler, Andreas Shimokawa, Carsten
    Pfeiffer, Daniele Gobbetti, José Rebelo, Taavi Eomäe

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

import static nodomain.freeyourgadget.gadgetbridge.util.BondingUtil.STATE_DEVICE_CANDIDATE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.greenrobot.dao.query.Query;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BondingInterface;
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleHardware;


public class PebblePairingActivity extends AbstractGBActivity implements BondingInterface {
    private static final Logger LOG = LoggerFactory.getLogger(PebblePairingActivity.class);
    private final BroadcastReceiver pairingReceiver = BondingUtil.getPairingReceiver(this);
    private final BroadcastReceiver bondingReceiver = BondingUtil.getBondingReceiver(this);

    private TextView message;
    private boolean isPairing;

    private GBDeviceCandidate deviceCandidate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pebble_pairing);

        message = findViewById(R.id.pebble_pair_message);
        Intent intent = getIntent();
        intent.setExtrasClassLoader(GBDeviceCandidate.class.getClassLoader());
        deviceCandidate = intent.getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);

        String macAddress = null;
        if (deviceCandidate != null) {
            macAddress = deviceCandidate.getMacAddress();
        }

        if (macAddress == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
            onBondingComplete(false);
            return;
        }

        BluetoothDevice btDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
        if (btDevice == null) {
            GB.toast(this, "No such Bluetooth Device: " + macAddress, Toast.LENGTH_LONG, GB.ERROR);
            onBondingComplete(false);
            return;
        }

        startBonding(btDevice);
    }

    private void startBonding(BluetoothDevice btDevice) {
        isPairing = true;
        message.setText(getString(R.string.pairing, btDevice.getAddress()));

        GBDevice device;
        if (PebbleHardware.isLePebbleCompanion(btDevice)) {
            if (!GBApplication.getPrefs().getBoolean("pebble_force_le", false)) {
                GB.toast(this, "Please switch on \"Always prefer BLE\" option in Pebble settings before pairing you Pebble LE", Toast.LENGTH_LONG, GB.ERROR);
                onBondingComplete(false);
                return;
            }

            device = getMatchingParentDeviceFromDBAndSetVolatileAddress(btDevice);
            if (device == null) {
                onBondingComplete(false);
                return;
            }

            registerBroadcastReceivers();
            BondingUtil.connectThenComplete(this, device);
            return;
        }

        // Pebble 2, Pebble Time 2, and Pebble 2 Duo are BLE-only and need GATT connection first.
        // They require writing to a pairing trigger characteristic before createBond() works.
        // PebbleGATTClient handles the pairing trigger write and initiates bonding.
        // Note: These devices are BLE-only, so we don't need the "pebble_force_le" setting -
        // PebbleIoThread automatically uses BLE based on isPebble2() detection.
        // Use deviceCandidate to leverage manufacturer data for reliable device identification.
        if (PebbleHardware.needsConnectFirstPairing(deviceCandidate)) {
            LOG.info("BLE-only device detected - using connect-first pairing");

            registerBroadcastReceivers();

            // Create device and initiate connection - BLE mode will be auto-detected in PebbleIoThread
            GBDevice gbDevice = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate);
            if (gbDevice != null) {
                GB.toast(this, getString(R.string.discovery_trying_to_connect_to, gbDevice.getName()), Toast.LENGTH_SHORT, GB.INFO);
                GBApplication.deviceService(gbDevice).connect(true);
                // Don't call onBondingComplete() here - wait for bonding via broadcast receiver
            } else {
                LOG.error("Failed to create GBDevice for Pebble 2/Time 2/2 Duo");
                onBondingComplete(false);
            }
            return;
        }

        if (btDevice.getBondState() == BluetoothDevice.BOND_BONDED ||
                btDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
            BondingUtil.connectThenComplete(this, deviceCandidate);
        } else {
            BondingUtil.tryBondThenComplete(this, deviceCandidate.getDevice());
        }
    }

    private void stopBonding() {
        isPairing = false;
        BondingUtil.stopBluetoothBonding(deviceCandidate.getDevice());
    }

    private GBDevice getMatchingParentDeviceFromDBAndSetVolatileAddress(BluetoothDevice btDevice) {
        String expectedSuffix = btDevice.getName();
        expectedSuffix = expectedSuffix.replace("Pebble-LE ", "");
        expectedSuffix = expectedSuffix.replace("Pebble Time LE ", "");
        expectedSuffix = expectedSuffix.substring(0, 2) + ":" + expectedSuffix.substring(2);
        LOG.info("Trying to find a Pebble with BT address suffix " + expectedSuffix);
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            DeviceDao deviceDao = session.getDeviceDao();
            Query<Device> query = deviceDao.queryBuilder().where(DeviceDao.Properties.TypeName.eq("PEBBLE"), DeviceDao.Properties.Identifier.like("%" + expectedSuffix)).build();

            List<Device> devices = query.list();
            if (devices.size() == 0) {
                GB.toast("Please pair your non-LE Pebble before pairing the LE one", Toast.LENGTH_SHORT, GB.INFO);
                onBondingComplete(false);
                return null;
            } else if (devices.size() > 1) {
                GB.toast("Can not match this Pebble LE to a unique device", Toast.LENGTH_SHORT, GB.INFO);
                onBondingComplete(false);
                return null;
            }

            GBDevice gbDevice = DeviceHelper.getInstance().toGBDevice(devices.get(0));
            gbDevice.setVolatileAddress(btDevice.getAddress());
            return gbDevice;
        } catch (Exception e) {
            GB.toast(getString(R.string.error_retrieving_devices_database), Toast.LENGTH_SHORT, GB.ERROR, e);
            onBondingComplete(false);
            return null;
        }
    }

    @Override
    public void onBondingComplete(boolean success) {
        LOG.debug("ONBONDINGCOMPLETE");
        unregisterBroadcastReceivers();

        BluetoothDevice btDevice = deviceCandidate != null ? deviceCandidate.getDevice() : null;

        if (success) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                    new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST));
        }

        if (success) {
            startActivity(new Intent(this, ControlCenterv2.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            startActivity(new Intent(this, DiscoveryActivityV2.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }

        if (btDevice != null && success && getAttemptToConnect() && shouldReconnectAfterBond()) {
            BondingUtil.attemptToFirstConnect(btDevice);
        }
        finish();
    }

    @Override
    public GBDeviceCandidate getCurrentTarget() {
        return this.deviceCandidate;
    }

    @Override
    public boolean getAttemptToConnect() {
        // LE companion manages its own connection via connectThenComplete
        return deviceCandidate == null || !PebbleHardware.isLePebbleCompanion(deviceCandidate.getDevice());
    }

    @Override
    public boolean shouldReconnectAfterBond() {
        // Connect-first pairing: bonding occurred within the existing GATT connection
        return !PebbleHardware.needsConnectFirstPairing(deviceCandidate);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BondingUtil.handleActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DEVICE_CANDIDATE, deviceCandidate);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
    }

    @Override
    protected void onStart() {
        registerBroadcastReceivers();
        super.onStart();
    }

    @Override
    protected void onResume() {
        registerBroadcastReceivers();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcastReceivers();
        if (isPairing) {
            stopBonding();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        unregisterBroadcastReceivers();
        if (isPairing) {
            stopBonding();
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        // WARN: Do not stop bonding process during pause!
        // Bonding process can pause the activity and you might miss broadcasts
        super.onPause();
    }

    @Override
    public void unregisterBroadcastReceivers() {
        AndroidUtils.safeUnregisterBroadcastReceiver(LocalBroadcastManager.getInstance(this), pairingReceiver);
        AndroidUtils.safeUnregisterBroadcastReceiver(this, bondingReceiver);
    }

    @Override
    public void registerBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(pairingReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));
        ContextCompat.registerReceiver(this, bondingReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public Context getContext() {
        return this;
    }
}
