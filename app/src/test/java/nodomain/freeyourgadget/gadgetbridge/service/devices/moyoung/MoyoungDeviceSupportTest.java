package nodomain.freeyourgadget.gadgetbridge.service.devices.moyoung;

import android.util.Pair;

import junit.framework.TestCase;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class MoyoungDeviceSupportTest extends TestBase {
    @Test
    public void testWorkoutColumnV76() {
        final MoyoungDeviceSupport support = new MoyoungDeviceSupport();
        // #6053
        final byte[][] packets = new byte[][] {
                GB.hexStringToByteArray("FEEA2023B2030000000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030100000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030200000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030300000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030400000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030500000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030600000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030700000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030800000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030900000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030A00000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030B00000000000000000000000000000000000000000000000000000000"),
                GB.hexStringToByteArray("FEEA2023B2030C00000000000000000000000000000000000000000000000052B8CB41"),

                // 00:36:34
                // 2.3km
                // 163kcal
                // 15'41"
                // 2767 steps
                // 76 steps/min
                // 80bpm
                GB.hexStringToByteArray("FEEA2023B2030D257C126AB984126A92080022CF0A00001A090000A3005900A9C1C440"),

                // 00:32:01
                // 1.7km
                // 122kcal
                // 18'20"
                // GPS trace
                // 2011 steps
                // 62 steps/min
                // 93bpm
                GB.hexStringToByteArray("FEEA2023B2030E9BAF136A1CB7136A8107001EDB070000D20600007A005D003DB8C740")
        };
        MoyoungPacketIn packetIn = new MoyoungPacketIn();
        for (byte[] value : packets) {
            if (packetIn.putFragment(value)) {
                Pair<Byte, byte[]> packet = MoyoungPacketIn.parsePacket(packetIn.getPacket());
                packetIn = new MoyoungPacketIn();

                if (packet != null) {
                    byte packetType = packet.first;
                    byte[] payload = packet.second;
                    support.handleTrainingData(payload);
                }
            }
        }
    }
}
