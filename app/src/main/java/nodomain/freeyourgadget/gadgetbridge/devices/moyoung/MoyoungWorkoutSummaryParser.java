/*  Copyright (C) 2025 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;

public class MoyoungWorkoutSummaryParser implements ActivitySummaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(MoyoungWorkoutSummaryParser.class);
    private final GBDevice gbDevice;

    public MoyoungWorkoutSummaryParser(final GBDevice device) {
        this.gbDevice = device;
    }

    @Override
    public BaseActivitySummary parseBinaryData(final BaseActivitySummary summary, final boolean forDetails) {
        final byte[] rawSummaryData = summary.getRawSummaryData();
        if (rawSummaryData == null) {
            return summary;
        }

        final ByteBuffer buffer = ByteBuffer.wrap(rawSummaryData).order(ByteOrder.LITTLE_ENDIAN);

        final ActivitySummaryData summaryData = new ActivitySummaryData();

        int protocolVersion = 0;
        if (rawSummaryData.length % 24 == 0) {
            protocolVersion = 1;
        } else if (rawSummaryData.length % 26 == 0) {
            protocolVersion = 2;
        }
        if (protocolVersion == 0) {
            LOG.error("Invalid raw data");
            return summary;
        }

        byte num = 0;
        byte avgHR = 0;
        if (protocolVersion == 2) {
            buffer.get();  // skip packet subtype
            num = buffer.get();
        }
        Date startTime = MoyoungConstants.WatchTimeToLocalTime(buffer.getInt());
        Date endTime = MoyoungConstants.WatchTimeToLocalTime(buffer.getInt());
        int validTime = buffer.getShort();
        if (protocolVersion == 1) {
            num = buffer.get(); // == i
        } else {
            avgHR = buffer.get();
        }
        byte type = buffer.get();
        int steps = buffer.getInt();
        int distance = buffer.getInt();
        int calories;
        if (protocolVersion == 1) {
            calories = buffer.getShort();
        } else {
            calories = buffer.getInt();
        }

        summaryData.add(ActivitySummaryEntries.ACTIVE_SECONDS, validTime, ActivitySummaryEntries.UNIT_SECONDS);
        summaryData.add(ActivitySummaryEntries.STEPS, steps, ActivitySummaryEntries.UNIT_STEPS);
        summaryData.add(ActivitySummaryEntries.DISTANCE_METERS, distance, ActivitySummaryEntries.UNIT_METERS);
        summaryData.add(ActivitySummaryEntries.CALORIES_BURNT, calories, ActivitySummaryEntries.UNIT_KCAL);
        if (protocolVersion == 2) {
            summaryData.add(ActivitySummaryEntries.HR_AVG, avgHR, ActivitySummaryEntries.UNIT_BPM);
        }

        summary.setSummaryData(summaryData.toString());

        return summary;
    }
}
