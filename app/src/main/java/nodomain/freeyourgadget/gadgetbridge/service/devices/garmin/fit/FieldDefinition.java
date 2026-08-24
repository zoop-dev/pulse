/*  Copyright (C) 2024-2026 Daniele Gobbetti, José Rebelo, Thomas Kuehne

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils.GARMIN_TIME_EPOCH;

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

    private static FieldDefinitionTimestamp TIMESTAMP_253;

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
            if (TIMESTAMP_253 == null) {
                TIMESTAMP_253 = new FieldDefinitionTimestamp(number, size, baseType, "253_timestamp");
            }
            return TIMESTAMP_253;
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
