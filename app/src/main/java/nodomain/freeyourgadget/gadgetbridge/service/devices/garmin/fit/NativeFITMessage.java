package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

/**
 * @noinspection DuplicateStringLiteralInspection, WeakerAccess
 */
public class NativeFITMessage {
    public static final Map<Integer, NativeFITMessage> KNOWN_MESSAGES = NativeFITMessages.mapKnownMessages();

    private final int number;
    private final String name;

    private final List<FieldDefinitionPrimitive> fieldDefinitionPrimitives;

    NativeFITMessage(int number, String name, List<FieldDefinitionPrimitive> fieldDefinitionPrimitives) {
        this.number = number;
        this.name = name;
        this.fieldDefinitionPrimitives = fieldDefinitionPrimitives;
    }

    static NativeFITMessage fromNumber(final int number) {
        final NativeFITMessage found = KNOWN_MESSAGES.get(number);
        if (found != null) {
            return found;
        }

        return new NativeFITMessage(number, "UNK_" + FitDebug.mesgNumLookup(number), null);
    }

    public String name() {
        return this.name;
    }

    public int getNumber() {
        return number;
    }

    public List<FieldDefinitionPrimitive> getFieldDefinitionPrimitives() {
        return fieldDefinitionPrimitives;
    }

    @Nullable
    List<FieldDefinition> getFieldDefinitions(int... ids) {
        if (null == fieldDefinitionPrimitives)
            return null;
        List<FieldDefinition> subset = new ArrayList<>(ids.length);
        for (int id :
                ids) {
            for (FieldDefinitionPrimitive fieldDefinitionPrimitive :
                    fieldDefinitionPrimitives) {
                if (fieldDefinitionPrimitive.number == id) {
                    subset.add(FieldDefinitionFactory.create(
                            fieldDefinitionPrimitive.number,
                            fieldDefinitionPrimitive.size,
                            fieldDefinitionPrimitive.type,
                            fieldDefinitionPrimitive.baseType,
                            fieldDefinitionPrimitive.name,
                            fieldDefinitionPrimitive.scale,
                            fieldDefinitionPrimitive.offset
                    ));
                }
            }
        }
        return subset;
    }

    FieldDefinition getFieldDefinition(final String name, final int count) {
        for (FieldDefinitionPrimitive fieldDefinitionPrimitive :
                fieldDefinitionPrimitives) {
            if (name.equals(fieldDefinitionPrimitive.name)) {
                return FieldDefinitionFactory.create(
                        fieldDefinitionPrimitive.number,
                        fieldDefinitionPrimitive.size * count,
                        fieldDefinitionPrimitive.type,
                        fieldDefinitionPrimitive.baseType,
                        fieldDefinitionPrimitive.name,
                        fieldDefinitionPrimitive.scale,
                        fieldDefinitionPrimitive.offset
                );
            }
        }

        throw new IllegalArgumentException("Unknown field name " + name);
    }

    @Nullable
    FieldDefinition getFieldDefinition(int id, int size, @NonNull BaseType baseType) {
        if (null == fieldDefinitionPrimitives)
            return null;
        for (NativeFITMessage.FieldDefinitionPrimitive fieldDefinitionPrimitive :
                fieldDefinitionPrimitives) {
            if (fieldDefinitionPrimitive.number == id) {
                // some .FIT encoders don't strictly stick to current standard
                // try to handle common cases gracefully
                Level logLevel = null;

                final BaseType stdBaseType = fieldDefinitionPrimitive.baseType;
                if (stdBaseType == BaseType.ENUM && baseType == BaseType.UINT8) {
                    // very common issue
                    logLevel = Level.DEBUG;
                } else if (stdBaseType == BaseType.UINT32Z && baseType == BaseType.UINT32) {
                    // quite common issue
                    logLevel = Level.INFO;
                } else if (stdBaseType != baseType) {
                    logLevel = Level.WARN;
                }

                if ((size % baseType.getSize()) != 0) {
                    logLevel = Level.WARN;
                }

                if (logLevel != null) {
                    final String msg = "Native for {}[{}] is of type {} with size {} (base: {}), but message declares {} with size {} (base: {})";
                    final Object[] args = new Object[]{name(), fieldDefinitionPrimitive.name, stdBaseType, fieldDefinitionPrimitive.size, stdBaseType.getSize(), baseType, size, baseType.getSize()};
                    switch (logLevel) {
                        case DEBUG: LOG.debug(msg, args); break;
                        case INFO:  LOG.info(msg, args); break;
                        default:    LOG.warn(msg, args); break;
                    }

                    if (size == 1 && (baseType == BaseType.UINT16 || baseType == BaseType.UINT32 || baseType == BaseType.UINT64)) {
                        // very common issue for COROS:
                        // Native for EVENT[data] is of type UINT32 with size 4 (base: 4), but message declares UINT32 with size 1 (base: 4)
                        LOG.warn("redefining field base type from {} to {} due to size",
                                baseType, BaseType.UINT8);
                        baseType = BaseType.UINT8;
                    }
                }

                return FieldDefinitionFactory.create(
                        fieldDefinitionPrimitive.number,
                        size,
                        fieldDefinitionPrimitive.type,
                        baseType,
                        fieldDefinitionPrimitive.name,
                        fieldDefinitionPrimitive.scale,
                        fieldDefinitionPrimitive.offset
                );
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        NativeFITMessage that = (NativeFITMessage) o;
        return number == that.number && Objects.equals(name, that.name) && Objects.equals(fieldDefinitionPrimitives, that.fieldDefinitionPrimitives);
    }

    @Override
    public int hashCode() {
        int result = number;
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(fieldDefinitionPrimitives);
        return result;
    }

    public static class FieldDefinitionPrimitive {
        final int number;
        final BaseType baseType;
        final String name;
        final FieldDefinitionFactory.FIELD type;
        final int scale;
        final int offset;
        final int size;

        FieldDefinitionPrimitive(int number, BaseType baseType, int size, String name, FieldDefinitionFactory.FIELD type, int scale, int offset) {
            this.number = number;
            this.baseType = baseType;
            this.size = size;
            this.name = name;
            this.type = type;
            this.scale = scale;
            this.offset = offset;
        }

        FieldDefinitionPrimitive(int number, BaseType baseType, String name, FieldDefinitionFactory.FIELD type, int scale, int offset) {
            this(number, baseType, baseType.getSize(), name, type, scale, offset);
        }

        FieldDefinitionPrimitive(int number, BaseType baseType, String name, FieldDefinitionFactory.FIELD type) {
            this(number, baseType, baseType.getSize(), name, type, 1, 0);
        }

        FieldDefinitionPrimitive(int number, BaseType baseType, String name) {
            this(number, baseType, baseType.getSize(), name, null, 1, 0);
        }

        FieldDefinitionPrimitive(int number, BaseType baseType, int size, String name) {
            this(number, baseType, size, name, null, 1, 0);
        }

        FieldDefinitionPrimitive(int number, BaseType baseType, String name, int scale, int offset) {
            this(number, baseType, baseType.getSize(), name, null, scale, offset);
        }

        public int getNumber() {
            return number;
        }

        public BaseType getBaseType() {
            return baseType;
        }

        public String getName() {
            return name;
        }

        public FieldDefinitionFactory.FIELD getType() {
            return type;
        }

        public int getScale() {
            return scale;
        }

        public int getOffset() {
            return offset;
        }

        public int getSize() {
            return size;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            FieldDefinitionPrimitive that = (FieldDefinitionPrimitive) o;
            return number == that.number && scale == that.scale && offset == that.offset && size == that.size && baseType == that.baseType && Objects.equals(name, that.name) && type == that.type;
        }

        @Override
        public int hashCode() {
            int result = number;
            result = 31 * result + Objects.hashCode(baseType);
            result = 31 * result + Objects.hashCode(name);
            result = 31 * result + Objects.hashCode(type);
            result = 31 * result + scale;
            result = 31 * result + offset;
            result = 31 * result + size;
            return result;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(NativeFITMessage.class);
}
