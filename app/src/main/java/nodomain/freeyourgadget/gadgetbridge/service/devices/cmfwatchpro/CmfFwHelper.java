/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class CmfFwHelper {
    private static final Logger LOG = LoggerFactory.getLogger(CmfFwHelper.class);

    private static final int HEADER_FOOTER_SIZE = 36;

    private static final byte[] HEADER_FIRMWARE = new byte[]{'A', 'O', 'T', 'A'};
    private static final byte[] HEADER_AGPS = new byte[]{0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x31, 0x30, 0x30, 0x30, 0x30};

    private final Uri uri;
    private byte[] fw;
    private boolean typeFirmware;
    private boolean typeWatchface;
    private boolean typeAgps;

    private String name;
    private String version;

    public CmfFwHelper(final Uri uri, final Context context) {
        this.uri = uri;

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri helper for {}", uri, e);
            return;
        }

        final int maxExpectedFileSize = 1024 * 1024 * 32; // 32MB

        if (uriHelper.getFileSize() > maxExpectedFileSize) {
            LOG.warn("File size is larger than the maximum expected file size of {}", maxExpectedFileSize);
            return;
        }

        try (final InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            this.fw = FileUtils.readAll(in, maxExpectedFileSize);
        } catch (final IOException e) {
            LOG.error("Failed to read bytes from {}", uri, e);
            return;
        }

        parseBytes();
    }

    public Uri getUri() {
        return uri;
    }

    public boolean isValid() {
        return isWatchface() || isFirmware() || isAgps();
    }

    public boolean isWatchface() {
        return typeWatchface;
    }

    public boolean isFirmware() {
        return typeFirmware;
    }

    public boolean isAgps() {
        return typeAgps;
    }

    public String getDetails() {
        return name != null ? name : (version != null ? version : "UNKNOWN");
    }

    public byte[] getBytes() {
        return fw;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public void unsetFwBytes() {
        this.fw = null;
    }

    private void parseBytes() {
        if (parseAsWatchface()) {
            assert name != null;
            typeWatchface = true;
        } else if (parseAsFirmware()) {
            assert version != null;
            typeFirmware = true;
        } else if (parseAsAgps()) {
            typeAgps = true;
        }
    }

    private boolean parseAsWatchface() {
        if (fw.length < HEADER_FOOTER_SIZE * 2 + 3) {
            LOG.warn("File too small to be a watchface");
            return false;
        }

        // Magic: 01 00 00 (00|02) - the last byte varies by watch generation
        // (00 seen on Watch Pro 2 / Watch 3 Pro faces, 02 on original Watch Pro faces)
        if (fw[4] != 0x01 || fw[5] != 0x00 || fw[6] != 0x00 || (fw[7] != 0x00 && fw[7] != 0x02)) {
            LOG.warn("File header not a watchface");
            return false;
        }

        final int fileLen = fw.length;
        final int footerStart = fileLen - HEADER_FOOTER_SIZE;

        // Footer must be a byte-identical copy of the header
        for (int i = 0; i < HEADER_FOOTER_SIZE; i++) {
            if (fw[i] != fw[footerStart + i]) {
                LOG.warn("Watchface footer does not match header");
                return false;
            }
        }

        final String nameHeader = StringUtils.untilNullTerminator(fw, 8);
        if (nameHeader == null) {
            LOG.warn("watchface name not found in {}", uri);
            return false;
        }

        final ByteBuffer headerBuf = ByteBuffer.wrap(fw, 0, HEADER_FOOTER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        final long crcHeaderField = headerBuf.getInt(0x00) & 0xFFFFFFFFL;
        final long bodyLen = headerBuf.getInt(0x18) & 0xFFFFFFFFL;
        final long resLen = headerBuf.getInt(0x1c) & 0xFFFFFFFFL;
        final long crcResField = headerBuf.getInt(0x20) & 0xFFFFFFFFL;

        // bodyLen is the absolute offset (from file start) where the footer begins
        if (bodyLen != (long) (fileLen - HEADER_FOOTER_SIZE)) {
            LOG.warn("Watchface body length field does not match actual file size");
            return false;
        }

        // Body: [0x20 root tag][u16 LE tree length][tree bytes], then resources
        if ((fw[HEADER_FOOTER_SIZE] & 0xFF) != 0x20) {
            LOG.warn("Watchface body does not start with the expected root tag");
            return false;
        }

        final int treeLen = (fw[HEADER_FOOTER_SIZE + 1] & 0xFF) | ((fw[HEADER_FOOTER_SIZE + 2] & 0xFF) << 8);
        final int treeSectionLen = 1 + 2 + treeLen; // tag + len + tree bytes
        final int resourcesStart = HEADER_FOOTER_SIZE + treeSectionLen;

        if (resourcesStart < 0 || resLen < 0 || resourcesStart + resLen != footerStart) {
            LOG.warn("Watchface resources section does not line up with the footer");
            return false;
        }

        // header[4:36] + tree section are contiguous in the file, so this is one slice
        final long computedHeaderCrc = crc32Raw(fw, 4, (HEADER_FOOTER_SIZE - 4) + treeSectionLen);
        if (computedHeaderCrc != crcHeaderField) {
            LOG.warn("Watchface header CRC mismatch");
            return false;
        }

        final long computedResCrc = crc32Raw(fw, resourcesStart, (int) resLen);
        if (computedResCrc != crcResField) {
            LOG.warn("Watchface resources CRC mismatch");
            return false;
        }

        name = nameHeader;

        return true;
    }

    /**
     * Raw CRC32: reflected IEEE polynomial 0xEDB88320, init = 0, no final XOR.
     * NOT the same as java.util.zip.CRC32 (which uses init/final XOR of 0xFFFFFFFF).
     */
    private static long crc32Raw(final byte[] data, final int offset, final int length) {
        int crc = 0;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xEDB88320;
                } else {
                    crc >>>= 1;
                }
            }
        }
        return crc & 0xFFFFFFFFL;
    }

    private boolean parseAsFirmware() {
        if (!ArrayUtils.equals(fw, HEADER_FIRMWARE, 0)) {
            LOG.warn("File header not a firmware");
            return false;
        }

        // FIXME: This is not really the version, but build number?
        final String versionHeader = StringUtils.untilNullTerminator(fw, 64);
        if (versionHeader == null) {
            LOG.warn("firmware version not found in {}", uri);
            return false;
        }

        version = versionHeader;

        return true;
    }

    private boolean parseAsAgps() {
        if (!ArrayUtils.equals(fw, HEADER_AGPS, 0)) {
            LOG.warn("File header not agps");
            return false;
        }

        // TODO parse? and set something

        return true;
    }
}