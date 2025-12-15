package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GarminJsonTest {
    private static final Gson GSON = new Gson();

    final String example1json = """
            {
                "data": {
                    "glanceTemplate": {
                        "template": "{{ states('sensor.solarnet_power_grid_import') }}%"
                    }
                },
                "type": "render_template"
            }
            """;

    final String example1hex = "abcdabcd000000710005646174610000" +
            "057479706500001072656e6465725f74" +
            "656d706c61746500000f676c616e6365" +
            "54656d706c61746500000974656d706c" +
            "6174650000337b7b2073746174657328" +
            "2773656e736f722e736f6c61726e6574" +
            "5f706f7765725f677269645f696d706f" +
            "72742729207d7d2500da7ada7a000000" +
            "2d0b0000000203000000000b00000001" +
            "0300000007030000000e03000000200b" +
            "000000010300000031030000003c";

    final String example2json = """
            {
                "a": 1,
                "b": {
                    "c": "d",
                    "e": "c",
                    "f": ["x", 1, { "123": "456","789": "100" }]
                },
                "array": [1, "two", true],
                "nested": {
                    "level1": {
                        "level2": {
                            "level3": "value"
                        },
                        "level2_2": 42
                    }
                }
            }
            """;

    final String example2hex = "abcdabcd000000790006617272617900000262000002610000076e6573746564" +
            "00000474776f000002660000026300000264000002650000076c6576656c3100" +
            "0002780000096c6576656c325f320000076c6576656c32000004313233000004" +
            "3435360000043738390000043130300000076c6576656c3300000676616c7565" +
            "00da7ada7a000000a20b000000040300000000050000000303000000080b0000" +
            "0003030000000c010000000103000000100b0000000101000000010300000019" +
            "0901030000001f050000000303000000230300000027030000002b0300000023" +
            "030000002f0b00000002030000003801000000010b00000002030000003c0100" +
            "00002a03000000470b0000000103000000500300000056030000005c03000000" +
            "6203000000680300000071";

    @Test
    public void testDecodeExample1() throws Exception {
        final byte[] bytes = GB.hexStringToByteArray(example1hex);
        final JsonObject result = (JsonObject) GarminJson.decode(bytes);

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.has("data"));
        Assert.assertTrue(result.has("type"));

        Assert.assertEquals("render_template", result.get("type").getAsString());

        final JsonObject data = result.getAsJsonObject("data");
        Assert.assertEquals(1, data.size());
        Assert.assertTrue(data.has("glanceTemplate"));

        final JsonObject glanceTemplate = data.getAsJsonObject("glanceTemplate");
        Assert.assertEquals(1, glanceTemplate.size());
        Assert.assertTrue(glanceTemplate.has("template"));
        Assert.assertEquals("{{ states('sensor.solarnet_power_grid_import') }}%",
                glanceTemplate.get("template").getAsString());
    }

    @Test
    public void testDecodeExample2() throws Exception {
        final byte[] bytes = GB.hexStringToByteArray(example2hex);
        final JsonObject result = (JsonObject) GarminJson.decode(bytes);

        // root
        Assert.assertEquals(4, result.size());
        Assert.assertTrue(result.has("a"));
        Assert.assertTrue(result.has("b"));
        Assert.assertTrue(result.has("array"));
        Assert.assertTrue(result.has("nested"));

        // "a"
        Assert.assertEquals(1, result.get("a").getAsInt());

        // "b"
        final JsonObject b = result.getAsJsonObject("b");
        Assert.assertEquals(3, b.size());
        Assert.assertEquals("d", b.get("c").getAsString());
        Assert.assertEquals("c", b.get("e").getAsString());
        // "b.f"
        final JsonArray f = b.getAsJsonArray("f");
        Assert.assertEquals(3, f.size());
        Assert.assertEquals("x", f.get(0).getAsString());
        Assert.assertEquals(1, f.get(1).getAsInt());
        final JsonObject fObj = f.get(2).getAsJsonObject();
        Assert.assertEquals(2, fObj.size());
        Assert.assertEquals("456", fObj.get("123").getAsString());
        Assert.assertEquals("100", fObj.get("789").getAsString());

        // "array"
        final JsonArray array = result.getAsJsonArray("array");
        Assert.assertEquals(3, array.size());
        Assert.assertEquals(1, array.get(0).getAsInt());
        Assert.assertEquals("two", array.get(1).getAsString());
        Assert.assertTrue(array.get(2).getAsBoolean());

        // "nested"
        final JsonObject nested = result.getAsJsonObject("nested");
        Assert.assertEquals(1, nested.size());
        final JsonObject level1 = nested.getAsJsonObject("level1");
        Assert.assertEquals(2, level1.size());
        Assert.assertEquals(42, level1.get("level2_2").getAsInt());
        final JsonObject level2 = level1.getAsJsonObject("level2");
        Assert.assertEquals(1, level2.size());
        Assert.assertEquals("value", level2.get("level3").getAsString());
    }

    @Test
    public void testEncodeExample1() throws Exception {
        final JsonElement object = GSON.fromJson(example1json, JsonElement.class);
        final byte[] encoded = GarminJson.encode(object);
        Assert.assertEquals(
                example1hex,
                GB.hexdump(encoded).toLowerCase()
        );
    }

    @Test
    @Ignore("Field order is not kept by gson, so this is failing..")
    public void testEncodeExample2() throws Exception {
        final JsonElement object = GSON.fromJson(example2json, JsonElement.class);
        final byte[] encoded = GarminJson.encode(object);
        Assert.assertEquals(
                example2hex,
                GB.hexdump(encoded).toLowerCase()
        );
    }

    @Test
    public void testEncodeDecodeAllTypes() throws Exception {
        final JsonObject json = new JsonObject();
        // basic types
        json.add("nullValue", JsonNull.INSTANCE);
        json.addProperty("boolTrue", true);
        json.addProperty("boolFalse", false);
        json.addProperty("int32", 42);
        json.addProperty("int64", 9223372036854775807L);
        json.addProperty("float", 3.14f);
        json.addProperty("double", 3.115281878499445d);
        json.addProperty("string", "Hello, World!");
        // array
        final JsonArray array = new JsonArray();
        array.add(1);
        array.add("two");
        array.add(true);
        json.add("array", array);
        // nested map
        final JsonObject nested = new JsonObject();
        nested.addProperty("nested_key", "nested_value");
        json.add("map", nested);

        // Encode and decode
        final byte[] encoded = GarminJson.encode(json);
        final JsonObject decoded = (JsonObject) GarminJson.decode(encoded);

        // Validate
        Assert.assertEquals(json.size(), decoded.size());
        Assert.assertTrue(decoded.get("nullValue").isJsonNull());
        Assert.assertEquals(json.get("boolTrue").getAsBoolean(), decoded.get("boolTrue").getAsBoolean());
        Assert.assertEquals(json.get("boolFalse").getAsBoolean(), decoded.get("boolFalse").getAsBoolean());
        Assert.assertEquals(json.get("int32").getAsInt(), decoded.get("int32").getAsInt());
        Assert.assertEquals(json.get("int64").getAsLong(), decoded.get("int64").getAsLong());
        Assert.assertEquals(json.get("float").getAsDouble(), decoded.get("float").getAsDouble(), 0.001);
        Assert.assertEquals(json.get("double").getAsDouble(), decoded.get("double").getAsDouble(), 0.000001);
        Assert.assertEquals(json.get("string").getAsString(), decoded.get("string").getAsString());
        Assert.assertEquals(json.getAsJsonArray("array").size(), decoded.getAsJsonArray("array").size());
        for (int i = 0; i < json.getAsJsonArray("array").size(); i++) {
            Assert.assertEquals(json.getAsJsonArray("array").get(i), decoded.getAsJsonArray("array").get(i));
        }
        Assert.assertEquals(
                json.getAsJsonObject("map").get("nested_key").getAsString(),
                decoded.getAsJsonObject("map").get("nested_key").getAsString()
        );
    }

    @Test
    public void testEmptyObject() throws Exception {
        final JsonObject json = new JsonObject();

        final byte[] encoded = GarminJson.encode(json);

        Assert.assertEquals(
                "da7ada7a000000050b00000000",
                GB.hexdump(encoded).toLowerCase()
        );

        final JsonObject decoded = (JsonObject) GarminJson.decode(encoded);

        Assert.assertEquals(0, decoded.size());
    }

    @Test
    public void testSimpleKeyValue() throws Exception {
        final JsonObject json = new JsonObject();
        json.addProperty("key", "value");

        final byte[] encoded = GarminJson.encode(json);

        Assert.assertEquals(
                "abcdabcd0000000e00046b657900000676616c756500da7ada7a0000000f0b0000000103000000000300000006",
                GB.hexdump(encoded).toLowerCase()
        );

        final JsonObject decoded = (JsonObject) GarminJson.decode(encoded);

        Assert.assertEquals(1, decoded.size());
        Assert.assertEquals("value", decoded.get("key").getAsString());
    }
}
