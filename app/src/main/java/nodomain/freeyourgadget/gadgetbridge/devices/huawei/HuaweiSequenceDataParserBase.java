package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public abstract class HuaweiSequenceDataParserBase<T> {

    private static final Logger LOG = LoggerFactory.getLogger(HuaweiSequenceDataParserBase.class);

    protected static int readAsInteger(byte[] data, int def) {
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

    protected static long readAsLong(byte[] data, long def) {
        if (data == null || data.length == 0 || data.length > 8) {
            return def;
        }
        long res = 0;
        for (int i = 0; i < data.length; i++) {
            res |= (long) (data[i] & 0xFF) << (((data.length - i) - 1) * 8);
        }
        return res;
    }

    protected static long getValueAsLong(int i, byte[] str2, long def) {
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

    protected static double getValueAsDouble(int i, byte[] str2, double def) {
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

    protected abstract void fillData(T details, int dictId, int dataType, byte[] value);

    protected abstract T getNewData();

    private T parseTLVData(byte[] str) {
        T details = getNewData();
        HuaweiTLV tlv = new HuaweiTLV().parse(str);
        List<HuaweiTLV> containers = tlv.getAllContainerObjects();
        for (HuaweiTLV tv : containers) {
            List<HuaweiTLV> containers2 = tv.getAllContainerObjects();
            for (HuaweiTLV tv2 : containers2) {
                try {
                    int dictId = tv2.getAsInteger(0x03);
                    int dataType = tv2.getAsInteger(0x04);
                    byte[] value = tv2.getBytes(0x5);
                    fillData(details, dictId, dataType, value);
                } catch (Exception e) {
                    LOG.error("SequenceDataParserBase dict sync: tag is missing", e);
                }
            }
        }
        return details;
    }

    public T parseData(HuaweiSequenceDataFileParser.SequenceFileData fileData, HuaweiSequenceDataFileParser.SequenceData data) {
        // summary type 2 is TLV. TLV works only for fileType != 2 (or maybe only for 1)
        // currently only data version 1 supported
        if (data != null && data.getDataVersion() == 1 && data.getSummaryType() == 2 && fileData.getFileType() != 2) {
            return parseTLVData(data.getSummary());
        }
        return null;
    }
}
