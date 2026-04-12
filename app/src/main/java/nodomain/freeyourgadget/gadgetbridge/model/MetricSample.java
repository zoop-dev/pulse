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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitEnduranceScore;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFunctionalMetrics;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitHillScore;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitMaxMetData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitTrainingReadiness;

public interface MetricSample extends TimeSample {
    default Metric getMetric() {
        int dbId = getMetricType();
        return Metric.fromDbId(dbId);
    }

    default void setMetric(@NonNull Metric type) {
        setMetricType(type.dbId);
    }

    default void setMetric(@NonNull Metric type, @Nullable Double value, @Nullable Long extra) {
        setMetricType(type.dbId);
        setMetricScore(value);
        setMetricExtra(extra);
    }

    int getMetricType();

    /// use {@link #setMetric(Metric)} or {@link #setMetric(Metric, Double, Long)} instead
    void setMetricType(@IntRange(from = 0, to = 8) int type);

    @Nullable
    Double getMetricScore();

    void setMetricScore(@Nullable Double value);

    @Nullable
    Long getMetricExtra();

    void setMetricExtra(@Nullable Long extra);

    enum Metric {
        UNKNOWN(0),
        /// @see FitEnduranceScore#getEnduranceScore()
        /// @see FitEnduranceScore#getLevel()
        GARMIN_ENDURANCE_SCORE(1),
        /// @see FitFunctionalMetrics#getFunctionalThresholdPower()
        /// @see FitFunctionalMetrics#getCyclingLactaceThresholdHr()
        GARMIN_FUNCTIONAL_THRESHOLD_POWER(2),
        /// @see FitHillScore#getHillEndurance()
        /// @see FitHillScore#getLevel()
        GARMIN_HILL_ENDURANCE(3),
        /// @see FitHillScore#getHillScore()
        /// @see FitHillScore#getLevel()
        GARMIN_HILL_SCORE(4),
        /// @see FitHillScore#getHillStrength()
        /// @see FitHillScore#getLevel()
        GARMIN_HILL_STRENGTH(5),
        /// @see FitMaxMetData#getVo2Max()
        /// @see FitMaxMetData#getMaxMetCategory()
        GARMIN_MET_MAX_VO2(6),
        /// @see FitFunctionalMetrics#getRunningLactateThresholdPower()
        /// @see FitFunctionalMetrics#getRunningLactateThresholdHr()
        GARMIN_RUNNING_LACTATE_THRESHOLD_POWER(7),
        /// @see FitTrainingReadiness#getTrainingReadiness()
        /// @see FitTrainingReadiness#getLevel()
        GARMIN_TRAINING_READINESS(8);

        final int dbId;

        Metric(int dbId) {
            this.dbId = dbId;
        }

        @Nullable
        static Metric fromDbId(int dbId) {
            for (Metric metric : values()) {
                if (metric.dbId == dbId) {
                    return metric;
                }
            }
            return null;
        }
    }
}
