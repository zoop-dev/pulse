package nodomain.freeyourgadget.gadgetbridge.devices.huawei.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.fragment.app.Fragment;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiStressCalibrationActivity extends AbstractGBActivity {
    private GBDevice device;


    protected String fragmentTag() {
        return HuaweiStressCalibrationFragment.FRAGMENT_TAG;
    }


    protected Fragment newFragment() {
        return HuaweiStressCalibrationFragment.newInstance(device);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);

        setContentView(R.layout.activity_device_settings);

        if (device == null || !device.isInitialized()) {
            GB.toast(getString(R.string.watch_not_connected), Toast.LENGTH_SHORT, GB.INFO);
            finish();
        }

        if (savedInstanceState == null) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag());
            if (fragment == null) {
                fragment = newFragment();
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment, fragmentTag())
                    .commit();
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // Simulate a back press, so that we don't actually exit the activity when
            // in a nested PreferenceScreen
            this.getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
