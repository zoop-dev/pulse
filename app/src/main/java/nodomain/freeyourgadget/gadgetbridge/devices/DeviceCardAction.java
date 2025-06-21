package nodomain.freeyourgadget.gadgetbridge.devices;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public interface DeviceCardAction {
    @DrawableRes
    int getIcon(final GBDevice device);

    String getDescription(final GBDevice device, final Context context);

    @Nullable
    default String getLabel(final GBDevice device, final Context context) {
        return null;
    }

    default boolean isVisible(final GBDevice device) {
        return device.isConnected();
    }

    void onClick(final GBDevice device, final Context context);
}
