package nodomain.freeyourgadget.gadgetbridge.service.devices.onemore_sonoflow;

public class OneMorePacket {

    // sent from phone
    public static final byte[] REQUEST_PREAMBLE = { 0x11, 0x01, 0x00 };

    // sent from headphones
    public static final byte[] RESPONSE_PREAMBLE = { 0x01, 0x01, 0x00 };

    // battery and firmware version (maybe more?)
    public static final byte GET_DEVICE_INFO_COMMAND = 0x4e;

    public static final byte GET_NOISE_CONTROL_COMMAND = 0x5f;
    public static final byte SET_NOISE_CONTROL_COMMAND = 0x5e;

    public static final byte GET_LDAC_COMMAND = 0x6c;
    public static final byte SET_LDAC_COMMAND = 0x6b;

    public static final byte GET_DUAL_DEVICE_COMMAND = 0x77;
    public static final byte SET_DUAL_DEVICE_COMMAND = 0x76;

    // TODO:
    //  - figure out flags
    //  - figure out checksums

    public static byte[] createGetDeviceInfoPacket() {
        byte[] flags = { 0x00, 0x00, 0x00 };
        byte[] checksum = { 0x1c, 0x42 };

        return concat(REQUEST_PREAMBLE, GET_DEVICE_INFO_COMMAND, flags, checksum);
    }

    public static byte[] createGetNoiseControlModePacket() {
        byte[] flags = { 0x00, 0x00, 0x00 };
        byte[] checksum = { 0x0c, 0x43 };

        return concat(REQUEST_PREAMBLE, GET_NOISE_CONTROL_COMMAND, flags, checksum);
    }

    public static byte[] createSetNoiseControlModePacket(String mode) {
        byte[] flags = { 0x00, 0x01, 0x00 };
        byte[] checksum = { 0x13, 0x5c };

        byte modeByte;
        switch (mode) {
            case "0":
                // Off
                modeByte = 0x00;
                break;
            case "1":
                // ANC
                modeByte = 0x01;
                break;
            case "2":
                // Pass-through
                modeByte = 0x03;
                break;
            default:
                throw new IllegalStateException("mode not one of the allowed values: [\"0\", \"1\", \"2\"]");
        }

        return concat(REQUEST_PREAMBLE, SET_NOISE_CONTROL_COMMAND, flags, checksum, modeByte);
    }

    public static byte[] createGetLdacModePacket() {
        byte[] flags = { 0x00, 0x00, 0x00 };
        byte[] checksum = { 0x0d, 0x71 };

        return concat(REQUEST_PREAMBLE, GET_LDAC_COMMAND, flags, checksum);
    }

    public static byte[] createSetLdacModePacket(boolean enabled) {
        byte[] flags = { 0x00, 0x01, 0x00 };
        byte[] checksum = { 0x2d, 0x57 };

        byte modeByte = (byte) (enabled ? 0x02 : 0x00);

        return concat(REQUEST_PREAMBLE, SET_LDAC_COMMAND, flags, checksum, modeByte);
    }

    public static byte[] createGetDualDeviceModePacket() {
        byte[] flags = { 0x00, 0x00, 0x00 };
        byte[] checksum = { 0x0c, 0x6b };

        return concat(REQUEST_PREAMBLE, GET_DUAL_DEVICE_COMMAND, flags, checksum);
    }

    public static byte[] createSetDualDeviceModePacket(boolean enabled) {
        byte[] flags = { 0x00, 0x01, 0x00 };
        byte[] checksum = { 0x0d, 0x6a };

        byte modeByte = (byte) (enabled ? 0x01 : 0x00);

        return concat(REQUEST_PREAMBLE, SET_DUAL_DEVICE_COMMAND, flags, checksum, modeByte);
    }

    private static byte[] concat(Object... args) {
        if (args == null || args.length == 0) {
            return new byte[0];
        }

        int totalLength = 0;
        for (Object arg : args) {
            if (arg instanceof byte[]) {
                totalLength += ((byte[]) arg).length;
            } else if (arg instanceof Byte) {
                totalLength++;
            } else {
                throw new IllegalArgumentException("Invalid argument type: " + arg.getClass().getName() + ".  Expected byte[] or Byte.");
            }
        }

        byte[] result = new byte[totalLength];
        int offset = 0;

        for (Object arg : args) {
            if (arg instanceof byte[]) {
                byte[] byteArray = (byte[]) arg;
                System.arraycopy(byteArray, 0, result, offset, byteArray.length);
                offset += byteArray.length;
            } else if (arg instanceof Byte) {
                result[offset++] = (Byte) arg;
            }
        }

        return result;
    }
}
