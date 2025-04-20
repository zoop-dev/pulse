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
import nodomain.freeyourgadget.gadgetbridge.util.GBZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;

public class ZeppOsAgpsFile {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAgpsFile.class);

    private final byte[] fileBytes;

    public ZeppOsAgpsFile(final byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public boolean isValid() {
        if (GBZipFile.isZipFile(fileBytes)) {
            return isValidAsEpoZip();
        } else {
            return isValidAsUihh();
        }
    }

    private boolean isValidAsEpoZip() {
        final GBZipFile zipFile = new GBZipFile(fileBytes);

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
            // EPO zip - repackage into UIHH
            final UIHHContainer uihh = new UIHHContainer();

            final GBZipFile zipFile = new GBZipFile(fileBytes);

            try {
                uihh.addFile(UIHHContainer.FileType.AGPS_EPO_GR_3, zipFile.getFileFromZip("EPO_GR_3.DAT"));
                uihh.addFile(UIHHContainer.FileType.AGPS_EPO_GAL_7, zipFile.getFileFromZip("EPO_GAL_7.DAT"));
                uihh.addFile(UIHHContainer.FileType.AGPS_EPO_BDS_3, zipFile.getFileFromZip("EPO_BDS_3.DAT"));
            } catch (final ZipFileException e) {
                throw new IllegalStateException("Failed to read file from zip", e);
            }

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
