/*  Copyright (C) 2025 Freeyourgadget

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 */
public class FitRecordDataFactory {
    private FitRecordDataFactory() {
        // use create
    }

    public static RecordData create(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        return switch (recordDefinition.getGlobalFITMessage().getNumber()) {
            case 0 -> new FitFileId(recordDefinition, recordHeader);
            case 1 -> new FitCapabilities(recordDefinition, recordHeader);
            case 2 -> new FitDeviceSettings(recordDefinition, recordHeader);
            case 3 -> new FitUserProfile(recordDefinition, recordHeader);
            case 4 -> new FitHrmProfile(recordDefinition, recordHeader);
            case 5 -> new FitSdmProfile(recordDefinition, recordHeader);
            case 6 -> new FitBikeProfile(recordDefinition, recordHeader);
            case 7 -> new FitZonesTarget(recordDefinition, recordHeader);
            case 8 -> new FitHrZone(recordDefinition, recordHeader);
            case 9 -> new FitPowerZone(recordDefinition, recordHeader);
            case 10 -> new FitMetZone(recordDefinition, recordHeader);
            case 12 -> new FitSport(recordDefinition, recordHeader);
            case 13 -> new FitTrainingSettings(recordDefinition, recordHeader);
            case 15 -> new FitGoals(recordDefinition, recordHeader);
            case 18 -> new FitSession(recordDefinition, recordHeader);
            case 19 -> new FitLap(recordDefinition, recordHeader);
            case 20 -> new FitRecord(recordDefinition, recordHeader);
            case 21 -> new FitEvent(recordDefinition, recordHeader);
            case 22 -> new FitDeviceUsed(recordDefinition, recordHeader);
            case 23 -> new FitDeviceInfo(recordDefinition, recordHeader);
            case 26 -> new FitWorkout(recordDefinition, recordHeader);
            case 27 -> new FitWorkoutStep(recordDefinition, recordHeader);
            case 28 -> new FitSchedule(recordDefinition, recordHeader);
            case 29 -> new FitLocation(recordDefinition, recordHeader);
            case 30 -> new FitWeightScale(recordDefinition, recordHeader);
            case 31 -> new FitCourse(recordDefinition, recordHeader);
            case 32 -> new FitCoursePoint(recordDefinition, recordHeader);
            case 33 -> new FitTotals(recordDefinition, recordHeader);
            case 34 -> new FitActivity(recordDefinition, recordHeader);
            case 35 -> new FitSoftware(recordDefinition, recordHeader);
            case 37 -> new FitFileCapabilities(recordDefinition, recordHeader);
            case 38 -> new FitMesgCapabilities(recordDefinition, recordHeader);
            case 39 -> new FitFieldCapabilities(recordDefinition, recordHeader);
            case 49 -> new FitFileCreator(recordDefinition, recordHeader);
            case 51 -> new FitBloodPressure(recordDefinition, recordHeader);
            case 53 -> new FitSpeedZone(recordDefinition, recordHeader);
            case 55 -> new FitMonitoring(recordDefinition, recordHeader);
            case 70 -> new FitMapLayer(recordDefinition, recordHeader);
            case 72 -> new FitTrainingFile(recordDefinition, recordHeader);
            case 78 -> new FitHrv(recordDefinition, recordHeader);
            case 79 -> new FitUserMetrics(recordDefinition, recordHeader);
            case 80 -> new FitAntRx(recordDefinition, recordHeader);
            case 81 -> new FitAntTx(recordDefinition, recordHeader);
            case 82 -> new FitAntChannelId(recordDefinition, recordHeader);
            case 101 -> new FitLength(recordDefinition, recordHeader);
            case 103 -> new FitMonitoringInfo(recordDefinition, recordHeader);
            case 104 -> new FitDeviceStatus(recordDefinition, recordHeader);
            case 105 -> new FitPad(recordDefinition, recordHeader);
            case 106 -> new FitSlaveDevice(recordDefinition, recordHeader);
            case 127 -> new FitConnectivity(recordDefinition, recordHeader);
            case 128 -> new FitWeather(recordDefinition, recordHeader);
            case 129 -> new FitWeatherAlert(recordDefinition, recordHeader);
            case 131 -> new FitCadenceZone(recordDefinition, recordHeader);
            case 132 -> new FitHr(recordDefinition, recordHeader);
            case 140 -> new FitPhysiologicalMetrics(recordDefinition, recordHeader);
            case 141 -> new FitEpoStatus(recordDefinition, recordHeader);
            case 142 -> new FitSegmentLap(recordDefinition, recordHeader);
            case 145 -> new FitMemoGlob(recordDefinition, recordHeader);
            case 147 -> new FitSensorSettings(recordDefinition, recordHeader);
            case 148 -> new FitSegmentId(recordDefinition, recordHeader);
            case 149 -> new FitSegmentLeaderboardEntry(recordDefinition, recordHeader);
            case 150 -> new FitSegmentPoint(recordDefinition, recordHeader);
            case 151 -> new FitSegmentFile(recordDefinition, recordHeader);
            case 158 -> new FitWorkoutSession(recordDefinition, recordHeader);
            case 159 -> new FitWatchfaceSettings(recordDefinition, recordHeader);
            case 160 -> new FitGpsMetadata(recordDefinition, recordHeader);
            case 161 -> new FitCameraEvent(recordDefinition, recordHeader);
            case 162 -> new FitTimestampCorrelation(recordDefinition, recordHeader);
            case 164 -> new FitGyroscopeData(recordDefinition, recordHeader);
            case 165 -> new FitAccelerometerData(recordDefinition, recordHeader);
            case 167 -> new FitThreeDSensorCalibration(recordDefinition, recordHeader);
            case 169 -> new FitVideoFrame(recordDefinition, recordHeader);
            case 174 -> new FitObdiiData(recordDefinition, recordHeader);
            case 177 -> new FitNmeaSentence(recordDefinition, recordHeader);
            case 178 -> new FitAviationAttitude(recordDefinition, recordHeader);
            case 184 -> new FitVideo(recordDefinition, recordHeader);
            case 185 -> new FitVideoTitle(recordDefinition, recordHeader);
            case 186 -> new FitVideoDescription(recordDefinition, recordHeader);
            case 187 -> new FitVideoClip(recordDefinition, recordHeader);
            case 188 -> new FitOhrSettings(recordDefinition, recordHeader);
            case 200 -> new FitExdScreenConfiguration(recordDefinition, recordHeader);
            case 201 -> new FitExdDataFieldConfiguration(recordDefinition, recordHeader);
            case 202 -> new FitExdDataConceptConfiguration(recordDefinition, recordHeader);
            case 206 -> new FitFieldDescription(recordDefinition, recordHeader);
            case 207 -> new FitDeveloperData(recordDefinition, recordHeader);
            case 208 -> new FitMagnetometerData(recordDefinition, recordHeader);
            case 209 -> new FitBarometerData(recordDefinition, recordHeader);
            case 210 -> new FitOneDSensorCalibration(recordDefinition, recordHeader);
            case 211 -> new FitMonitoringHrData(recordDefinition, recordHeader);
            case 216 -> new FitTimeInZone(recordDefinition, recordHeader);
            case 222 -> new FitAlarmSettings(recordDefinition, recordHeader);
            case 225 -> new FitSet(recordDefinition, recordHeader);
            case 227 -> new FitStressLevel(recordDefinition, recordHeader);
            case 229 -> new FitMaxMetData(recordDefinition, recordHeader);
            case 258 -> new FitDiveSettings(recordDefinition, recordHeader);
            case 259 -> new FitDiveGas(recordDefinition, recordHeader);
            case 262 -> new FitDiveAlarm(recordDefinition, recordHeader);
            case 264 -> new FitExerciseTitle(recordDefinition, recordHeader);
            case 268 -> new FitDiveSummary(recordDefinition, recordHeader);
            case 269 -> new FitSpo2(recordDefinition, recordHeader);
            case 273 -> new FitSleepDataInfo(recordDefinition, recordHeader);
            case 274 -> new FitSleepDataRaw(recordDefinition, recordHeader);
            case 275 -> new FitSleepStage(recordDefinition, recordHeader);
            case 285 -> new FitJump(recordDefinition, recordHeader);
            case 289 -> new FitAadAccelFeatures(recordDefinition, recordHeader);
            case 290 -> new FitBeatIntervals(recordDefinition, recordHeader);
            case 297 -> new FitRespirationRate(recordDefinition, recordHeader);
            case 302 -> new FitHsaAccelerometerData(recordDefinition, recordHeader);
            case 304 -> new FitHsaStepData(recordDefinition, recordHeader);
            case 305 -> new FitHsaSpo2Data(recordDefinition, recordHeader);
            case 306 -> new FitHsaStressData(recordDefinition, recordHeader);
            case 307 -> new FitHsaRespirationData(recordDefinition, recordHeader);
            case 308 -> new FitHsaHeartRateData(recordDefinition, recordHeader);
            case 312 -> new FitSplit(recordDefinition, recordHeader);
            case 313 -> new FitSplitSummary(recordDefinition, recordHeader);
            case 314 -> new FitHsaBodyBatteryData(recordDefinition, recordHeader);
            case 315 -> new FitHsaEvent(recordDefinition, recordHeader);
            case 317 -> new FitClimbPro(recordDefinition, recordHeader);
            case 319 -> new FitTankUpdate(recordDefinition, recordHeader);
            case 323 -> new FitTankSummary(recordDefinition, recordHeader);
            case 326 -> new FitGpsEvent(recordDefinition, recordHeader);
            case 336 -> new FitEcgSummary(recordDefinition, recordHeader);
            case 337 -> new FitEcgRawSample(recordDefinition, recordHeader);
            case 338 -> new FitEcgSmoothSample(recordDefinition, recordHeader);
            case 346 -> new FitSleepStats(recordDefinition, recordHeader);
            case 370 -> new FitHrvSummary(recordDefinition, recordHeader);
            case 371 -> new FitHrvValue(recordDefinition, recordHeader);
            case 372 -> new FitRawBbi(recordDefinition, recordHeader);
            case 375 -> new FitDeviceAuxBatteryInfo(recordDefinition, recordHeader);
            case 376 -> new FitHsaGyroscopeData(recordDefinition, recordHeader);
            case 378 -> new FitTrainingLoad(recordDefinition, recordHeader);
            case 379 -> new FitSleepSchedule(recordDefinition, recordHeader);
            case 387 -> new FitChronoShotSession(recordDefinition, recordHeader);
            case 388 -> new FitChronoShotData(recordDefinition, recordHeader);
            case 389 -> new FitHsaConfigurationData(recordDefinition, recordHeader);
            case 393 -> new FitDiveApneaAlarm(recordDefinition, recordHeader);
            case 394 -> new FitCpeStatus(recordDefinition, recordHeader);
            case 397 -> new FitSkinTempRaw(recordDefinition, recordHeader);
            case 398 -> new FitSkinTempOvernight(recordDefinition, recordHeader);
            case 409 -> new FitHsaWristTemperatureData(recordDefinition, recordHeader);
            case 412 -> new FitNap(recordDefinition, recordHeader);
             default -> new RecordData(recordDefinition, recordHeader);
        };
    }
}
