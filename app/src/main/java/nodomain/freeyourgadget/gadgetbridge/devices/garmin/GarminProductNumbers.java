package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.FitDevice;

// source: https://raw.githubusercontent.com/muktihari/fit/master/profile/typedef/garmin_product_gen.go
public final class GarminProductNumbers {
    private GarminProductNumbers() {
    }

    private static final Map<Integer, DeviceType> mProductNumbers = new HashMap<>(200);

    @Nullable
    public static DeviceType getDeviceType(final int productNumber) {
        return mProductNumbers.get(productNumber);
    }

    static {
        for (FitDevice device : FitDevice.values()) {
            if (device.manufacturer == 1 && device.type != null) {
                mProductNumbers.put(device.product, device.type);
            }
        }
    }
}
