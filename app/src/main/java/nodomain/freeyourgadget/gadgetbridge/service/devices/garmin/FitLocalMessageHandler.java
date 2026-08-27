package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitLocalMessageBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.NativeFITMessages;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDefinitionMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FitDataStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FitDefinitionStatusMessage;

public class FitLocalMessageHandler implements MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FitLocalMessageHandler.class);

    // GFDI message framing overhead: 2 bytes packet size + 2 bytes message id + 2 bytes CRC
    private static final int FIT_DATA_MESSAGE_OVERHEAD = 6;

    private final GarminSupport deviceSupport;
    private final List<RecordDefinition> recordDefinitionList;
    private final int maxPacketSize;
    private List<RecordData> recordDataList;

    // Chunk the outgoing record data
    private int outgoingIndex = 0;

    public FitLocalMessageHandler(GarminSupport deviceSupport, FitLocalMessageBuilder localMessageBuilder, int maxPacketSize) {
        this.deviceSupport = deviceSupport;
        this.recordDefinitionList = localMessageBuilder.getDefinitions();
        this.recordDataList = localMessageBuilder.getRecordDataList();
        this.maxPacketSize = maxPacketSize;
    }

    public FitLocalMessageHandler(GarminSupport deviceSupport, List<RecordDefinition> recordDefinitionList, int maxPacketSize) {
        this.deviceSupport = deviceSupport;
        this.recordDefinitionList = recordDefinitionList;
        this.recordDataList = new ArrayList<>();
        this.maxPacketSize = maxPacketSize;
    }

    public FitDefinitionMessage init() {
        return new FitDefinitionMessage(this.recordDefinitionList);
    }

    private FitDataMessage sendFollowUp() {
        if (outgoingIndex >= this.recordDataList.size()) {
            return null;
        }

        final List<RecordData> chunk = new ArrayList<>();
        int chunkSize = FIT_DATA_MESSAGE_OVERHEAD;
        while (outgoingIndex < this.recordDataList.size()) {
            final RecordData next = this.recordDataList.get(outgoingIndex);
            final int nextSize = next.getEncodedSize();
            if (!chunk.isEmpty() && chunkSize + nextSize > maxPacketSize) {
                break;
            }
            chunk.add(next);
            chunkSize += nextSize;
            outgoingIndex++;
        }

        LOG.debug("Sending {} record(s) ({} of {} total, {} bytes)", chunk.size(), outgoingIndex, recordDataList.size(), chunkSize);
        return new FitDataMessage(chunk);
    }

    private void parseIncomingFitDataMessage(FitDataMessage incoming) {
        final List<GBDeviceEvent> deviceEventList = new ArrayList<>();
        recordDataList = (incoming).applyDefinitions(recordDefinitionList);
        for (RecordData d : recordDataList) {
            LOG.info("Incoming FitDataMessage: {}", d);
            List<GBDeviceEvent> processed = processRecordData(d);
            if (processed != null) {
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
        if (d.getRecordDefinition().getNativeFITMessage() == NativeFITMessages.FIT_CAPABILITIES) {
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
        if (message instanceof FitDefinitionStatusMessage fitDefinitionStatusMessage) {
            if (fitDefinitionStatusMessage.getFitDefinitionStatusCode() != FitDefinitionStatusMessage.FitDefinitionStatusCode.APPLIED)
                LOG.warn("FitDefinition was not applied, will send FitData anyway.");
            // Definitions were just applied - send the first data message
            return sendFollowUp();
        } else if (message instanceof FitDataStatusMessage) {
            if (outgoingIndex < recordDataList.size()) {
                return sendFollowUp();
            } else {
                unregisterSelf();
            }
        } else if (message instanceof FitDataMessage) {
            parseIncomingFitDataMessage((FitDataMessage) message);
        }
        return null;
    }
}
