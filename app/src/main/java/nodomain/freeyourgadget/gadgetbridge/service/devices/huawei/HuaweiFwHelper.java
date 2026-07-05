/*  Copyright (C) 2024 Vitalii Tomin

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiBinAppParser;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.ota.HuaweiOTAFileList;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileUpload;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;
import nodomain.freeyourgadget.gadgetbridge.util.audio.AudioInfo;
import nodomain.freeyourgadget.gadgetbridge.util.audio.MusicUtils;

public class HuaweiFwHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiFwHelper.class);

    private final Uri uri;

    private byte[] fw;
    private byte fileType = 0;
    String fileName = "";


    Bitmap previewBitmap;
    HuaweiWatchfaceManager.WatchfaceDescription watchfaceDescription;
    HuaweiAppManager.AppConfig appConfig;
    AudioInfo musicInfo;
    Context mContext;

    public boolean isOfflineMap = false;
    public String mapName;
    public int mapVersion = 0;
    public boolean isMapContour = false;


    public boolean isFirmware = false;
    public HuaweiOTAFileList.OTAFileInfo fwInfo = null;

    public HuaweiFwHelper(final Uri uri, final Context context) {
        this.uri = uri;
        this.mContext = context;

        parseFile();
    }

    private void parseFile() {
        if (parseAsOfflineMap()) {
            isOfflineMap = true;
        } else if (parseAsFirmware()) {
            isFirmware = true;
        } else if (parseAsMusic()) {
            fileType = FileUpload.Filetype.music;
        } else if (parseAsApp()) {
            assert appConfig != null;
            assert appConfig.bundleName != null;
            fileType = FileUpload.Filetype.app;
        } else if (parseAsWatchFace()) {
            assert watchfaceDescription != null;
            assert watchfaceDescription.screen != null;
            assert watchfaceDescription.title != null;
            fileType = FileUpload.Filetype.watchface;
        }
    }

    private boolean parseAsOfflineMap() {
        try {
            final UriHelper uriHelper = UriHelper.get(uri, this.mContext);

            String currentFileName = uriHelper.getFileName();

            if (TextUtils.isEmpty(currentFileName) || !currentFileName.toLowerCase().endsWith(".bin")) {
                return false;
            }

            String withoutExt = currentFileName.substring(0, currentFileName.length() - 4);
            boolean isContour = withoutExt.endsWith("_contour");
            if(!withoutExt.equals("global")) {
                String idPart = isContour ? withoutExt.substring(0, withoutExt.length() - 8) : withoutExt;
                long mapId;
                try {
                    mapId = Long.parseLong(idPart);
                } catch (NumberFormatException e) {
                    return false;
                }
                LOG.info("MapId: {}", mapId);
            } else {
                LOG.info("World overview map");
            }

            LOG.info("Contour: {}", isContour);
            isMapContour = isContour;

            InputStream inputStream = uriHelper.openInputStream();

            byte[] header = new byte[0x440];
            int nRead = inputStream.read(header, 0, header.length);
            if (nRead != header.length)
                return false;
            inputStream.close();

            if (header[0] != 'o' || header[1] != 'f' || header[2] != 'f' ||
                    header[3] != 'v' || header[4] != 'm' || header[5] != 'p') { // offvmp
                return false;
            }
            ByteBuffer buffer = ByteBuffer.wrap(header);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            buffer.position(6);

            int version = buffer.getInt();
            LOG.info("version: {}", version);

            int indexOffset = buffer.getInt();
            LOG.info("index_offset: {}", indexOffset);

            if (indexOffset > uriHelper.getFileSize()) {
                return false;
            }

            buffer.position(63);
            byte[] countryBytes = new byte[511];
            buffer.get(countryBytes);

            String countryName = new String(countryBytes, StandardCharsets.UTF_8).trim();
            LOG.info("country_name: {}", countryName);

            mapName = countryName;
            mapVersion = version;

            fw = null;
            fileName = currentFileName;
            LOG.info("OfflineMap: valid");
            return true;
        } catch (Exception e) {
            LOG.error("OfflineMap: error occurred", e);
        }
        return false;
    }

    private boolean parseAsFirmware() {
        try {
            final UriHelper uriHelper = UriHelper.get(uri, this.mContext);

            ZipInputStream stream = new ZipInputStream(uriHelper.openInputStream());
            byte[] fileListXml = null;
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                if (entry.getName().equals("filelist.xml")) {
                    fileListXml = GBZipFile.readAllBytes(stream);
                    break;
                }
            }
            stream.close();

            if (fileListXml == null) {
                LOG.info("Firmware: filelist.xml not found");
                return false;
            }

            HuaweiOTAFileList fileList = HuaweiOTAFileList.getFileList(new String(fileListXml));
            if (fileList == null) {
                LOG.error("Firmware: filelist.xml is invalid");
                return false;
            }

            LOG.info("Firmware: {}", fileList.component.name);

            for (HuaweiOTAFileList.OTAFileInfo info : fileList.files) {
                LOG.info("Firmware: file {}", info.dpath);
                if (info.dpath.endsWith(".bin.apk")) {
                    fwInfo = info;
                }
            }

            if (fwInfo == null) {
                LOG.error("Firmware: required files not found");
                return false;
            }

            boolean valid = false;
            stream = new ZipInputStream(uriHelper.openInputStream());
            while ((entry = stream.getNextEntry()) != null) {
                if (entry.getName().equals(fwInfo.dpath)) {
                    valid = true;
                    break;
                }
            }
            stream.close();

            LOG.info("Firmware: valid: {}", valid);
            return valid;
        } catch (Exception e) {
            LOG.error("Firmware: error occurred", e);
        }

        return false;
    }

    private byte[] getFileData(UriHelper uriHelper) throws IOException {
        InputStream inputStream = uriHelper.openInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[1000];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        inputStream.close();
        return buffer.toByteArray();
    }

    boolean parseAsMusic() {
        try {
            final UriHelper uriHelper = UriHelper.get(uri, this.mContext);
            musicInfo = MusicUtils.audioInfoFromUri(mContext, uri);
            if (musicInfo == null)
                return false;

            byte[] musicData = getFileData(uriHelper);

            fileName = musicInfo.getFileName();
            fw = musicData;

            return true;
        } catch (FileNotFoundException e) {
            LOG.error("Music: File was not found {}", e.getMessage());
        } catch (IOException e) {
            LOG.error("Music: General IO error occurred {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Music: Unknown error occurred", e);
        }
        return false;
    }


    boolean parseAsApp() {

        try {
            final UriHelper uriHelper = UriHelper.get(uri, this.mContext);

            byte[] appData = getFileData(uriHelper);

            HuaweiBinAppParser app = new HuaweiBinAppParser();
            app.parseData(appData);

            byte[] config = app.getEntryContent("config.json");
            if (config == null)
                return false;
            appConfig = new HuaweiAppManager.AppConfig(new String(config));
            fileName = app.getPackageName() + "_INSTALL"; //TODO: INSTALL or UPDATE suffix

            fw = appData;

            byte[] icon = app.getEntryContent("icon_small.png");
            if (icon != null) {
                previewBitmap = BitmapFactory.decodeByteArray(icon, 0, icon.length);
            }

            return true;

        } catch (FileNotFoundException e) {
            LOG.error("App: File was not found{}", e.getMessage());
        } catch (IOException e) {
            LOG.error("App: General IO error occurred {}", e.getMessage());
        } catch (HuaweiBinAppParser.HuaweiBinAppParseError e) {
            LOG.error("App: Error parsing app File {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("App: Unknown error occurred", e);
        }
        return false;
    }

    public UriHelper getUriHelper() {
        try {
            return UriHelper.get(uri, this.mContext);
        } catch (IOException e) {
            LOG.error("getUriHelper: General IO error occurred {}", e.getMessage());
        }
        return null;
    }

    public byte[] getBytes() {
        return fw;
    }

    public void unsetFwBytes() {
        this.fw = null;
    }

    boolean parseAsWatchFace() {
        boolean isWatchface = false;

        try {
            final UriHelper uriHelper = UriHelper.get(uri, this.mContext);

            GBZipFile watchfacePackage = new GBZipFile(uriHelper.openInputStream());
            byte[] bytesDescription = watchfacePackage.getFileFromZip("description.xml");

            // check if description file contents BOM
            ByteBuffer bb = ByteBuffer.wrap(bytesDescription);
            byte[] bom = new byte[3];
            // get the first 3 bytes
            bb.get(bom, 0, bom.length);
            String content = GB.hexdump(bom);
            String xmlDescription;
            if ("efbbbf".equalsIgnoreCase(content)) {
                byte[] contentAfterFirst3Bytes = new byte[bytesDescription.length - 3];
                bb.get(contentAfterFirst3Bytes, 0, contentAfterFirst3Bytes.length);
                xmlDescription = new String(contentAfterFirst3Bytes);
            } else {
                xmlDescription = new String(bytesDescription);
            }

            watchfaceDescription = new HuaweiWatchfaceManager.WatchfaceDescription(xmlDescription);
            if (watchfacePackage.fileExists("preview/cover.jpg")) {
                final byte[] preview = watchfacePackage.getFileFromZip("preview/cover.jpg");
                previewBitmap = BitmapFactory.decodeByteArray(preview, 0, preview.length);
            }

            String watchfacePath;
            if (watchfaceDescription.isHonor) {
                watchfacePath = "com.honor.watchface";
            } else {
                watchfacePath = "com.huawei.watchface";
            }

            byte[] watchfaceZip = watchfacePackage.getFileFromZip(watchfacePath);
            try {
                GBZipFile watchfaceBinZip = new GBZipFile(watchfaceZip);
                fw = watchfaceBinZip.getFileFromZip("watchface.bin");
            } catch (ZipFileException e) {
                LOG.error("Unable to get watchfaceZip,  it seems older already watchface.bin");
                fw = watchfaceZip;
            }
            isWatchface = true;

        } catch (ZipFileException e) {
            LOG.error("Watchface: Unable to read file {}", e.getMessage());
        } catch (FileNotFoundException e) {
            LOG.error("Watchface: File was not found {}", e.getMessage());
        } catch (IOException e) {
            LOG.error("Watchface: General IO error occurred {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Watchface: Unknown error occurred", e);
        }

        return isWatchface;
    }

    public boolean isWatchface() {
        return fileType == FileUpload.Filetype.watchface;
    }

    public boolean isAPP() {
        return fileType == FileUpload.Filetype.app;
    }

    public boolean isMusic() {
        return fileType == FileUpload.Filetype.music;
    }

    public boolean isValid() {
        return isWatchface() || isAPP() || isMusic() || isFirmware || isOfflineMap;
    }

    public Bitmap getPreviewBitmap() {
        return previewBitmap;
    }

    public HuaweiWatchfaceManager.WatchfaceDescription getWatchfaceDescription() {
        return watchfaceDescription;
    }

    public HuaweiAppManager.AppConfig getAppConfig() {
        return appConfig;
    }

    public AudioInfo getMusicInfo() {
        return musicInfo;
    }

    public byte getFileType() {
        return fileType;
    }

    public String getFileName() {
        return fileName;
    }


}
