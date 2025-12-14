package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class FitDataStatusMessage extends GFDIStatusMessage {

    private final Status status;
    private final FitDataStatusCode fitDataStatusCode;
    private final boolean sendOutgoing;

    public FitDataStatusMessage(GarminMessage garminMessage, Status status, FitDataStatusCode fitDataStatusCode, boolean sendOutgoing) {
        this.garminMessage = garminMessage;
        this.status = status;
        this.fitDataStatusCode = fitDataStatusCode;
        this.sendOutgoing = sendOutgoing;
    }

    public static FitDataStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final Status status = Status.fromCode(reader.readByte());
        final int fitDataStatusCodeByte = reader.readByte();
        final FitDataStatusCode fitDataStatusCode = FitDataStatusCode.fromCode(fitDataStatusCodeByte);
        if (fitDataStatusCode == null) {
            LOG.warn("Unknown fit data status code {}", fitDataStatusCodeByte);
            return null;
        }
        switch (fitDataStatusCode) {
            case APPLIED:
                LOG.info("FIT DATA RETURNED STATUS: {}", fitDataStatusCode.name());
                break;
            default:
                LOG.warn("FIT DATA RETURNED STATUS: {}", fitDataStatusCode.name());
        }
        return new FitDataStatusMessage(garminMessage, status, fitDataStatusCode, false);
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(garminMessage.getId());
        writer.writeByte(status.ordinal());
        writer.writeByte(fitDataStatusCode.ordinal());
        return sendOutgoing;
    }

    public enum FitDataStatusCode {
        APPLIED,
        NO_DEFINITION,
        MISMATCH,
        NOT_READY,
        ;

        @Nullable
        public static FitDataStatusCode fromCode(final int code) {
            for (final FitDataStatusCode fitDataStatusCode : FitDataStatusCode.values()) {
                if (fitDataStatusCode.ordinal() == code) {
                    return fitDataStatusCode;
                }
            }
            return null;
        }
    }
}
