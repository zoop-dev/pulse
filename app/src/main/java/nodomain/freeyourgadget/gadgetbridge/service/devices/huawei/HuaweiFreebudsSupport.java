/*  Copyright (C) 2024-2025 Martin.JM, Ilya Nikitenkov

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiFreebudsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiHeadphonesCapabilities;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.HeadphoneHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetBetterAudioQualityRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetProductInformationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetANCModeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetAudioModeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetBetterAudioQualityRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetVoiceBoostRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetPauseWhenRemovedFromEarRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

// TODO: Move from HuaweiBRSupport to AbstractBTBRDeviceSupport
public class HuaweiFreebudsSupport extends HuaweiBRSupport implements HeadphoneHelper.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiFreebudsSupport.class);

    private HeadphoneHelper headphoneHelper;

    public HuaweiFreebudsSupport() {
        super();
        addSupportedService(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
    }

    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        super.setContext(gbDevice, btAdapter, context);
        headphoneHelper = new HeadphoneHelper(getContext(), getDevice(), this);
    }

    protected void initializeDeviceConfigure() {
        try {
            HuaweiFreebudsCoordinator coordinator = (HuaweiFreebudsCoordinator) this.gbDevice.getDeviceCoordinator();
            if (coordinator.supports(this.gbDevice, HuaweiHeadphonesCapabilities.BetterAudioQuality)) {
                GetBetterAudioQualityRequest req = new GetBetterAudioQualityRequest(super.getSupportProvider());
                req.doPerform();
            }

        } catch (IOException e) {
            GB.toast(this.getContext(), "Final initialization of Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Final initialization of Huawei device failed", e);
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Huawei Freebuds init");

        super.getSupportProvider().setup(getDevice(), getContext());

        builder.setDeviceState(GBDevice.State.INITIALIZING);
        try {
            builder.setCallback(this);
            final GetProductInformationRequest deviceProductReq = new GetProductInformationRequest(super.getSupportProvider());
            deviceProductReq.setFinalizeReq(new Request.RequestCallback(getSupportProvider()) {
                @Override
                public void call() {
                    // This also (optionally) starts the battery polling
                    getSupportProvider().getBatteryLevel();
                    initializeDeviceConfigure();
                }
            });
            deviceProductReq.doPerform();
        } catch (IOException e) {
            LOG.error("Connection failed", e);
            GB.toast("Connection failed", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            if (headphoneHelper != null)
                headphoneHelper.dispose();
            super.dispose();
        }
    }

    @Override
    public void onSocketRead(byte[] data) {
        super.getSupportProvider().onSocketRead(data);
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        headphoneHelper.onSetCallState(callSpec);
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        headphoneHelper.onNotification(notificationSpec);
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        // Do nothing.
    }

    @Override
    public void onSendConfiguration(String config) {
        if (headphoneHelper.onSendConfiguration(config))
            return;

        try {
            switch (config) {
                case DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_INEAR:
                    new SetPauseWhenRemovedFromEarRequest(getSupportProvider()).doPerform();
                    break;
                case DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_AUDIOMODE:
                    new SetAudioModeRequest(getSupportProvider()).doPerform();
                    break;
                case DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_ANC_MODE:
                    new SetANCModeRequest(getSupportProvider()).doPerform();
                    break;
                case DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_VOICE_BOOST:
                    new SetVoiceBoostRequest(getSupportProvider()).doPerform();
                    break;
                case DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_BETTER_AUDIO_QUALITY:
                    new SetBetterAudioQualityRequest(getSupportProvider()).doPerform();
                    break;
                case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_ENABLE:
                    if (!GBApplication.getDevicePrefs(gbDevice).getBatteryPollingEnabled()) {
                        getSupportProvider().stopBatteryRunnerDelayed();
                        break;
                    }
                    // Fall through if enabled
                case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_INTERVAL:
                    if (!getSupportProvider().startBatteryRunnerDelayed()) {
                        GB.toast(getContext(), R.string.battery_polling_failed_start, Toast.LENGTH_SHORT, GB.ERROR);
                        LOG.error("Failed to start the battery polling");
                    }
                    break;

            }
        } catch (IOException e) {
            GB.toast(getContext(), "Configuration of Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Configuration of Huawei device failed", e);
        }
    }
}
