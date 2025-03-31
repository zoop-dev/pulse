/*  Copyright (C) 2024-2025 Damien Gaignon, Martin.JM, Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiWorkoutGbParser;

public class GetWorkoutSpO2Request extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetWorkoutSpO2Request.class);

    Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers;
    List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder;
    short number;
    Long databaseId;

    public GetWorkoutSpO2Request(HuaweiSupportProvider support, Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers, List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder, short number, Long databaseId) {
        super(support);

        this.serviceId = Workout.id;
        this.commandId = Workout.WorkoutSpO2.id;

        this.workoutNumbers = workoutNumbers;
        this.remainder = remainder;
        this.number = number;

        this.databaseId = databaseId;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Workout.WorkoutSpO2.Request(paramsProvider, this.workoutNumbers.workoutNumber, this.number).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Workout.WorkoutSpO2.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Workout.WorkoutSections.Response.class);

        Workout.WorkoutSpO2.Response packet = (Workout.WorkoutSpO2.Response) receivedPacket;


        LOG.info("Workout {} current {}:", this.workoutNumbers.workoutNumber, this.number);
        LOG.info("spO2Number1: {}", packet.spO2Number1);
        LOG.info("spO2Number2: {}", packet.spO2Number2);
        LOG.info("Block num  : {}", packet.blocks.size());
        LOG.info("Blocks     : {}", Arrays.toString(packet.blocks.toArray()));

        supportProvider.addWorkoutSpO2Data(this.databaseId, packet.blocks, this.number);

        if (this.workoutNumbers.spO2Count > this.number + 1) {
            GetWorkoutSpO2Request nextRequest = new GetWorkoutSpO2Request(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) (this.number + 1),
                    this.databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else if (this.workoutNumbers.sectionsCount > 0) {
            GetWorkoutSectionsRequest nextRequest = new GetWorkoutSectionsRequest(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) 0,
                    this.databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else {
            new HuaweiWorkoutGbParser(getDevice(), getContext()).parseWorkout(this.databaseId);
            supportProvider.downloadWorkoutGpsFiles(this.workoutNumbers.workoutNumber, this.databaseId, new Runnable() {
                @Override
                public void run() {
                    if (!remainder.isEmpty()) {
                        GetWorkoutTotalsRequest nextRequest = new GetWorkoutTotalsRequest(
                                GetWorkoutSpO2Request.this.supportProvider,
                                remainder.remove(0),
                                remainder
                        );
                        nextRequest.setFinalizeReq(GetWorkoutSpO2Request.this.finalizeReq);
                        // Cannot do this with nextRequest because it's in a callback
                        try {
                            nextRequest.doPerform();
                        } catch (IOException e) {
                            finalizeReq.handleException(new ResponseParseException("Cannot send next request", e));
                        }
                    } else {
                        supportProvider.endOfWorkoutSync();
                    }
                }
            });
        }

    }
}
