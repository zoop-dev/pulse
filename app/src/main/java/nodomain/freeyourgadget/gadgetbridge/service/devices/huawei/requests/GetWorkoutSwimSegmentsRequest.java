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

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiWorkoutGbParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GetWorkoutSwimSegmentsRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetWorkoutSwimSegmentsRequest.class);

    Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers;
    List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder;
    short number;
    Long databaseId;

    public GetWorkoutSwimSegmentsRequest(HuaweiSupportProvider support, Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers, List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder, short number, Long databaseId) {
        super(support);

        this.serviceId = Workout.id;
        this.commandId = Workout.WorkoutSwimSegments.id;

        this.workoutNumbers = workoutNumbers;
        this.remainder = remainder;
        this.number = number;

        this.databaseId = databaseId;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Workout.WorkoutSwimSegments.Request(paramsProvider, this.workoutNumbers.workoutNumber, this.number).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Workout.WorkoutSwimSegments.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Workout.WorkoutSwimSegments.Response.class);

        Workout.WorkoutSwimSegments.Response packet = (Workout.WorkoutSwimSegments.Response) receivedPacket;

        if (packet.error != null) {
            LOG.warn("Error {} occurred during workout swim segments sync. ignoring", packet.error);
            GB.toast("Error occurred during workout sync", Toast.LENGTH_LONG, GB.WARN);
            supportProvider.nextWorkoutSync(remainder, GetWorkoutSwimSegmentsRequest.this.finalizeReq);
            return;
        }

        if (packet.workoutNumber != this.workoutNumbers.workoutNumber)
            throw new WorkoutParseException("Incorrect workout number!");

        if (packet.segmentNumber != this.number)
            throw new WorkoutParseException("Incorrect pace number!");

        LOG.info("Workout {} segment {}:", this.workoutNumbers.workoutNumber, this.number);
        LOG.info("Workout  : {}", packet.workoutNumber);
        LOG.info("Segments : {}", packet.segmentNumber);
        LOG.info("Block num: {}", packet.blocks.size());
        LOG.info("Blocks   : {}", Arrays.toString(packet.blocks.toArray()));

        supportProvider.addWorkoutSwimSegmentsData(this.databaseId, packet.blocks, packet.segmentNumber);

        if (this.workoutNumbers.segmentsCount > this.number + 1) {
            GetWorkoutSwimSegmentsRequest nextRequest = new GetWorkoutSwimSegmentsRequest(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) (this.number + 1),
                    this.databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else if (this.workoutNumbers.spO2Count > 0) {
            GetWorkoutSpO2Request nextRequest = new GetWorkoutSpO2Request(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) 0,
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
            supportProvider.downloadWorkoutGpsFiles(this.workoutNumbers.workoutNumber, this.databaseId, () -> supportProvider.nextWorkoutSync(remainder, GetWorkoutSwimSegmentsRequest.this.finalizeReq));
        }

    }
}
