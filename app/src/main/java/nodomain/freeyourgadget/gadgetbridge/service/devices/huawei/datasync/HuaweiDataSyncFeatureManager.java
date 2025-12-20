/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.datasync;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiUploadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFileUploadInfo;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiDataSyncFeatureManager implements HuaweiDataSyncCommon.DataCallback {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiDataSyncFeatureManager.class);

    private final HuaweiSupportProvider support;

    // Increase each time when futures changed
    private static final int version = 1;

    public static final String SRC_PKG_NAME = "hw.unitedevice.configManager";
    public static final String PKG_NAME = "featureManager";

    public HuaweiDataSyncFeatureManager(HuaweiSupportProvider support) {
        LOG.info("HuaweiDataSyncFeatureManager");
        this.support = support;
        this.support.getHuaweiDataSyncManager().registerCallback(PKG_NAME, this);
    }

    private boolean sendCommonFeaturesData(byte configAction, byte[] configData) {

        LOG.info("HuaweiDataSyncFeatureManager sendCommonFeaturesData");
        HuaweiDataSyncCommon.ConfigCommandData data = new HuaweiDataSyncCommon.ConfigCommandData();
        HuaweiDataSyncCommon.ConfigData featuresConfigData = new HuaweiDataSyncCommon.ConfigData();

        featuresConfigData.configId = 900100007;
        featuresConfigData.configAction = configAction;
        featuresConfigData.configData = configData;
        List<HuaweiDataSyncCommon.ConfigData> list = new ArrayList<>();
        list.add(featuresConfigData);
        data.setConfigDataList(list);
        return this.support.getHuaweiDataSyncManager().sendConfigCommand(SRC_PKG_NAME, PKG_NAME, data);
    }

    private JsonObject getPwvDetectionFeature() {
        JsonObject res = new JsonObject();
        res.addProperty("devPackageName", "com.huawei.health.pwvdetection");
        res.addProperty("support", 1);
        JsonObject extInfo = new JsonObject();
        extInfo.addProperty("medical", false);
        res.add("extInfo", extInfo);
        return res;
    }

    private JsonObject getBloodPressureFeature() {
        JsonObject res = new JsonObject();
        res.addProperty("devPackageName", "com.huawei.health.bloodpressure");
        res.addProperty("support", 1);
        JsonObject extInfo = new JsonObject();
        JsonArray ABPM = new JsonArray();
        ABPM.add(135);
        ABPM.add(85);
        ABPM.add(120);
        ABPM.add(70);
        ABPM.add(130);
        ABPM.add(80);
        extInfo.add("ABPM", ABPM);
        extInfo.addProperty("local", 2);
        res.add("extInfo", extInfo);
        return res;
    }

    private JsonObject getEcgAnalysisFeature() {
        JsonObject res = new JsonObject();
        res.addProperty("devPackageName", "com.huawei.watch.health.ecganalysis");
        res.addProperty("support", 1);
        JsonObject extInfo = new JsonObject();
        extInfo.addProperty("medical", false);
        res.add("extInfo", extInfo);
        return res;
    }

    private JsonObject getHealthCheckFeature() {
        JsonObject res = new JsonObject();
        res.addProperty("devPackageName", "com.huawei.health.healthcheck");
        res.addProperty("support", 1);
        JsonObject extInfo = new JsonObject();
        extInfo.addProperty("checkItem", 23);
        res.add("extInfo", extInfo);
        return res;
    }

    private JsonObject getEmotionalFeature() {
        JsonObject res = new JsonObject();
        res.addProperty("devPackageName", "com.huawei.hmos.watch.emotional");
        res.addProperty("support", 1);
        return res;
    }

    private JsonObject getPlateauCareFeature() {
        JsonObject res = new JsonObject();
        res.addProperty("devPackageName", "com.huawei.hmos.watch.plateaucare");
        res.addProperty("support", 1);
        JsonObject extInfo = new JsonObject();
        extInfo.addProperty("medical", false);
        res.add("extInfo", extInfo);
        return res;
    }

    private JsonObject getArrhythmiaFeature() {
        JsonObject res = new JsonObject();
        res.addProperty("devPackageName", "com.huawei.watch.health.arrhythmia");
        res.addProperty("support", 1);
        JsonObject extInfo = new JsonObject();
        extInfo.addProperty("medical", false);
        res.add("extInfo", extInfo);
        return res;
    }

    private String getFeaturesFileContent(final String countryCode) {
        JsonArray featureList = new JsonArray();

        featureList.add(getPwvDetectionFeature());
        featureList.add(getBloodPressureFeature());
        featureList.add(getEcgAnalysisFeature());
        featureList.add(getHealthCheckFeature());
        featureList.add(getEmotionalFeature());
        featureList.add(getPlateauCareFeature());
        featureList.add(getArrhythmiaFeature());

        JsonObject featureData = new JsonObject();

        featureData.add("featureList", featureList);
        featureData.addProperty("country", countryCode);
        featureData.addProperty("ver", version);
        featureData.addProperty("name", String.format(Locale.ROOT, "feature_%s_device", countryCode));

        return new Gson().toJson(featureData);
    }

    private String getNotifyJsonData(final String countryCode) {
        JsonArray filterCondition = new JsonArray();
        JsonObject filterConditionItem = new JsonObject();
        filterConditionItem.addProperty("filterCountry", countryCode);
        filterCondition.add(filterConditionItem);

        JsonArray fileData = new JsonArray();
        JsonObject fileDataItem = new JsonObject();
        fileDataItem.addProperty("version", version);
        fileDataItem.addProperty("fileName", String.format(Locale.ROOT, "feature_%s_device.txt", countryCode));
        fileData.add(fileDataItem);

        JsonObject responseData = new JsonObject();
        responseData.add("filterCondition", filterCondition);
        responseData.addProperty("configName", "com.huawei.health_deviceFeature_config");
        responseData.add("fileData", fileData);

        return new Gson().toJson(responseData);
    }

    private void sendFeaturesFile() {
        LOG.info("HuaweiDataSyncFeatureManager Send feature file upload info");

        final String countryCode = support.getDeviceState().getCountryCode(support.getDevice());

        final String json = getFeaturesFileContent(countryCode);
        LOG.info("feature file content: {}", json);

        HuaweiUploadManager.FileUploadInfo fileInfo = new HuaweiUploadManager.FileUploadInfo();

        fileInfo.setFileType((byte) 0x0f);
        fileInfo.setFileName(String.format(Locale.ROOT, "feature_%s_device.txt", countryCode));
        fileInfo.setBytes(json.getBytes(StandardCharsets.UTF_8));

        fileInfo.setFileUploadCallback(new HuaweiUploadManager.FileUploadCallback() {
            @Override
            public void onUploadStart() {
            }

            @Override
            public void onUploadProgress(int progress) {
            }

            @Override
            public void onUploadComplete() {
                final String json = getNotifyJsonData(countryCode);
                LOG.info("Notify file content: {}", json);
                sendCommonFeaturesData((byte) 1, json.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public void onError(int code) {
                if (support.getDevice().isBusy()) {
                    support.getDevice().unsetBusyTask();
                    support.getDevice().sendDeviceUpdateIntent(support.getContext());
                }
            }
        });

        HuaweiUploadManager huaweiUploadManager = support.getUploadManager();

        huaweiUploadManager.setFileUploadInfo(fileInfo);

        try {
            SendFileUploadInfo sendFileUploadInfo = new SendFileUploadInfo(support, huaweiUploadManager);
            sendFileUploadInfo.doPerform();
        } catch (IOException e) {
            LOG.error("Failed to send file upload info", e);
        }
    }


    @Override
    public void onConfigCommand(HuaweiDataSyncCommon.ConfigCommandData data) {
        //TODO: handle this
        LOG.info("HuaweiDataSyncConfigManager code: {}", data.getCode());
        if (data.getConfigDataList() != null && !data.getConfigDataList().isEmpty()) {
            HuaweiDataSyncCommon.ConfigData dt = data.getConfigDataList().get(0);
            LOG.info("HuaweiDataSyncConfigManager config Action: {}, ID: {}, Data: {}", dt.configAction, dt.configId, GB.hexdump(dt.configData));
            if (dt.configAction == 2) {
                if (sendCommonFeaturesData((byte) 2, new byte[]{0x30, 0x31})) {
                    sendFeaturesFile();
                }
            }
        }
    }

    @Override
    public void onEventCommand(HuaweiDataSyncCommon.EventCommandData data) {

    }

    @Override
    public void onDataCommand(HuaweiDataSyncCommon.DataCommandData data) {

    }

    @Override
    public void onDictDataCommand(HuaweiDataSyncCommon.DictDataCommandData data) {

    }
}
