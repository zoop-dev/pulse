package nodomain.freeyourgadget.gadgetbridge.devices.evenrealities;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.DeviceCandidateAdapter;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities.G1Constants;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BondingInterface;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

/**
 * This class manages the pairing of both the left and right device for G1 glasses.
 * The user will select either the left or the right and this activity will search for the other
 * side pair both.
 */
public class G1PairingActivity extends AbstractGBActivity
        implements BondingInterface, AdapterView.OnItemClickListener {
    private static final Logger LOG = LoggerFactory.getLogger(G1PairingActivity.class);
    private final ArrayList<GBDeviceCandidate> nextLensCandidates = new ArrayList<>();
    private final BroadcastReceiver bluetoothReceiver = new G1PairingActivity.BluetoothReceiver();
    private final BroadcastReceiver deviceChangedReceiver =
            new G1PairingActivity.DeviceChangedReceiver();

    // Variables used to determine the initial state. The user can select the left or right lens to
    // start the pairing so these are used to determine the other device that needs connection.
    private GBDeviceCandidate initialDeviceCandidate;
    private G1Constants.Side initialDeviceCandidateSide;

    // Variables used for tracking the bonding state of both devices. The bonding steps involve
    // setting the current target to left, then initiating bonding on the current target. When the
    // bond has completed, the current target is set to the right device then bonding is initiated
    // on the current target again. BondingState is used to track the status of the state machine.
    // We use a combination of listeners on ACTION_BOND_STATE_CHANGED and ACTION_DEVICE_CHANGED to
    // advance the state.
    private GBDeviceCandidate currentBondingCandidate;
    private final Object stateLock = new Object();

    private enum BondingState {
        STARTING,
        WAITING_ON_LEFT_BOND,
        WAITING_ON_RIGHT_BOND,
        WAITING_FOR_FIRST_DISCONNECT,
        WAITING_FOR_SECOND_DISCONNECT,
        READY_TO_FINISH
    }

    private BondingState state;
    private GBDeviceCandidate leftDeviceCandidate;
    private GBDeviceCandidate rightDeviceCandidate;

    // References to UI elements so that any function can update the interface.
    private TextView hintTextView;
    private ProgressBar progressBar;
    private ListView nextLensCandidatesListView;

    @Override
    protected void onDestroy() {
        unregisterBroadcastReceivers();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        unregisterBroadcastReceivers();
        super.onStop();
    }

    @Override
    protected void onPause() {
        unregisterBroadcastReceivers();
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerBroadcastReceivers();
        super.onResume();
    }

    @Override
    public GBDeviceCandidate getCurrentTarget() {
        return currentBondingCandidate;
    }

    @Override
    public boolean getAttemptToConnect() {
        return true;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_even_realities_g1_pairing);

        // State machine starts in the starting state.
        state = BondingState.STARTING;

        // Initialize the references to all the UI element objects.
        hintTextView = findViewById(R.id.even_g1_pairing_status);
        nextLensCandidatesListView = findViewById(R.id.next_lens_candidates_list);
        progressBar = findViewById(R.id.pairing_progress_bar);

        // Pull the candidate device out of the intent.
        Intent intent = getIntent();
        intent.setExtrasClassLoader(GBDeviceCandidate.class.getClassLoader());
        initialDeviceCandidate =
                intent.getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);

        // Extract the name of the device and null check it.
        String name = initialDeviceCandidate.getName();
        if (name == null) {
            GB.toast(getContext(),
                     getString(R.string.pairing_even_realities_g1_invalid_device, "null"),
                     Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        // The name of the device will be something like 'Even G1_87_L_39E92'.
        // Extract the Even G1_87 out and null check it.
        String compositeDeviceName = G1Constants.getNameFromFullName(name);
        if (compositeDeviceName == null) {
            GB.toast(getContext(),
                     getString(R.string.pairing_even_realities_g1_invalid_device, name),
                     Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        // The name of the device will be something like 'Even G1_87_L_39E92'.
        // Extract the L or R from out and null check it.
        initialDeviceCandidateSide =
                G1Constants.getSideFromFullName(initialDeviceCandidate.getName());
        if (initialDeviceCandidateSide == null) {
            GB.toast(getContext(),
                     getString(R.string.pairing_even_realities_g1_invalid_device, name),
                     Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        // Determine the current and next side. This is used to show the correct UI element to the
        // user.
        int currentSide = 0;
        int nextSide = 0;
        if (initialDeviceCandidateSide == G1Constants.Side.LEFT) {
            currentSide = R.string.watchface_dialog_widget_preset_left;
            nextSide = R.string.watchface_dialog_widget_preset_right;
        } else {
            currentSide = R.string.watchface_dialog_widget_preset_right;
            nextSide = R.string.watchface_dialog_widget_preset_left;
        }
        hintTextView.setText(getString(R.string.pairing_even_realities_g1_select_next_lens,
                                       getString(currentSide), getString(nextSide)));

        // Populate the list of next side candidates. We examine all other devices in the discovery
        // list and filter them based on name.
        final List<Parcelable> allCandidates =
                intent.getParcelableArrayListExtra(DeviceCoordinator.EXTRA_DEVICE_ALL_CANDIDATES);
        if (allCandidates != null) {
            nextLensCandidates.clear();
            for (final Parcelable p : allCandidates) {
                final GBDeviceCandidate nextCandidate = (GBDeviceCandidate) p;
                // Filter out all devices that don't match the selected device name and also filter
                // out the selected device.
                String nextCandidatePrefix =
                        G1Constants.getNameFromFullName(nextCandidate.getName());
                if (!initialDeviceCandidate.equals(nextCandidate) &&
                    compositeDeviceName.equals(nextCandidatePrefix)) {
                    nextLensCandidates.add(nextCandidate);
                }
            }
        }

        // No matching device found.
        if (nextLensCandidates.isEmpty()) {
            GB.toast(getContext(), R.string.pairing_even_realities_g1_find_both_fail,
                     Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        // Setup the callbacks so we get notified when the devices are done bonding and when
        // connection state changes.
        registerBroadcastReceivers();

        // If there is only one matching device, initiate pairing with it, no need to ask the user.
        if (nextLensCandidates.size() == 1) {
            if (initialDeviceCandidateSide == G1Constants.Side.LEFT) {
                pairDevices(initialDeviceCandidate, nextLensCandidates.get(0));
            } else {
                pairDevices(nextLensCandidates.get(0), initialDeviceCandidate);
            }
        } else {
            // There is more than one matching candidate, display all of the candidates as a list
            // and let the user choose the correct one. This should be rare an only happen if the
            // user has multiple pairs of glasses around them. Even then, the two digit id should
            // not be the same between devices, but since it can only be 00-99, there are only 100
            // options, so collisions are inevitable. Better to have this and not need it than have
            // users get stuck.
            DeviceCandidateAdapter nextLensCandidatesAdapter =
                    new DeviceCandidateAdapter(this, nextLensCandidates);
            nextLensCandidatesListView.setAdapter(nextLensCandidatesAdapter);
            nextLensCandidatesListView.setOnItemClickListener(this);

            // Hide the progress bar. The list is visible by default, so it will be shown.
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final GBDeviceCandidate nextDeviceCandidate = nextLensCandidates.get(position);
        // The user may have selected either the right or the left lens. We have both devices, pair
        // them as left and right.
        if (initialDeviceCandidateSide == G1Constants.Side.LEFT) {
            pairDevices(initialDeviceCandidate, nextDeviceCandidate);
        } else {
            pairDevices(nextDeviceCandidate, initialDeviceCandidate);
        }
    }

    @Override
    public void registerBroadcastReceivers() {
        final IntentFilter bluetoothIntents = new IntentFilter();
        bluetoothIntents.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        ContextCompat.registerReceiver(this, bluetoothReceiver, bluetoothIntents,
                                       ContextCompat.RECEIVER_EXPORTED);

        IntentFilter filter = new IntentFilter();
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(deviceChangedReceiver, filter);
    }

    @Override
    public void unregisterBroadcastReceivers() {
        AndroidUtils.safeUnregisterBroadcastReceiver(this, bluetoothReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceChangedReceiver);
    }

    private void pairDevices(GBDeviceCandidate leftCandidate, GBDeviceCandidate rightCandidate) {
        // Change the UI to pairing in progress mode.
        progressBar.setVisibility(View.VISIBLE);
        nextLensCandidatesListView.setVisibility(View.GONE);
        String displayName = G1Constants.getNameFromFullName(leftCandidate.getName()) + " " +
                             getString(R.string.watchface_dialog_widget_preset_left);
        hintTextView.setText(getString(R.string.pairing_even_realities_g1_working, displayName));

        // Set the global left and right for the callback to use later.
        leftDeviceCandidate = leftCandidate;
        rightDeviceCandidate = rightCandidate;

        // Need to remove the preferences, otherwise creating the devices will get confused.
        GBApplication.getDeviceSpecificSharedPrefs(leftDeviceCandidate.getDevice().getAddress())
                     .edit().remove(G1Constants.Side.getIndexKey())
                     .remove(G1Constants.Side.RIGHT.getNameKey())
                     .remove(G1Constants.Side.RIGHT.getAddressKey()).apply();
        GBApplication.getDeviceSpecificSharedPrefs(rightDeviceCandidate.getDevice().getAddress())
                     .edit().remove(G1Constants.Side.getIndexKey())
                     .remove(G1Constants.Side.LEFT.getNameKey())
                     .remove(G1Constants.Side.LEFT.getAddressKey()).apply();

        // Bond the left device. When it is completed, onBondingComplete() will be called which will
        // bond the right.
        currentBondingCandidate = leftDeviceCandidate;
        nextBondingStep();
    }

    private void nextBondingStep() {
        synchronized (stateLock) {
            switch (state) {
                case STARTING: {
                    state = BondingState.WAITING_ON_LEFT_BOND;
                    // Initiate the connection to the left device.
                    GBDevice device =
                            DeviceHelper.getInstance().toSupportedDevice(currentBondingCandidate);
                    GBApplication.deviceService(device).disconnect();
                    GBApplication.deviceService(device).connect(true);
                    return;
                }
                case WAITING_ON_LEFT_BOND: {
                    // Update the UI to reflect that we are bonding the right device now.
                    String displayName =
                            G1Constants.getNameFromFullName(rightDeviceCandidate.getName()) +
                            " " + getString(R.string.watchface_dialog_widget_preset_right);
                    hintTextView.setText(
                            getString(R.string.pairing_even_realities_g1_working, displayName));
                    state = BondingState.WAITING_ON_RIGHT_BOND;

                    // Initiate the connection to the right device.
                    currentBondingCandidate = rightDeviceCandidate;
                    GBDevice device =
                            DeviceHelper.getInstance().toSupportedDevice(currentBondingCandidate);
                    GBApplication.deviceService(device).disconnect();
                    GBApplication.deviceService(device).connect(true);
                    return;
                }
                case WAITING_ON_RIGHT_BOND: {
                    state = BondingState.WAITING_FOR_FIRST_DISCONNECT;

                    // Add the right device info to the left device's prefs and then mark it as the
                    // parent.
                    GBApplication.getDeviceSpecificSharedPrefs(
                                         leftDeviceCandidate.getDevice().getAddress()).edit()
                         .putInt(G1Constants.Side.getIndexKey(),
                                 G1Constants.Side.LEFT.getDeviceIndex())
                         .putString(G1Constants.Side.RIGHT.getNameKey(),
                                    rightDeviceCandidate.getName())
                         .putString(G1Constants.Side.RIGHT.getAddressKey(),
                                    rightDeviceCandidate.getDevice().getAddress())
                         .putBoolean(GBPrefs.DEVICE_CONNECT_BACK, true).apply();


                    // Add the left device info to the right device's pref and then mark it as the
                    // child.
                    GBApplication.getDeviceSpecificSharedPrefs(
                                         rightDeviceCandidate.getDevice().getAddress()).edit()
                         .putInt(G1Constants.Side.getIndexKey(),
                                 G1Constants.Side.RIGHT.getDeviceIndex())
                         .putString(G1Constants.Side.LEFT.getNameKey(),
                                    leftDeviceCandidate.getName())
                         .putString(G1Constants.Side.LEFT.getAddressKey(),
                                    leftDeviceCandidate.getDevice().getAddress())
                         .putBoolean(GBPrefs.DEVICE_CONNECT_BACK, true).apply();

                    GBDevice leftDevice =
                            DeviceHelper.getInstance().toSupportedDevice(leftDeviceCandidate);
                    GBDevice rightDevice =
                            DeviceHelper.getInstance().toSupportedDevice(rightDeviceCandidate);
                    GBApplication.deviceService(leftDevice).disconnect();
                    GBApplication.deviceService(rightDevice).disconnect();
                    return;
                }
                case WAITING_FOR_FIRST_DISCONNECT:
                    state = BondingState.WAITING_FOR_SECOND_DISCONNECT;
                    return;
                case WAITING_FOR_SECOND_DISCONNECT: {
                    // Update the message on the UI.
                    hintTextView.setText(
                            getString(R.string.pairing_even_realities_g1_final_connect));

                    GBDevice leftDevice =
                            DeviceHelper.getInstance().toSupportedDevice(leftDeviceCandidate);
                    if (leftDevice.getDeviceInfo(G1Constants.Side.RIGHT.getAddressKey()) ==
                        null ||
                        leftDevice.getDeviceInfo(G1Constants.Side.RIGHT.getNameKey()) ==
                        null) {
                        GB.toast(getContext(),
                                 getString(R.string.pairing_even_realities_g1_invalid_device,
                                           leftDevice.getAddress()), Toast.LENGTH_LONG, GB.ERROR);
                        onBondingComplete(false /* success */);
                        return;
                    }
                    state = BondingState.READY_TO_FINISH;
                    GBApplication.deviceService(leftDevice).connect();
                    return;
                }
                case READY_TO_FINISH:
                    onBondingComplete(true /* success */);
            }
        }
    }

    @Override
    public void onBondingComplete(boolean success) {
        // This function can be called from broadcast handlers, if we call finish() under the
        // context of a broadcast handler, the handler objects will be deleted and things will crash
        // hard in the android Activity libraries. Instead of causing that, schedule a thread to
        // close things down.
        Looper mainLooper = Looper.getMainLooper();
        new Handler(mainLooper).post(() -> {
            if (success && state == BondingState.READY_TO_FINISH) {
                setResult(RESULT_OK, null);
                finish();
            } else {
                // On error, just exit. There will be a toast from the bonding code to says what went wrong.
                finish();
            }
        });
    }


    private final class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (!Objects.requireNonNull(intent.getAction())
                        .equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                return;
            }

            LOG.debug("ACTION_BOND_STATE_CHANGED");
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) {
                return;
            }

            final int bondState =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
            LOG.debug("{} | {} Bond state: {}", intent, device.getAddress(), bondState);
            if (bondState != BluetoothDevice.BOND_BONDED) {
                return;
            }

            if (!device.getAddress().equals(currentBondingCandidate.getMacAddress())) {
                // We got a callback from the wrong device. This shouldn't be possible.
                GB.toast(getContext(), getString(R.string.pairing_even_realities_g1_invalid_device,
                                                 device.getAddress()), Toast.LENGTH_LONG, GB.ERROR);
                ((BondingInterface) context).onBondingComplete(false);
                return;
            }

            synchronized (stateLock) {
                if (state == BondingState.WAITING_ON_LEFT_BOND ||
                    state == BondingState.WAITING_ON_RIGHT_BOND) {
                    nextBondingStep();
                }
            }
        }
    }

    private final class DeviceChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (!GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                return;
            }

            GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
            if (device == null) {
                return;
            }


            // Ignore devices that we are not trying to pair.
            if (!(device.getAddress().equals(leftDeviceCandidate.getMacAddress()) ||
                  device.getAddress().equals(rightDeviceCandidate.getMacAddress()))) {
                return;
            }

            LOG.info("Device changed with intent {} | {} | {}", intent, device, state);

            synchronized (stateLock) {
                if ((state == BondingState.WAITING_ON_LEFT_BOND ||
                     state == BondingState.WAITING_ON_RIGHT_BOND) && device.isConnected() &&
                    currentBondingCandidate.isBonded()) {
                    // Device was already bonded, the BLE callback will never be called, so we need
                    // to advance to the next step. This step was a no-op anyway.
                    nextBondingStep();
                } else if ((state == BondingState.WAITING_FOR_FIRST_DISCONNECT ||
                            state == BondingState.WAITING_FOR_SECOND_DISCONNECT) &&
                           !device.isConnected()) {
                    nextBondingStep();
                } else if (state == BondingState.READY_TO_FINISH && device.isConnected()) {
                    ItemWithDetails right_name =
                            device.getDeviceInfo(G1Constants.Side.RIGHT.getNameKey());
                    ItemWithDetails right_address =
                            device.getDeviceInfo(G1Constants.Side.RIGHT.getAddressKey());
                    if (right_name != null && !right_name.getDetails().isEmpty() &&
                        right_address != null && !right_address.getDetails().isEmpty() &&
                        right_address.getDetails().equals(rightDeviceCandidate.getMacAddress())) {
                        nextBondingStep();
                    }
                }
            }
        }
    }
}
