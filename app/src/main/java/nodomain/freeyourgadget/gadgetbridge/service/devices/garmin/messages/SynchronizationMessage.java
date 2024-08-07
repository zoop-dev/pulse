package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import org.apache.commons.lang3.EnumUtils;

import java.util.EnumSet;


public class SynchronizationMessage extends GFDIMessage {
    private final SynchronizationType synchronizationType;
    private final EnumSet<FileType> syncBitmask;

    public SynchronizationMessage(GarminMessage garminMessage, SynchronizationType synchronizationType, EnumSet<FileType> bitmask) {
        this.garminMessage = garminMessage;
        this.synchronizationType = synchronizationType;
        this.syncBitmask = bitmask;

        this.statusMessage = super.getStatusMessage();

        LOG.debug("type: {}, bitmask: {}", synchronizationType, bitmask);
    }

    public static SynchronizationMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final int type = reader.readByte();
        final SynchronizationType synchronizationType = SynchronizationType.fromCode(type);
        final int size = reader.readByte();
        final long bitmask;
        if (size == 8) {
            bitmask = reader.readLong();
        } else if (size == 4) {
            bitmask = reader.readInt();
        } else {
            LOG.warn("SynchronizationMessage bitmask size unexpected, was: {}", size);
            return null;
        }
        final EnumSet<FileType> syncBitmask = EnumUtils.processBitVector(FileType.class, bitmask);
        return new SynchronizationMessage(garminMessage, synchronizationType, syncBitmask);
    }

    public boolean shouldProceed() {
        return syncBitmask.contains(FileType.WORKOUTS) || syncBitmask.contains(FileType.ACTIVITIES)
                || syncBitmask.contains(FileType.ACTIVITY_SUMMARY) || syncBitmask.contains(FileType.SLEEP);
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }

    public enum SynchronizationType {
        TYPE_0,
        TYPE_1,
        TYPE_2,
        ;

        public static SynchronizationType fromCode(final int code) {
            for (final SynchronizationType type : SynchronizationType.values()) {
                if (type.ordinal() == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown synchronization type " + code);
        }
    }

    public enum FileType {
        unk_0,
        unk_1,
        unk_2,
        WORKOUTS,
        unk_4,
        ACTIVITIES,
        unk_6,
        unk_7,
        SOFTWARE_UPDATE,
        unk_9,
        unk_10,
        unk_11,
        unk_12,
        unk_13,
        unk_14,
        unk_15,
        unk_16,
        unk_17,
        unk_18,
        unk_19,
        unk_20,
        ACTIVITY_SUMMARY,
        unk_22,
        unk_23,
        unk_24,
        unk_25,
        SLEEP,
        unk_27,
        unk_28,
        unk_29,
        unk_30,
        unk_31,
        unk_32,
        unk_33,
        unk_34,
        unk_35,
        unk_36,
        ;
    }
}
