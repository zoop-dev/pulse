/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class GarminPrgFile {
    private static final Logger LOG = LoggerFactory.getLogger(GarminPrgFile.class);

    // This does not match the known 4-byte headers for older files
    private static final byte[] MAGIC = new byte[]{(byte) 0xd0, 0x00, (byte) 0xd0};

    private final Uri uri;
    private byte[] fw;
    private String name;
    private boolean valid;

    public GarminPrgFile(final Uri uri, final Context context) {
        this.uri = uri;

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri helper for {}", uri, e);
            return;
        }

        name = uriHelper.getFileName();

        // Quick 3-byte check to avoid reading the entire file
        try (final InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            final byte[] magic = new byte[3];
            if (in.read(magic) != magic.length) {
                LOG.error("Failed to read magic");
                return;
            }
            valid = Arrays.equals(magic, MAGIC);
        } catch (final IOException e) {
            LOG.error("Failed to read bytes from {}", uri, e);
            return;
        }

        final int maxExpectedFileSize = 10 * 1024 * 1024; // 10MB
        if (uriHelper.getFileSize() > maxExpectedFileSize) {
            LOG.warn("File size is larger than the maximum expected file size of {}", maxExpectedFileSize);
            return;
        }

        try (final InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            this.fw = FileUtils.readAll(in, maxExpectedFileSize);
        } catch (final IOException e) {
            LOG.error("Failed to read bytes from {}", uri, e);
        }

        // TODO parse bytes
    }

    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public boolean isValid() {
        return valid && fw != null;
    }

    public byte[] getBytes() {
        return fw;
    }
}
