/*  Copyright (C) 2019 krzys_h

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.moyoung;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FetchDataOperation extends AbstractBTLEOperation<MoyoungDeviceSupport> {

    private static final Logger LOG = LoggerFactory.getLogger(FetchDataOperation.class);

    private final boolean[] receivedSteps = new boolean[3];
    private final boolean[] receivedSleep = new boolean[3];
    private boolean receivedTrainingData = false;

    private MoyoungPacketIn packetIn = new MoyoungPacketIn();

    public FetchDataOperation(MoyoungDeviceSupport support) {
        super(support);
    }

    @Override
    protected void prePerform() {
        getDevice().setBusyTask(R.string.busy_task_fetch_activity_data, getContext());
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    @Override
    protected void doPerform() throws IOException {
        TransactionBuilder builder = performInitialized("FetchDataOperation");
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_SYNC_PAST_SLEEP_AND_STEP, new byte[]{MoyoungConstants.ARG_SYNC_YESTERDAY_SLEEP}));
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_SYNC_PAST_SLEEP_AND_STEP, new byte[]{MoyoungConstants.ARG_SYNC_DAY_BEFORE_YESTERDAY_SLEEP}));
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_SYNC_SLEEP, new byte[0]));
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_SYNC_PAST_SLEEP_AND_STEP, new byte[]{MoyoungConstants.ARG_SYNC_YESTERDAY_STEPS}));
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_SYNC_PAST_SLEEP_AND_STEP, new byte[]{MoyoungConstants.ARG_SYNC_DAY_BEFORE_YESTERDAY_STEPS}));
        builder.read(MoyoungConstants.UUID_CHARACTERISTIC_STEPS);
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_QUERY_MOVEMENT_HEART_RATE, new byte[]{}));
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_QUERY_PAST_HEART_RATE_1, new byte[]{0x00}));
        builder.queue();

        updateProgressAndCheckFinish();
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        if (!isOperationRunning()) {
            LOG.error("onCharacteristicRead but operation is not running!");
        } else {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(MoyoungConstants.UUID_CHARACTERISTIC_STEPS) && status == BluetoothGatt.GATT_SUCCESS) {
                LOG.info("TODAY STEPS data: {}", Logging.formatBytes(value));
                receivedSteps[0] = true;
                decodeSteps(0, value);
                return true;
            }
        }

        return super.onCharacteristicRead(gatt, characteristic, value, status);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!isOperationRunning()) {
            LOG.error("onCharacteristicChanged but operation is not running!");
        } else {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(MoyoungConstants.UUID_CHARACTERISTIC_DATA_IN)) {
                if (packetIn.putFragment(value)) {
                    Pair<Byte, byte[]> packet = MoyoungPacketIn.parsePacket(packetIn.getPacket());
                    packetIn = new MoyoungPacketIn();
                    if (packet != null) {
                        byte packetType = packet.first;
                        byte[] payload = packet.second;

                        if (handlePacket(packetType, payload))
                            return true;
                    }
                }
            }
        }

        return super.onCharacteristicChanged(gatt, characteristic, value);
    }

    private boolean handlePacket(byte packetType, byte[] payload) {
        if (packetType == MoyoungConstants.CMD_SYNC_SLEEP) {
            LOG.info("TODAY SLEEP data: {}", Logging.formatBytes(payload));
            receivedSleep[0] = true;
            decodeSleep(0, payload);
            return true;
        }
        if (packetType == MoyoungConstants.CMD_SYNC_PAST_SLEEP_AND_STEP) {
            byte dataType = payload[0];
            byte[] data = new byte[payload.length - 1];
            System.arraycopy(payload, 1, data, 0, data.length);

            // For sleep, the watch considers 20h/8pm and later to be "yesterday".
            // So we introduce daysAgoOffset to account for that.
            final int sleepOffsetHours = 4;
            final int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int daysAgoOffset = 0;
            if (currentHour + sleepOffsetHours >= 24) daysAgoOffset = 1;

            if (dataType == MoyoungConstants.ARG_SYNC_DAY_BEFORE_YESTERDAY_STEPS) {
                LOG.info("2 DAYS AGO STEPS data: {}", Logging.formatBytes(data));
                receivedSteps[2] = true;
                decodeSteps(2, data);
                return true;
            } else if (dataType == MoyoungConstants.ARG_SYNC_YESTERDAY_STEPS) {
                LOG.info("YESTERDAY STEPS data: {}", Logging.formatBytes(data));
                receivedSteps[1] = true;
                decodeSteps(1, data);
                return true;
            } else if (dataType == MoyoungConstants.ARG_SYNC_DAY_BEFORE_YESTERDAY_SLEEP) {
                LOG.info("2 DAYS AGO SLEEP data: {}", Logging.formatBytes(data));
                receivedSleep[2] = true;
                decodeSleep(2 - daysAgoOffset, data);
                return true;
            } else if (dataType == MoyoungConstants.ARG_SYNC_YESTERDAY_SLEEP) {
                LOG.info("YESTERDAY SLEEP data: {}", Logging.formatBytes(data));
                receivedSleep[1] = true;
                decodeSleep(1 - daysAgoOffset, data);
                return true;
            }
        }
        if (packetType == MoyoungConstants.CMD_QUERY_MOVEMENT_HEART_RATE) {
            decodeTrainingData(payload);
            return true;
        }
        return false;
    }

    private void decodeSteps(int daysAgo, byte[] data) {
        getSupport().handleStepsHistory(daysAgo, data, false);
        updateProgressAndCheckFinish();
    }

    private void decodeSleep(int daysAgo, byte[] data) {
        getSupport().handleSleepHistory(daysAgo, data);
        updateProgressAndCheckFinish();
    }

    private void decodeTrainingData(byte[] data) {
        getSupport().handleTrainingData(data);
        receivedTrainingData = true;
        updateProgressAndCheckFinish();
    }

    private void updateProgressAndCheckFinish() {
        int count_steps = 0;
        int count_sleep = 0;
        int count_training = 0;
        int total = receivedSteps.length + receivedSleep.length;
        for (boolean receivedStep : receivedSteps)
            if (receivedStep)
                ++count_steps;
        for (boolean b : receivedSleep)
            if (b)
                ++count_sleep;
        if (receivedTrainingData)
            ++count_training;
        int count = count_steps + count_sleep + count_training;
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, 100 * count / total, getContext());
        LOG.debug("Fetching activity data status: {} out of {} steps packets and {} out of {} sleep packets", count_steps, receivedSteps.length, count_sleep, receivedSleep.length);
        if (count == total)
            operationFinished();
    }

    @Override
    protected void operationFinished() {
        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null && getDevice().isConnected()) {
            unsetBusy();
            GB.signalActivityDataFinish(getDevice());
        }
    }
}
