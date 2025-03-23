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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiSleepSessionSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiSleepSessionSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.RangeMap;

public class HuamiSleepSessionSampleProvider extends AbstractTimeSampleProvider<HuamiSleepSessionSample> {
    private static final Logger LOG = LoggerFactory.getLogger(HuamiSleepSessionSampleProvider.class);

    public HuamiSleepSessionSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<HuamiSleepSessionSample, ?> getSampleDao() {
        return getSession().getHuamiSleepSessionSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuamiSleepSessionSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuamiSleepSessionSampleDao.Properties.DeviceId;
    }

    @Override
    public HuamiSleepSessionSample createSample() {
        return new HuamiSleepSessionSample();
    }

    public RangeMap<Long, ActivityKind> getSleepStages(final long timestampFrom, final long timestampTo) {
        final RangeMap<Long, ActivityKind> stagesMap = new RangeMap<>(RangeMap.Mode.LOWER_BOUND);

        final List<HuamiSleepSessionSample> sessions = getAllSamples(timestampFrom, timestampTo + 86400_000L);

        for (final HuamiSleepSessionSample rawSession : sessions) {
            final byte[] bytes = rawSession.getData();

            final int numStages = BLETypeConversions.toUnsigned(bytes, 0x54);
            if (numStages == 0) {
                continue;
            }

            final SleepSession sleepSession = new SleepSession(rawSession.getData());

            stagesMap.put((sleepSession.timestampMidnight - 24 * 3600 + sleepSession.sleepEnd * 60L) * 1000L, ActivityKind.UNKNOWN);

            for (final SleepStage stage : sleepSession.stages) {
                final int stageStart = sleepSession.timestampMidnight - 24 * 3600 + stage.start * 60;
                LOG.trace("Sleep stage start at {}", DateTimeUtils.formatIso8601(new Date(stageStart * 1000L)));
                stagesMap.put(stageStart * 1000L, stage.asActivityKind());
            }
        }

        return stagesMap;
    }

    public static class SleepSession {
        private final int timestampSession;
        private final int timestampMidnight;
        private final int sleepStart;
        private final int sleepEnd;
        private final int avgHr;
        private final int score;

        private final List<SleepStage> stages;

        private final int totalRemMinutes;
        private final int totalLightMinutes;
        private final int totalDeepMinutes;
        private final int totalWakeMinutes;

        public SleepSession(final byte[] bytes) {
            this.timestampSession = BLETypeConversions.toUint32(bytes, 0x00);
            // midnight boundary of the day, in the user's timezone
            this.timestampMidnight = BLETypeConversions.toUint32(bytes, 0x04);
            final int one1 = bytes[0x08]; // 1
            final int one2 = bytes[0x09]; // 1
            this.sleepStart = BLETypeConversions.toUint16(bytes, 0x0a);
            this.sleepEnd = BLETypeConversions.toUint16(bytes, 0x0c);
            this.avgHr = BLETypeConversions.toUnsigned(bytes, 0x15);
            this.score = BLETypeConversions.toUnsigned(bytes, 0x16);

            final int numStages = BLETypeConversions.toUnsigned(bytes, 0x54);
            this.stages = new ArrayList<>(numStages);

            for (int i = 0; i < numStages; i++) {
                // Each stage is 5b: start, end, type
                final int stageStart = BLETypeConversions.toUint16(bytes, 0x56 + 5 * i);
                final int stageEnd = BLETypeConversions.toUint16(bytes, 0x56 + 5 * i + 2);
                final int stageType = BLETypeConversions.toUnsigned(bytes, 0x56 + 5 * i + 4);
                LOG.trace("Sleep stage start={}, end={}, type={}", stageStart, stageEnd, stageType);
                this.stages.add(new SleepStage(stageStart, stageEnd, stageType));
            }

            this.totalRemMinutes = BLETypeConversions.toUint16(bytes, 0x024a);
            this.totalLightMinutes = BLETypeConversions.toUint16(bytes, 0x024c);
            this.totalDeepMinutes = BLETypeConversions.toUint16(bytes, 0x024e);
            this.totalWakeMinutes = BLETypeConversions.toUint16(bytes, 0x0250);

            LOG.trace(
                    "Sleep session at {}/{}: sleepStart={}, sleepEnd={}, avgHr={}, score={}",
                    DateTimeUtils.formatIso8601(new Date(timestampSession * 1000L)),
                    DateTimeUtils.formatIso8601(new Date(timestampMidnight * 1000L)),
                    sleepStart, sleepEnd, avgHr, score
            );
        }
    }

    public static class SleepStage {
        private final int start;
        private final int end;
        private final int type;

        public SleepStage(final int start, final int end, final int type) {
            this.start = start;
            this.end = end;
            this.type = type;
        }

        /**
         * Minutes since noon of the previous day?
         */
        public int getStart() {
            return start;
        }

        /**
         * Minutes since noon of the previous day?
         */
        public int getEnd() {
            return end;
        }

        /**
         * 4 = light, 5 = deep, 8 = rem, 7 = awake
         */
        public int getType() {
            return type;
        }

        public ActivityKind asActivityKind() {
            switch (type) {
                case 4:
                    return ActivityKind.LIGHT_SLEEP;
                case 5:
                    return ActivityKind.DEEP_SLEEP;
                case 8:
                    return ActivityKind.REM_SLEEP;
                case 7:
                    return ActivityKind.AWAKE_SLEEP;
            }

            return ActivityKind.SLEEP_ANY;
        }
    }
}
