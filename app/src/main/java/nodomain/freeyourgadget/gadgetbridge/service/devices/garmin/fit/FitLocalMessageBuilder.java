package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecordDataFactory;

public class FitLocalMessageBuilder {
    private static final int MAX_DEFINITIONS = 15;

    private final List<RecordData> recordData = new ArrayList<>();
    private final Map<Integer, RecordDefinition> definitionsByLocalType = new LinkedHashMap<>();

    public FitLocalMessageBuilder() {
    }

    public FitLocalMessageBuilder addRecordData(RecordData data) {
        Objects.requireNonNull(data, "RecordData cannot be null");
        Objects.requireNonNull(data.getRecordDefinition(), "RecordDefinition cannot be null");

        processRecordData(data);
        return this;
    }

    public FitLocalMessageBuilder(List<RecordData> recordDataList) {
        Objects.requireNonNull(recordDataList, "recordDataList cannot be null");

        for (RecordData data : recordDataList) {
            Objects.requireNonNull(data.getRecordDefinition(), "recordDefinition cannot be null");

            addRecordData(data);
        }
    }

    public int getNextAvailableLocalMessageType() {
        for (int i = 0; i < MAX_DEFINITIONS; i++) {
            if (!definitionsByLocalType.containsKey(i)) {
                return i;
            }
        }

        throw new IllegalStateException(
                String.format("No available localMessageType: all %d slots are occupied", MAX_DEFINITIONS)
        );
    }

    private void processRecordData(RecordData data) {
        final RecordDefinition definition = data.getRecordDefinition();
        final int localMessageType = definition.getRecordHeader().getLocalMessageType();

        validateLocalMessageType(localMessageType);

        if (!definitionsByLocalType.containsKey(localMessageType)) {
            addNewDefinition(data, definition, localMessageType);
        } else {
            processExistingDefinition(data, definition, localMessageType);
        }
    }

    private void validateLocalMessageType(int localMessageType) {
        if (localMessageType < 0 || localMessageType >= MAX_DEFINITIONS) {
            throw new IllegalArgumentException(
                    String.format("localMessageType must be between 0 and %d, got: %d",
                            MAX_DEFINITIONS - 1, localMessageType)
            );
        }
    }

    private void addNewDefinition(RecordData data, RecordDefinition definition, int localMessageType) {
        if (definitionsByLocalType.size() >= MAX_DEFINITIONS) {
            throw new IllegalStateException(
                    String.format("Cannot add more than %d definitions", MAX_DEFINITIONS)
            );
        }
        RecordDefinition previousValue = definitionsByLocalType.putIfAbsent(localMessageType, definition);
        if (previousValue != null) {
            throw new IllegalStateException(
                    String.format("Duplicate localMessageType %d detected", localMessageType)
            );
        }
        recordData.add(data);
    }

    private void processExistingDefinition(RecordData data, RecordDefinition definition, int localMessageType) {
        final RecordDefinition existingDefinition = definitionsByLocalType.get(localMessageType);

        if (!existingDefinition.equals(definition)) {
            recordData.add(createRecordDataWithExistingDefinition(data, existingDefinition));
        } else {
            recordData.add(data);
        }
    }

    private RecordData createRecordDataWithExistingDefinition(RecordData source, RecordDefinition reference) {
        //TODO: verify if the record header of source should be used instead
        final RecordData newData = FitRecordDataFactory.create(reference, reference.getRecordHeader());

        for (FieldDefinition field : reference.getFieldDefinitions()) {
            newData.setFieldByName(field.getName(), source.getFieldByName(field.getName()));
        }

        //TODO: verify if we should iterate also on devFields

        return newData;
    }

    public List<RecordDefinition> getDefinitions() {
        return new ArrayList<>(definitionsByLocalType.values());
    }

    public List<RecordData> getRecordDataList() {
        return new ArrayList<>(recordData);
    }
}