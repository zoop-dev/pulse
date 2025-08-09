/*  Copyright (C) 2025 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.weightScale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.content.Intent;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;

public class WeightScaleScaleProfileTest extends AbstractBTLESingleDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(WeightScaleScaleProfileTest.class);
    private ArrayList<WeightScaleMeasurement> Results;
    public WeightScaleScaleProfileTest() {
        super(LOG);
    }

    @Before
    public void Setup() {
        Results = new ArrayList<>(10);
    }

    @Test
    public void simple() {
        WeightScaleProfile<WeightScaleScaleProfileTest> weightScaleProfile = new WeightScaleProfileProxy(this);
        weightScaleProfile.processWeightMeasurement(new byte[]{(byte) 0, (byte) 0, (byte) 0});
        weightScaleProfile.processWeightMeasurement(new byte[]{(byte) 0, (byte) 1, (byte) 0});
        weightScaleProfile.processWeightMeasurement(new byte[]{(byte) 0, (byte) 0, (byte) 1});

        weightScaleProfile.processWeightMeasurement(new byte[]{(byte) 1, (byte) 0, (byte) 0});
        weightScaleProfile.processWeightMeasurement(new byte[]{(byte) 1, (byte) 1, (byte) 0});
        weightScaleProfile.processWeightMeasurement(new byte[]{(byte) 1, (byte) 0, (byte) 1});

        assertThat(Results.size(), is(6));

        assertThat(Results.get(0).getWeightKilogram().floatValue(), is(0.0f));
        assertThat(Results.get(1).getWeightKilogram().floatValue(), is(0.005f));
        assertThat(Results.get(2).getWeightKilogram().floatValue(), is(1.28f));

        assertThat(Results.get(3).getWeightKilogram().floatValue(), is(0.0f));
        assertThat(Results.get(4).getWeightKilogram().floatValue(), is(0.0045359237f));
        assertThat(Results.get(5).getWeightKilogram().floatValue(), is(1.1611965f));
    }

    @Test
    public void simpleConcatenated() {
        WeightScaleProfile<WeightScaleScaleProfileTest> weightScaleProfile = new WeightScaleProfileProxy(this);
        weightScaleProfile.processWeightMeasurement(new byte[]{
                (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 1, (byte) 0,
                (byte) 0, (byte) 0, (byte) 1,
                (byte) 1, (byte) 0, (byte) 0,
                (byte) 1, (byte) 1, (byte) 0,
                (byte) 1, (byte) 0, (byte) 1,
                (byte) 0, (byte) 0xFF, (byte) 0xFF,
                (byte) 1, (byte) 0xFF, (byte) 0xFF,
        });

        assertThat(Results.size(), is(8));

        assertThat(Results.get(0).getWeightKilogram().floatValue(), is(0.0f));
        assertThat(Results.get(1).getWeightKilogram().floatValue(), is(0.005f));
        assertThat(Results.get(2).getWeightKilogram().floatValue(), is(1.28f));

        assertThat(Results.get(3).getWeightKilogram().floatValue(), is(0.0f));
        assertThat(Results.get(4).getWeightKilogram().floatValue(), is(0.0045359237f));
        assertThat(Results.get(5).getWeightKilogram().floatValue(), is(1.1611965f));

        assertThat(Results.get(6).getWeightKilogram(), Matchers.nullValue());
        assertThat(Results.get(7).getWeightKilogram(), Matchers.nullValue());
    }

    @Test
    public void userId() {
        WeightScaleProfile<WeightScaleScaleProfileTest> weightScaleProfile = new WeightScaleProfileProxy(this);
        weightScaleProfile.processWeightMeasurement(new byte[]{
                (byte) (1 << 2), (byte) 1, (byte) 0, (byte) 1,
                (byte) 0, (byte) 2, (byte) 0,
                (byte) (1 << 2), (byte) 3, (byte) 0, (byte) 254,
                (byte) (1 << 2), (byte) 4, (byte) 0, (byte) 255,
        });

        assertThat(Results.size(), is(4));

        assertThat(Results.get(0).getWeightKilogram().floatValue(), is(0.005f));
        assertThat(Results.get(0).getUserId(), is(1));

        assertThat(Results.get(1).getWeightKilogram().floatValue(), is(0.010f));
        assertThat(Results.get(1).getUserId(), Matchers.nullValue());

        assertThat(Results.get(2).getWeightKilogram().floatValue(), is(0.015f));
        assertThat(Results.get(2).getUserId(), is(254));

        assertThat(Results.get(3).getWeightKilogram().floatValue(), is(0.020f));
        assertThat(Results.get(3).getUserId(), Matchers.nullValue());
    }

    @Test
    public void time() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        try {
            WeightScaleProfile<WeightScaleScaleProfileTest> weightScaleProfile = new WeightScaleProfileProxy(this);
            weightScaleProfile.processWeightMeasurement(new byte[]{
                    (byte) (1 << 1), (byte) 1, (byte) 0,
                    (byte) 0xE9, (byte) 0x07, // year 2025
                    (byte) 8, // month 8
                    (byte) 10, // day 10
                    (byte) 23, // hour 23
                    (byte) 59, // minute 59
                    (byte) 58, // second 58
                    (byte) 0, (byte) 2, (byte) 0
            });

            assertThat(Results.size(), is(2));

            assertThat(Results.get(0).getWeightKilogram().floatValue(), is(0.005f));
            // timestamp was in local time zone Pacific Daylight Time:
            // UTC-08:00 with DST (+01:00)
            assertThat(Results.get(0).getTime().toString(), is("2025-08-11T06:59:58Z"));
            assertThat(Results.get(0).getTime().getEpochSecond(), is(1754895598L));

            assertThat(Results.get(1).getWeightKilogram().floatValue(), is(0.010f));
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    @Test
    public void bmi() {
        WeightScaleProfile<WeightScaleScaleProfileTest> weightScaleProfile = new WeightScaleProfileProxy(this);
        weightScaleProfile.processWeightMeasurement(new byte[]{
                (byte) (1 << 3), (byte) 1, (byte) 0,
                (byte) 1, (byte) 2, // BMI
                (byte) 3, (byte) 4, // height
                (byte) ((1 << 3) | 1), (byte) 1, (byte) 0,
                (byte) 1, (byte) 2, // BMI
                (byte) 3, (byte) 4, // height
                (byte) 0, (byte) 2, (byte) 0
        });

        assertThat(Results.size(), is(3));

        assertThat(Results.get(0).getWeightKilogram().floatValue(), is(0.005f));
        assertThat(Results.get(0).getBMI(), is(51.3f));
        assertThat(Results.get(0).getHeightMeter(), is(1.027f));

        assertThat(Results.get(1).getWeightKilogram().floatValue(), is(0.0045359237f));
        assertThat(Results.get(1).getBMI(), is(51.3f));
        assertThat(Results.get(1).getHeightMeter(), is(2.60858F));

        assertThat(Results.get(2).getWeightKilogram().floatValue(), is(0.010f));
    }

    @Test
    public void complex() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kathmandu"));
        try {
            WeightScaleProfile<WeightScaleScaleProfileTest> weightScaleProfile = new WeightScaleProfileProxy(this);
            weightScaleProfile.processWeightMeasurement(new byte[]{
                    (byte) ((1 << 1) | (1 << 2) | (1 << 3)), (byte) 1, (byte) 0,
                    (byte) 0xE9, (byte) 0x07, // year 2025
                    (byte) 8, // month 8
                    (byte) 10, // day 10
                    (byte) 23, // hour 23
                    (byte) 59, // minute 59
                    (byte) 58, // second 58
                    (byte) 254, // user
                    (byte) 1, (byte) 2, // BMI
                    (byte) 3, (byte) 4, // height
                    (byte) 0, (byte) 2, (byte) 0
            });

            assertThat(Results.size(), is(2));

            assertThat(Results.get(0).getWeightKilogram().floatValue(), is(0.005f));
            // timestamp was in local time zone Kathmandu:
            // UTC+5:45 no daylight saving time
            assertThat(Results.get(0).getTime().toString(), is("2025-08-10T18:14:58Z"));
            assertThat(Results.get(0).getTime().getEpochSecond(), is(1754849698L));
            assertThat(Results.get(0).getUserId(), is(254));
            assertThat(Results.get(0).getBMI(), is(51.3f));
            assertThat(Results.get(0).getHeightMeter(), is(1.027f));

            assertThat(Results.get(1).getWeightKilogram().floatValue(), is(0.010f));
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private class WeightScaleProfileProxy extends WeightScaleProfile<WeightScaleScaleProfileTest> {
        WeightScaleProfileProxy(WeightScaleScaleProfileTest support) {
            super(support);
        }

        @Override
        protected Intent createIntent(final WeightScaleMeasurement measurement) {
            Results.add(measurement);
            return null;
        }
    }
}
