package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetWorkoutCapability extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetWorkoutCapability.class);

    public GetWorkoutCapability(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = Workout.id;
        this.commandId = Workout.WorkoutCapability.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsWorkoutCapability();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Workout.WorkoutCapability.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle WorkoutCapability");
        if (!(receivedPacket instanceof Workout.WorkoutCapability.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Workout.WorkoutCapability.Response.class);

        LOG.info("Workout capability: NewSteps: {}", ((Workout.WorkoutCapability.Response) receivedPacket).supportNewStep);

        supportProvider.getHuaweiCoordinator().setSupportsWorkoutNewSteps(((Workout.WorkoutCapability.Response) receivedPacket).supportNewStep);
    }
}
