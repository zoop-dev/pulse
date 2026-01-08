package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitLocalMessageBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalFITMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDefinitionMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FitDataStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FitDefinitionStatusMessage;

public class FitLocalMessageHandler implements MessageHandler{
    private static final Logger LOG = LoggerFactory.getLogger(FitLocalMessageHandler.class);
    private final GarminSupport deviceSupport;
    private final List<RecordDefinition> recordDefinitionList;
    private List<RecordData> recordDataList;

    public FitLocalMessageHandler(GarminSupport deviceSupport, FitLocalMessageBuilder localMessageBuilder) {
        this.deviceSupport = deviceSupport;
        this.recordDefinitionList = localMessageBuilder.getDefinitions();
        this.recordDataList = localMessageBuilder.getRecordDataList();
    }

    public FitLocalMessageHandler(GarminSupport deviceSupport, List<RecordDefinition> recordDefinitionList) {
        this.deviceSupport = deviceSupport;
        this.recordDefinitionList = recordDefinitionList;
        this.recordDataList = new ArrayList<>();
    }

    public FitDefinitionMessage init() {
        return new FitDefinitionMessage(this.recordDefinitionList);
    }

    private FitDataMessage sendFollowUp(FitDefinitionStatusMessage fitDefinitionStatusMessage) {
        if (fitDefinitionStatusMessage.getFitDefinitionStatusCode() != FitDefinitionStatusMessage.FitDefinitionStatusCode.APPLIED)
            LOG.warn("FitDefinition was not applied, will send FitData anyway.");
        return new FitDataMessage(this.recordDataList);
    }

    private void parseIncomingFitDataMessage(FitDataMessage incoming) {
        final List<GBDeviceEvent> deviceEventList = new ArrayList<>();
        recordDataList = (incoming).applyDefinitions(recordDefinitionList);
        for(RecordData d: recordDataList){
            LOG.info("Incoming FitDataMessage: {}", d);
            List<GBDeviceEvent> processed = processRecordData(d);
            if(processed!=null) {
                deviceEventList.addAll(processed);
            }
        }
        LOG.info("Some incoming FitDataMessages are not processed any further, just logged.");
        for (final GBDeviceEvent event : deviceEventList) {
            deviceSupport.evaluateGBDeviceEvent(event);
        }
        unregisterSelf();
    }

    private List<GBDeviceEvent> processRecordData(RecordData d) {
        if (d.getRecordDefinition().getGlobalFITMessage() == GlobalFITMessage.CAPABILITIES) {
            //TODO: we are not sure this is correct!
            return GarminCapability.getGBDeviceEvent(
                    GarminCapability.setFromLong((Long) d.getFieldByName("connectivity_supported"))
            );
        }
        return null;
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
        } else if (message instanceof  FitDataMessage) {
            parseIncomingFitDataMessage((FitDataMessage) message);
        }
        return null;
    }
}
