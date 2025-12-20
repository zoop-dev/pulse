/*  Copyright (C) 2024 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiHeartRateZonesSpec;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZones;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZonesConfig;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZonesSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;

public class HuaweiP2PTrackService extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PTrackService.class);

    public static final String MODULE = "hw.unitedevice.track";

    private static final int HEADER_LENGTH = 36;

    static class Sequence {
        int counter;

        private Sequence() {
            this.counter = 0;
        }

        public int getNext() {
            synchronized (this) {
                this.counter = (this.counter + 1) % 10000;
                return this.counter;
            }
        }
    }

    private final Sequence counter = new Sequence();

    public HuaweiP2PTrackService(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("HuaweiP2PTrackService");
    }

    @Override
    public String getModule() {
        return HuaweiP2PTrackService.MODULE;
    }

    @Override
    public String getPackage() {
        return "hw.watch.health.p2p";
    }

    @Override
    public String getFingerprint() {
        return "SystemApp";
    }

    private boolean isNotValid(HeartRateZonesConfig cfg) {
        if(cfg == null)
            return true;
        return !cfg.isValid();
    }

    private boolean isNotValidZone(HeartRateZones cfg) {
        if(cfg == null)
            return true;
        return !cfg.isValid();
    }

    public byte[] getHRZonesData() {

        HuaweiHeartRateZonesSpec spec = new HuaweiHeartRateZonesSpec(manager.getSupportProvider().getDevice(), manager.getSupportProvider().getDeviceState());
        List<HeartRateZonesConfig> zones = spec.getDeviceConfig();
        HeartRateZonesConfig uprightConfig = HuaweiHeartRateZonesSpec.getByPosture(zones, HeartRateZonesSpec.PostureType.UPRIGHT);
        HeartRateZonesConfig sittingConfig = HuaweiHeartRateZonesSpec.getByPosture(zones, HeartRateZonesSpec.PostureType.SITTING);
        HeartRateZonesConfig swimmingConfig = HuaweiHeartRateZonesSpec.getByPosture(zones, HeartRateZonesSpec.PostureType.SWIMMING);
        HeartRateZonesConfig otherConfig = HuaweiHeartRateZonesSpec.getByPosture(zones, HeartRateZonesSpec.PostureType.OTHER);

        if (isNotValid(uprightConfig) || isNotValid(sittingConfig) || isNotValid(swimmingConfig) || isNotValid(otherConfig)) {
            return null;
        }
        HeartRateZones uprightMhr = HuaweiHeartRateZonesSpec.getByMethod(uprightConfig, HeartRateZones.CalculationMethod.MHR);
        HeartRateZones uprightHrr = HuaweiHeartRateZonesSpec.getByMethod(uprightConfig, HeartRateZones.CalculationMethod.HRR);
        HeartRateZones uprightLthr = HuaweiHeartRateZonesSpec.getByMethod(uprightConfig, HeartRateZones.CalculationMethod.LTHR);
        if(isNotValidZone(uprightMhr) || isNotValidZone(uprightHrr) || isNotValidZone(uprightLthr)) {
            return null;
        }

        HeartRateZones sittingMhr = HuaweiHeartRateZonesSpec.getByMethod(sittingConfig, HeartRateZones.CalculationMethod.MHR);
        HeartRateZones sittingHrr = HuaweiHeartRateZonesSpec.getByMethod(sittingConfig, HeartRateZones.CalculationMethod.HRR);
        if(isNotValidZone(sittingMhr) || isNotValidZone(sittingHrr)) {
            return null;
        }
        HeartRateZones swimmingMhr = HuaweiHeartRateZonesSpec.getByMethod(swimmingConfig, HeartRateZones.CalculationMethod.MHR);
        HeartRateZones swimmingHrr = HuaweiHeartRateZonesSpec.getByMethod(swimmingConfig, HeartRateZones.CalculationMethod.HRR);
        if(isNotValidZone(swimmingMhr) || isNotValidZone(swimmingHrr)) {
            return null;
        }
        HeartRateZones otherMhr = HuaweiHeartRateZonesSpec.getByMethod(otherConfig, HeartRateZones.CalculationMethod.MHR);
        HeartRateZones otherHrr = HuaweiHeartRateZonesSpec.getByMethod(otherConfig, HeartRateZones.CalculationMethod.HRR);
        if(isNotValidZone(otherMhr) || isNotValidZone(otherHrr)) {
            return null;
        }


        HuaweiTLV tlv = new HuaweiTLV();

        if (uprightMhr.hasValidData()) {
            tlv.put(0x2, (byte) uprightMhr.getZone1())
                    .put(0x3, (byte) uprightMhr.getZone2())
                    .put(0x4, (byte) uprightMhr.getZone3())
                    .put(0x5, (byte) uprightMhr.getZone4())
                    .put(0x6, (byte) uprightMhr.getZone5())
                    .put(0x7, (byte) uprightMhr.getHRThreshold())
                    .put(0x8, (byte) (uprightConfig.getWarningEnable() ? 1 : 0))
                    .put(0x9, (byte) uprightConfig.getWarningHRLimit())
                    .put(0xa, (byte) HuaweiHeartRateZonesSpec.toHuaweiCalculationMethod(uprightConfig.getCurrentCalculationMethod()))
                    .put(0xb, (byte) uprightMhr.getHRThreshold());
        }
        tlv.put(0xc, (byte) uprightHrr.getHRResting());
        if (uprightHrr.hasValidData()) {
            tlv.put(0xd, (byte) uprightHrr.getZone1())
                    .put(0xe, (byte) uprightHrr.getZone2())
                    .put(0xf, (byte) uprightHrr.getZone3())
                    .put(0x10, (byte) uprightHrr.getZone4())
                    .put(0x11, (byte) uprightHrr.getZone5());

        }
        if (uprightLthr.hasValidData()) {
            tlv.put(0x3f, (byte) uprightLthr.getHRThreshold())
                    .put(0x40, (byte) uprightLthr.getZone5())
                    .put(0x41, (byte) uprightLthr.getZone4())
                    .put(0x42, (byte) uprightLthr.getZone3())
                    .put(0x43, (byte) uprightLthr.getZone2())
                    .put(0x44, (byte) uprightLthr.getZone1());
        }

        if (sittingMhr.hasValidData()) {
            tlv.put(0x12, (byte) (sittingConfig.getWarningEnable() ? 1 : 0))
                    .put(0x13, (byte) HuaweiHeartRateZonesSpec.toHuaweiCalculationMethod(sittingConfig.getCurrentCalculationMethod()))
                    .put(0x14, (byte) sittingConfig.getWarningHRLimit())
                    .put(0x15, (byte) sittingMhr.getZone1())
                    .put(0x16, (byte) sittingMhr.getZone2())
                    .put(0x17, (byte) sittingMhr.getZone3())
                    .put(0x18, (byte) sittingMhr.getZone4())
                    .put(0x19, (byte) sittingMhr.getZone5())
                    .put(0x1a, (byte) sittingMhr.getHRThreshold());
        }
        if (sittingHrr.hasValidData()) {
            tlv.put(0x1b, (byte) sittingHrr.getHRResting())
                    .put(0x1c, (byte) sittingHrr.getZone1())
                    .put(0x1d, (byte) sittingHrr.getZone2())
                    .put(0x1e, (byte) sittingHrr.getZone3())
                    .put(0x1f, (byte) sittingHrr.getZone4())
                    .put(0x20, (byte) sittingHrr.getZone5());
        }

        if (swimmingMhr.hasValidData()) {
            tlv.put(0x21, (byte) (swimmingConfig.getWarningEnable() ? 1 : 0))
                    .put(0x22, (byte) HuaweiHeartRateZonesSpec.toHuaweiCalculationMethod(swimmingConfig.getCurrentCalculationMethod()))
                    .put(0x23, (byte) swimmingConfig.getWarningHRLimit())
                    .put(0x24, (byte) swimmingMhr.getZone1())
                    .put(0x25, (byte) swimmingMhr.getZone2())
                    .put(0x26, (byte) swimmingMhr.getZone3())
                    .put(0x27, (byte) swimmingMhr.getZone4())
                    .put(0x28, (byte) swimmingMhr.getZone5())
                    .put(0x29, (byte) swimmingMhr.getHRThreshold());
        }
        if (swimmingHrr.hasValidData()) {
            tlv.put(0x2a, (byte) swimmingHrr.getHRResting())
                    .put(0x2b, (byte) swimmingHrr.getZone1())
                    .put(0x2c, (byte) swimmingHrr.getZone2())
                    .put(0x2d, (byte) swimmingHrr.getZone3())
                    .put(0x2e, (byte) swimmingHrr.getZone4())
                    .put(0x2f, (byte) swimmingHrr.getZone5());
        }

        if (otherMhr.hasValidData()) {
            tlv.put(0x30, (byte) (otherConfig.getWarningEnable() ? 1 : 0))
                    .put(0x31, (byte) HuaweiHeartRateZonesSpec.toHuaweiCalculationMethod(otherConfig.getCurrentCalculationMethod()))
                    .put(0x32, (byte) otherConfig.getWarningHRLimit())
                    .put(0x33, (byte) otherMhr.getZone1())
                    .put(0x34, (byte) otherMhr.getZone2())
                    .put(0x35, (byte) otherMhr.getZone3())
                    .put(0x36, (byte) otherMhr.getZone4())
                    .put(0x37, (byte) otherMhr.getZone5())
                    .put(0x38, (byte) otherMhr.getHRThreshold());
        }
        if (otherHrr.hasValidData()) {
            tlv.put(0x39, (byte) otherHrr.getHRResting())
                    .put(0x3a, (byte) otherHrr.getZone1())
                    .put(0x3b, (byte) otherHrr.getZone2())
                    .put(0x3c, (byte) otherHrr.getZone3())
                    .put(0x3d, (byte) otherHrr.getZone4())
                    .put(0x3e, (byte) otherHrr.getZone5());
        }

        return tlv.serialize();
    }

    public void sendHeartZoneConfig() {
        byte[] data = getHRZonesData();
        if (data == null) {
            LOG.error("Incorrect Heart Rate config");
            return;
        }

        ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(2); // session id ??
        header.putInt(1); // version
        header.putInt(HEADER_LENGTH + data.length); // total length
        header.putInt(0); // unknown, sub header length??
        header.putInt(counter.getNext()); // message id
        header.flip();

        ByteBuffer packet = ByteBuffer.allocate(HEADER_LENGTH + data.length);
        packet.put(header.array());
        packet.put(data);
        packet.flip();

        LOG.info("HuaweiP2PTrackService sendHeartZoneConfig");

        sendCommand(packet.array(), null);
    }

    @Override
    public void registered() {
        sendHeartZoneConfig();
    }

    @Override
    public void unregister() {

    }

    @Override
    public void handleData(byte[] data) {
        LOG.info("HuaweiP2PTrackService handleData: {}", data.length);
    }

    public static HuaweiP2PTrackService getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PTrackService) manager.getRegisteredService(HuaweiP2PTrackService.MODULE);
    }
}
