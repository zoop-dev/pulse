package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

public class FilterStatusMessage extends GFDIStatusMessage {
    private final Status status;

    public FilterStatusMessage(GarminMessage garminMessage, Status status, int unk) {
        this.garminMessage = garminMessage;
        this.status = status;

        LOG.info("Received ACK for message {}", garminMessage);

    }

    public static FilterStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final Status status = Status.fromCode(reader.readByte());

        if (!status.equals(Status.ACK)) {
            return null;
        }
        final int unk = reader.readByte();

        return new FilterStatusMessage(garminMessage, status, unk);
    }
}
