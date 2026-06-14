/*  Copyright (C) 2026 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static nodomain.freeyourgadget.gadgetbridge.util.BundleUtils.addToBundle;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Size;
import android.util.SizeF;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.Serial;
import java.io.Serializable;
import java.nio.CharBuffer;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class BundleUtilsTest extends TestBase {
    @Test
    public void testCommonTypes() {
        Object[] values = {
                null,
                true,
                new boolean[]{true, false},
                (byte) 12,
                new byte[]{12, 23},
                (short) 1234,
                new short[]{1234, 2345},
                (char) 1234,
                new char[]{1234, 2345},
                123456,
                new int[]{123456},
                123456789L,
                new long[]{123456789L},
                1.1f,
                new float[]{1.1f, 1.2f},
                2.1,
                new double[]{2.1, 2.2},
                "abc",
                new String[]{"abc"},
                Uri.parse("http://example.com"),
                new Parcelable[]{Uri.parse("http://example.com")},
                CharBuffer.wrap("abc"),
                new CharBuffer[]{CharBuffer.wrap("abc")},
                new Serializable() {
                    @Serial
                    private static final long serialVersionUID = 8578229259758102775L;
                },
                new Bundle(),
                new SizeF(1.2f, 3.4f),
                new Size(1, 3),
        };


        for (int i = 0; i < values.length; i++) {
            Bundle bundle = new Bundle();
            Object value = values[i];
            assertTrue("add[" + i + "] " + value, addToBundle(bundle, "key", value));
            Object actual = bundle.get("key");
            assertEquals("check[" + i + "]", value, actual);
            assertEquals(1, bundle.size());
        }

        {
            Bundle bundle = new Bundle();
            assertFalse(addToBundle(bundle, "object", new Object()));
            assertEquals(0, bundle.size());
        }
    }

    @Test
    public void testJson() throws JSONException {
        JSONObject[] objects = {
                new JSONObject().put("key", true),
                new JSONObject().put("key", false),
                new JSONObject().put("key", Integer.MAX_VALUE),
                new JSONObject().put("key", Long.MAX_VALUE),
                new JSONObject().put("key", 1.2),
                new JSONObject().put("key", "xyz"),
                new JSONObject("{\"key\": null }"),
        };

        for (int i = 0; i < objects.length; i++) {
            Bundle bundle = new Bundle();
            JSONObject object = objects[i];
            Object value = object.get("key");
            assertTrue("add[" + i + "]: " + object, addToBundle(bundle, "key", value));
            Object actual = bundle.get("key");
            if(value == JSONObject.NULL){
                value = null;
            }
            assertEquals("check[" + i + "]: " + object, value, actual);
            assertEquals(1, bundle.size());
        }

        {
            Bundle bundle = new Bundle();
            Object value = new JSONObject("{ \"key\" : [false, true] }").get("key");
            assertTrue(addToBundle(bundle, "key", value));
            Object actual = bundle.get("key");
            assertArrayEquals(new boolean[]{false, true}, (boolean[]) actual);
            assertEquals(1, bundle.size());
        }

        {
            Bundle bundle = new Bundle();
            Object value = new JSONObject("{ \"key\" : [123, 9223372036854775807] }").get("key");
            assertTrue(addToBundle(bundle, "key", value));
            Object actual = bundle.get("key");
            assertArrayEquals(new long[]{123L, 9223372036854775807L}, (long[]) actual);
            assertEquals(1, bundle.size());
        }

        {
            Bundle bundle = new Bundle();
            Object value = new JSONObject("{ \"key\" : [1.2, 3.4] }").get("key");
            assertTrue(addToBundle(bundle, "key", value));
            Object actual = bundle.get("key");
            assertArrayEquals(new double[]{1.2, 3.4}, (double[]) actual, 0.001);
            assertEquals(1, bundle.size());
        }

        {
            Bundle bundle = new Bundle();
            Object value = new JSONObject("{ \"key\" : [\"123\", \"abc\"] }").get("key");
            assertTrue(addToBundle(bundle, "key", value));
            Object actual = bundle.get("key");
            assertArrayEquals(new String[]{"123", "abc"}, (String[]) actual);
            assertEquals(1, bundle.size());
        }

        {
            Bundle bundle = new Bundle();
            Object value = new JSONObject("{ \"key\" : [] }").get("key");
            assertFalse(addToBundle(bundle, "key", value));
            assertEquals(0, bundle.size());
        }
    }
}