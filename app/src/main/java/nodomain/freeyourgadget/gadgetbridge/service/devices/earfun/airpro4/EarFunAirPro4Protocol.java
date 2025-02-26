package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airpro4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunPacketEncoder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunProtocol;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Equalizer;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class EarFunAirPro4Protocol extends EarFunProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(EarFunAirPro4Protocol.class);

    public byte[] encodeSendConfigurationCustomizer(String config) {

        if (containsKey(Equalizer.TenBandEqualizer, config)) {
            Prefs prefs = getDevicePrefs();
            return EarFunPacketEncoder.encodeSetEqualizerTenBands(prefs);
        }
        return null;
    }

    private static boolean containsKey(Equalizer.BandConfig[] array, String key) {
        return Arrays.stream(array)
                .anyMatch(element -> element.key.equals(key));
    }

    protected EarFunAirPro4Protocol(GBDevice device) {
        super(device);
        DeviceType type = device.getType();
    }
}
