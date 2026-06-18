package nodomain.freeyourgadget.gadgetbridge.devices.soundbrenner;

import androidx.annotation.NonNull;

import android.content.Context;

import java.util.regex.Pattern;
import java.util.Arrays;
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
    // Start/Stop buttons in gadget card
    // -------------------------------------------------------------------------

    @Override
    public List<DeviceCardAction> getCustomActions() {
        return Arrays.asList(

            // START
            new DeviceCardAction() {
                @Override
                public int getIcon(@NonNull final GBDevice device) {
                    return R.drawable.ic_play;
                }

                @NonNull
                @Override
                public String getDescription(@NonNull final GBDevice device,
                                             @NonNull final Context context) {
                    return context.getString(R.string.soundbrenner_action_start);
                }

                @Override
                public void onClick(@NonNull final GBDevice device,
                                    @NonNull final Context context) {
                    GBApplication.deviceService(device)
                            .onSendConfiguration(
                                    SoundbrennerConstants.PREF_METRONOME_RUNNING + "_start");
                }
            },

            // STOP
            new DeviceCardAction() {
                @Override
                public int getIcon(@NonNull final GBDevice device) {
                    return R.drawable.ic_stop;
                }

                @NonNull
                @Override
                public String getDescription(@NonNull final GBDevice device,
                                             @NonNull final Context context) {
                    return context.getString(R.string.soundbrenner_action_stop);
                }

                @Override
                public void onClick(@NonNull final GBDevice device,
                                    @NonNull final Context context) {
                    GBApplication.deviceService(device)
                            .onSendConfiguration(
                                    SoundbrennerConstants.PREF_METRONOME_RUNNING + "_stop");
                }
            }
        );
    }
}
