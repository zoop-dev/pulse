/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.nothing;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class CmfBuds2Coordinator extends AbstractEarCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^CMF Buds 2$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_nothing_cmf_buds_2;
    }

    @Override
    public boolean incrementCounter() {
        return false;
    }

    @Override
    public boolean supportsLightAnc() {
        return true;
    }

    @Override
    public boolean supportsTransparency() {
        return true;
    }
    @Override
    public boolean supportsMediumAnc() { return false; }

    @Override
    public boolean supportsAdaptiveAnc() { return false; }

    @Override
    public List<NothingEqualizer> getEqualizerPresets() {
        return Arrays.asList(
                NothingEqualizer.POP,
                NothingEqualizer.ROCK,
                NothingEqualizer.ELECTRONIC,
                NothingEqualizer.ENHANCE_VOCALS,
                NothingEqualizer.CLASSICAL,
                NothingEqualizer.CUSTOM,
                NothingEqualizer.DIRAC
        );
    }

    @Override
    public boolean supportsUltraBass() { return true; }

    @Override
    public boolean supportsSpatialAudio() { return true; }
}
