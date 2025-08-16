/*  Copyright (C) 2020-2024 José Rebelo, Petr Vaněk, Reiner Herrmann,
    Sebastian Krey

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
package nodomain.freeyourgadget.gadgetbridge.model;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;

public class ActivitySummaryJsonSummary {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryJsonSummary.class);
    private ActivitySummaryData summaryData;
    private final ActivitySummaryParser summaryParser;
    private final BaseActivitySummary baseActivitySummary;

    public ActivitySummaryJsonSummary(final ActivitySummaryParser summaryParser, BaseActivitySummary baseActivitySummary) {
        this.summaryParser = summaryParser;
        this.baseActivitySummary = baseActivitySummary;
    }

    private ActivitySummaryData setSummaryData(BaseActivitySummary item, final boolean forDetails) {
        final ActivitySummaryData summary = ActivitySummaryData.fromJson(getCorrectSummary(item, forDetails));

        //add additionally computed values here
        if (item.getBaseAltitude() != null && item.getBaseAltitude() != -20000 && !summary.has("baseAltitude")) {
            summary.add("baseAltitude", item.getBaseAltitude(), UNIT_METERS);
        }

        if (!summary.has("averageSpeed") && summary.has("distanceMeters") && summary.has("activeSeconds")) {
            double distance = summary.getNumber("distanceMeters", 0).doubleValue();
            double duration = summary.getNumber("activeSeconds", 1).doubleValue();
            summary.add("averageSpeed", distance / duration, UNIT_METERS_PER_SECOND);
        }

        if (!summary.has(STEP_RATE_AVG) && summary.has(STEPS) && summary.has(ACTIVE_SECONDS)) {
            double stepcount = summary.getNumber(STEPS, 0).doubleValue();
            double duration = summary.getNumber(ACTIVE_SECONDS, 1).doubleValue();
            summary.add(STEP_RATE_AVG, (double)((int)(((stepcount / duration) * 60)+0.5)), UNIT_SPM);
        }

        return summary;
    }

    public ActivitySummaryData getSummaryData(final boolean forDetails) {
        //returns json with summaryData
        if (summaryData == null) summaryData = setSummaryData(baseActivitySummary, forDetails);
        return summaryData;
    }

    private String getCorrectSummary(BaseActivitySummary item, final boolean forDetails) {
        if (summaryParser == null) {
            return item.getSummaryData();
        }
        try {
            item = summaryParser.parseBinaryData(item, forDetails);
        } catch (final Exception e) {
            LOG.error("Failed to re-parse corrected summary", e);
        }
        return item.getSummaryData();
    }
}
