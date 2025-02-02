package nodomain.freeyourgadget.gadgetbridge.devices.evenrealities;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.DeviceCandidateAdapter;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities.G1DeviceConstants;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BondingInterface;
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

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

    // Variables used to determine the initial state. The user can select the left or right lens to
    // start the pairing so these are used to determine the other device that needs connection.
    private GBDeviceCandidate initialDeviceCandidate;
    private G1DeviceConstants.Side initialDeviceCandidateSide;

    // Variables used for tracking the bonding state of both devices. The bonding steps involve
    // setting the current target to left, then initiating bonding on the current target. When the
    // bond has completed, the current target is set to the right device then bonding is initiated
    // on the current target again. currentBondingCompleteFromCallback is used to differentiate
    // calls to onBondingComplete(). onBondingComplete() will be called prematurely by GB so we need
    // to ignore that call, however when the BLE api invokes ACTION_BOND_STATE_CHANGED, it is before
    // the device has been marked as bonded so it's impossible to tell who is calling
    // onBondingComplete() just from the device state.
    private GBDeviceCandidate currentBondingCandidate;
    private boolean currentBondingCompleteFromCallback;
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
    public String getMacAddress() {
        return currentBondingCandidate.getDevice().getAddress();
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
        String compositeDeviceName = G1DeviceConstants.getNameFromFullName(name);
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
                G1DeviceConstants.getSideFromFullName(initialDeviceCandidate.getName());
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
        if (initialDeviceCandidateSide == G1DeviceConstants.Side.LEFT) {
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
                        G1DeviceConstants.getNameFromFullName(nextCandidate.getName());
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

        // Setup the BLE callbacks so we get notified when the devices are done bonding.
        registerBroadcastReceivers();

        // If there is only one matching device, initiate pairing with it, no need to ask the user.
        if (nextLensCandidates.size() == 1) {
            if (initialDeviceCandidateSide == G1DeviceConstants.Side.LEFT) {
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
    public void onBondingComplete(boolean success) {
        // On error, just exit. There will be a toast from the bonding code to says what went wrong.
        if (!success) {
            finish();
        }

        // Left device is done, start pairing the right device. This function will be called again
        // when the right device is finished.
        if (currentBondingCandidate == leftDeviceCandidate && currentBondingCompleteFromCallback) {
            currentBondingCandidate = rightDeviceCandidate;
            currentBondingCompleteFromCallback = false;
            String displayName =
                    G1DeviceConstants.getNameFromFullName(rightDeviceCandidate.getName()) + " " +
                    getString(R.string.watchface_dialog_widget_preset_right);
            hintTextView.setText(
                    getString(R.string.pairing_even_realities_g1_working, displayName));
            BondingUtil.connectThenComplete(this, currentBondingCandidate);
        }

        // Both devices are bonded. Finish up.
        if (currentBondingCandidate == rightDeviceCandidate && currentBondingCompleteFromCallback) {
            // The initial connection prompts the bonding, but it will be a generic GATT connection.
            // Now that the device is bonded, we need to disconnect and reconnect one more time to
            // have full access to all GATT attributes.
            BondingUtil.attemptToFirstConnect(leftDeviceCandidate.getDevice());
            BondingUtil.attemptToFirstConnect(rightDeviceCandidate.getDevice());
            setResult(RESULT_OK, null);
            finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final GBDeviceCandidate nextDeviceCandidate = nextLensCandidates.get(position);
        // The user may have selected either the right or the left lens. We have both devices, pair
        // them as left and right.
        if (initialDeviceCandidateSide == G1DeviceConstants.Side.LEFT) {
            pairDevices(initialDeviceCandidate, nextDeviceCandidate);
        } else {
            pairDevices(nextDeviceCandidate, initialDeviceCandidate);
        }
    }

    private void pairDevices(GBDeviceCandidate leftCandidate, GBDeviceCandidate rightCandidate) {
        // Change the UI to pairing in progress mode.
        progressBar.setVisibility(View.VISIBLE);
        nextLensCandidatesListView.setVisibility(View.GONE);
        String displayName = G1DeviceConstants.getNameFromFullName(leftCandidate.getName()) + " " +
                             getString(R.string.watchface_dialog_widget_preset_left);
        hintTextView.setText(getString(R.string.pairing_even_realities_g1_working, displayName));

        // Set the global left and right for the callback to use later.
        leftDeviceCandidate = leftCandidate;
        rightDeviceCandidate = rightCandidate;

        // Bond the left device. When it is completed, onBondingComplete() will be called which will
        // bond the right.
        currentBondingCandidate = leftDeviceCandidate;
        currentBondingCompleteFromCallback = false;
        BondingUtil.connectThenComplete(this, currentBondingCandidate);
    }

    @Override
    public void registerBroadcastReceivers() {
        final IntentFilter bluetoothIntents = new IntentFilter();
        bluetoothIntents.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        ContextCompat.registerReceiver(this, bluetoothReceiver, bluetoothIntents,
                                       ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public void unregisterBroadcastReceivers() {
        AndroidUtils.safeUnregisterBroadcastReceiver(this, bluetoothReceiver);
    }

    private final class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (Objects.requireNonNull(intent.getAction())
                       .equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                LOG.debug("ACTION_BOND_STATE_CHANGED");
                final BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null) {
                    final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                                             BluetoothDevice.BOND_NONE);
                    LOG.debug("{} Bond state: {}", device.getAddress(), bondState);

                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        if (device.getAddress().equals(currentBondingCandidate.getMacAddress())) {
                            currentBondingCompleteFromCallback = true;
                            ((BondingInterface) context).onBondingComplete(true);
                        } else {
                            // We got a callback from the wrong device. This shouldn't be possible.
                            GB.toast(getContext(),
                                     getString(R.string.pairing_even_realities_g1_invalid_device,
                                               device.getAddress()), Toast.LENGTH_LONG, GB.ERROR);
                            ((BondingInterface) context).onBondingComplete(false);
                        }
                    }
                }
            }
        }
    }
}
