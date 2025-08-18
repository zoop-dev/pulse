package nodomain.freeyourgadget.gadgetbridge.devices;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class AbstractDeviceCoordinatorTest extends TestBase {
    @Test
    public void deviceMatchingByNameTest() {
        final Map<String, DeviceType> bluetoothNameToExpectedType = new HashMap<>() {{
            put("Active 2 (Round)", DeviceType.AMAZFITACTIVE2);
            put("Active 2 (Square)", DeviceType.AMAZFITACTIVE2SQUARE);
            put("Active 2 NFC (Round)", DeviceType.AMAZFITACTIVE2NFC);
            put("Active 2 NFC (Square)", DeviceType.AMAZFITACTIVE2SQUARE);
            put("Amazfit Active Edge", DeviceType.AMAZFITACTIVEEDGE);
            put("Amazfit Active", DeviceType.AMAZFITACTIVE);
            put("Amazfit Balance 2", DeviceType.AMAZFITBALANCE2);
            put("Amazfit Balance", DeviceType.AMAZFITBALANCE);
            put("Amazfit Band 7", DeviceType.AMAZFITBAND7);
            put("Amazfit Bip 5 Unity", DeviceType.AMAZFITBIP5UNITY);
            put("Amazfit Bip 5", DeviceType.AMAZFITBIP5);
            put("Amazfit Bip 6", DeviceType.AMAZFITBIP6);
            put("Amazfit Cheetah Pro", DeviceType.AMAZFITCHEETAHPRO);
            put("Amazfit Cheetah R", DeviceType.AMAZFITCHEETAHROUND);
            put("Amazfit Cheetah S", DeviceType.AMAZFITCHEETAHSQUARE);
            put("Amazfit Falcon", DeviceType.AMAZFITFALCON);
            put("Amazfit GTR 3 Pro", DeviceType.AMAZFITGTR3PRO);
            put("Amazfit GTR 3", DeviceType.AMAZFITGTR3);
            put("Amazfit GTR 4", DeviceType.AMAZFITGTR4);
            put("Amazfit GTR Mini", DeviceType.AMAZFITGTRMINI);
            put("Amazfit GTS 3", DeviceType.AMAZFITGTS3);
            put("Amazfit GTS 4 Mini", DeviceType.AMAZFITGTS4MINI);
            put("Amazfit GTS 4", DeviceType.AMAZFITGTS4);
            put("Amazfit Helio Ring", DeviceType.AMAZFITHELIORING);
            put("Amazfit Helio Strap", DeviceType.AMAZFITHELIOSTRAP);
            put("Amazfit T-Rex 2", DeviceType.AMAZFITTREX2);
            put("Amazfit T-Rex 3", DeviceType.AMAZFITTREX3);
            put("Amazfit T-Rex Ultra", DeviceType.AMAZFITTREXULTRA);
            put("Xiaomi Smart Band 7", DeviceType.MIBAND7);
            put("P8", DeviceType.WASPOS);
            put("P8DFU", DeviceType.WASPOS);
            put("P80", DeviceType.COLMI_P80);
        }};

        for (Map.Entry<String, DeviceType> e : bluetoothNameToExpectedType.entrySet()) {
            final List<DeviceType> matches = new ArrayList<>(1);
            for (DeviceType type : DeviceType.values()) {
                final Pattern pattern = ((AbstractDeviceCoordinator) type.getDeviceCoordinator()).getSupportedDeviceName();
                if (pattern != null) {
                    if (pattern.matcher(e.getKey()).matches()) {
                        matches.add(type);
                    }
                }
            }

            Assert.assertEquals(
                    "Bluetooth name " + e.getKey() + " should only match the expected DeviceType",
                    Collections.singletonList(e.getValue()),
                    matches
            );
        }
    }
}
