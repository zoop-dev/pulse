package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.*;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts.DefaultWorkoutCharts;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryProgressEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryTableBuilder;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryValue;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.workout.Workout;
import nodomain.freeyourgadget.gadgetbridge.model.workout.WorkoutChart;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.exception.FitParseException;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionExerciseCategory.ExerciseCategory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitDiveGas;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitDiveSummary;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLap;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitPhysiologicalMetrics;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSession;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSet;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitTimeInZone;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitUserProfile;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class GarminWorkoutParser implements ActivitySummaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(GarminWorkoutParser.class);

    private final Context context;

    private final List<FitTimeInZone> timesInZone = new ArrayList<>();
    private final List<ActivityPoint> activityPoints = new ArrayList<>();
    private FitSession session = null;
    private FitSport sport = null;
    private FitUserProfile userProfile = null;
    private FitPhysiologicalMetrics physiologicalMetrics = null;
    private FitDiveSummary diveSummary = null;
    private final List<FitDiveGas> diveGases = new ArrayList<>();
    private final List<FitSet> sets = new ArrayList<>();
    private final List<FitLap> laps = new ArrayList<>();

    public GarminWorkoutParser(final Context context) {
        this.context = context;
    }

    @Override
    public BaseActivitySummary parseBinaryData(BaseActivitySummary summary, boolean forDetails) {
        // FIXME Do not use this
        return parseWorkout(summary, forDetails).getSummary();
    }

    @Override
    public Workout parseWorkout(BaseActivitySummary summary, final boolean forDetails) {
        if (!forDetails) {
            // Our parsing is too slow, especially without a RecyclerView
            return new Workout(summary, ActivitySummaryData.fromJson(summary.getSummaryData()));
        }

        final long nanoStart = System.nanoTime();

        reset();

        final String rawDetailsPath = summary.getRawDetailsPath();
        if (rawDetailsPath == null) {
            LOG.warn("No rawDetailsPath");
            return new Workout(summary, ActivitySummaryData.fromJson(summary.getSummaryData()));
        }
        final File file = FileUtils.tryFixPath(new File(rawDetailsPath));
        if (file == null || !file.isFile() || !file.canRead()) {
            LOG.warn("Unable to read {}", rawDetailsPath);
            return new Workout(summary, ActivitySummaryData.fromJson(summary.getSummaryData()));
        }

        final FitFile fitFile;
        try {
            fitFile = FitFile.parseIncoming(file);
        } catch (final IOException | FitParseException e) {
            LOG.error("Failed to parse fit file", e);
            return new Workout(summary, ActivitySummaryData.fromJson(summary.getSummaryData()));
        }

        for (final RecordData record : fitFile.getRecords()) {
            handleRecord(record);
        }

        final ActivitySummaryData activitySummaryData = updateSummary(summary);
        final ActivityKind activityKind = ActivityKind.fromCode(summary.getActivityKind());
        final ActivityKind.CycleUnit cycleUnit = ActivityKind.getCycleUnit(activityKind);

        if (cycleUnit == ActivityKind.CycleUnit.STEPS) {
            activityPoints.forEach(ap -> ap.setCadence(ap.getCadence() * 2));
        }

        final List<WorkoutChart> charts = new LinkedList<>();
        if (!this.activityPoints.isEmpty()) {
            charts.addAll(DefaultWorkoutCharts.buildDefaultCharts(context, activityPoints, activityKind));
        }

        final long nanoEnd = System.nanoTime();
        final long executionTime = (nanoEnd - nanoStart) / 1000000;
        LOG.trace("Updating summary took {}ms", executionTime);

        return new Workout(
                summary,
                activitySummaryData,
                charts
        );
    }

    public void reset() {
        timesInZone.clear();
        activityPoints.clear();
        session = null;
        sport = null;
        userProfile = null;
        physiologicalMetrics = null;
        diveSummary = null;
        diveGases.clear();
        sets.clear();
        laps.clear();
    }

    public boolean handleRecord(final RecordData record) {
        if (record instanceof FitRecord) {
            activityPoints.add(((FitRecord) record).toActivityPoint());
        } else if (record instanceof FitSession) {
            LOG.debug("Session: {}", record);
            if (session != null) {
                LOG.warn("Got multiple sessions - NOT SUPPORTED: {}", record);
            } else {
                // We only support 1 session
                session = (FitSession) record;
            }
        } else if (record instanceof FitPhysiologicalMetrics) {
            LOG.debug("Physiological Metrics: {}", record);
            physiologicalMetrics = (FitPhysiologicalMetrics) record;
        } else if (record instanceof FitSport) {
            LOG.debug("Sport: {}", record);
            if (sport != null) {
                LOG.warn("Got multiple sports - NOT SUPPORTED: {}", record);
            } else {
                // We only support 1 sport
                sport = (FitSport) record;
            }
        } else if (record instanceof FitTimeInZone) {
            LOG.trace("Time in zone: {}", record);
            timesInZone.add((FitTimeInZone) record);
        } else if (record instanceof FitSet) {
            LOG.trace("Set: {}", record);
            sets.add((FitSet) record);
        } else if (record instanceof FitLap) {
            LOG.trace("Lap: {}", record);
            laps.add((FitLap) record);
        } else if (record instanceof FitUserProfile) {
            LOG.trace("User Profile: {}", record);
            if (userProfile != null) {
                LOG.warn("Got multiple user profiles - NOT SUPPORTED: {}", record);
            } else {
                // We only support 1 user profile
                userProfile = (FitUserProfile) record;
            }
        } else if (record instanceof FitDiveSummary) {
            LOG.trace("Dive summary: {}", record);
            if (diveSummary != null) {
                return false; // for some reason there is more than one message, in my activities the first one contains all data
            }
            diveSummary = (FitDiveSummary) record;
        } else if (record instanceof FitDiveGas) {
            LOG.trace("Dive gas: {}", record);
            diveGases.add((FitDiveGas) record);
        } else {
            return false;
        }

        return true;
    }

    public ActivitySummaryData updateSummary(final BaseActivitySummary summary) {
        final ActivitySummaryData summaryData = new ActivitySummaryData();

        if (session == null) {
            LOG.error("Got workout, but no session");
            return summaryData;
        }

        final ActivityKind activityKind;
        if (sport != null) {
            if (StringUtils.isNullOrEmpty(summary.getName())) {
                summary.setName(sport.getName());
            }
            activityKind = getActivityKind(sport.getSport(), sport.getSubSport());
        } else {
            activityKind = getActivityKind(session.getSport(), session.getSubSport());
        }
        final ActivityKind.CycleUnit cycleUnit = ActivityKind.getCycleUnit(activityKind);

        final String weightUnit;
        if (userProfile != null && userProfile.getWeightSetting() != null) {
            weightUnit = FieldDefinitionMeasurementSystem.Type.metric.equals(userProfile.getWeightSetting()) ? UNIT_KG : UNIT_LB;
        } else {
            weightUnit = UNIT_KG;
        }

        summary.setActivityKind(activityKind.getCode());

        if (session.getTotalElapsedTime() != null) {
            summary.setEndTime(new Date(summary.getStartTime().getTime() + session.getTotalElapsedTime().intValue()));
        }

        if (session.getTotalTimerTime() != null) {
            summaryData.add(ACTIVE_SECONDS, session.getTotalTimerTime() / 1000f, UNIT_SECONDS);
        }
        if (session.getTotalDistance() != null) {
            summaryData.add(DISTANCE_METERS, session.getTotalDistance() / 100f, UNIT_METERS);
        }
        if (session.getPoolLength() != null) {
            summaryData.add(POOL_LENGTH, session.getPoolLength(), UNIT_METERS);
        }
        if (session.getAvgSwolf() != null) {
            summaryData.add(SWOLF_AVG, session.getAvgSwolf(), UNIT_NONE);
        }
        if (session.getTotalCycles() != null && cycleUnit != ActivityKind.CycleUnit.NONE) {
            if (cycleUnit == ActivityKind.CycleUnit.STEPS) {
                summaryData.addTotal(session.getTotalCycles() * 2, cycleUnit);
            } else {
                // FIXME some of the rest might also need adjusting...
                summaryData.addTotal(session.getTotalCycles(), cycleUnit);
            }
        }
        summaryData.add(STEP_LENGTH_AVG, session.getAvgStepLength(), UNIT_MM);
        if (session.getTotalCalories() != null) {
            summaryData.add(CALORIES_BURNT, session.getTotalCalories(), UNIT_KCAL);
            if (session.getRestingCalories() != null) {
                summaryData.add(CALORIES_ACTIVE, session.getTotalCalories() - session.getRestingCalories(), UNIT_KCAL);
                summaryData.add(CALORIES_RESTING, session.getRestingCalories(), UNIT_KCAL);
            }
        }
        if (session.getEstimatedSweatLoss() != null) {
            summaryData.add(ESTIMATED_SWEAT_LOSS, session.getEstimatedSweatLoss(), UNIT_ML);
        }
        if (session.getAverageHeartRate() != null) {
            summaryData.add(HR_AVG, session.getAverageHeartRate(), UNIT_BPM);
        }
        if (session.getMaxHeartRate() != null) {
            summaryData.add(HR_MAX, session.getMaxHeartRate(), UNIT_BPM);
        }
        if (session.getHrvSdrr() != null) {
            summaryData.add(HRV_SDRR, session.getHrvSdrr(), UNIT_MILLISECONDS);
        }
        if (session.getHrvRmssd() != null) {
            summaryData.add(HRV_RMSSD, session.getHrvRmssd(), UNIT_MILLISECONDS);
        }
        if (session.getAvgSpo2() != null) {
            summaryData.add(SPO2_AVG, session.getAvgSpo2(), UNIT_PERCENTAGE);
        }
        if (session.getEnhancedMinRespirationRate() != null) {
            summaryData.add(RESPIRATION_MIN, session.getEnhancedMinRespirationRate(), UNIT_BREATHS_PER_MIN);
        }
        if (session.getEnhancedMaxRespirationRate() != null) {
            summaryData.add(RESPIRATION_MAX, session.getEnhancedMaxRespirationRate(), UNIT_BREATHS_PER_MIN);
        }
        if (session.getEnhancedAvgRespirationRate() != null) {
            summaryData.add(RESPIRATION_AVG, session.getEnhancedAvgRespirationRate(), UNIT_BREATHS_PER_MIN);
        }
        if (session.getAvgStress() != null) {
            summaryData.add(STRESS_AVG, session.getAvgStress(), UNIT_NONE);
        }
        if (session.getAvgCadence() != null) {
            if (cycleUnit == ActivityKind.CycleUnit.STEPS) {
                summaryData.addCadenceAvg(session.getAvgCadence() * 2, cycleUnit);
            } else {
                // FIXME some of the rest might also need adjusting...
                summaryData.addCadenceAvg(session.getAvgCadence(), cycleUnit);
            }
        }
        if (session.getMaxCadence() != null) {
            if (cycleUnit == ActivityKind.CycleUnit.STEPS) {
                summaryData.addCadenceMax(session.getMaxCadence() * 2, cycleUnit);
            } else {
                // FIXME some of the rest might also need adjusting...
                summaryData.addCadenceMax(session.getMaxCadence(), cycleUnit);
            }
        }
        if (session.getTotalAscent() != null && !ActivityKind.isDiving(activityKind)) {
            summaryData.add(TOTAL_ASCENT, session.getTotalAscent(), UNIT_METERS);
        }
        if (session.getTotalDescent() != null && !ActivityKind.isDiving(activityKind)) {
            summaryData.add(TOTAL_DESCENT, session.getTotalDescent(), UNIT_METERS);
        }
        if (session.getAvgSwimCadence() != null) {
            summaryData.add(SWIM_AVG_CADENCE, session.getAvgSwimCadence(), UNIT_STROKES_PER_LENGTH);
        }

        if (session.getEnhancedAvgSpeed() != null) {
            if (ActivityKind.isPaceActivity(activityKind)) {
                summaryData.add(PACE_AVG_SECONDS_KM, Math.round((60 / (session.getEnhancedAvgSpeed() * 3.6)) * 60), UNIT_SECONDS);
            } else {
                summaryData.add(SPEED_AVG, Math.round((session.getEnhancedAvgSpeed() * 3600 / 1000) * 100.0) / 100.0, UNIT_KMPH);
            }
        }

        if (session.getEnhancedMaxSpeed() != null) {
            if (ActivityKind.isPaceActivity(activityKind)) {
                summaryData.add(PACE_MAX, Math.round((60 / (session.getEnhancedMaxSpeed() * 3.6)) * 60), UNIT_SECONDS);
            } else if (ActivityKind.isDiving(activityKind)) { // Hide average speed for diving activities
            } else {
                summaryData.add(SPEED_MAX, Math.round((session.getEnhancedMaxSpeed() * 3600 / 1000) * 100.0) / 100.0, UNIT_KMPH);
            }
        }

        summaryData.add(AVG_POWER, session.getAvgPower(), UNIT_WATT);
        summaryData.add(MAX_POWER, session.getMaxPower(), UNIT_WATT);
        summaryData.add(NORMALIZED_POWER, session.getNormalizedPower(), UNIT_WATT);

        if (session.getStandTime() != null) {
            summaryData.add(STANDING_TIME, session.getStandTime() / 1000, UNIT_SECONDS);
        }
        summaryData.add(STANDING_COUNT, session.getStandCount(), UNIT_NONE);
        summaryData.add(AVG_LEFT_PCO, session.getAvgLeftPco(), UNIT_MM);
        summaryData.add(AVG_RIGHT_PCO, session.getAvgRightPco(), UNIT_MM);

        summaryData.add(AVG_VERTICAL_OSCILLATION, session.getAvgVerticalOscillation(), UNIT_MM);
        summaryData.add(AVG_GROUND_CONTACT_TIME, session.getAvgStanceTime(), UNIT_MILLISECONDS);
        summaryData.add(AVG_VERTICAL_RATIO, session.getAvgVerticalRatio(), UNIT_PERCENTAGE);
        if (session.getAvgStanceTimeBalance() != null) {
            summaryData.add(
                    AVG_GROUND_CONTACT_TIME_BALANCE,
                    context.getString(
                            R.string.range_percentage_float,
                            session.getAvgStanceTimeBalance(),
                            100f - session.getAvgStanceTimeBalance()
                    )
            );
        }

        summaryData.add(STEP_SPEED_LOSS, session.getStepSpeedLoss(), UNIT_CENTIMETERS_PER_SECOND);
        summaryData.add(STEP_SPEED_LOSS_PERCENTAGE, session.getStepSpeedLossPercentage(), UNIT_PERCENTAGE);

        final Number[] avgLeftPowerPhase = session.getAvgLeftPowerPhase();
        if (avgLeftPowerPhase != null && avgLeftPowerPhase.length == 4) {
            final Number startAngle = avgLeftPowerPhase[0];
            final Number endAngle = avgLeftPowerPhase[1];
            if (startAngle != null && endAngle != null) {
                summaryData.add(
                        AVG_LEFT_POWER_PHASE,
                        context.getString(
                                R.string.range_degrees,
                                Math.round(startAngle.floatValue() / 0.7111111),
                                Math.round(endAngle.floatValue() / 0.7111111)
                        )
                );
            }
        }

        final Number[] avgRightPowerPhase = session.getAvgRightPowerPhase();
        if (avgRightPowerPhase != null && avgRightPowerPhase.length == 4) {
            final Number startAngle = avgRightPowerPhase[0];
            final Number endAngle = avgRightPowerPhase[1];
            if (startAngle != null && endAngle != null) {
                summaryData.add(
                        AVG_RIGHT_POWER_PHASE,
                        context.getString(
                                R.string.range_degrees,
                                Math.round(startAngle.floatValue() / 0.7111111),
                                Math.round(endAngle.floatValue() / 0.7111111)
                        )
                );
            }
        }

        final Number[] avgLeftPowerPhasePeak = session.getAvgLeftPowerPhasePeak();
        if (avgLeftPowerPhasePeak != null && avgLeftPowerPhasePeak.length == 4) {
            final Number startAngle = avgLeftPowerPhasePeak[0];
            final Number endAngle = avgLeftPowerPhasePeak[1];
            if (startAngle != null && endAngle != null) {
                summaryData.add(
                        AVG_LEFT_POWER_PHASE_PEAK,
                        context.getString(
                                R.string.range_degrees,
                                Math.round(startAngle.floatValue() / 0.7111111),
                                Math.round(endAngle.floatValue() / 0.7111111)
                        )
                );
            }
        }

        final Number[] avgRightPowerPhasePeak = session.getAvgRightPowerPhasePeak();
        if (avgRightPowerPhasePeak != null && avgRightPowerPhasePeak.length == 4) {
            final Number startAngle = avgRightPowerPhasePeak[0];
            final Number endAngle = avgRightPowerPhasePeak[1];
            if (startAngle != null && endAngle != null) {
                summaryData.add(
                        AVG_RIGHT_POWER_PHASE_PEAK,
                        context.getString(
                                R.string.range_degrees,
                                Math.round(startAngle.floatValue() / 0.7111111),
                                Math.round(endAngle.floatValue() / 0.7111111)
                        )
                );
            }
        }

        final Number[] avgPowerPosition = session.getAvgPowerPosition();
        if (avgPowerPosition != null && avgPowerPosition.length == 2) {
            summaryData.add(AVG_POWER_SEATING, avgPowerPosition[0], UNIT_WATT);
            summaryData.add(AVG_POWER_STANDING, avgPowerPosition[1], UNIT_WATT);
        }

        final Number[] maxPowerPosition = session.getMaxPowerPosition();
        if (maxPowerPosition != null && maxPowerPosition.length == 2) {
            summaryData.add(MAX_POWER_SEATING, maxPowerPosition[0], UNIT_WATT);
            summaryData.add(MAX_POWER_STANDING, maxPowerPosition[1], UNIT_WATT);
        }

        final Number[] avgCadencePosition = session.getAvgCadencePosition();
        if (avgCadencePosition != null && avgCadencePosition.length == 2) {
            summaryData.add(AVG_CADENCE_SEATING, avgCadencePosition[0], UNIT_RPM);
            summaryData.add(AVG_CADENCE_STANDING, avgCadencePosition[1], UNIT_RPM);
        }

        final Number[] maxCadencePosition = session.getMaxCadencePosition();
        if (maxCadencePosition != null && maxCadencePosition.length == 2) {
            summaryData.add(MAX_CADENCE_SEATING, maxCadencePosition[0], UNIT_RPM);
            summaryData.add(MAX_CADENCE_STANDING, maxCadencePosition[1], UNIT_RPM);
        }

        summaryData.add(FRONT_GEAR_SHIFTS, session.getFrontShifts(), UNIT_NONE);
        summaryData.add(REAR_GEAR_SHIFTS, session.getRearShifts(), UNIT_NONE);

        final Integer balance = session.getLeftRightBalance();
        if (balance != null) {
            final float balancePercentage = (balance & 0x3fff) / 100f;
            final boolean isRight = (balance & 0x8000) != 0;
            final float balanceL;
            final float balanceR;
            if (isRight) {
                balanceL = 100f - balancePercentage;
                balanceR = balancePercentage;
            } else {
                balanceL = balancePercentage;
                balanceR = 100f - balancePercentage;
            }
            summaryData.add(
                    LEFT_RIGHT_BALANCE,
                    context.getString(
                            R.string.range_percentage_float,
                            balanceL,
                            balanceR
                    )
            );
        }

        final Float avgLeftPedalSmoothness = session.getAvgLeftPedalSmoothness();
        final Float avgRightPedalSmoothness = session.getAvgRightPedalSmoothness();
        if (avgLeftPedalSmoothness != null && avgRightPedalSmoothness != null) {
            summaryData.add(
                    AVG_PEDAL_SMOOTHNESS,
                    context.getString(R.string.range_percentage, Math.round(avgLeftPedalSmoothness), Math.round(avgRightPedalSmoothness))
            );
        }

        final Float avgLeftTorqueEffectiveness = session.getAvgLeftTorqueEffectiveness();
        final Float avgRightTorqueEffectiveness = session.getAvgRightTorqueEffectiveness();
        if (avgLeftTorqueEffectiveness != null && avgRightTorqueEffectiveness != null) {
            summaryData.add(
                    AVG_TORQUE_EFFECTIVENESS,
                    context.getString(R.string.range_percentage, Math.round(avgLeftTorqueEffectiveness), Math.round(avgRightTorqueEffectiveness))
            );
        }

        for (final FitTimeInZone fitTimeInZone : timesInZone) {
            // Find the first time in zone for the session (assumes single-session)
            if (fitTimeInZone.getReferenceMessage() != null && fitTimeInZone.getReferenceMessage() == 18) {
                final Double[] timeInZones = fitTimeInZone.getTimeInZone();
                if (timeInZones == null) {
                    continue;
                }
                final double totalTime = Arrays.stream(timeInZones).mapToDouble(Number::doubleValue).sum();
                if (totalTime == 0) {
                    continue;
                }
                if (timeInZones[0] != null && timeInZones[0] == totalTime) {
                    // The total time is N/A, so do not add the section
                    continue;
                }
                final List<String> zoneOrder = Arrays.asList(HR_ZONE_NA, HR_ZONE_WARM_UP, HR_ZONE_EASY, HR_ZONE_AEROBIC, HR_ZONE_THRESHOLD, HR_ZONE_MAXIMUM);
                final int[] zoneColors = new int[]{
                        0,
                        context.getResources().getColor(R.color.hr_zone_warm_up_color),
                        context.getResources().getColor(R.color.hr_zone_easy_color),
                        context.getResources().getColor(R.color.hr_zone_aerobic_color),
                        context.getResources().getColor(R.color.hr_zone_threshold_color),
                        context.getResources().getColor(R.color.hr_zone_maximum_color),
                };
                for (int i = 0; i < zoneOrder.size(); i++) {
                    double timeInZone = timeInZones[i] != null ? Math.rint(timeInZones[i]) : 0;
                    summaryData.add(
                            zoneOrder.get(i),
                            new ActivitySummaryProgressEntry(
                                    timeInZone,
                                    UNIT_SECONDS,
                                    (int) (100 * timeInZone / totalTime),
                                    zoneColors[i]
                            )
                    );
                }
                break;
            }
        }

        if (physiologicalMetrics != null) {
            if (physiologicalMetrics.getAerobicEffect() != null) {
                summaryData.add(TRAINING_EFFECT_AEROBIC, physiologicalMetrics.getAerobicEffect(), UNIT_NONE, true);
            }
            if (physiologicalMetrics.getAnaerobicEffect() != null) {
                summaryData.add(TRAINING_EFFECT_ANAEROBIC, physiologicalMetrics.getAnaerobicEffect(), UNIT_NONE, true);
            }
            if (physiologicalMetrics.getMetMax() != null) {
                summaryData.add(MAXIMUM_OXYGEN_UPTAKE, physiologicalMetrics.getMetMax().floatValue() * 3.5f, UNIT_ML_KG_MIN);
            }
            if (physiologicalMetrics.getRecoveryTime() != null) {
                summaryData.add(RECOVERY_TIME, physiologicalMetrics.getRecoveryTime() * 60, UNIT_SECONDS);
            }
            if (physiologicalMetrics.getLactateThresholdHeartRate() != null) {
                summaryData.add(LACTATE_THRESHOLD_HR, physiologicalMetrics.getLactateThresholdHeartRate(), UNIT_BPM);
            }
        }
        summaryData.add(TRAINING_LOAD, safeRound(session.getTrainingLoadPeak()), UNIT_NONE);

        if (diveSummary != null) {
            if (diveSummary.getAvgDepth() != null) {
                summaryData.add(AVG_DEPTH, diveSummary.getAvgDepth(), UNIT_METERS);
            }
            if (diveSummary.getMaxDepth() != null) {
                summaryData.add(MAX_DEPTH, diveSummary.getMaxDepth(), UNIT_METERS);
            }
            if (diveSummary.getStartCns() != null) {
                summaryData.add(START_CNS, diveSummary.getStartCns(), UNIT_PERCENTAGE);
            }
            if (diveSummary.getEndCns() != null) {
                summaryData.add(END_CNS, diveSummary.getEndCns(), UNIT_PERCENTAGE);
            }
            if (diveSummary.getStartN2() != null) {
                summaryData.add(START_N2, diveSummary.getStartN2(), UNIT_PERCENTAGE);
            }
            if (diveSummary.getEndN2() != null) {
                summaryData.add(END_N2, diveSummary.getEndN2(), UNIT_PERCENTAGE);
            }
            if (diveSummary.getDiveNumber() != null) {
                summaryData.add(DIVE_NUMBER, diveSummary.getDiveNumber(), UNIT_NONE);
            }
            if (diveSummary.getBottomTime() != null) {
                summaryData.add(BOTTOM_TIME, diveSummary.getBottomTime(), UNIT_SECONDS);
            }
        }

        summaryData.add(TRAINING_LOAD, safeRound(session.getTrainingLoadPeak()), UNIT_NONE);
        summaryData.add(INTENSITY_FACTOR, session.getIntensityFactor(), UNIT_NONE);
        summaryData.add(TRAINING_STRESS_SCORE, session.getTrainingStressScore(), UNIT_NONE);

        if (!diveGases.isEmpty()) {
            final ActivitySummaryTableBuilder tableBuilder = new ActivitySummaryTableBuilder(GAS, "gases_header", Arrays.asList(
                    "diving_gas",
                    "helium_content",
                    "oxygen_content",
                    "nitrogen_content"
            ));

            int i = 1;
            for (final FitDiveGas gas : diveGases) {
                int helium = gas.getHeliumContent() != null ? gas.getHeliumContent() : 0;
                int oxygen = gas.getOxygenContent() != null ? gas.getOxygenContent() : 0;
                int nitrogen = 100 - helium - oxygen;
                tableBuilder.addRow(
                        "gas_" + i,
                        Arrays.asList(
                                new ActivitySummaryValue(i, UNIT_NONE),
                                new ActivitySummaryValue(helium, UNIT_PERCENTAGE),
                                new ActivitySummaryValue(oxygen, UNIT_PERCENTAGE),
                                new ActivitySummaryValue(nitrogen, UNIT_PERCENTAGE)
                        )
                );
                i++;
            }

            tableBuilder.addToSummaryData(summaryData);
        }

        if (!sets.isEmpty()) {
            final ActivitySummaryTableBuilder tableBuilder = new ActivitySummaryTableBuilder(SETS, "sets_header", Arrays.asList(
                    "set",
                    "category",
                    "workout_set_reps",
                    "menuitem_weight",
                    "activity_detail_duration_label"
            ));

            int i = 1;
            for (final FitSet set : sets) {
                if (set.getSetType() != null && set.getDuration() != null && set.getSetType() == 1) {
                    ExerciseCategory category = null;
                    if (set.getCategory() != null && set.getCategory().length > 0) {
                        category = set.getCategory()[0];
                    }

                    tableBuilder.addRow(
                            "set_" + i,
                            Arrays.asList(
                                    new ActivitySummaryValue(i, UNIT_NONE),
                                    new ActivitySummaryValue(category != null ? context.getString(category.getNameResId()) : null, UNIT_NONE),
                                    new ActivitySummaryValue(set.getRepetitions() != null ? String.valueOf(set.getRepetitions()) : null),
                                    new ActivitySummaryValue(set.getWeight(), weightUnit),
                                    new ActivitySummaryValue(set.getDuration().longValue(), UNIT_SECONDS)
                            )
                    );

                    i++;
                }
            }

            tableBuilder.addToSummaryData(summaryData);
        }

        // FIXME: For now we only support swimming intervals
        final boolean anyValidLaps = laps.stream()
                .anyMatch(lap -> lap.getTotalDistance() != null && lap.getTotalDistance() != 0 && lap.getSwimStyle() != null);

        if (anyValidLaps) {
            final ActivitySummaryTableBuilder tableBuilder = new ActivitySummaryTableBuilder(GROUP_INTERVALS, "intervals_header", Arrays.asList(
                    "#",
                    "swimming_stroke",
                    "Distance",
                    "pref_header_time"
            ));

            int i = 1;
            for (final FitLap lap : laps) {
                if (lap.getTotalDistance() == null || lap.getTotalDistance() == 0) {
                    continue;
                }

                tableBuilder.addRow(
                        "interval_" + i,
                        Arrays.asList(
                                new ActivitySummaryValue(i, UNIT_NONE),
                                new ActivitySummaryValue(lap.getSwimStyle() != null ? context.getString(lap.getSwimStyle().getNameResId()) : null, UNIT_NONE),
                                new ActivitySummaryValue(lap.getTotalDistance(), UNIT_METERS),
                                new ActivitySummaryValue(lap.getTotalTimerTime(), UNIT_SECONDS)
                        )
                );

                i++;
            }

            tableBuilder.addToSummaryData(summaryData);
        }

        summaryData.add(
                INTERNAL_HAS_GPS,
                String.valueOf(activityPoints.stream().anyMatch(p -> p.getLocation() != null))
        );

        summary.setSummaryData(summaryData.toString());

        return summaryData;
    }

    public Number safeRound(final Number number) {
        if (number == null) {
            return null;
        }

        return Math.round(number.doubleValue());
    }

    private static ActivityKind getActivityKind(Integer sport, Integer subsport) {
        // Garmin inReach Mini 2: sport and subsport are missing (null)
        if (sport == null) {
            sport = 0;
        }
        if (subsport == null) {
            subsport = 0;
        }

        final Optional<GarminSport> garminSport = GarminSport.fromCodes(sport, subsport);
        if (garminSport.isPresent()) {
            return garminSport.get().getActivityKind();
        } else {
            LOG.warn("Unknown garmin sport {}/{}", sport, subsport);

            final Optional<GarminSport> optGarminSportFallback = GarminSport.fromCodes(sport, 0);
            if (!optGarminSportFallback.isEmpty()) {
                return optGarminSportFallback.get().getActivityKind();
            }
        }

        return ActivityKind.UNKNOWN;
    }
}
