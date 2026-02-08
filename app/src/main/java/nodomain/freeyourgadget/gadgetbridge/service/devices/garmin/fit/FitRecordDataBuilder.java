package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecordDataFactory;

public class FitRecordDataBuilder {
    private final NativeFITMessage nativeFITMessage;

    private final Map<String, Object[]> values = new LinkedHashMap<>();

    public FitRecordDataBuilder(final NativeFITMessage nativeFITMessage) {
        this.nativeFITMessage = nativeFITMessage;
    }

    public FitRecordDataBuilder(final int nativeFITMessageNumber) {
        this.nativeFITMessage = NativeFITMessage.KNOWN_MESSAGES.get(nativeFITMessageNumber);
        if (this.nativeFITMessage == null) {
            throw new IllegalArgumentException("Unknown native FIT message " + nativeFITMessageNumber);
        }
    }

    public FitRecordDataBuilder setFieldByNumber(final int number, final Object... value) {
        final List<FieldDefinition> fieldDefinition = nativeFITMessage.getFieldDefinitions(number);
        if (fieldDefinition == null || fieldDefinition.isEmpty()) {
            throw new IllegalArgumentException("Unknown field number " + number + " for " + nativeFITMessage);
        }

        setFieldByName(fieldDefinition.get(0).getName(), value);

        return this;
    }

    public FitRecordDataBuilder setFieldByName(final String name, final Object... value) {
        values.put(name, value);

        return this;
    }

    public RecordData build() {
        return build(0);
    }

    public RecordData build(final int localMessageType) {
        final RecordData recordData = FitRecordDataFactory.create(
                new RecordDefinition(
                        new RecordHeader(true, localMessageType),
                        ByteOrder.BIG_ENDIAN,
                        nativeFITMessage,
                        values.entrySet().stream().map(e -> nativeFITMessage.getFieldDefinition(e.getKey(), e.getValue().length)).collect(Collectors.toList()),
                        null
                ),
                new RecordHeader(false, localMessageType)
        );

        for (final Map.Entry<String, Object[]> e : values.entrySet()) {
            recordData.setFieldByName(e.getKey(), e.getValue());
        }

        return recordData;
    }
}
