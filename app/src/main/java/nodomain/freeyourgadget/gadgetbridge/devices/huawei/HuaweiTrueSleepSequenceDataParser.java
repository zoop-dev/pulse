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
import java.util.List;

public class HuaweiTrueSleepSequenceDataParser extends HuaweiSequenceDataParserBase<HuaweiTrueSleepSequenceDataParser.SleepSummary> {
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
            return "SleepStages{" + "time=" + time +
                    ", stage=" + stage +
                    '}';
        }
    }

    public static class SleepSummary {
        public long fallAsleepTime = -1;
        public long bedTime = -1;
        public long risingTime = -1;
        public long wakeupTime = -1;
        public int validData = -1;
        public int sleepDataQuality = -1;
        public int deepPart = -1;
        public int snoreFreq = -1;
        public int sleepScore = -1;
        public int sleepLatency = -1;
        public int sleepEfficiency = -1;
        public int minHeartRate = -1;
        public int maxHeartRate = -1;
        public double minOxygenSaturation = -1;
        public double maxOxygenSaturation = -1;
        public double minBreathRate = -1;
        public double maxBreathRate = -1;
        public int hrvDayToBaseline = -1;
        public int maxHrvBaseline = -1;
        public int minHrvBaseline = -1;
        public int avgHrv = -1;
        public int breathRateDayToBaseline = -1;
        public int maxBreathRateBaseline = -1;
        public int minBreathRateBaseline = -1;
        public int avgBreathRate = -1;
        public int oxygenSaturationDayToBaseline = -1;
        public int maxOxygenSaturationBaseline = -1;
        public int minOxygenSaturationBaseline = -1;
        public int avgOxygenSaturation = -1;
        public int heartRateDayToBaseline = -1;
        public int maxHeartRateBaseline = -1;
        public int minHeartRateBaseline = -1;
        public int avgHeartRate = -1;
        public int rdi = -1; // Respiratory Disturbance Index, RDI is a numeric index which helps to define the degree of the apnea.
        public int wakeCount = -1;
        public int turnOverCount = -1;
        public long prepareSleepTime = -1;
        public int wakeUpFeeling = -1;
        public int sleepVersion = -1;

        @NonNull
        @Override
        public String toString() {
            return "SleepSummary{" + "fallAsleepTime=" + fallAsleepTime +
                    ", bedTime=" + bedTime +
                    ", risingTime=" + risingTime +
                    ", wakeupTime=" + wakeupTime +
                    ", validData=" + validData +
                    ", sleepDataQuality=" + sleepDataQuality +
                    ", deepPart=" + deepPart +
                    ", snoreFreq=" + snoreFreq +
                    ", sleepScore=" + sleepScore +
                    ", sleepLatency=" + sleepLatency +
                    ", sleepEfficiency=" + sleepEfficiency +
                    ", minHeartRate=" + minHeartRate +
                    ", maxHeartRate=" + maxHeartRate +
                    ", minOxygenSaturation=" + minOxygenSaturation +
                    ", maxOxygenSaturation=" + maxOxygenSaturation +
                    ", minBreathRate=" + minBreathRate +
                    ", maxBreathRate=" + maxBreathRate +
                    ", hrvDayToBaseline=" + hrvDayToBaseline +
                    ", maxHrvBaseline=" + maxHrvBaseline +
                    ", minHrvBaseline=" + minHrvBaseline +
                    ", avgHrv=" + avgHrv +
                    ", breathRateDayToBaseline=" + breathRateDayToBaseline +
                    ", maxBreathRateBaseline=" + maxBreathRateBaseline +
                    ", minBreathRateBaseline=" + minBreathRateBaseline +
                    ", avgBreathRate=" + avgBreathRate +
                    ", oxygenSaturationDayToBaseline=" + oxygenSaturationDayToBaseline +
                    ", maxOxygenSaturationBaseline=" + maxOxygenSaturationBaseline +
                    ", minOxygenSaturationBaseline=" + minOxygenSaturationBaseline +
                    ", avgOxygenSaturation=" + avgOxygenSaturation +
                    ", heartRateDayToBaseline=" + heartRateDayToBaseline +
                    ", maxHeartRateBaseline=" + maxHeartRateBaseline +
                    ", minHeartRateBaseline=" + minHeartRateBaseline +
                    ", avgHeartRate=" + avgHeartRate +
                    ", rdi=" + rdi +
                    ", wakeCount=" + wakeCount +
                    ", turnOverCount=" + turnOverCount +
                    ", prepareSleepTime=" + prepareSleepTime +
                    ", wakeUpFeeling=" + wakeUpFeeling +
                    ", sleepVersion=" + sleepVersion +
                    '}';
        }
    }


    protected void fillData(SleepSummary details, int dictId, int dataType, byte[] value) {
        switch (dictId) {
            case 700013686:
                long fallAsleepTime = getValueAsLong(dataType, value, -1);
                if(fallAsleepTime >=0)
                    details.fallAsleepTime = fallAsleepTime;
                break;
            case 700013298:
                long bedTime = getValueAsLong(dataType, value, -1);
                if(bedTime >=0)
                    details.bedTime = bedTime;
                break;
            case 700013973:
                long risingTime = getValueAsLong(dataType, value, -1);
                if(risingTime >=0)
                    details.risingTime = risingTime;
                break;
            case 700013156:
                long wakeupTime = getValueAsLong(dataType, value, -1);
                if(wakeupTime >=0)
                    details.wakeupTime = wakeupTime;
                break;
            case 700013786:
                long validData = getValueAsLong(dataType, value, -1);
                if(validData >=0 && validData <= Integer.MAX_VALUE)
                    details.validData = (int) validData;
                break;
            case 700013254:
                long sleepDataQuality = getValueAsLong(dataType, value, -1);
                if(sleepDataQuality >=0 && sleepDataQuality <= Integer.MAX_VALUE)
                    details.sleepDataQuality = (int) sleepDataQuality;
                break;
            case 700013679:
                long deepPart = getValueAsLong(dataType, value, -1);
                if(deepPart >=0 && deepPart <= Integer.MAX_VALUE)
                    details.deepPart = (int) deepPart;
                break;
            case 700013721:
                long snoreFreq = getValueAsLong(dataType, value, -1);
                if(snoreFreq >=0 && snoreFreq <= Integer.MAX_VALUE)
                    details.snoreFreq = (int) snoreFreq;
                break;
            case 700013245:
                long sleepScore = getValueAsLong(dataType, value, -1);
                if(sleepScore >=0 && sleepScore <= Integer.MAX_VALUE)
                    details.sleepScore = (int) sleepScore;
                break;
            case 700013713:
                long sleepLatency = getValueAsLong(dataType, value, -1);
                if(sleepLatency >=0 && sleepLatency <= Integer.MAX_VALUE)
                    details.sleepLatency = (int) sleepLatency;
                break;
            case 700013232:
                long sleepEfficiency = getValueAsLong(dataType, value, -1);
                if(sleepEfficiency >=0 && sleepEfficiency <= Integer.MAX_VALUE)
                    details.sleepEfficiency = (int) sleepEfficiency;
                break;
            case 700013436:
                long minHeartRate = getValueAsLong(dataType, value, -1);
                if(minHeartRate >= -1 && minHeartRate <= 255)
                    details.minHeartRate = (int) minHeartRate;
                break;
            case 700013502:
                long maxHeartRate = getValueAsLong(dataType, value, -1);
                if(maxHeartRate >=0 && maxHeartRate <= 255)
                    details.maxHeartRate = (int) maxHeartRate;
                break;
            case 700013340:
                double minOxygenSaturation = getValueAsDouble(dataType, value, -1);
                if(minOxygenSaturation >=0)
                    details.minOxygenSaturation = minOxygenSaturation;
                break;
            case 700013026:
                double maxOxygenSaturation = getValueAsDouble(dataType, value, -1);
                if(maxOxygenSaturation >=0)
                    details.maxOxygenSaturation = maxOxygenSaturation;
                break;
            case 700013646:
                long minBreathRate = getValueAsLong(dataType, value, -1);
                if(minBreathRate >=0 && minBreathRate <= Integer.MAX_VALUE)
                    details.minBreathRate = (int) minBreathRate;
                break;
            case 700013492:
                long maxBreathRate = getValueAsLong(dataType, value, -1);
                if(maxBreathRate >=0 && maxBreathRate <= Integer.MAX_VALUE)
                    details.maxBreathRate = (int) maxBreathRate;
                break;
            case 700013824:
                long hrvDayToBaseline = getValueAsLong(dataType, value, -1);
                if(hrvDayToBaseline >=0 && hrvDayToBaseline <= 30)
                    details.hrvDayToBaseline = (int) hrvDayToBaseline;
                break;
            case 700013355:
                long maxHrvBaseline = getValueAsLong(dataType, value, -1);
                if(maxHrvBaseline >=0 && maxHrvBaseline <= 200)
                    details.maxHrvBaseline = (int) maxHrvBaseline;
                break;
            case 700013305:
                long minHrvBaseline = getValueAsLong(dataType, value, -1);
                if(minHrvBaseline >=0 && minHrvBaseline <= 200)
                    details.minHrvBaseline = (int) minHrvBaseline;
                break;
            case 700013878:
                long avgHrv = getValueAsLong(dataType, value, -1);
                if(avgHrv >=0 && avgHrv <= 200)
                    details.avgHrv = (int) avgHrv;
                break;
            case 700013236:
                long breathRateDayToBaseline = getValueAsLong(dataType, value, -1);
                if(breathRateDayToBaseline >= 0 && breathRateDayToBaseline <= 30)
                    details.breathRateDayToBaseline = (int) breathRateDayToBaseline;
                break;
            case 700013225:
                long maxBreathRateBaseline = getValueAsLong(dataType, value, -1);
                if(maxBreathRateBaseline >= 0 && maxBreathRateBaseline <= 80)
                    details.maxBreathRateBaseline = (int) maxBreathRateBaseline;
                break;
            case 700013839:
                long minBreathRateBaseline = getValueAsLong(dataType, value, -1);
                if(minBreathRateBaseline >= 0 && minBreathRateBaseline <= 80)
                    details.minBreathRateBaseline = (int) minBreathRateBaseline;
                break;
            case 700013886:
                long avgBreathRate = getValueAsLong(dataType, value, -1);
                if(avgBreathRate >= 0 && avgBreathRate <= 80)
                    details.avgBreathRate = (int) avgBreathRate;
                break;
            case 700013718:
                long oxygenSaturationDayToBaseline = getValueAsLong(dataType, value, -1);
                if(oxygenSaturationDayToBaseline >= 0 && oxygenSaturationDayToBaseline <= 30)
                    details.oxygenSaturationDayToBaseline = (int) oxygenSaturationDayToBaseline;
                break;
            case 700013227:
                long maxOxygenSaturationBaseline = getValueAsLong(dataType, value, -1);
                if(maxOxygenSaturationBaseline >= 0 && maxOxygenSaturationBaseline <= 100)
                    details.maxOxygenSaturationBaseline = (int) maxOxygenSaturationBaseline;
                break;
            case 700013633:
                long minOxygenSaturationBaseline = getValueAsLong(dataType, value, -1);
                if(minOxygenSaturationBaseline >= 0 && minOxygenSaturationBaseline <= 100)
                    details.minOxygenSaturationBaseline = (int) minOxygenSaturationBaseline;
                break;
            case 700013468:
                long avgOxygenSaturation = getValueAsLong(dataType, value, -1);
                if(avgOxygenSaturation >= 0 && avgOxygenSaturation <= 100)
                    details.avgOxygenSaturation = (int) avgOxygenSaturation;
                break;
            case 700013810:
                long heartRateDayToBaseline = getValueAsLong(dataType, value, -1);
                if(heartRateDayToBaseline >= 0 && heartRateDayToBaseline <= 30)
                    details.heartRateDayToBaseline = (int) heartRateDayToBaseline;
                break;
            case 700013841:
                long maxHeartRateBaseline = getValueAsLong(dataType, value, -1);
                if(maxHeartRateBaseline >= 0 && maxHeartRateBaseline <= 255)
                    details.maxHeartRateBaseline = (int) maxHeartRateBaseline;
                break;
            case 700013722:
                long minHeartRateBaseline = getValueAsLong(dataType, value, -1);
                if(minHeartRateBaseline >= 0 && minHeartRateBaseline <= 255)
                    details.minHeartRateBaseline = (int) minHeartRateBaseline;
                break;
            case 700013580:
                long avgHeartRate = getValueAsLong(dataType, value, -1);
                if(avgHeartRate >= 0 && avgHeartRate <= 255)
                    details.avgHeartRate = (int) avgHeartRate;
                break;
            case 700013759:
                long rdi = getValueAsLong(dataType, value, -1);
                if(rdi >= 0 && rdi <= 100)
                    details.rdi = (int) rdi;
                break;
            case 700013635:
                long wakeCount = getValueAsLong(dataType, value, -1);
                if(wakeCount >= 0)
                    details.wakeCount = (int) wakeCount;
                break;
            case 700013670:
                long turnOverCount = getValueAsLong(dataType, value, -1);
                if(turnOverCount >= 0)
                    details.turnOverCount = (int) turnOverCount;
                break;
            case 700013821:
                long prepareSleepTime = getValueAsLong(dataType, value, -1);
                if(prepareSleepTime >= 0)
                    details.prepareSleepTime = prepareSleepTime;
                break;
            case 700013925:
                long wakeUpFeeling = getValueAsLong(dataType, value, -1);
                if(wakeUpFeeling >= 0)
                    details.wakeUpFeeling = (int) wakeUpFeeling;
                break;
            case 700013697:
                long sleepVersion = getValueAsLong(dataType, value, -1);
                if(sleepVersion >= 0 && sleepVersion <= 100)
                    details.sleepVersion = (int) sleepVersion;
                break;
            default:
                LOG.info("Unknown dictId: {}", dictId);
        }
    }

    protected SleepSummary getNewData() {
        return new SleepSummary();
    }

    public static void correctSummary(SleepSummary summary) {
        if(summary.validData == -1) {
            summary.bedTime = 0;
            summary.risingTime = 0;
            summary.sleepScore = -1;
            summary.sleepDataQuality = -1;
            summary.deepPart = -1;
            summary.snoreFreq = -1;
            summary.sleepEfficiency = -1;
            summary.sleepLatency = -1;
            summary.prepareSleepTime = -1;
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
