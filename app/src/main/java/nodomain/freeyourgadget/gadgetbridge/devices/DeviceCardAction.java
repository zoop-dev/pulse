/*  Copyright (C) 2025-2026 José Rebelo, Thomas Kuehne

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.CameraActivity;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public interface DeviceCardAction {
    @DrawableRes
    int getIcon(@NonNull final GBDevice device);

    @NonNull
    String getDescription(@NonNull final GBDevice device, @NonNull final Context context);

    @Nullable
    default String getLabel(@NonNull final GBDevice device, @NonNull final Context context) {
        return null;
    }

    default boolean isVisible(@NonNull final GBDevice device) {
        return device.isConnected();
    }

    void onClick(@NonNull final GBDevice device, @NonNull final Context context);

    /**
     * Creates an action that launches an Activity, passing the device as {@link GBDevice#EXTRA_DEVICE}.
     */
    static DeviceCardAction forActivity(@DrawableRes final int icon, @StringRes final int description, @NonNull final Class<?> activityClass) {
        return new ActivityAction(icon, description, activityClass);
    }

    /**
     * Creates an action that sends a local broadcast, passing the device as {@link GBDevice#EXTRA_DEVICE}.
     */
    static DeviceCardAction forBroadcast(@DrawableRes final int icon, @StringRes final int description, @NonNull final String intentAction) {
        return new BroadcastAction(icon, description, intentAction);
    }

    /**
     * Creates an action that calls {@link nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport#onSendConfiguration}
     * on the device service with the given key.
     */
    static DeviceCardAction forConfiguration(@DrawableRes final int icon, @StringRes final int description, @NonNull final String configKey) {
        return new ConfigurationAction(icon, description, configKey);
    }

    class ActivityAction implements DeviceCardAction {
        private final int icon;
        private final int description;
        private final Class<?> activityClass;

        ActivityAction(@DrawableRes final int icon,
                       @StringRes final int description,
                       @NonNull final Class<?> activityClass) {
            this.icon = icon;
            this.description = description;
            this.activityClass = activityClass;
        }

        @Override
        public int getIcon(@NonNull final GBDevice device) {
            return icon;
        }

        @NonNull
        @Override
        public String getDescription(@NonNull final GBDevice device,
                                     @NonNull final Context context) {
            return context.getString(description);
        }

        @Override
        public void onClick(@NonNull final GBDevice device,
                            @NonNull final Context context) {
            final Intent intent = new Intent(context, activityClass);
            intent.putExtra(GBDevice.EXTRA_DEVICE, device);
            context.startActivity(intent);
        }
    }

    class BroadcastAction implements DeviceCardAction {
        private final int icon;
        private final int description;
        private final String intentAction;

        BroadcastAction(@DrawableRes final int icon,
                        @StringRes final int description,
                        @NonNull final String intentAction) {
            this.icon = icon;
            this.description = description;
            this.intentAction = intentAction;
        }

        @Override
        public int getIcon(@NonNull final GBDevice device) {
            return icon;
        }

        @NonNull
        @Override
        public String getDescription(@NonNull final GBDevice device,
                                     @NonNull final Context context) {
            return context.getString(description);
        }

        @Override
        public void onClick(@NonNull final GBDevice device,
                            @NonNull final Context context) {
            final Intent intent = new Intent(intentAction);
            intent.putExtra(GBDevice.EXTRA_DEVICE, device);
            intent.setPackage(context.getPackageName());
            context.sendBroadcast(intent);
        }
    }

    class ConfigurationAction implements DeviceCardAction {
        private final int icon;
        private final int description;
        private final String configKey;

        ConfigurationAction(@DrawableRes final int icon,
                            @StringRes final int description,
                            @NonNull final String configKey) {
            this.icon = icon;
            this.description = description;
            this.configKey = configKey;
        }

        @Override
        public int getIcon(@NonNull final GBDevice device) {
            return icon;
        }

        @NonNull
        @Override
        public String getDescription(@NonNull final GBDevice device,
                                     @NonNull final Context context) {
            return context.getString(description);
        }

        @Override
        public void onClick(@NonNull final GBDevice device,
                            @NonNull final Context context) {
            GBApplication.deviceService(device).onSendConfiguration(configKey);
        }
    }

    class CameraAction implements DeviceCardAction {
        @Override
        public int getIcon(@NonNull GBDevice device) {
            return R.drawable.ic_camera_remote;
        }

        @NonNull
        @Override
        public String getDescription(@NonNull GBDevice device,
                                     @NonNull Context context) {
            return context.getString(R.string.open_camera);
        }

        @Override
        public void onClick(@NonNull GBDevice device,
                            @NonNull Context context) {
            final Intent cameraIntent = new Intent(context, CameraActivity.class);
            cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            cameraIntent.putExtra(
                    CameraActivity.intentExtraEvent,
                    GBDeviceEventCameraRemote.eventToInt(GBDeviceEventCameraRemote.Event.OPEN_CAMERA)
            );
            context.startActivity(cameraIntent);
        }
    }
}
