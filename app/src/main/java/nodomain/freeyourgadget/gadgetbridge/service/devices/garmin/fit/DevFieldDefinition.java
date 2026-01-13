package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import java.nio.ByteBuffer;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class DevFieldDefinition {
    public final ByteBuffer valueHolder;
    private final int fieldDefinitionNumber;
    private final int size;
    private final int developerDataIndex;
    private BaseType baseType;
    private String name;
    private int nativeMesgNum = -1;
    private int nativeFieldNum = -1;

    public DevFieldDefinition(int fieldDefinitionNumber, int size, int developerDataIndex, String name) {
        this.fieldDefinitionNumber = fieldDefinitionNumber;
        this.size = size;
        this.developerDataIndex = developerDataIndex;
        this.name = name;
        this.valueHolder = ByteBuffer.allocate(size);
    }

    public static DevFieldDefinition parseIncoming(GarminByteBufferReader garminByteBufferReader) {
        int number = garminByteBufferReader.readByte();
        int size = garminByteBufferReader.readByte();
        int developerDataIndex = garminByteBufferReader.readByte();

        return new DevFieldDefinition(number, size, developerDataIndex, "");

    }

    public BaseType getBaseType() {
        return baseType;
    }

    public void setBaseType(BaseType baseType) {
        this.baseType = baseType;
    }

    public int getDeveloperDataIndex() {
        return developerDataIndex;
    }

    public int getFieldDefinitionNumber() {
        return fieldDefinitionNumber;
    }

    public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNativeMesgNum() {
        return nativeMesgNum;
    }

    public void setNativeMesgNum(final int nativeMesgNum) {
        this.nativeMesgNum = nativeMesgNum;
    }

    public int getNativeFieldNum() {
        return nativeFieldNum;
    }

    public void setNativeFieldNum(final int nativeFieldNum) {
        this.nativeFieldNum = nativeFieldNum;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        DevFieldDefinition that = (DevFieldDefinition) o;
        return fieldDefinitionNumber == that.fieldDefinitionNumber && size == that.size && developerDataIndex == that.developerDataIndex && valueHolder.equals(that.valueHolder) && baseType == that.baseType && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = valueHolder.hashCode();
        result = 31 * result + fieldDefinitionNumber;
        result = 31 * result + size;
        result = 31 * result + developerDataIndex;
        result = 31 * result + Objects.hashCode(baseType);
        result = 31 * result + Objects.hashCode(name);
        return result;
    }
}
