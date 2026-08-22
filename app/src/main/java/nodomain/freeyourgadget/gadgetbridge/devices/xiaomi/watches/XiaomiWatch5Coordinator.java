/*  Copyright (C) 2026 Baptiste Debut

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watches;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_XIAOMI_DEVICE_ID;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/// #6614
/// Xiaomi Watch 5 (miwear.watch.o61w). Runs Wear OS, but still speaks the Xiaomi protobuf protocol over SPP.
public class XiaomiWatch5Coordinator extends XiaomiCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_xiaomi_watch_5;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // Unlike the other Xiaomi watches, this one runs Wear OS, which replaces the
        // usual "Xiaomi Watch 5 ABCD" bluetooth name with a localized, owner-specific
        // one (eg. "Xiaomi Watch 5 appartenant a <owner>" on a french phone), so we
        // can only match on the prefix. The lookahead keeps a future "Xiaomi Watch 5 Pro"
        // or "Lite" from being matched as a plain Watch 5.
        return Pattern.compile("^Xiaomi Watch 5(?! (Pro|Lite))( .*)?$");
    }

    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.BT_CLASSIC;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miwatch;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }

    // The watch reports resting/max/min/average heart rate in its daily summary, which
    // DailySummaryParser already persists.
    @Override
    public boolean supportsHeartRateStats(@NonNull final GBDevice device) {
        return true;
    }

    // Media playback is Wear OS' own, so the watch has no Xiaomi media screen to drive - it
    // never answers on the music channel.
    @Override
    public boolean supportsMusicInfo(@NonNull final GBDevice device) {
        return false;
    }

    // World clocks and contacts are deliberately not enabled: the watch answers the world
    // clock get with an empty list but ignores the set, and it never answers on the phonebook
    // channel at all. Both are Wear OS' own business on this device.

    // In addition to the auth key, this watch needs the identity of the phone that was
    // registered with it when it was paired with the vendor app - see XiaomiAuthService. It is
    // asked for while pairing, and can be corrected afterwards from the authentication settings.
    @Override
    public String getSecondaryAuthKeyPref() {
        return PREF_XIAOMI_DEVICE_ID;
    }

    @Override
    public int getSecondaryAuthKeyHint() {
        return R.string.pref_title_xiaomi_device_id;
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return ArrayUtils.add(
                super.getSupportedDeviceSpecificAuthenticationSettings(),
                R.xml.devicesettings_xiaomi_device_id
        );
    }

    // Apps and watch faces are managed by Wear OS and the Play Store, not by Xiaomi's RPK
    // quick app and watch face store protocol. The watch does not answer the watch face
    // (type 4) or RPK (type 20) list requests, so the app manager would only ever be empty.
    @Override
    public boolean supportsAppsManagement(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsInstalledAppManagement(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsAppListFetching(@NonNull final GBDevice device) {
        return false;
    }
}
