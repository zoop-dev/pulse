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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class ZeppOsMapsFile {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsMapsFile.class);

    private final Pattern imgPathPattern = Pattern.compile("^11/([0-9]+)/([0-9]+).img$");

    private static final byte[] SIGNATURE = {'D', 'S', 'K', 'I', 'M', 'G', '\0'};

    private final Uri uri;
    private final Context context;

    private long fileSize = 0;
    private long uncompressedSize = 0;

    public ZeppOsMapsFile(final Uri uri, final Context context) {
        this.uri = uri;
        this.context = context;
    }

    public boolean isValid() {
        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to create urihelper", e);
            return false;
        }
        fileSize = uriHelper.getFileSize();

        try (InputStream is = context.getContentResolver().openInputStream(uri); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }

                final Matcher matcher = imgPathPattern.matcher(zipEntry.getName());
                if (!matcher.find()) {
                    LOG.error("Unknown file {}", zipEntry.getName());
                    return false;
                }

                final int num1 = Integer.parseInt(Objects.requireNonNull(matcher.group(1)), 10);
                final int num2 = Integer.parseInt(Objects.requireNonNull(matcher.group(2)), 10);
                if (num1 > 2048 || num2 > 2048) {
                    LOG.error("Invalid path {}", zipEntry.getName());
                    return false;
                }

                final byte[] header = new byte[512];
                final int read = zis.read(header, 0, 512);

                if (read < 512 || !ArrayUtils.equals(header, SIGNATURE, 0x10)) {
                    LOG.error("DSKIMG signature not found in {}", zipEntry.getName());
                    return false;
                }

                uncompressedSize += zipEntry.getSize();
            }
        } catch (final Exception e) {
            LOG.error("Failed to read {}", uri, e);
            return false;
        }

        return uncompressedSize > 0;
    }

    public Uri getUri() {
        return uri;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getUncompressedSize() {
        return uncompressedSize;
    }
}
