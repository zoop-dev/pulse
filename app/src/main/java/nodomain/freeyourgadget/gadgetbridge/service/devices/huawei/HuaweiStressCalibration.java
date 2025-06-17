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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.os.CountDownTimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiStressParser;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.HrRriTest;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.stress.HuaweiStressHRVCalculation;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.stress.HuaweiStressScoreCalculation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendHROpenCloseRequest;

public class HuaweiStressCalibration {

    public interface HuaweiStressCalibrateCallback {
        void onFinish(HuaweiStressParser.StressData stressData);

        void onProgress(long j);

        void onError();
    }

    private static final Logger LOG = LoggerFactory.getLogger(HuaweiStressCalibration.class);

    private final HuaweiSupportProvider supportProvider;

    private RriDataReceiver rriDataReceiver;

    private HuaweiStressCalibrateCallback callback = null;

    private List<Integer> rriData = new ArrayList<>();
    private List<Integer> sqiData = new ArrayList<>();

    private long startTime = 0;
    private long endTime = 0;

    private CountDownTimer measureTimer = null;

    private static class RriDataReceiver extends Request {

        List<HrRriTest.RriData.Response.rriSqiData> data;

        public RriDataReceiver(HuaweiSupportProvider supportProvider) {
            super(supportProvider);
        }

        @Override
        public boolean handleResponse(HuaweiPacket response) {
            if ((response.serviceId == HrRriTest.id && response.commandId == HrRriTest.RriData.id)) {
                receivedPacket = response;
                return true;
            }
            return false;
        }

        @Override
        public boolean autoRemoveFromResponseHandler() {
            // This needs to be removed manually
            return false;
        }

        @Override
        protected void processResponse() throws ResponseParseException {
            if (this.receivedPacket instanceof HrRriTest.RriData.Response) {
                HrRriTest.RriData.Response response = (HrRriTest.RriData.Response) this.receivedPacket;
                data = response.containers;
            } else {
                throw new ResponseTypeMismatchException(this.receivedPacket, HrRriTest.RriData.class);
            }
        }
    }

    public HuaweiStressCalibration(HuaweiSupportProvider supportProvider) {
        this.supportProvider = supportProvider;
    }

    private void openOrClose(byte type) {
        try {
            SendHROpenCloseRequest req = new SendHROpenCloseRequest(this.supportProvider, type);
            req.doPerform();
        } catch (IOException e) {
            LOG.error("Failed to SendHROpenCloseRequest", e);
        }
    }

    public boolean startMeasurements(HuaweiStressCalibrateCallback callback) {
        if (this.measureTimer != null) {
            // measurement in progress
            return false;
        }

        this.callback = callback;

        if (rriDataReceiver == null) {
            // We can only init fileDataReceiver if the device is already connected
            rriDataReceiver = new RriDataReceiver(supportProvider);
            rriDataReceiver.setFinalizeReq(new Request.RequestCallback() {
                @Override
                public void call() {
                    for (int i = 0; i < rriDataReceiver.data.size(); i++) {
                        HrRriTest.RriData.Response.rriSqiData d = rriDataReceiver.data.get(i);
                        if (d.sqi != 0) {
                            rriData.add((int) d.rri);
                            sqiData.add((int) d.sqi);
                        }
                    }
                }

                @Override
                public void handleException(Request.ResponseParseException e) {
                    stopMeasurements(true);
                }
            });
        }
        this.supportProvider.addInProgressRequest(rriDataReceiver);

        rriData.clear();
        sqiData.clear();

        startTime = System.currentTimeMillis();
        endTime = 0;
        openOrClose((byte) 3);

        this.measureTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long j) {
                if (callback != null) {
                    callback.onProgress(j);
                }
            }

            @Override
            public void onFinish() {
                stopMeasurements(false);
            }
        };
        this.measureTimer.start();
        return true;
    }

    void stopMeasurements(boolean error) {
        openOrClose((byte) 4);
        endTime = System.currentTimeMillis();
        CountDownTimer timer = this.measureTimer;
        if (timer != null) {
            timer.cancel();
            this.measureTimer = null;
        }
        this.supportProvider.removeInProgressRequests(rriDataReceiver);
        if (error) {
            if (callback != null) {
                callback.onError();
            }
            return;
        }
        LOG.info("startTime = {}", startTime);
        LOG.info("endTime = {}", endTime);
        LOG.info("len = {}", rriData.size());
        LOG.info("rri data = {}", Arrays.toString(rriData.toArray()));
        LOG.info("sqi data = {}", Arrays.toString(sqiData.toArray()));
        int signalTime = (int) (endTime - startTime) / 1000;

        HuaweiStressHRVCalculation hrvCalc = new HuaweiStressHRVCalculation();
        float[] hvrParams = hrvCalc.calculateStressHRVParameters(rriData, sqiData, signalTime);
        if (hvrParams == null || hvrParams.length != 10) {
            if (callback != null) {
                callback.onError();
            }
            return;
        }
        LOG.info("hvrParams = {}", Arrays.toString(hvrParams));

        float scoreFactor = HuaweiStressScoreCalculation.calculateScoreFactor(hvrParams);
        byte stressScore = HuaweiStressScoreCalculation.calculateNormalizedFinalScore(scoreFactor);

        LOG.info("Stress Score = {}", stressScore);

        HuaweiStressParser.StressData stressData = new HuaweiStressParser.StressData();

        for (float p : hvrParams) {
            stressData.features.add(p);
        }
        // 10 and 11 elements are always 0
        stressData.features.add(0.0F);
        stressData.features.add(0.0F);

        stressData.startTime = startTime;
        stressData.endTime = endTime;
        stressData.score = stressScore;
        stressData.scoreFactor = scoreFactor;
        stressData.level = HuaweiStressScoreCalculation.calculateLevel(stressScore);

        if (this.callback != null) {
            callback.onFinish(stressData);
        }
    }


}
