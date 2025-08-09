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

public class HuaweiTrueSleepSequenceDataParser {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiTrueSleepSequenceDataParser.class);

    public static class SleepStage {
        private final long time;
        private final int stage;

        public SleepStage(long time, int stage) {
            this.time = time;
            this.stage = stage;
        }

        public int getStage() {
            return stage;
        }

        public long getTime() {
            return time;
        }

        @NonNull
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("SleepStages{");
            sb.append("time=").append(time);
            sb.append(", stage=").append(stage);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class SleepSummary {
        private long fallAsleepTime = -1;
        private long bedTime = -1;
        private long risingTime = -1;
        private long wakeupTime = -1;
        private int validData = -1;
        private int sleepDataQuality = -1;
        private int deepPart = -1;
        private int snoreFreq = -1;
        private int sleepScore = -1;
        private int sleepLatency = -1;
        private int sleepEfficiency = -1;
        private int minHeartrate = -1;
        private int maxHeartrate = -1;
        private double minOxygenSaturation = -1;
        private double maxOxygenSaturation = -1;
        private double minBreathrate = -1;
        private double maxBreathrate = -1;

        public long getFallAsleepTime() { return fallAsleepTime; }

        public void setFallAsleepTime(long fallAsleepTime) { this.fallAsleepTime = fallAsleepTime; }

        public long getBedTime() { return bedTime; }

        public void setBedTime(long bedTime) { this.bedTime = bedTime;}

        public long getRisingTime() { return risingTime;}

        public void setRisingTime(long risingTime) {this.risingTime = risingTime;}

        public long getWakeupTime() {return wakeupTime;}

        public void setWakeupTime(long wakeupTime) {this.wakeupTime = wakeupTime;}

        public int getValidData() { return validData; }

        public void setValidData(int validData) { this.validData = validData;}

        public int getSleepDataQuality() { return sleepDataQuality;}

        public void setSleepDataQuality(int sleepDataQuality) {this.sleepDataQuality = sleepDataQuality;}

        public int getDeepPart() {return deepPart;}

        public void setDeepPart(int deepPart) {this.deepPart = deepPart;}

        public int getSnoreFreq() {return snoreFreq;}

        public void setSnoreFreq(int snoreFreq) {this.snoreFreq = snoreFreq;}

        public int getSleepScore() {return sleepScore;}

        public void setSleepScore(int sleepScore) {this.sleepScore = sleepScore;}

        public int getSleepLatency() {return sleepLatency;}

        public void setSleepLatency(int sleepLatency) {this.sleepLatency = sleepLatency;}

        public int getSleepEfficiency() {return sleepEfficiency;}

        public void setSleepEfficiency(int sleepEfficiency) {this.sleepEfficiency = sleepEfficiency;}

        public int getMinHeartrate() {return minHeartrate;}

        public void setMinHeartrate(int minHeartrate) {this.minHeartrate = minHeartrate;}

        public int getMaxHeartrate() {return maxHeartrate;}

        public void setMaxHeartrate(int maxHeartrate) {this.maxHeartrate = maxHeartrate;}

        public double getMinOxygenSaturation() {return minOxygenSaturation;}

        public void setMinOxygenSaturation(double minOxygenSaturation) {this.minOxygenSaturation = minOxygenSaturation;}

        public double getMaxOxygenSaturation() {return maxOxygenSaturation;}

        public void setMaxOxygenSaturation(double maxOxygenSaturation) {this.maxOxygenSaturation = maxOxygenSaturation;}

        public double getMinBreathrate() {return minBreathrate;}

        public void setMinBreathrate(double minBreathrate) {this.minBreathrate = minBreathrate;}

        public double getMaxBreathrate() {return maxBreathrate;}

        public void setMaxBreathrate(double maxBreathrate) {this.maxBreathrate = maxBreathrate;}

        @NonNull
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SleepDetails{");
            sb.append("fallAsleepTime=").append(fallAsleepTime);
            sb.append(", bedTime=").append(bedTime);
            sb.append(", risingTime=").append(risingTime);
            sb.append(", wakeupTime=").append(wakeupTime);
            sb.append(", validData=").append(validData);
            sb.append(", sleepDataQuality=").append(sleepDataQuality);
            sb.append(", deepPart=").append(deepPart);
            sb.append(", snoreFreq=").append(snoreFreq);
            sb.append(", sleepScore=").append(sleepScore);
            sb.append(", sleepLatency=").append(sleepLatency);
            sb.append(", sleepEfficiency=").append(sleepEfficiency);
            sb.append(", minHeartrate=").append(minHeartrate);
            sb.append(", maxHeartrate=").append(maxHeartrate);
            sb.append(", minOxygenSaturation=").append(minOxygenSaturation);
            sb.append(", maxOxygenSaturation=").append(maxOxygenSaturation);
            sb.append(", minBreathrate=").append(minBreathrate);
            sb.append(", maxBreathrate=").append(maxBreathrate);
            sb.append('}');
            return sb.toString();
        }
    }

    public static int readAsInteger(byte[] data, int def) {
        if (data == null || data.length == 0 || data.length > 4) {
            return def;
        }
        // NOTE: Looks like validData should be float but the watch returns it as integer.
        // unsigned integer  stored as 0xBF800000 = 10111111 10000000 00000000 00000000
        // for float -1.
        if(Arrays.equals(data,new byte[]{(byte) 0xBF, (byte) 0x80, 0x00, 0x00})) {
            return def;
        }
        int res = 0;
        for (int i = 0; i < data.length; i++) {
            res |= (data[i] & 0xFF) << (((data.length - i) - 1) * 8);
        }
        return res;
    }

    public static long readAsLong(byte[] data, long def) {
        if (data == null || data.length == 0 || data.length > 8) {
            return def;
        }
        long res = 0;
        for (int i = 0; i < data.length; i++) {
            res |= (long) (data[i] & 0xFF) << (((data.length - i) - 1) * 8);
        }
        return res;
    }

    private static long getValueAsLong(int i, byte[] str2, long def) {
        if (i == 1 || i == 2 || i == 4) { // int, byte, short (I suppose)
            return readAsInteger(str2, (int) def);
        } else if (i == 3) { // long
            return readAsLong(str2, def);
        } else if (i == 5) { // string
            return def;
        } else if (i == 6) { // double
            return (long) Double.longBitsToDouble(readAsLong(str2, def));
        } else {
            return def;
        }
    }

    private static double getValueAsDouble(int i, byte[] str2, double def) {
        if (i == 1 || i == 2 || i == 4) { // int, byte, short (I suppose)
            return readAsInteger(str2, (int) def);
        } else if (i == 3) { // long
            return readAsLong(str2, (int) def);
        } else if (i == 5) { // string
            return def;
        } else if (i == 6) { // double
            return Double.longBitsToDouble(readAsLong(str2, (int) def));
        } else {
            return def;
        }
    }

    private static void fillSleepSummary(SleepSummary details, int dictId, int dataType, byte[] value) {
        switch (dictId) {
            case 700013686:
                long fallAsleepTime = getValueAsLong(dataType, value, -1);
                if(fallAsleepTime >=0)
                    details.setFallAsleepTime(fallAsleepTime);
                break;
            case 700013298:
                long bedTime = getValueAsLong(dataType, value, -1);
                if(bedTime >=0)
                    details.setBedTime(bedTime);
                break;
            case 700013973:
                long risingTime = getValueAsLong(dataType, value, -1);
                if(risingTime >=0)
                    details.setRisingTime(risingTime);
                break;
            case 700013156:
                long wakeupTime = getValueAsLong(dataType, value, -1);
                if(wakeupTime >=0)
                    details.setWakeupTime(wakeupTime);
                break;
            case 700013786:
                long validData = getValueAsLong(dataType, value, -1);
                if(validData >=0 && validData <= Integer.MAX_VALUE)
                    details.setValidData((int) validData);
                break;
            case 700013254:
                long sleepDataQuality = getValueAsLong(dataType, value, -1);
                if(sleepDataQuality >=0 && sleepDataQuality <= Integer.MAX_VALUE)
                    details.setSleepDataQuality((int) sleepDataQuality);
                break;
            case 700013679:
                long deepPart = getValueAsLong(dataType, value, -1);
                if(deepPart >=0 && deepPart <= Integer.MAX_VALUE)
                    details.setDeepPart((int) deepPart);
                break;
            case 700013721:
                long snoreFreq = getValueAsLong(dataType, value, -1);
                if(snoreFreq >=0 && snoreFreq <= Integer.MAX_VALUE)
                    details.setSnoreFreq((int) snoreFreq);
                break;
            case 700013245:
                long sleepScore = getValueAsLong(dataType, value, -1);
                if(sleepScore >=0 && sleepScore <= Integer.MAX_VALUE)
                    details.setSleepScore((int) sleepScore);
                break;
            case 700013713:
                long sleepLatency = getValueAsLong(dataType, value, -1);
                if(sleepLatency >=0 && sleepLatency <= Integer.MAX_VALUE)
                    details.setSleepLatency((int) sleepLatency);
                break;
            case 700013232:
                long sleepEfficiency = getValueAsLong(dataType, value, -1);
                if(sleepEfficiency >=0 && sleepEfficiency <= Integer.MAX_VALUE)
                    details.setSleepEfficiency((int) sleepEfficiency);
                break;
            case 700013436:
                long minHeartrate = getValueAsLong(dataType, value, -1);
                if(minHeartrate >= -1 && minHeartrate <= 255)
                    details.setMinHeartrate((int) minHeartrate);
                break;
            case 700013502:
                long maxHeartrate = getValueAsLong(dataType, value, -1);
                if(maxHeartrate >=0 && maxHeartrate <= 255)
                    details.setMaxHeartrate((int) maxHeartrate);
                break;
            case 700013340:
                double minOxygenSaturation = getValueAsDouble(dataType, value, -1);
                if(minOxygenSaturation >=0)
                    details.setMinOxygenSaturation((int) minOxygenSaturation);
                break;
            case 700013026:
                double maxOxygenSaturation = getValueAsDouble(dataType, value, -1);
                if(maxOxygenSaturation >=0)
                    details.setMaxOxygenSaturation((int) maxOxygenSaturation);
                break;
            case 700013646:
                double minBreathrate = getValueAsDouble(dataType, value, -1);
                if(minBreathrate >=0 && minBreathrate <= Integer.MAX_VALUE)
                    details.setMinBreathrate((int) minBreathrate);
                break;
            case 700013492:
                double maxBreathrate = getValueAsDouble(dataType, value, -1);
                if(maxBreathrate >=0 && maxBreathrate <= Integer.MAX_VALUE)
                    details.setMaxBreathrate((int) maxBreathrate);
                break;
            default:
                LOG.info("Unknown dictId: {}", dictId);
        }
    }

    private static SleepSummary parseTLVData(byte[] str) {
        SleepSummary details = new SleepSummary();
        HuaweiTLV tlv = new HuaweiTLV().parse(str);
        List<HuaweiTLV> tlvs = tlv.getAllContainerObjects();
        for (HuaweiTLV tv : tlvs) {
            List<HuaweiTLV> tlvs2 = tv.getAllContainerObjects();
            for (HuaweiTLV tv2 : tlvs2) {
                try {
                    int dictId = tv2.getAsInteger(0x03);
                    int dataType = tv2.getAsInteger(0x04);
                    byte[] value = tv2.getBytes(0x5);
                    fillSleepSummary(details, dictId, dataType, value);
                } catch (Exception e) {
                    LOG.error("Sleep dict sync: tag is missing", e);
                }
            }

        }
        return details;
    }


    public static SleepSummary parseSleepDataSummary(HuaweiSequenceDataParser.SequenceFileData fileData, HuaweiSequenceDataParser.SequenceData data) {
        // summary type 2 is TLV. TLV works only for fileType != 2 (or maybe only for 1)
        // currently only data version 1 supported
        if (data != null && data.getDataVersion() == 1 && data.getSummaryType() == 2 && fileData.getFileType() != 2) {
            return parseTLVData(data.getSummary());
        }
        return null;
    }

    public static void correctSummary(SleepSummary summary) {
        if(summary.validData == -1) {
            summary.setBedTime(0);
            summary.setRisingTime(0);
            summary.setSleepScore(-1);
            summary.setSleepDataQuality(-1);
            summary.setDeepPart(-1);
            summary.setSnoreFreq(-1);
            summary.setSleepEfficiency(-1);
            summary.setSleepLatency(-1);
        }
    }

    private static long adjustTimeToMinute(long time) {
        if (time % 60 != 0) {
            time = (time / 60) * 60;
        }
        return time;
    }

    public static long getTime(long fallAsleepTime, long bedTime, int validData, boolean supportsBedTime) {
        if (!supportsBedTime || validData == -1) {
            return adjustTimeToMinute(fallAsleepTime);
        }
        return  adjustTimeToMinute(bedTime);
    }

    public static List<SleepStage> parseSleepDetails(byte[] data, long time) {
        if (data.length % 4 != 0) {
            LOG.warn("detail length error");
            return null;
        }
        List<SleepStage> result = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() >= 4) {
            int duration = buffer.getInt();
            int status = buffer.get();
            byte[] extra = new byte[3];
            buffer.get(extra);

            if (duration == 0 || status == 0 || duration % 60 != 0) {
                LOG.error("invalid record duration: {} status: {} pos: {}", duration, status, buffer.position());
                continue;
            }
            int minutes = duration / 60;
            if (minutes > 1440) {
                LOG.error("more than in one day: {} pos: {}", minutes, buffer.position());
                continue;
            }
            for (int i = 0; i < minutes; i++) {
                result.add(new SleepStage(time, status));
                time += 60;
            }
        }
        return result;
    }
}
