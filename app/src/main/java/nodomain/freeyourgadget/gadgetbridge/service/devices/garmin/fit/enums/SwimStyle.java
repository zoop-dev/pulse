package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public enum SwimStyle {
    FREESTYLE(0, R.string.freestyle),
    BACKSTROKE(1, R.string.backstroke),
    BREASTSTROKE(2, R.string.breaststroke),
    BUTTERFLY(3, R.string.swim_style_butterfly),
    DRILL(4, R.string.swim_style_drill),
    MIXED(5, R.string.swim_style_mixed),
    ;

    public final int id;
    @StringRes
    public final int nameResId;

    SwimStyle(final int i, @StringRes final int nameRes) {
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
