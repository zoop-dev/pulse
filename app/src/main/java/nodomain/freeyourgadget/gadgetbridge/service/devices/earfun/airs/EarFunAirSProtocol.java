package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunPacketEncoder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunProtocol;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Equalizer;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class EarFunAirSProtocol extends EarFunProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(EarFunAirSProtocol.class);

    public byte[] encodeSendConfigurationCustomizer(String config) {

        if (containsKey(Equalizer.SixBandEqualizer, config)) {
            Prefs prefs = getDevicePrefs();
            return EarFunPacketEncoder.encodeSetEqualizerSixBands(prefs);
        }
        return null;
    }

    private static boolean containsKey(Equalizer.BandConfig[] array, String key) {
        return Arrays.stream(array)
                .anyMatch(element -> element.key.equals(key));
    }

    protected EarFunAirSProtocol(GBDevice device) {
        super(device);
        DeviceType type = device.getType();
    }
}
