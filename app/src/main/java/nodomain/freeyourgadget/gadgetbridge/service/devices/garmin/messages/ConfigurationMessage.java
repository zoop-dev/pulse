package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCapability;


public class ConfigurationMessage extends GFDIMessage {
    private final byte[] incomingConfigurationPayload;
    private final Set<GarminCapability> capabilities;
    private final byte[] ourConfigurationPayload = GarminCapability.setToBinary(GarminCapability.OUR_CAPABILITIES);

    public ConfigurationMessage(GarminMessage garminMessage, byte[] configurationPayload) {
        this.garminMessage = garminMessage;
        if (configurationPayload.length > 255)
            throw new IllegalArgumentException("Too long payload");
        this.incomingConfigurationPayload = configurationPayload;
        this.capabilities = GarminCapability.setFromBinary(configurationPayload);
        LOG.info("Received configuration message; capabilities: {}", GarminCapability.setToString(capabilities));

        this.statusMessage = this.getStatusMessage();
    }

    public static ConfigurationMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final int numBytes = reader.readByte();
        return new ConfigurationMessage(garminMessage, reader.readBytes(numBytes));
    }

    @Override
    public List<GBDeviceEvent> getGBDeviceEvent() {
        return GarminCapability.getGBDeviceEvent(capabilities);
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // placeholder for packet size
        writer.writeShort(garminMessage.getId());
        writer.writeByte(ourConfigurationPayload.length);
        writer.writeBytes(ourConfigurationPayload);
        return true;
    }

}
