package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HuaweiEcgFileParser {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiEcgFileParser.class);

    public static class EcgParseException extends Exception {
        public EcgParseException(String message) {
            super(message);
        }
    }

    public static class EcgFileData {
        private final List<EcgData> ecgDataList = new ArrayList<>();
        private int unkn1;
        private long unkn2;
        private byte[] extraData;
        private int packageNameLength;
        private String packageName;
        private int version;


        public void setUnkn1(int unkn1) {
            this.unkn1 = unkn1;
        }
        public void setUnkn2(long unkn2) {
            this.unkn2 = unkn2;
        }

        public int getVersion() {
            return this.version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public void setExtraData(byte[] extraData) {
            this.extraData = extraData;
        }

        public List<EcgData> getEcgDataList() {
            return this.ecgDataList;
        }

        public int getPackageNameLength() {
            return this.packageNameLength;
        }

        public void setPackageNameLength(int packageNameLength) {
            this.packageNameLength = packageNameLength;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        @NonNull
        @Override
        public String toString() {
            return "EcgFile{" + "ecgDataList=" + ecgDataList +
                    ", unkn1=" + unkn1 +
                    ", unkn2=" + unkn2 +
                    ", extraData='" + Arrays.toString(extraData) + '\'' +
                    ", packageNameLength=" + packageNameLength +
                    ", packageName='" + packageName + '\'' +
                    ", version=" + version +
                    '}';
        }
    }

    public static class EcgData {
        private long startTime;
        private long endTime;
        private int ecgDataLength;
        private String appVersion;
        private int averageHeartRate;
        private long arrhythmiaType;
        private long userSymptoms;
        private List<Float> ecgData;

        public long getStartTime() {
            return this.startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return this.endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public int getEcgDataLength() {
            return this.ecgDataLength;
        }

        public void setEcgDataLength(int ecgDataLength) {
            this.ecgDataLength = ecgDataLength;
        }

        public long getArrhythmiaType() {
            return this.arrhythmiaType;
        }

        public void setArrhythmiaType(long arrhythmiaType) {
            this.arrhythmiaType = arrhythmiaType;
        }

        public int getAverageHeartRate() {
            return this.averageHeartRate;
        }

        public void setAverageHeartRate(int averageHeartRate) {
            this.averageHeartRate = averageHeartRate;
        }

        public long getUserSymptoms() {
            return this.userSymptoms;
        }

        public void setUserSymptoms(long userSymptoms) {
            this.userSymptoms = userSymptoms;
        }

        public List<Float> getEcgData() {
            return this.ecgData;
        }

        public void setEcgData(List<Float> ecgData) {
            this.ecgData = ecgData;
        }

        public String getAppVersion() {
            return this.appVersion;
        }

        public void setAppVersion(String appVersion) {
            this.appVersion = appVersion;
        }

        @NonNull
        @Override
        public String toString() {
            return "EcgData{" + "startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", averageHeartRate=" + averageHeartRate +
                    ", arrhythmiaType=" + arrhythmiaType +
                    ", ecgAppVersion='" + appVersion + '\'' +
                    ", ecgDataLength=" + ecgDataLength +
                    ", ecgData=" + ecgData +
                    ", userSymptoms=" + userSymptoms +
                    '}';
        }
    }
    private static long checkTime(long tm) throws EcgParseException {
        long time = tm * 1000;
        if (time < 0 || time > System.currentTimeMillis()) {
            LOG.error("wrong data.");
            throw new EcgParseException("wrong time: " + tm);
        }
        return time;
    }

    private static int checkDataLength(long len) {
        if (len <= 0 || len > 2400000L) {
            LOG.error("length is invalid: {}", len);
            return 0;
        }
        return (int) len;
    }

    private static void parseEcgData(ByteBuffer buffer, EcgFileData ecgFileData) throws EcgParseException {
        while (buffer.remaining() >= 56) {
            EcgData ecgData = new EcgData();
            long startTime = checkTime(buffer.getInt() & 0xFFFFFFFFL);
            long endTime = checkTime(buffer.getInt() & 0xFFFFFFFFL);
            if (startTime > endTime) {
                throw new EcgParseException("wrong time. startTime: " + startTime + "endTime: " + endTime);
            }
            ecgData.setStartTime(startTime);
            ecgData.setEndTime(endTime);
            ecgData.setEcgDataLength(checkDataLength(buffer.getInt() & 0xFFFFFFFFL));
            byte[] appVersionBytes = new byte[32];
            buffer.get(appVersionBytes);
            ecgData.setAppVersion(new String(appVersionBytes, StandardCharsets.UTF_8).trim());
            ecgData.setArrhythmiaType(buffer.getInt() & 0xFFFFFFFFL);
            ecgData.setAverageHeartRate(buffer.getShort() & 0xFFFF);
            buffer.getShort(); // unknown, always 0, can be a part of symptom or heart rate
            ecgData.setUserSymptoms(buffer.getInt() & 0xFFFFFFFFL);

            if (buffer.remaining() >= ecgData.getEcgDataLength()) {
                ArrayList<Float> arrayList = new ArrayList<>();
                int i = ecgData.getEcgDataLength();
                while (i > 0) {
                    float valueOf = buffer.getFloat(); //Float.intBitsToFloat((int) (buffer.getInt() & 0xFFFFFFFFL));
                    if (Float.isNaN(valueOf)) {
                        System.out.println("unitStringFloat isNaN");
                        valueOf = 0.0f;
                    }
                    arrayList.add(valueOf);
                    i -= 4;
                }
                ecgData.setEcgData(arrayList);
                if (ecgFileData.getEcgDataList() != null) {
                    ecgFileData.getEcgDataList().add(ecgData);
                }
            } else {
                throw new EcgParseException("ecgData is invalid");
            }
        }
    }

    public static EcgFileData parseEcgFile(byte[] data) throws EcgParseException {
        if (data.length < 32) {
            throw new EcgParseException("data is invalid");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);

        EcgFileData ecgFileData = new EcgFileData();
        ecgFileData.setUnkn2(buffer.getInt() & 0xFFFFFFFFL);
        ecgFileData.setUnkn1(buffer.getShort() & 0xFFFF);
        ecgFileData.setVersion(buffer.getShort() & 0xFFFF);
        ecgFileData.setPackageNameLength(buffer.get() & 0xFF);
        byte[] extraData = new byte[23];
        buffer.get(extraData);
        ecgFileData.setExtraData(extraData);

        if (ecgFileData.getPackageNameLength() > buffer.remaining()) {
            throw new EcgParseException("not enough data for package name");
        }

        byte[] packageNameBytes = new byte[ecgFileData.getPackageNameLength()];
        buffer.get(packageNameBytes);
        ecgFileData.setPackageName(new String(packageNameBytes, StandardCharsets.UTF_8).trim());

        if (ecgFileData.getVersion() == 0) {
            parseEcgData(buffer, ecgFileData);
        } else {
            LOG.error("version is not supported");
        }
        return ecgFileData;
    }
}

