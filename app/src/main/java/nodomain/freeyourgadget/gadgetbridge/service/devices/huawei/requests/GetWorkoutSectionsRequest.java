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

public class GetWorkoutSectionsRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetWorkoutSectionsRequest.class);

    Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers;
    List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder;
    short number;
    Long databaseId;

    public GetWorkoutSectionsRequest(HuaweiSupportProvider support, Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers, List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder, short number, Long databaseId) {
        super(support);

        this.serviceId = Workout.id;
        this.commandId = Workout.WorkoutSections.id;

        this.workoutNumbers = workoutNumbers;
        this.remainder = remainder;
        this.number = number;

        this.databaseId = databaseId;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Workout.WorkoutSections.Request(paramsProvider, this.workoutNumbers.workoutNumber, this.number).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Workout.WorkoutSections.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Workout.WorkoutSections.Response.class);

        Workout.WorkoutSections.Response packet = (Workout.WorkoutSections.Response) receivedPacket;

        if (packet.error == null) {
            LOG.info("Workout {} section {}:", this.workoutNumbers.workoutNumber, this.number);
            LOG.info("workoutId  : {}", packet.workoutId);
            LOG.info("number     : {}", packet.number);
            LOG.info("Block num  : {}", packet.blocks.size());
            LOG.info("Blocks     : {}", Arrays.toString(packet.blocks.toArray()));

            supportProvider.addWorkoutSectionsData(this.databaseId, packet.blocks, this.number);
        } else if (packet.error == 0x0001E079) {
            // We don't know why it often returns this error, but it seems to be normal, so we ignore it
            LOG.warn("Error 0001E079 occurred during workout swim segments sync. This seems to be normal, so it's ignored.");
        } else {
            LOG.warn("Error {} occurred during workout swim segments sync. Skipping workout", packet.error);
            GB.toast("Error occurred during workout sync", Toast.LENGTH_LONG, GB.WARN);
            supportProvider.nextWorkoutSync(remainder, GetWorkoutSectionsRequest.this.finalizeReq);
            return;
        }

        if (this.workoutNumbers.sectionsCount > this.number + 1) {
            GetWorkoutSectionsRequest nextRequest = new GetWorkoutSectionsRequest(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) (this.number + 1),
                    this.databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else {
            new HuaweiWorkoutGbParser(getDevice(), getContext()).parseWorkout(this.databaseId);
            supportProvider.downloadWorkoutGpsFiles(this.workoutNumbers.workoutNumber, this.databaseId, () -> supportProvider.nextWorkoutSync(remainder, GetWorkoutSectionsRequest.this.finalizeReq));
        }
    }
}
