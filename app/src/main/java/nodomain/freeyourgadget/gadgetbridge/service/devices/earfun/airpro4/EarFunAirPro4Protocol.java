package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airpro4;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunPacketEncoder.joinPackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunPacketEncoder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunProtocol;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Equalizer;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class EarFunAirPro4Protocol extends EarFunProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(EarFunAirPro4Protocol.class);

    @Override
    public byte[] encodeTestNewFunction() {
        return joinPackets(
                // new EarFunPacket(EarFunPacket.Command.COMMAND_REBOOT).encode()
        );
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        if (Equalizer.containsKey(Equalizer.TenBandEqualizer, config)) {
            Prefs prefs = getDevicePrefs();
            return EarFunPacketEncoder.encodeSetEqualizerTenBands(prefs);
        }
        return super.encodeSendConfiguration(config);
    }

    @Override
    public byte[] encodeSettingsReq() {
        return EarFunPacketEncoder.encodeAirPro4SettingsReq();
    }

    protected EarFunAirPro4Protocol(GBDevice device) {
        super(device);
        DeviceType type = device.getType();
    }
}
