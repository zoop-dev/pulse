package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public enum BatteryStatus {
    New(1, R.string.battery_status_new),
    Good(2, R.string.battery_status_good),
    Ok(3, R.string.battery_status_ok),
    Low(4, R.string.battery_status_low),
    Critical(5, R.string.battery_status_critical),
    Charging(6, R.string.battery_status_charging),
    Unknown(7, R.string.battery_status_unknown),
    ;

    public final int id;
    @StringRes
    public final int nameResId;

    BatteryStatus(final int i, @StringRes final int nameRes) {
        id = i;
        nameResId = nameRes;
    }

    @Override
    public String toString() {
        if (0 != nameResId) {
            try {
                Context context = GBApplication.getContext();
                return context.getString(nameResId);
            } catch (final Throwable t) {

            }
        }
        return name();
    }
}
