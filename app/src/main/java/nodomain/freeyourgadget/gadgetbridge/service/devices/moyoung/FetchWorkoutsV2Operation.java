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
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FetchWorkoutsV2Operation extends AbstractBTLEOperation<MoyoungDeviceSupport> {

    private static final Logger LOG = LoggerFactory.getLogger(FetchWorkoutsV2Operation.class);

    private int totalWorkouts = 0;
    private int receivedWorkouts = 0;

    private MoyoungPacketIn packetIn = new MoyoungPacketIn();

    public FetchWorkoutsV2Operation(MoyoungDeviceSupport support) {
        super(support);
    }

    @Override
    protected void prePerform() {
        getDevice().setBusyTask(getContext().getString(R.string.busy_task_fetch_activity_data));
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    @Override
    protected void doPerform() throws IOException {
        TransactionBuilder builder = performInitialized("FetchWorkoutsV2Operation");
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(getSupport().getMtu(), MoyoungConstants.CMD_QUERY_V2_WORKOUT, new byte[] { MoyoungConstants.CMD_QUERY_V2_WORKOUT_LIST_REQUEST }));
        builder.queue(getQueue());

        updateProgressAndCheckFinish();
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!isOperationRunning()) {
            LOG.error("onCharacteristicChanged but operation is not running!");
        } else {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(MoyoungConstants.UUID_CHARACTERISTIC_DATA_IN)) {
                if (packetIn.putFragment(characteristic.getValue())) {
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

        return super.onCharacteristicChanged(gatt, characteristic);
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
        receivedWorkouts++;
        updateProgressAndCheckFinish();
        if (receivedWorkouts < totalWorkouts) {
            requestWorkoutDetails(receivedWorkouts);
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
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error while sending workout details request: ", e);
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
