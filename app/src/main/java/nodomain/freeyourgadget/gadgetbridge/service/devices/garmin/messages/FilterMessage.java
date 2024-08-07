package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

public class FilterMessage extends GFDIMessage {


    public FilterMessage() {
        this.garminMessage = GarminMessage.FILTER;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());
        writer.writeByte(FilterType.UNK_3.ordinal());

        return true;
    }

    public enum FilterType {
        NO_0,
        UNK_1,
        UNK_2,
        UNK_3
    }
}
