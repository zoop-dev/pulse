/*  Copyright (C) 2025 Arjan Schrijver

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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples.MoyoungHeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FetchWorkoutsV2Operation extends AbstractBTLEOperation<MoyoungDeviceSupport> {

    private static final Logger LOG = LoggerFactory.getLogger(FetchWorkoutsV2Operation.class);

    private int totalWorkouts = 0;
    private int receivedWorkouts = 0;
    private final Calendar currentTimestamp = Calendar.getInstance();

    private MoyoungPacketIn packetIn = new MoyoungPacketIn();

    public FetchWorkoutsV2Operation(MoyoungDeviceSupport support) {
        super(support);
    }

    @Override
    protected void prePerform() {
        getDevice().setBusyTask(R.string.busy_task_fetch_activity_data, getContext());
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    @Override
    protected void doPerform() throws IOException {
        TransactionBuilder builder = performInitialized("FetchWorkoutsV2Operation");
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_QUERY_V2_WORKOUT, new byte[]{MoyoungConstants.CMD_QUERY_V2_WORKOUT_LIST_REQUEST}));
        builder.queue();

        updateProgressAndCheckFinish();
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
        if (packetType == MoyoungConstants.CMD_QUERY_V2_WORKOUT) {
            byte subtype = payload[0];
            switch (subtype) {
                case MoyoungConstants.CMD_QUERY_V2_WORKOUT_LIST_RESPONSE:
                    decodeWorkoutsList(payload);
                    break;
                case MoyoungConstants.CMD_QUERY_V2_WORKOUT_DETAIL_RESPONSE:
                    decodeWorkoutDetails(payload);
                    break;
                case MoyoungConstants.CMD_QUERY_V2_WORKOUT_HR_RESPONSE:
                    decodeWorkoutHR(payload);
                    break;
            }
            return true;
        }
        return false;
    }

    private void decodeWorkoutsList(byte[] data) {
        LOG.info("Decoding workouts list packet");
        totalWorkouts = data.length / 5;
        requestWorkoutDetails(receivedWorkouts);
    }

    private void decodeWorkoutDetails(byte[] data) {
        LOG.info("Decoding workout details packet");
        getSupport().handleTrainingData(data);

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(2);  // skip packet subtype and workoutNR to get to the workout start timestamp
        int workoutStartTS = (buffer.getInt() / 10) * 10;  // round to nearest 10 seconds
        currentTimestamp.setTime(MoyoungConstants.WatchTimeToLocalTime(workoutStartTS));
        currentTimestamp.set(Calendar.MILLISECOND, 0);

        updateProgressAndCheckFinish();
        if (receivedWorkouts < totalWorkouts) {
            requestWorkoutHR(receivedWorkouts, 0);
        }
    }

    private void decodeWorkoutHR(byte[] data) {
        LOG.info("Decoding workout HR packet");
        if (data.length < 5) {
            LOG.warn("Not enough data in workout HR packet, stopping fetch");
            receivedWorkouts++;
            updateProgressAndCheckFinish();
            if (receivedWorkouts < totalWorkouts) {
                requestWorkoutDetails(receivedWorkouts);
            }
            return;
        }
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.get();  // packet subtype (0x05)
        buf.get();  // workout nr
        int index = buf.getShort() & 0xffff;
        final ArrayList<MoyoungHeartRateSample> hrSamples = new ArrayList<>();
        while (buf.hasRemaining()) {
            int hr = buf.get() & 0xff;
            if (HeartRateUtils.getInstance().isValidHeartRateValue(hr)) {
                MoyoungHeartRateSample sample = new MoyoungHeartRateSample();
                sample.setTimestamp(currentTimestamp.getTimeInMillis());
                sample.setHeartRate(hr);
                hrSamples.add(sample);
            }
            currentTimestamp.add(Calendar.SECOND, 10);
        }
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            MoyoungHeartRateSampleProvider sampleProvider = new MoyoungHeartRateSampleProvider(getDevice(), dbHandler.getDaoSession());
            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();

            for (MoyoungHeartRateSample sample : hrSamples) {
                sample.setDeviceId(deviceId);
                sample.setUserId(userId);
            }

            sampleProvider.addSamples(hrSamples);
        } catch (Exception e) {
            LOG.error("Error acquiring database for recording heart rate samples", e);
        }
        if (index == 0xffff) {
            // Last HR packet for this workout
            receivedWorkouts++;
            updateProgressAndCheckFinish();
            if (receivedWorkouts < totalWorkouts) {
                requestWorkoutDetails(receivedWorkouts);
            }
        } else {
            // More data remaining
            requestWorkoutHR(receivedWorkouts, index);
        }
    }

    private void requestWorkoutDetails(int workoutId) {
        try {
            TransactionBuilder builder = performInitialized("FetchWorkoutsV2Operation");
            byte[] payload = new byte[]{
                    MoyoungConstants.CMD_QUERY_V2_WORKOUT_DETAIL_REQUEST,
                    (byte) workoutId
            };
            getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_QUERY_V2_WORKOUT, payload));
            builder.queue();
        } catch (IOException e) {
            LOG.error("Error while sending workout details request: ", e);
        }
    }

    private void requestWorkoutHR(int workoutId, int index) {
        try {
            TransactionBuilder builder = performInitialized("FetchWorkoutsV2Operation");
            ByteBuffer payload = ByteBuffer.allocate(4);
            payload.put(MoyoungConstants.CMD_QUERY_V2_WORKOUT_HR_REQUEST);
            payload.put((byte) workoutId);
            payload.putShort((short) index);
            getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_QUERY_V2_WORKOUT, payload.array()));
            builder.queue();
        } catch (IOException e) {
            LOG.error("Error while sending workout HR request: ", e);
        }
    }

    private void updateProgressAndCheckFinish() {
        int percentage = 0;
        if (totalWorkouts > 0) {
            percentage = 100 * receivedWorkouts / totalWorkouts;
        }
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, percentage, getContext());
        LOG.debug("Fetching activity data status: {} out of {}", receivedWorkouts, totalWorkouts);
        if (percentage == 100) {
            operationFinished();
        }
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
