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


        LOG.info("Workout {} section {}:", this.workoutNumbers.workoutNumber, this.number);
        LOG.info("workoutId  : {}", packet.workoutId);
        LOG.info("number     : {}", packet.number);
        LOG.info("Block num  : {}", packet.blocks.size());
        LOG.info("Blocks     : {}", Arrays.toString(packet.blocks.toArray()));

        supportProvider.addWorkoutSectionsData(this.databaseId, packet.blocks, this.number);

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
            supportProvider.downloadWorkoutGpsFiles(this.workoutNumbers.workoutNumber, this.databaseId, new Runnable() {
                @Override
                public void run() {
                    if (!remainder.isEmpty()) {
                        GetWorkoutTotalsRequest nextRequest = new GetWorkoutTotalsRequest(
                                GetWorkoutSectionsRequest.this.supportProvider,
                                remainder.remove(0),
                                remainder
                        );
                        nextRequest.setFinalizeReq(GetWorkoutSectionsRequest.this.finalizeReq);
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
