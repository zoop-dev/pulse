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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class HuaweiStressParser {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiStressParser.class);

    public static class StressData {
        public long startTime;
        public long endTime;
        public int algorithm;
        public byte score;
        public float scoreFactor;
        public byte level;
        public byte cFlag;
        public byte type;
        public byte accFlag;
        public byte ppgFlag;
        public List<Float> features = new ArrayList<>();


        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("StressData{");
            sb.append("startTime=").append(startTime);
            sb.append(", endTime=").append(endTime);
            sb.append(", algorithm=").append(algorithm);
            sb.append(", score=").append(score);
            sb.append(", scoreFactor=").append(scoreFactor);
            sb.append(", level=").append(level);
            sb.append(", cFlag=").append(cFlag);
            sb.append(", type=").append(type);
            sb.append(", accFlag=").append(accFlag);
            sb.append(", ppgFlag=").append(ppgFlag);
            sb.append(", features=").append(features);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class RriFileData {
        public long fileSize;
        public int version;
        public int bitmap;
        public List<StressData> stressData;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("RriFileData{");
            sb.append("fileSize=").append(fileSize);
            sb.append(", version=").append(version);
            sb.append(", bitmap=").append(bitmap);
            sb.append(", stressData=").append(stressData);
            sb.append('}');
            return sb.toString();
        }
    }

    public static String stressDataToJsonStr(StressData data) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("startTime", data.startTime);
            obj.put("endTime", data.endTime);
            obj.put("algorithm", data.algorithm);
            obj.put("score", data.score);
            obj.put("scoreFactor", data.scoreFactor);
            obj.put("level", data.level);
            obj.put("cFlag", data.cFlag);
            obj.put("type", data.type);
            obj.put("accFlag", data.accFlag);
            obj.put("ppgFlag", data.ppgFlag);
            JSONArray arr = new JSONArray();
            for(Float f: data.features) {
                arr.put(f);
            }
            obj.put("features", arr);
            return obj.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public static StressData stressDataFromJsonStr(String jsonStr) {
        StressData st = new StressData();
        try {
            JSONObject obj = new JSONObject(jsonStr);
            st.startTime = obj.getLong("startTime");
            st.endTime = obj.getLong("endTime");
            st.algorithm = obj.getInt("algorithm");
            st.score = (byte) obj.getInt("score");
            st.scoreFactor = (byte) obj.getInt("scoreFactor");
            st.level = (byte) obj.getInt("level");
            st.cFlag = (byte) obj.getInt("cFlag");
            st.type = (byte) obj.getInt("type");
            st.accFlag = (byte) obj.getInt("accFlag");
            st.ppgFlag = (byte) obj.getInt("ppgFlag");
            JSONArray arr = obj.getJSONArray("features");
            for(int i = 0; i < arr.length(); i++) {
                st.features.add((float) arr.getDouble(i));
            }
        } catch (JSONException e) {
            return null;
        }
        if(st.endTime == 0 || st.score == 0 || st.features.size() != 12) {
            return null;
        }

        return st;
    }

    public static RriFileData parseRri(byte[] rriData) {
        if (rriData == null || rriData.length < 48) {
            LOG.error("stress data is invalid");
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(rriData);
        buffer.order(ByteOrder.BIG_ENDIAN);

        RriFileData fileData = new RriFileData();

        fileData.fileSize = buffer.getLong();
        fileData.bitmap = buffer.getShort();
        fileData.version = buffer.getShort();


        LOG.info("configFileSize :{}", fileData.fileSize);
        LOG.info("configBitmap :{}", fileData.bitmap);
        LOG.info("configVersion :{}", fileData.version);

        // TODO: I don't have files with other version. Implement when discovered.
        if (fileData.version == 3) {
            // Skip unknown or reserved data
            buffer.position(48);

            fileData.stressData = new ArrayList<>();

            // Each entry 66 bytes.
            while (buffer.remaining() >= 66) {
                ByteBuffer entryBuffer = ByteBuffer.allocate(66);
                buffer.get(entryBuffer.array());

                StressData stressData = new StressData();
                int startTime = entryBuffer.getInt();
                if (startTime == 0) {
                    LOG.warn("startTime == 0, skip ??");
                    continue;
                }
                stressData.startTime = startTime * 1000L;
                int endTime = entryBuffer.getInt();
                if (endTime == 0) {
                    LOG.warn("endTime == 0, skip ??");
                    continue;
                }
                stressData.endTime = endTime * 1000L;
                stressData.algorithm = entryBuffer.getInt();
                stressData.score = entryBuffer.get();
                stressData.level = entryBuffer.get();
                stressData.cFlag = entryBuffer.get();
                stressData.type = entryBuffer.get();
                stressData.accFlag = entryBuffer.get();
                stressData.ppgFlag = entryBuffer.get();

                for (int i = 0; i < 12; i++) {
                    stressData.features.add(entryBuffer.getFloat());
                }
                fileData.stressData.add(stressData);
            }
            // Sort data by end time. We need to save latest data for sending to device. Not sure.
            if (fileData.stressData != null && !fileData.stressData.isEmpty()) {
                fileData.stressData.sort((a, b) -> {
                    if (a != null && b != null) {
                        return Long.compare(a.endTime, b.endTime);
                    }
                    return 0;
                });
            }
        } else {
            LOG.warn("Rri file: unknown version: {}", fileData.version);
        }
        LOG.info("fileData:{}", fileData);
        return fileData;

    }
}
