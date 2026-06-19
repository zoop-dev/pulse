package nodomain.freeyourgadget.gadgetbridge.devices.soundbrenner;

import androidx.annotation.NonNull;

import android.content.Context;

import java.util.regex.Pattern;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundbrenner.SoundbrennerSupport;


public class SoundbrennerCoordinator extends AbstractBLEDeviceCoordinator {

    @Override
    public String getManufacturer() {
        return "Soundbrenner";
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Soundbrenner Core.*");
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return SoundbrennerSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_soundbrenner_core;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_notification;
    }

    @NonNull
    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceCoordinator.DeviceKind.WATCH;
    }

    // -------------------------------------------------------------------------
    // Device-specific settings
    // -------------------------------------------------------------------------

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_soundbrenner_core
        };
    }

    @NonNull
    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(@NonNull final GBDevice device) {
        return new SoundbrennerSettingsCustomizer();
    }

    // -------------------------------------------------------------------------
    // Start/Stop button in gadget card
    // -------------------------------------------------------------------------
    //
    // The running state is mirrored into the device-specific SharedPreferences
    // by SoundbrennerSupport (see persistMetronomeRunning()), so a single
    // action can read it and flip its icon/label accordingly instead of
    // exposing two separate Start/Stop buttons.

    @Override
    public List<DeviceCardAction> getCustomActions() {
        return Collections.singletonList(
            new DeviceCardAction() {
                @Override
                public int getIcon(@NonNull final GBDevice device) {
                    return isMetronomeRunning(device) ? R.drawable.ic_stop : R.drawable.ic_play;
                }

                @NonNull
                @Override
                public String getDescription(@NonNull final GBDevice device,
                                             @NonNull final Context context) {
                    return isMetronomeRunning(device)
                            ? context.getString(R.string.stop)
                            : context.getString(R.string.start);
                }

                @Override
                public void onClick(@NonNull final GBDevice device,
                                    @NonNull final Context context) {
                    GBApplication.deviceService(device)
                            .onSendConfiguration(
                                    SoundbrennerConstants.PREF_METRONOME_RUNNING + "_toggle");
                }

                private boolean isMetronomeRunning(@NonNull final GBDevice device) {
                    return GBApplication
                            .getDeviceSpecificSharedPrefs(device.getAddress())
                            .getBoolean(SoundbrennerConstants.PREF_METRONOME_RUNNING, false);
                }
            }
        );
    }
}
