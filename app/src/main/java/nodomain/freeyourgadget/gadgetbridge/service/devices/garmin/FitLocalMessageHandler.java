package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitLocalMessageBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDefinitionMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FitDataStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FitDefinitionStatusMessage;

public class FitLocalMessageHandler implements MessageHandler{
    private static final Logger LOG = LoggerFactory.getLogger(FitLocalMessageHandler.class);
    private final GarminSupport deviceSupport;
    private final FitLocalMessageBuilder localMessageBuilder;

    public FitLocalMessageHandler(GarminSupport deviceSupport, FitLocalMessageBuilder localMessageBuilder) {
        this.deviceSupport = deviceSupport;
        this.localMessageBuilder = localMessageBuilder;
    }

    public FitDefinitionMessage init() {
        return new FitDefinitionMessage(localMessageBuilder.getDefinitions());
    }

    private FitDataMessage sendFollowUp(FitDefinitionStatusMessage fitDefinitionStatusMessage) {
        if (fitDefinitionStatusMessage.getFitDefinitionStatusCode() != FitDefinitionStatusMessage.FitDefinitionStatusCode.APPLIED)
            LOG.warn("FitDefinition was not applied, will send FitData anyway.");
        return new FitDataMessage(localMessageBuilder.getRecordDataList());
    }

    private void unregisterSelf() {
        deviceSupport.unregisterHandler(this);
    }

    @Override
    public GFDIMessage handle(GFDIMessage message) {
        if (message instanceof FitDefinitionStatusMessage)
            return sendFollowUp((FitDefinitionStatusMessage) message);
        else if (message instanceof FitDataStatusMessage) {
            unregisterSelf();
        }
        return null;
    }
}
