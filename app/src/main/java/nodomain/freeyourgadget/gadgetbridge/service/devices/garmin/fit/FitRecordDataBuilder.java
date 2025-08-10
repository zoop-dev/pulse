package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecordDataFactory;

public class FitRecordDataBuilder {
    private final GlobalFITMessage globalMessage;

    private final Map<String, Object[]> values = new LinkedHashMap<>();

    public FitRecordDataBuilder(final GlobalFITMessage globalMessage) {
        this.globalMessage = globalMessage;
    }

    public FitRecordDataBuilder(final int globalMessageNumber) {
        this.globalMessage = GlobalFITMessage.KNOWN_MESSAGES.get(globalMessageNumber);
        if (this.globalMessage == null) {
            throw new IllegalArgumentException("Unknown global message " + globalMessageNumber);
        }
    }

    public FitRecordDataBuilder setFieldByNumber(final int number, final Object... value) {
        final List<FieldDefinition> fieldDefinition = globalMessage.getFieldDefinitions(number);
        if (fieldDefinition == null || fieldDefinition.isEmpty()) {
            throw new IllegalArgumentException("Unknown field number " + number + " for " + globalMessage);
        }

        setFieldByName(fieldDefinition.get(0).getName(), value);

        return this;
    }

    public FitRecordDataBuilder setFieldByName(final String name, final Object... value) {
        values.put(name, value);

        return this;
    }

    public RecordData build() {
        final RecordData recordData = FitRecordDataFactory.create(
                new RecordDefinition(
                        new RecordHeader((byte) 0x40),
                        ByteOrder.BIG_ENDIAN,
                        globalMessage,
                        values.entrySet().stream().map(e -> globalMessage.getFieldDefinition(e.getKey(), e.getValue().length)).collect(Collectors.toList()),
                        null
                ),
                new RecordHeader((byte) 0x00)
        );

        for (final Map.Entry<String, Object[]> e : values.entrySet()) {
            recordData.setFieldByName(e.getKey(), e.getValue());
        }

        return recordData;
    }
}
