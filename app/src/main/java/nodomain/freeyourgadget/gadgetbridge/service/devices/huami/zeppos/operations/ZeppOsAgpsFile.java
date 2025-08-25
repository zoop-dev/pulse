/*  Copyright (C) 2023-2024 José Rebelo

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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.UIHHContainer;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;

public class ZeppOsAgpsFile {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAgpsFile.class);

    private static final byte[] BRM_HEADER = new byte[]{
            (byte) 0xb5, 0x62, 0x13, 0x20,
            0x4c, 0x00, 0x00, 0x00, 0x01,
    };

    private final byte[] fileBytes;

    public ZeppOsAgpsFile(final byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public boolean isValid() {
        if (GBZipFile.isZipFile(fileBytes)) {
            final GBZipFile zipFile = new GBZipFile(fileBytes);
            return isValidAsEpoZip(zipFile) || isValidAsBrmZip(zipFile);
        } else {
            return isValidAsRawBrm() || isValidAsUihh();
        }
    }

    private boolean isValidAsEpoZip(final GBZipFile zipFile) {
        try {
            final byte[] manifestBin = zipFile.getFileFromZip("META-INF/MANIFEST.MF");
            if (manifestBin == null) {
                LOG.warn("Failed to get MANIFEST from zip");
                return false;
            }

            final String appJsonString = new String(manifestBin, StandardCharsets.UTF_8)
                    // Remove UTF-8 BOM if present
                    .replace("\uFEFF", "");
            final JSONObject jsonObject = new JSONObject(appJsonString);
            return jsonObject.getString("manifestVersion").equals("2.0") &&
                    zipFile.fileExists("EPO_BDS_3.DAT") &&
                    zipFile.fileExists("EPO_GAL_7.DAT") &&
                    zipFile.fileExists("EPO_GR_3.DAT");
        } catch (final Exception e) {
            LOG.error("Failed to parse read MANIFEST or check file", e);
        }

        return false;
    }

    private boolean isValidAsBrmZip(final GBZipFile zipFile) {
        try {
            // There's another lto2dv5.brm but we don't what type it gets sent as
            return zipFile.fileExists("lto7dv5.brm");
        } catch (final Exception e) {
            LOG.error("Failed to check brm files", e);
        }

        return false;
    }

    private boolean isValidAsRawBrm() {
        // Avoid installing the smaller lto2dv5.brm, since the header seems to be the same
        return ArrayUtils.equals(fileBytes, BRM_HEADER, 0)
                && fileBytes.length > 300_000 && fileBytes.length < 500_000;
    }

    private boolean isValidAsUihh() {
        final UIHHContainer uihh = UIHHContainer.fromRawBytes(fileBytes);
        if (uihh == null) {
            return false;
        }

        final List<UIHHContainer.FileType> fileTypes = uihh.getFileTypes();
        final List<UIHHContainer.FileType> expectedFileTypes = Arrays.asList(
                UIHHContainer.FileType.GPS_ALM_BIN,
                UIHHContainer.FileType.GLN_ALM_BIN,
                UIHHContainer.FileType.LLE_BDS_LLE,
                UIHHContainer.FileType.LLE_GPS_LLE,
                UIHHContainer.FileType.LLE_GLO_LLE,
                UIHHContainer.FileType.LLE_GAL_LLE,
                UIHHContainer.FileType.LLE_QZSS_LLE
        );

        if (fileTypes.size() != expectedFileTypes.size()) {
            LOG.warn("uihh file types mismatch - expected {}, found {}", expectedFileTypes.size(), fileTypes.size());
            return false;
        }

        for (final UIHHContainer.FileType fileType : expectedFileTypes) {
            if (!fileTypes.contains(fileType)) {
                LOG.warn("uihh is missing file type {}", fileType);
                return false;
            }
        }

        return true;
    }

    public byte[] getUihhBytes() {
        if (GBZipFile.isZipFile(fileBytes)) {
            // zip - repackage into UIHH
            final UIHHContainer uihh = new UIHHContainer();

            final GBZipFile zipFile = new GBZipFile(fileBytes);

            try {
                if (isValidAsEpoZip(zipFile)) {
                    uihh.addFile(UIHHContainer.FileType.AGPS_EPO_GR_3, zipFile.getFileFromZip("EPO_GR_3.DAT"));
                    uihh.addFile(UIHHContainer.FileType.AGPS_EPO_GAL_7, zipFile.getFileFromZip("EPO_GAL_7.DAT"));
                    uihh.addFile(UIHHContainer.FileType.AGPS_EPO_BDS_3, zipFile.getFileFromZip("EPO_BDS_3.DAT"));
                } else if (isValidAsBrmZip(zipFile)) {
                    uihh.addFile(UIHHContainer.FileType.AGPS_BRM_LTO_7D, zipFile.getFileFromZip("lto7dv5.brm"));
                } else {
                    throw new IllegalStateException("Unknown agps zip file - this should never happen");
                }
            } catch (final ZipFileException e) {
                throw new IllegalStateException("Failed to read file from zip", e);
            }

            return uihh.toRawBytes();
        } else if (isValidAsRawBrm()) {
            // lto7dv5.brm - repackage into UIHH
            final UIHHContainer uihh = new UIHHContainer();
            uihh.addFile(UIHHContainer.FileType.AGPS_BRM_LTO_7D, fileBytes);
            return uihh.toRawBytes();
        } else {
            final UIHHContainer uihhContainer = UIHHContainer.fromRawBytes(fileBytes);
            if (uihhContainer != null) {
                return fileBytes;
            }
        }

        throw new IllegalStateException("Unknown file bytes - this should never happen");
    }
}
