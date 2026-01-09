package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GarminJson {
    private static final byte[] STRING_SECTION_MAGIC = {(byte) 0xab, (byte) 0xcd, (byte) 0xab, (byte) 0xcd};
    private static final byte[] DATA_SECTION_MAGIC = {(byte) 0xda, (byte) 0x7a, (byte) 0xda, (byte) 0x7a};

    private static final byte TYPE_NULL = 0x00;
    private static final byte TYPE_SINT32 = 0x01;
    private static final byte TYPE_FLOAT = 0x02;
    private static final byte TYPE_STRING = 0x03;
    private static final byte TYPE_ARRAY = 0x05;
    private static final byte TYPE_BOOL = 0x09;
    private static final byte TYPE_MAP = 0x0b;
    private static final byte TYPE_SINT64 = 0x0e;
    private static final byte TYPE_DOUBLE = 0x0f;

    public static byte[] encode(final JsonElement object) throws GarminJsonException {
        try {
            // First pass: collect all strings
            Set<String> strings = new LinkedHashSet<>();
            collectStrings(object, strings);

            // Build string section
            final ByteArrayOutputStream stringSection = new ByteArrayOutputStream();
            int currentOffset = 0;
            final Map<String, Integer> finalOffsets = new LinkedHashMap<>();
            for (final String str : strings) {
                finalOffsets.put(str, currentOffset);
                final byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
                final int length = strBytes.length + 1; // +1 for null terminator
                stringSection.write((length >> 8) & 0xFF);
                stringSection.write(length & 0xFF);
                stringSection.write(strBytes);
                stringSection.write(0x00);
                currentOffset += 2 + strBytes.length + 1;
            }

            // Build data section (breadth-first)
            final ByteArrayOutputStream dataSection = new ByteArrayOutputStream();
            encodeValueBreadthFirst(object, dataSection, finalOffsets);

            final ByteArrayOutputStream output = new ByteArrayOutputStream();

            // Write string section, if any
            if (stringSection.size() > 0) {
                output.write(STRING_SECTION_MAGIC);
                writeUint32BE(output, stringSection.size());
                output.write(stringSection.toByteArray());
            }

            // Write data section
            output.write(DATA_SECTION_MAGIC);
            writeUint32BE(output, dataSection.size());
            output.write(dataSection.toByteArray());

            return output.toByteArray();
        } catch (final IOException e) {
            throw new GarminJsonException("Failed to encode GarminJson", e);
        }
    }

    private static void encodeValueBreadthFirst(final Object rootObj,
                                                final ByteArrayOutputStream out,
                                                final Map<String, Integer> stringOffsets) throws GarminJsonException {
        final Queue<Object> queue = new LinkedList<>();
        queue.add(rootObj);

        while (!queue.isEmpty()) {
            final Object obj = queue.poll();

            if (obj == null || obj instanceof JsonNull) {
                out.write(TYPE_NULL);
            } else if (obj instanceof JsonPrimitive jsonPrimitive) {
                if (jsonPrimitive.isBoolean()) {
                    out.write(TYPE_BOOL);
                    out.write(jsonPrimitive.getAsBoolean() ? 0x01 : 0x00);
                } else if (jsonPrimitive.isNumber()) {
                    final Number num = jsonPrimitive.getAsNumber();
                    // Check the actual value to determine the appropriate type
                    // This handles LazilyParsedNumber and other Number implementations
                    final double doubleValue = num.doubleValue();
                    final long longValue = num.longValue();

                    // Check if it's a floating point number
                    if (doubleValue != longValue || num instanceof Float || num instanceof Double) {
                        // It has a fractional part or is explicitly a float/double
                        if (num instanceof Float || (Math.abs(doubleValue) < Float.MAX_VALUE && doubleValue == (float) doubleValue)) {
                            out.write(TYPE_FLOAT);
                            writeFloat(out, num.floatValue());
                        } else {
                            out.write(TYPE_DOUBLE);
                            writeDouble(out, num.doubleValue());
                        }
                    } else {
                        // It's an integer value
                        if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                            out.write(TYPE_SINT32);
                            writeUint32BE(out, (int) longValue);
                        } else {
                            out.write(TYPE_SINT64);
                            writeUint64BE(out, longValue);
                        }
                    }
                } else if (jsonPrimitive.isString()) {
                    out.write(TYPE_STRING);
                    final String str = jsonPrimitive.getAsString();
                    final Integer offset = stringOffsets.get(str);
                    if (offset == null) {
                        throw new GarminJsonException("String not found in offset map: " + str);
                    }
                    writeUint32BE(out, offset);
                } else {
                    throw new GarminJsonException("Unexpected json primitive: " + jsonPrimitive.getClass().getName());
                }
            } else if (obj instanceof String str) {
                out.write(TYPE_STRING);
                final Integer offset = stringOffsets.get(str);
                if (offset == null) {
                    throw new GarminJsonException("String not found in offset map: " + str);
                }
                writeUint32BE(out, offset);
            } else if (obj instanceof JsonArray jsonArray) {
                out.write(TYPE_ARRAY);
                writeUint32BE(out, jsonArray.size());
                // Add array elements to queue for breadth-first processing
                for (final JsonElement element : jsonArray) {
                    queue.add(element);
                }
            } else if (obj instanceof JsonObject jsonObj) {
                out.write(TYPE_MAP);
                writeUint32BE(out, jsonObj.size());
                // Add key-value pairs to queue for breadth-first processing
                for (final Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
                    queue.add(entry.getKey());
                    queue.add(entry.getValue());
                }
            } else {
                throw new GarminJsonException("Unsupported type: " + obj.getClass().getName());
            }
        }
    }

    public static JsonElement decode(final byte[] bytes) throws GarminJsonException {
        if (bytes.length < 4 + 4 + 1) {
            throw new GarminJsonException("Not enough bytes for GarminJson in " + GB.hexdump(bytes));
        }

        final ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

        // Parse string section if present
        final Map<Integer, String> strings = new LinkedHashMap<>();
        final byte[] magic = new byte[4];
        buffer.get(magic);

        if (Arrays.equals(magic, STRING_SECTION_MAGIC)) {
            final int stringSectionLength = buffer.getInt();
            final int stringSectionEnd = buffer.position() + stringSectionLength;
            final int stringSectionStart = buffer.position();

            while (buffer.position() < stringSectionEnd) {
                final int stringStart = buffer.position() - stringSectionStart;
                final int length = buffer.getShort() & 0xFFFF;
                final byte[] strBytes = new byte[length - 1]; // -1 for null terminator
                buffer.get(strBytes);
                buffer.get(); // skip null terminator
                final String str = new String(strBytes, StandardCharsets.UTF_8);
                strings.put(stringStart, str);
            }

            // Read data section magic
            buffer.get(magic);
        }

        if (!Arrays.equals(magic, DATA_SECTION_MAGIC)) {
            throw new GarminJsonException("Expected data section magic, got " + GB.hexdump(magic));
        }

        final int dataSectionLength = buffer.getInt();
        if (buffer.position() + dataSectionLength > bytes.length) {
            throw new GarminJsonException(String.format(
                    "Not enough bytes to decode data section: length=%d, remaining=%d",
                    dataSectionLength,
                    bytes.length - buffer.position()
            ));
        }

        final Object rootObject = decodeValue(buffer, strings);

        // Process placeholders breadth-first using a queue
        final Queue<Object> queue = new LinkedList<>();
        queue.add(rootObject);

        while (!queue.isEmpty()) {
            Object current = queue.poll();

            if (current instanceof MapPlaceholder mp) {
                for (int i = 0; i < mp.size; i++) {
                    final Object key = decodeValue(buffer, strings);
                    final Object value = decodeValue(buffer, strings);

                    // Unwrap placeholders to get the actual JSON object/array
                    Object actualValue = value;
                    if (value instanceof MapPlaceholder) {
                        actualValue = ((MapPlaceholder) value).obj;
                        queue.add(value);
                    } else if (value instanceof ArrayPlaceholder) {
                        actualValue = ((ArrayPlaceholder) value).array;
                        queue.add(value);
                    }

                    mp.obj.add(String.valueOf(((JsonPrimitive) key).getAsString()), (JsonElement) actualValue);
                }
            } else if (current instanceof ArrayPlaceholder ap) {
                for (int i = 0; i < ap.length; i++) {
                    Object value = decodeValue(buffer, strings);

                    // Unwrap placeholders to get the actual JSON object/array
                    Object actualValue = value;
                    if (value instanceof MapPlaceholder) {
                        actualValue = ((MapPlaceholder) value).obj;
                        queue.add(value);
                    } else if (value instanceof ArrayPlaceholder) {
                        actualValue = ((ArrayPlaceholder) value).array;
                        queue.add(value);
                    }

                    ap.array.add((JsonElement) actualValue);
                }
            }
        }

        if (rootObject instanceof ArrayPlaceholder ap) {
            return ap.array;
        } else if (rootObject instanceof MapPlaceholder mp) {
            return mp.obj;
        } else if (rootObject instanceof JsonElement jsonElement) {
            return jsonElement;
        } else {
            throw new GarminJsonException("Unexpected root object " + rootObject.getClass());
        }
    }

    private static void collectStrings(final JsonElement rootObj, final Set<String> strings) {
        final Queue<JsonElement> queue = new LinkedList<>();
        queue.add(rootObj);

        while (!queue.isEmpty()) {
            final JsonElement obj = queue.poll();

            if (obj instanceof JsonObject jsonObj) {
                for (final Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
                    strings.add(entry.getKey());
                    final JsonElement value = entry.getValue();
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        strings.add(value.getAsString());
                    } else if (value.isJsonObject() || value.isJsonArray()) {
                        queue.add(value);
                    }
                }
            } else if (obj instanceof JsonArray jsonArray) {
                for (final JsonElement element : jsonArray) {
                    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                        strings.add(element.getAsString());
                    } else if (element.isJsonObject() || element.isJsonArray()) {
                        queue.add(element);
                    }
                }
            } else if (obj instanceof JsonPrimitive primitive) {
                if (primitive.isString()) {
                    strings.add(primitive.getAsString());
                }
            }
        }
    }

    private static Object decodeValue(final ByteBuffer buffer, final Map<Integer, String> strings) throws GarminJsonException {
        byte type = buffer.get();

        switch (type) {
            case TYPE_NULL:
                return JsonNull.INSTANCE;
            case TYPE_BOOL:
                return new JsonPrimitive(buffer.get() != 0x00);
            case TYPE_SINT32:
                return new JsonPrimitive(buffer.getInt());
            case TYPE_SINT64:
                return new JsonPrimitive(buffer.getLong());
            case TYPE_FLOAT:
                return new JsonPrimitive(buffer.getFloat());
            case TYPE_DOUBLE:
                return new JsonPrimitive(buffer.getDouble());
            case TYPE_STRING:
                final int offset = buffer.getInt();
                final String str = strings.get(offset);
                if (str == null) {
                    throw new GarminJsonException("String not found in offset map: " + offset);
                }
                return new JsonPrimitive(str);
            case TYPE_ARRAY:
                final int arrayLength = buffer.getInt();
                // Decoding is breadth-first - don't decode children yet
                // return placeholder with the expected number of children
                return new ArrayPlaceholder(new JsonArray(), arrayLength);
            case TYPE_MAP:
                final int mapSize = buffer.getInt();
                // Decoding is breadth-first - don't decode children yet
                // return placeholder with the expected number of children
                return new MapPlaceholder(new JsonObject(), mapSize);
            default:
                throw new GarminJsonException("Unknown type: 0x" + Integer.toHexString(type & 0xFF));
        }
    }

    private static void writeUint32BE(final ByteArrayOutputStream out, final int value) {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void writeUint64BE(final ByteArrayOutputStream out, final long value) {
        out.write((int) ((value >> 56) & 0xFF));
        out.write((int) ((value >> 48) & 0xFF));
        out.write((int) ((value >> 40) & 0xFF));
        out.write((int) ((value >> 32) & 0xFF));
        out.write((int) ((value >> 24) & 0xFF));
        out.write((int) ((value >> 16) & 0xFF));
        out.write((int) ((value >> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    private static void writeFloat(final ByteArrayOutputStream out, final float value) {
        writeUint32BE(out, Float.floatToIntBits(value));
    }

    private static void writeDouble(final ByteArrayOutputStream out, final double value) {
        writeUint64BE(out, Double.doubleToLongBits(value));
    }

    private record MapPlaceholder(JsonObject obj, int size) {
    }

    private record ArrayPlaceholder(JsonArray array, int length) {
    }
}
