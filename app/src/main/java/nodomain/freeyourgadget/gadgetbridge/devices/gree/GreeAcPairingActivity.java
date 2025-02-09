package nodomain.freeyourgadget.gadgetbridge.devices.gree;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.gree.GreeAcPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class GreeAcPairingActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(GreeAcPairingActivity.class);

    public static final String ACTION_BIND_STATUS = "nodomain.freeyourgadget.gadgetbridge.gree.bind_status";
    public static final String EXTRA_BIND_KEY = "extra_bind_key";
    public static final String EXTRA_BIND_MESSAGE = "extra_bind_message";

    private GBDeviceCandidate deviceCandidate;

    private TextInputLayout textLayoutSsid;
    private TextInputLayout textLayoutPassword;
    private TextInputLayout textLayoutHost;
    private TextInputEditText editTextSsid;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextHost;
    private ProgressBar progressBar;
    private TextView pairResultTextView;
    private Button buttonPair;
    private Button buttonCopy;

    private GBDevice gbDevice;
    private String bindKey;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action == null) {
                return;
            }

            if (ACTION_BIND_STATUS.equals(action)) {
                final String bindMessage = intent.getStringExtra(EXTRA_BIND_MESSAGE);
                bindKey = intent.getStringExtra(EXTRA_BIND_KEY);

                progressBar.setVisibility(View.GONE);
                if ("1".equals(bindMessage)) {
                    pairResultTextView.setText(getString(R.string.gree_pair_status_success, String.valueOf(bindKey)));
                    buttonCopy.setVisibility(View.VISIBLE);
                } else {
                    pairResultTextView.setText(getString(R.string.gree_pair_status_failure, bindMessage));
                }

                pairResultTextView.setVisibility(View.VISIBLE);
                return;
            }

            if (GBDevice.ACTION_DEVICE_CHANGED.equals(action)) {
                final GBDevice actionDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (actionDevice == null || !actionDevice.getAddress().equals(deviceCandidate.getMacAddress())) {
                    return;
                }

                LOG.debug("Got device state: {}", actionDevice.getState());

                if (actionDevice.getState() == GBDevice.State.NOT_CONNECTED && buttonCopy.getVisibility() != View.VISIBLE) {
                    pairResultTextView.setText(getString(R.string.gree_pair_status_failure, actionDevice.getState().toString()));
                    pairResultTextView.setVisibility(View.VISIBLE);

                    editTextSsid.setEnabled(true);
                    editTextPassword.setEnabled(true);
                    editTextHost.setEnabled(true);
                    buttonPair.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            }

            LOG.error("Unknown action {}", action);
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        deviceCandidate = intent.getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);

        if (deviceCandidate == null) {
            GB.toast(this, "Device candidate missing", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        setContentView(R.layout.activity_gree_ac_pairing);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BIND_STATUS);
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        ContextCompat.registerReceiver(this, mReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        final TextView textPairInfo = findViewById(R.id.gree_pair_info);
        textLayoutSsid = findViewById(R.id.gree_pair_ssid_layout);
        textLayoutPassword = findViewById(R.id.gree_pair_password_layout);
        textLayoutHost = findViewById(R.id.gree_pair_host_layout);
        editTextSsid = findViewById(R.id.gree_pair_ssid_text);
        editTextPassword = findViewById(R.id.gree_pair_password_text);
        editTextHost = findViewById(R.id.gree_pair_host_text);
        progressBar = findViewById(R.id.gree_pair_progress_bar);
        pairResultTextView = findViewById(R.id.gree_pair_result);
        buttonPair = findViewById(R.id.gree_button_pair);
        buttonCopy = findViewById(R.id.gree_button_copy);

        textPairInfo.setText(getString(R.string.gree_pair_info, deviceCandidate.getName(), deviceCandidate.getMacAddress()));

        final DevicePrefs devicePrefs = new DevicePrefs(GBApplication.getDeviceSpecificSharedPrefs(deviceCandidate.getMacAddress()), gbDevice);
        editTextSsid.setText(devicePrefs.getString(GreeAcPrefs.PREF_SSID, ""));
        editTextPassword.setText(devicePrefs.getString(GreeAcPrefs.PREF_PASSWORD, ""));
        editTextHost.setText(devicePrefs.getString(GreeAcPrefs.PREF_HOST, ""));

        editTextSsid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                textLayoutSsid.setError(null);
            }
        });
        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                textLayoutPassword.setError(null);
            }
        });
        editTextHost.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                textLayoutHost.setError(null);
            }
        });

        buttonPair.setOnClickListener(v -> {
            savePrefs();

            final String ssid = editTextSsid.getText() != null ? editTextSsid.getText().toString() : "";
            if (ssid.isEmpty() || ssid.length() > 32) {
                textLayoutSsid.setError("Invalid SSID");
                return;
            }
            final String password = editTextPassword.getText() != null ? editTextPassword.getText().toString() : "";
            if (password.isEmpty() || password.length() < 8 || password.length() > 63) {
                textLayoutPassword.setError("Invalid password");
                return;
            }
            final String host = editTextHost.getText() != null ? editTextHost.getText().toString() : "";
            if (host.isEmpty()) {
                textLayoutHost.setError("Invalid host");
                return;
            }

            editTextSsid.setEnabled(false);
            editTextPassword.setEnabled(false);
            editTextHost.setEnabled(false);
            buttonPair.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            pairResultTextView.setVisibility(View.GONE);

            GBApplication.deviceService().disconnect();
            gbDevice = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate.getDevice());
            GBApplication.deviceService(gbDevice).connect(true);
        });

        buttonCopy.setOnClickListener(v -> {
            final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            final String clipboardData = getString(
                    R.string.gree_pair_clipboard,
                    deviceCandidate.getName(),
                    deviceCandidate.getMacAddress(),
                    bindKey
            );
            final ClipData clip = ClipData.newPlainText(deviceCandidate.getName(), clipboardData);
            clipboard.setPrimaryClip(clip);
            GB.toast(getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG, GB.INFO);
        });

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(deviceCandidate.getName());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gbDevice != null) {
            GBApplication.deviceService(gbDevice).disconnect();
        }
        unregisterReceiver(mReceiver);
    }

    private void savePrefs() {
        final DevicePrefs devicePrefs = new DevicePrefs(GBApplication.getDeviceSpecificSharedPrefs(deviceCandidate.getMacAddress()), gbDevice);
        final SharedPreferences.Editor editor = devicePrefs.getPreferences().edit();
        if (editTextSsid != null && editTextSsid.getText() != null) {
            editor.putString(GreeAcPrefs.PREF_SSID, editTextSsid.getText().toString());
        }
        if (editTextPassword != null && editTextPassword.getText() != null) {
            editor.putString(GreeAcPrefs.PREF_PASSWORD, editTextPassword.getText().toString());
        }
        if (editTextHost != null && editTextHost.getText() != null) {
            editor.putString(GreeAcPrefs.PREF_HOST, editTextHost.getText().toString());
        }
        editor.apply();
    }
}
