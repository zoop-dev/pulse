package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public enum WaterType {
    Fresh(0, R.string.water_type_fresh),
    Salt(1, R.string.water_type_salt),
    En13319(2, R.string.water_type_en13319),
    Custom(3, R.string.water_type_custom),
    ;

    public final int id;

    @StringRes
    public final int nameResId;

    WaterType(final int i, @StringRes final int name) {
        id = i;
        nameResId = name;
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
