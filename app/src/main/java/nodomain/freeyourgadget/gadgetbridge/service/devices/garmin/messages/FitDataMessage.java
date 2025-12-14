package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FitDataStatusMessage;

public class FitDataMessage extends GFDIMessage {
    private List<RecordData> recordDataList;
    private GarminByteBufferReader reader;
    private final boolean generateOutgoing;

    public FitDataMessage(GarminByteBufferReader messageReader, GarminMessage garminMessage) {
        this.reader = messageReader;
        this.recordDataList = null;
        this.garminMessage = garminMessage;
        this.statusMessage = new FitDataStatusMessage(garminMessage, Status.ACK, FitDataStatusMessage.FitDataStatusCode.APPLIED, true);
        this.generateOutgoing = false;
    }

    public FitDataMessage(List<RecordData> recordDataList) {
        this.reader = null;
        this.recordDataList = recordDataList;
        this.garminMessage = GarminMessage.FIT_DATA;
        this.statusMessage = null;
        this.generateOutgoing = true;
    }

    public static FitDataMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        return new FitDataMessage(new GarminByteBufferReader(reader.readBytes(reader.remaining())), garminMessage);
    }

    public List<RecordData> applyDefinitions(List<RecordDefinition> recordDefinitions) {
        final List<RecordData> recordDataList = new ArrayList<>();;
        Objects.requireNonNull(reader, "reader cannot be null");
        try {
            while (reader.remaining() > 0) {
                RecordHeader recordHeader = new RecordHeader((byte) reader.readByte());
                if (recordHeader.isDefinition())
                    return null;

                RecordDefinition localMessageDefinition = recordDefinitions.stream()
                        .filter(d -> d.getRecordHeader().getLocalMessageType() == recordHeader.getLocalMessageType())
                        .findFirst()
                        .orElse(null);

                if (localMessageDefinition == null) {
                    LOG.warn("Cannot find a local message definition for type: {}", recordHeader.getLocalMessageType());
                    return null;
                }
                RecordData recordData = new RecordData(
                        localMessageDefinition,
                        localMessageDefinition.getRecordHeader()
                );
                recordData.parseDataMessage(reader, null);
                recordDataList.add(recordData);
            }
        } catch (Exception e) {
            LOG.error("Error while applying definitions", e);
            throw new IllegalStateException("Failed to apply definitions to FIT data", e);
        } finally {
            reader.setPosition(0);
        }
        return recordDataList;
    }

    @Override
    protected boolean generateOutgoing() {
        if(this.recordDataList == null)
            return false;
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());
        for (RecordData recordData : recordDataList) {
            recordData.generateOutgoingDataPayload(writer);
        }
        return this.generateOutgoing;
    }
}
