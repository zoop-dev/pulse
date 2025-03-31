/*  Copyright (C) 2024-2025 Me7c7, Martin.JM

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
