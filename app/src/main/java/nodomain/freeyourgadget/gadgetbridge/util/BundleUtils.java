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

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Size;
import android.util.SizeF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public final class BundleUtils {
    private static final Logger LOG = LoggerFactory.getLogger(BundleUtils.class);

    private BundleUtils() {
        throw new UnsupportedOperationException("static utility class");
    }

    /// Put the {@code value} into the {@code bundle}. Supports simple {@link JSONObject} as well as
    /// non-empty {@link JSONArray} that contain exclusively one of {@link Boolean}, {@link Double},
    /// {@link Integer}, {@link Long}, or {@link String}.
    ///
    /// @return {@code true} if the {@code value} was actually put into the {@code bundle}
    public static boolean addToBundle(@NonNull final Bundle bundle,
                                      @NonNull final String key,
                                      @Nullable final Object value) {
        if (value == null || JSONObject.NULL.equals(value)) {
            bundle.putString(key, null);
            return true;
        }

        if (value instanceof final Boolean extra) {
            bundle.putBoolean(key, extra);
            return true;
        }

        if (value instanceof final boolean[] extra) {
            bundle.putBooleanArray(key, extra);
            return true;
        }

        if (value instanceof final Byte extra) {
            bundle.putByte(key, extra);
            return true;
        }

        if (value instanceof final byte[] extra) {
            bundle.putByteArray(key, extra);
            return true;
        }

        if (value instanceof final Short extra) {
            bundle.putShort(key, extra);
            return true;
        }

        if (value instanceof final short[] extra) {
            bundle.putShortArray(key, extra);
            return true;
        }

        if (value instanceof final Character extra) {
            bundle.putChar(key, extra);
            return true;
        }

        if (value instanceof final char[] extra) {
            bundle.putCharArray(key, extra);
            return true;
        }

        if (value instanceof final Integer extra) {
            bundle.putInt(key, extra);
            return true;
        }

        if (value instanceof final int[] extra) {
            bundle.putIntArray(key, extra);
            return true;
        }

        if (value instanceof final Long extra) {
            bundle.putLong(key, extra);
            return true;
        }

        if (value instanceof final long[] extra) {
            bundle.putLongArray(key, extra);
            return true;
        }

        if (value instanceof final Float extra) {
            bundle.putFloat(key, extra);
            return true;
        }

        if (value instanceof final float[] extra) {
            bundle.putFloatArray(key, extra);
            return true;
        }

        if (value instanceof final Double extra) {
            bundle.putDouble(key, extra);
            return true;
        }

        if (value instanceof final double[] extra) {
            bundle.putDoubleArray(key, extra);
            return true;
        }

        if (value instanceof final String extra) {
            bundle.putString(key, extra);
            return true;
        }

        if (value instanceof final String[] extra) {
            bundle.putStringArray(key, extra);
            return true;
        }

        if (value instanceof final CharSequence extra) {
            bundle.putCharSequence(key, extra);
            return true;
        }

        if (value instanceof final CharSequence[] extra) {
            bundle.putCharSequenceArray(key, extra);
            return true;
        }

        if (value instanceof final Bundle extra) {
            bundle.putBundle(key, extra);
            return true;
        }

        if (value instanceof final SizeF extra) {
            bundle.putSizeF(key, extra);
            return true;
        }

        if (value instanceof final IBinder extra) {
            bundle.putBinder(key, extra);
            return true;
        }

        if (value instanceof final Parcelable extra) {
            bundle.putParcelable(key, extra);
            return true;
        }

        if (value instanceof final Parcelable[] extra) {
            bundle.putParcelableArray(key, extra);
            return true;
        }

        if (value instanceof final Serializable extra) {
            bundle.putSerializable(key, extra);
            return true;
        }

        if (value instanceof final Size extra) {
            bundle.putSize(key, extra);
            return true;
        }

        if (value instanceof final JSONArray jsonArray) {
            try {
                if (jsonArray.length() < 1) {
                    LOG.warn("empty array '{}' is unsupported", key);
                    return false;
                }

                Object first = jsonArray.get(0);
                if (first instanceof Boolean) {
                    boolean[] array = new boolean[jsonArray.length()];
                    for (int arrayIndex = 0; arrayIndex < jsonArray.length(); arrayIndex++) {
                        Object element = jsonArray.get(arrayIndex);
                        if (element instanceof final Boolean x) {
                            array[arrayIndex] = x;
                        } else {
                            LOG.warn("boolean array '{}' contains unsupported value '{}' at index {}",
                                    key, element, arrayIndex);
                            return false;
                        }
                    }
                    bundle.putBooleanArray(key, array);
                    return true;
                }

                if (first instanceof Double) {
                    double[] array = new double[jsonArray.length()];
                    for (int arrayIndex = 0; arrayIndex < jsonArray.length(); arrayIndex++) {
                        Object element = jsonArray.get(arrayIndex);
                        if (element instanceof final Double x) {
                            array[arrayIndex] = x;
                        } else {
                            LOG.warn("double array '{}' contains unsupported value '{}' at index {}",
                                    key, element, arrayIndex);
                            return false;
                        }
                    }
                    bundle.putDoubleArray(key, array);
                    return true;
                }

                if (first instanceof Integer || first instanceof Long) {
                    long[] array = new long[jsonArray.length()];
                    for (int arrayIndex = 0; arrayIndex < jsonArray.length(); arrayIndex++) {
                        Object element = jsonArray.get(arrayIndex);
                        if (element instanceof final Long x) {
                            array[arrayIndex] = x;
                        } else if (element instanceof final Integer x) {
                            array[arrayIndex] = x.longValue();
                        } else {
                            LOG.warn("long array '{}' contains unsupported value '{}' at index {}",
                                    key, element, arrayIndex);
                            return false;
                        }
                    }
                    bundle.putLongArray(key, array);
                    return true;
                }

                if (first instanceof String) {
                    String[] array = new String[jsonArray.length()];
                    for (int arrayIndex = 0; arrayIndex < jsonArray.length(); arrayIndex++) {
                        Object element = jsonArray.get(arrayIndex);
                        if (element instanceof final String x) {
                            array[arrayIndex] = x;
                        } else if (element == null) {
                            array[arrayIndex] = null;
                        } else {
                            LOG.warn("String array '{}' contains unsupported value '{}' at index {}",
                                    key, element, arrayIndex);
                            return false;
                        }
                    }
                    bundle.putStringArray(key, array);
                    return true;
                }
            } catch (final JSONException exception) {
                LOG.warn("failed to add JSONArray: {}", exception.getLocalizedMessage(), exception);
                return false;
            }
        }

        LOG.warn("failed to add {}: {}", value.getClass(), value);
        return false;
    }
}
