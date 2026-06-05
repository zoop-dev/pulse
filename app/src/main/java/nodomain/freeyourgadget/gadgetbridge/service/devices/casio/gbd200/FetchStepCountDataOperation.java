/*  Copyright (C) 2026 Davide Gessa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbd200;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.entities.CasioGBX100ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FetchStepCountDataOperation extends AbstractBTLEOperation<CasioGBD200DeviceSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(FetchStepCountDataOperation.class);
    private static final int TIMEOUT_MS = 30_000;

    private final CasioGBD200DeviceSupport support;
    private byte mLastWrittenCmd = 0x00;
    private final Handler mTimeoutHandler = new Handler(Looper.getMainLooper());

    public FetchStepCountDataOperation(CasioGBD200DeviceSupport support) {
        super(support);
        this.support = support;
    }

    private void enableRequiredNotifications(boolean enable) {
        try {
            TransactionBuilder builder = performInitialized("enableRequiredNotifications");
            builder.setCallback(this);
            builder.notify(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID, enable);
            builder.notify(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID, enable);
            builder.queue();
        } catch (IOException e) {
            LOG.error("Error enabling required notifications", e);
        }
    }

    private void requestStepCountData() {
        mLastWrittenCmd = 0x00;
        try {
            TransactionBuilder builder = performInitialized("requestStepCountData");
            builder.setCallback(this);
            builder.writeLegacy(
                    getCharacteristic(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID),
                    new byte[]{0x00, 0x11, 0x00, 0x00, 0x00});
            builder.queue();
        } catch (IOException e) {
            LOG.error("Error requesting step count data", e);
        }
    }

    private void writeStepCountAck() {
        mLastWrittenCmd = 0x04;
        try {
            TransactionBuilder builder = performInitialized("writeStepCountAck");
            builder.setCallback(this);
            builder.writeLegacy(
                    getCharacteristic(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID),
                    new byte[]{0x04, 0x11, 0x00, 0x00, 0x00});
            builder.queue();
        } catch (IOException e) {
            LOG.error("Error writing step count ack — finishing anyway", e);
            enableRequiredNotifications(false);
            operationFinished();
        }
    }

    @Override
    protected void prePerform() throws IOException {
        super.prePerform();
        getDevice().setBusyTask(R.string.busy_task_fetch_steps, getContext());
        GB.updateTransferNotification(null,
                getContext().getString(R.string.busy_task_fetch_activity_data), true, 0,
                getContext());
    }

    @Override
    protected void doPerform() throws IOException {
        mTimeoutHandler.postDelayed(this::onTimeout, TIMEOUT_MS);
        enableRequiredNotifications(true);
        requestStepCountData();
    }

    private void onTimeout() {
        LOG.warn("FetchStepCountDataOperation timed out");
        GB.toast(getContext(), getContext().getString(R.string.busy_task_fetch_activity_data)
                + ": timeout", Toast.LENGTH_SHORT, GB.WARN);
        enableRequiredNotifications(false);
        operationFinished();
    }

    @Override
    protected void operationFinished() {
        if (operationStatus == OperationStatus.FINISHED) return;
        mTimeoutHandler.removeCallbacksAndMessages(null);
        LOG.info("FetchStepCountDataOperation finished");
        unsetBusy();
        GB.updateTransferNotification(null,
                getContext().getString(R.string.busy_task_fetch_activity_data), false, 100,
                getContext());
        GB.signalActivityDataFinish(getDevice());

        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null) {
            try {
                TransactionBuilder builder = performInitialized("finished");
                builder.setCallback(null);
                builder.sleep(0);
                builder.queue();
            } catch (IOException ex) {
                LOG.error("Error resetting Gatt callback", ex);
            }
        }
        support.onStepCountFetchFinished();
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] data) {
        UUID characteristicUUID = characteristic.getUuid();

        if (data == null || data.length == 0) return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID)) {
            int length = 0;
            if (data.length > 3) {
                length = (data[2] & 0xff) | ((data[3] & 0xff) << 8);
            }
            LOG.debug("Step count response: {} bytes", length);
            GB.updateTransferNotification(null,
                    getContext().getString(R.string.busy_task_fetch_activity_data), true, 10,
                    getContext());
            return true;
        } else if (characteristicUUID.equals(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID)) {
            if (data.length < 18) {
                LOG.info("CONVOY data too short");
            } else {
                // XOR all bytes with 0xFF (step count encoding)
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) (~data[i]);
                }

                int payloadLength = (data[0] & 0xff) | ((data[1] & 0xff) << 8);
                if (data.length != (payloadLength + 2)) {
                    LOG.warn("Payload length mismatch: {} vs {}", payloadLength, data.length);
                }

                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                ArrayList<CasioGBX100ActivitySample> stepCountData = new ArrayList<>();

                int year    = data[2];
                int month   = data[3] - 1; // 0-based
                int day     = data[4];
                int hour    = data[5];
                int minute  = data[6];

                int stepCount = (data[7] & 0xff) | ((data[8] & 0xff) << 8)
                        | ((data[9] & 0xff) << 16) | ((data[10] & 0xff) << 24);
                if (stepCount == 0xfffffffe) stepCount = 0;

                int calories = (data[11] & 0xff) | ((data[12] & 0xff) << 8);
                if (calories == 0xfffe) calories = 0;

                LOG.debug("Steps: {}  Calories: {}", stepCount, calories);

                cal.set(year + 2000, month, day, hour, 30, 0);
                int ts_to   = (int) (cal.getTimeInMillis() / 1000);
                cal.set(year + 2000, month, day, 0, 0, 0);
                int ts_from = (int) (cal.getTimeInMillis() / 1000);

                CasioGBX100ActivitySample sum = support.getSumWithinRange(ts_from, ts_to);
                int caloriesToday = sum.getCalories();
                int stepsToday    = sum.getSteps();

                cal.set(year + 2000, month, day, hour, 30, 0);

                // Parse hourly history blocks (dtype 0x04=steps, 0x05=calories)
                if (data[17] == 0x00 && data.length > 18) {
                    int index    = 18;
                    boolean inPkt = false;
                    int pktIdx   = 0;
                    int pktLen   = 0;
                    int type     = 0;

                    while (index < data.length) {
                        if (!inPkt) {
                            if (index + 3 > data.length) break;
                            type   = data[index];
                            pktLen = (data[index + 1] & 0xff) | ((data[index + 2] & 0xff) << 8);
                            pktIdx = 0;
                            inPkt  = true;
                            index += 3;
                        }
                        if (index + 1 >= data.length) break;
                        int count = (data[index] & 0xff) | ((data[index + 1] & 0xff) << 8);
                        if (count == 0xfffe) count = 0;
                        index += 2;

                        if (type == CasioConstants.CASIO_CONVOY_DATATYPE_STEPS) {
                            cal.add(Calendar.HOUR, -1);
                            int ts = (int) (cal.getTimeInMillis() / 1000);
                            CasioGBX100ActivitySample sample = new CasioGBX100ActivitySample();
                            sample.setSteps(count);
                            sample.setTimestamp(ts);
                            sample.setRawKind(ActivityKind.ACTIVITY.getCode());
                            stepCountData.add(sample);
                            if (ts > ts_from && ts < ts_to) stepsToday += count;
                        } else if (type == CasioConstants.CASIO_CONVOY_DATATYPE_CALORIES) {
                            int idx = pktIdx / 2;
                            if (idx < stepCountData.size() && stepCountData.get(idx).getSteps() > 0) {
                                stepCountData.get(idx).setCalories(count);
                                int ts = stepCountData.get(idx).getTimestamp();
                                if (ts > ts_from && ts < ts_to) caloriesToday += count;
                            }
                        }

                        pktIdx += 2;
                        if (pktIdx >= pktLen) inPkt = false;
                    }
                }

                int remainSteps = stepCount - stepsToday;
                int remainCals  = calories - caloriesToday;
                if (remainSteps > 0 && remainCals > 0) {
                    cal.set(year + 2000, month, day, hour, 30, 0);
                    int ts = (int) (cal.getTimeInMillis() / 1000);
                    CasioGBX100ActivitySample sample = new CasioGBX100ActivitySample();
                    sample.setSteps(remainSteps);
                    sample.setCalories(remainCals);
                    sample.setTimestamp(ts);
                    sample.setRawKind(ActivityKind.ACTIVITY.getCode());
                    stepCountData.add(0, sample);
                }

                support.stepCountDataFetched(stepCount, calories, stepCountData);
            }

            GB.updateTransferNotification(null,
                    getContext().getString(R.string.busy_task_fetch_activity_data), true, 80,
                    getContext());
            writeStepCountAck();
            return true;
        }

        return super.onCharacteristicChanged(gatt, characteristic, data);
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        UUID uuid = characteristic.getUuid();
        if (uuid.equals(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID)) {
            if (mLastWrittenCmd == 0x00) {
                LOG.debug("Step count request sent");
            } else if (mLastWrittenCmd == 0x04) {
                LOG.debug("Step count ACK sent, finishing");
                enableRequiredNotifications(false);
                operationFinished();
            }
            return true;
        }
        return super.onCharacteristicWrite(gatt, characteristic, status);
    }
}
