/*  Copyright (C) 2026 Thomas Kuehne

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

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KCAL_PER_DAY;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_ML_KG_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_NONE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_WATT;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitEnduranceScore;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFunctionalMetrics;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitHillScore;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitMaxMetData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitMonitoringInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitPhysiologicalMetrics;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitTrainingLoad;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitTrainingReadiness;

/**
 * This is for infrequent statistics samples - for example one record per day and metric.
 */
public interface MetricSample extends TimeSample {
    default Metric getMetric() {
        int dbId = getMetricType();
        return Metric.fromDbId(dbId);
    }

    default void setMetric(@NonNull Metric type) {
        setMetricType(type.dbId);
    }

    default void setMetric(@NonNull Metric type, double score) {
        setMetric(type, score, null);
    }

    default void setMetric(@NonNull Metric type, double score, @Nullable Long extra) {
        setMetricType(type.dbId);
        setMetricScore(score);
        setMetricExtra(extra);
    }

    @IntRange(from = 0, to = 12)
    int getMetricType();

    /// use {@link #setMetric(Metric)} or {@link #setMetric(Metric, Double, Long)} instead
    void setMetricType(@IntRange(from = 1, to = 12) int type);

    double getMetricScore();

    void setMetricScore(double score);

    @Nullable
    Long getMetricExtra();

    void setMetricExtra(@Nullable Long extra);

    enum Metric {
        UNKNOWN(0, UNIT_NONE),
        /// @see FitEnduranceScore#getEnduranceScore()
        /// @see FitEnduranceScore#getLevel()
        GARMIN_ENDURANCE_SCORE(1, UNIT_NONE),
        /// @see FitFunctionalMetrics#getFunctionalThresholdPower()
        /// @see FitFunctionalMetrics#getCyclingLactaceThresholdHr()
        GARMIN_FUNCTIONAL_THRESHOLD_POWER(2, UNIT_WATT),
        /// @see FitHillScore#getHillEndurance()
        /// @see FitHillScore#getLevel()
        GARMIN_HILL_ENDURANCE(3, UNIT_NONE),
        /// @see FitHillScore#getHillScore()
        /// @see FitHillScore#getLevel()
        GARMIN_HILL_SCORE(4, UNIT_NONE),
        /// @see FitHillScore#getHillStrength()
        /// @see FitHillScore#getLevel()
        GARMIN_HILL_STRENGTH(5, UNIT_NONE),
        /// This is a metabolic equivalent (MET) version of {@link #GENERIC_MAXIMUM_OXYGEN_UPTAKE}.
        /// Estimated using 24/7 monitoring instead of high resolution activity recordings.
        ///
        /// @see FitMaxMetData#getVo2Max()
        /// @see FitMaxMetData#getMaxMetCategory()
        GARMIN_MET_MAX_VO2(6, UNIT_ML_KG_MIN),
        /// @see FitFunctionalMetrics#getRunningLactateThresholdPower()
        /// @see FitFunctionalMetrics#getRunningLactateThresholdHr()
        GARMIN_RUNNING_LACTATE_THRESHOLD_POWER(7, UNIT_WATT),
        /// @see FitTrainingReadiness#getTrainingReadiness()
        /// @see FitTrainingReadiness#getLevel()
        GARMIN_TRAINING_READINESS(8, UNIT_NONE),
        /// @see FitTrainingLoad#getTrainingLoadAcute
        GENERIC_TRAINING_LOAD_ACUTE(9, UNIT_NONE),
        /// @see FitTrainingLoad#getTrainingLoadChronic()
        GENERIC_TRAINING_LOAD_CHRONIC(10, UNIT_NONE),
        /// @see FitMonitoringInfo#getRestingMetabolicRate()
        GENERIC_RESTING_METABOLIC_RATE(11, UNIT_KCAL_PER_DAY),
        /// @see FitPhysiologicalMetrics#getMetMax()
        GENERIC_MAXIMUM_OXYGEN_UPTAKE(12, UNIT_ML_KG_MIN),
        ;

        final public int dbId;

        /// @see nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries
        @NonNull
        final public String uomKey;

        Metric(int dbId, @NonNull String uomKey) {
            this.dbId = dbId;
            this.uomKey = uomKey;
        }

        @Nullable
        public static Metric fromDbId(int dbId) {
            for (Metric metric : values()) {
                if (metric.dbId == dbId) {
                    return metric;
                }
            }
            return null;
        }
    }
}
