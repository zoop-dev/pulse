/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HuaweiSequenceDataParser {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiSequenceDataParser.class);

    public static class SequenceData {
        private int endTime;
        private byte[] details;
        private byte[] extraData;
        private int startTime;
        private int summaryType;
        private int dataVersion; //??
        private byte[] summary;

        public int getStartTime() { return this.startTime;}
        public void setStartTime(int startTime) {
            this.startTime = startTime;
        }

        public int getEndTime() {
            return this.endTime;
        }
        public void setEndTime(int endTime) {
            this.endTime = endTime;
        }

        public int getDataVersion() { return this.dataVersion;}
        public void setDataVersion(int version) { this.dataVersion = version;}

        public int getSummaryType() { return this.summaryType;}
        public void setSummaryType(int summaryType) {
            this.summaryType = summaryType;
        }

        public void setExtraData(byte[] extraData) { this.extraData = extraData;}

        public byte[] getSummary() { return this.summary;}
        public void setSummary(byte[] summary) { this.summary = summary;}

        public byte[] getDetails() {
            return this.details;
        }
        public void setDetails(byte[] details) { this.details = details; }

        @NonNull
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SequenceData{");
            sb.append("startTime=").append(startTime);
            sb.append(", endTime=").append(endTime);
            sb.append(", version=").append(dataVersion);
            sb.append(", summaryType=").append(summaryType);
            sb.append(", summary=").append(Arrays.toString(summary));
            sb.append(", details=").append(Arrays.toString(details));
            sb.append(", extraData=").append(Arrays.toString(extraData));
            sb.append('}');
            return sb.toString();
        }
    }

    public static class SequenceFileData {

        private long dataSize;
        private int fileType;
        private byte[] extraData;
        private int dictId;
        private final List<SequenceData> sequenceDataList = new ArrayList<>();
        private int fileVersion;

        public void setDataSize(long dataSize) {
            this.dataSize = dataSize;
        }

        public int getDictId() { return this.dictId; }

        public void setDictId(int dictId) {
            this.dictId = dictId;
        }

        public int getFileType() {
            return this.fileType;
        }

        public void setFileType(int fileType) {
            this.fileType = fileType;
        }

        public int getFileVersion() {
            return this.fileVersion;
        }

        public void setFileVersion(int fileVersion) {
            this.fileVersion = fileVersion;
        }

        public void setExtraData(byte[] extraData) { this.extraData = extraData;}

        public List<SequenceData> getSequenceDataList() {
            return this.sequenceDataList;
        }

        @NonNull
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SequenceFileData{");
            sb.append("dataSize=").append(dataSize);
            sb.append(", fileType=").append(fileType);
            sb.append(", dictId=").append(dictId);
            sb.append(", fileVersion=").append(fileVersion);
            sb.append(", sequenceDataList=").append(sequenceDataList);
            sb.append(", extraData=").append(Arrays.toString(extraData));
            sb.append('}');
            return sb.toString();
        }
    }

    private static void parseSequenceData(ByteBuffer buffer, SequenceFileData fileData) {

        while (buffer.remaining() >= 32) {
            SequenceData data = new SequenceData();

            data.setStartTime(buffer.getInt());
            data.setEndTime(buffer.getInt());
            data.setDataVersion(buffer.get());
            data.setSummaryType(buffer.get());
            int summaryDataLen = buffer.getShort();
            int detailLen = buffer.getInt();
            byte[] extraData = new byte[16];
            buffer.get(extraData);
            data.setExtraData(extraData);

            // parseSummary
            if (data.getDataVersion() == 1) {
                if (buffer.remaining() < summaryDataLen) {
                    LOG.error("summaryDataLen {} exceeds remaining: {}", summaryDataLen, buffer.remaining());
                } else {
                    byte[] bArr1 = new byte[summaryDataLen];
                    buffer.get(bArr1);
                    data.setSummary(bArr1);
                }
            }
            // parse details
            // TODO: In some my test files this value is too big. I suppose 20 Mb will be enough for now. Additional investigation required
            if (detailLen <= 0 || detailLen > (20 * 1025 * 1024)) {
                LOG.error("detailLen is invalid: {}", detailLen);
            } else {
                byte[] bArr2 = new byte[detailLen];
                if (buffer.remaining() < detailLen) {
                    LOG.error("detailLen {} exceeds remaining: {}", detailLen, buffer.remaining());
                }
                buffer.get(bArr2);
                data.setDetails(bArr2);
            }
            if (fileData.getSequenceDataList() != null) {
                fileData.getSequenceDataList().add(data);
            }
        }
        if (buffer.remaining() > 0) {
            LOG.error("sequence data remaining: {}", buffer.remaining());
        }
    }


    public static SequenceFileData parseSequenceFileData(byte[] data) {
        if(data == null) {
            LOG.error("Data is null");
            return null;
        }
        if (data.length < 32) {
            LOG.error("Invalid file length: {}", data.length);
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);

        SequenceFileData fileData = new SequenceFileData();
        fileData.setDataSize(buffer.getInt());
        fileData.setDictId(buffer.getInt());
        fileData.setFileType(buffer.getShort());
        fileData.setFileVersion(buffer.getShort());
        byte[] extraData = new byte[20];
        buffer.get(extraData);
        fileData.setExtraData(extraData);
        parseSequenceData(buffer, fileData);
        return fileData;
    }
}
