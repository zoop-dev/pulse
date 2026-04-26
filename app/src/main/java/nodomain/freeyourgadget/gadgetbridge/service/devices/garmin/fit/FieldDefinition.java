package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTimestamp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;
import nodomain.freeyourgadget.gadgetbridge.util.GBToStringBuilder;

public class FieldDefinition implements FieldInterface {
    protected static final Logger LOG = LoggerFactory.getLogger(FieldDefinition.class);

    protected final BaseType baseType;
    protected final int scale;
    protected final int offset;
    private final int number;
    private final int size;
    private final String name;

    public FieldDefinition(int number, int size, BaseType baseType, String name, int scale, int offset) {
        this.number = number;
        this.size = size;
        this.baseType = baseType;
        this.name = name;
        this.scale = scale;
        this.offset = offset;
    }

    public FieldDefinition(int number, int size, BaseType baseType, String name) {
        this(number, size, baseType, name, 1, 0);
    }

    public static FieldDefinition parseIncoming(GarminByteBufferReader garminByteBufferReader, NativeFITMessage nativeFITMessage) {
        int number = garminByteBufferReader.readByte();
        int size = garminByteBufferReader.readByte();
        int baseTypeIdentifier = garminByteBufferReader.readByte();
        BaseType baseType = BaseType.fromIdentifier(baseTypeIdentifier);
        FieldDefinition nativeFITMessageFieldDefinition = nativeFITMessage.getFieldDefinition(number, size, baseType);
        if (nativeFITMessageFieldDefinition != null) {
            return nativeFITMessageFieldDefinition;
        }

        if (number == 253 && size == 4 && baseType.equals(BaseType.UINT32)) {
            return new FieldDefinitionTimestamp(number, size, baseType, "253_timestamp");
        }

        if (0 != (size % baseType.getSize())) {
            LOG.warn("inconsistent size of field {} in record {}/{} - total size: {}, base size: {}, base type: {}",
                    number, nativeFITMessage.getNumber(), nativeFITMessage.name(), size,
                    baseType.getSize(), baseType);
        }
        return new FieldDefinition(number, size, baseType, "");
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }

    public BaseType getBaseType() {
        return baseType;
    }

    public String getName() {
        return name;
    }

    public void generateOutgoingPayload(MessageWriter writer) {
        writer.writeByte(number);
        writer.writeByte(size);
        writer.writeByte(baseType.getIdentifier());
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        return baseType.decode(byteBuffer, scale, offset);
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        baseType.encode(byteBuffer, o, scale, offset);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        baseType.invalidate(byteBuffer);
    }

    @NonNull
    @Override
    public String toString() {
        final GBToStringBuilder tsb = new GBToStringBuilder(this);
        tsb.append("baseType", baseType);
        if (scale != 1) {
            tsb.append("scale", scale);
        }
        if (offset != 0) {
            tsb.append("offset", offset);
        }
        if (size != 1) {
            tsb.append("size", size);
        }
        return tsb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        FieldDefinition that = (FieldDefinition) o;
        return scale == that.scale && offset == that.offset && number == that.number && size == that.size && baseType == that.baseType && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(baseType);
        result = 31 * result + scale;
        result = 31 * result + offset;
        result = 31 * result + number;
        result = 31 * result + size;
        result = 31 * result + Objects.hashCode(name);
        return result;
    }
}
