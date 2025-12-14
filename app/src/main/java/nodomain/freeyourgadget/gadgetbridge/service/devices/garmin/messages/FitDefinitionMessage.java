package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.IncomingFitDefinitionDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FitDefinitionStatusMessage;

public class FitDefinitionMessage extends GFDIMessage {
    private final IncomingFitDefinitionDeviceEvent incomingFitDefinitionDeviceEvent;
    private final List<RecordDefinition> recordDefinitions;
    private final boolean generateOutgoing;

    public FitDefinitionMessage(List<RecordDefinition> recordDefinitions, GarminMessage garminMessage) {//incoming
        this.recordDefinitions = recordDefinitions;
        this.garminMessage = garminMessage;
        this.statusMessage = new FitDefinitionStatusMessage(garminMessage, Status.ACK, FitDefinitionStatusMessage.FitDefinitionStatusCode.APPLIED, true);
        this.generateOutgoing = false;

        this.incomingFitDefinitionDeviceEvent = new IncomingFitDefinitionDeviceEvent(recordDefinitions);
    }

    public FitDefinitionMessage(List<RecordDefinition> recordDefinitions) { //outgoing
        this.recordDefinitions = recordDefinitions;
        this.garminMessage = GarminMessage.FIT_DEFINITION;
        this.statusMessage = null;
        this.generateOutgoing = true;

        this.incomingFitDefinitionDeviceEvent = null;
    }

    public static FitDefinitionMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        List<RecordDefinition> recordDefinitions = new ArrayList<>();

        while (reader.remaining() > 0) {
            RecordHeader recordHeader = new RecordHeader((byte) reader.readByte());
            recordDefinitions.add(RecordDefinition.parseIncoming(reader, recordHeader));
        }

        return new FitDefinitionMessage(recordDefinitions, garminMessage);
    }

    public List<RecordDefinition> getRecordDefinitions() {
        return recordDefinitions;
    }

    @Override
    public List<GBDeviceEvent> getGBDeviceEvent() {
        return Collections.singletonList(incomingFitDefinitionDeviceEvent);
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(garminMessage.getId());
        for (RecordDefinition recordDefinition : recordDefinitions) {
            recordDefinition.generateOutgoingPayload(writer);
        }
        return this.generateOutgoing;
    }

}
