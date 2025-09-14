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
            case 2 -> new FitDeviceSettings(recordDefinition, recordHeader);
            case 3 -> new FitUserProfile(recordDefinition, recordHeader);
            case 7 -> new FitZonesTarget(recordDefinition, recordHeader);
            case 12 -> new FitSport(recordDefinition, recordHeader);
            case 15 -> new FitGoals(recordDefinition, recordHeader);
            case 18 -> new FitSession(recordDefinition, recordHeader);
            case 19 -> new FitLap(recordDefinition, recordHeader);
            case 20 -> new FitRecord(recordDefinition, recordHeader);
            case 21 -> new FitEvent(recordDefinition, recordHeader);
            case 23 -> new FitDeviceInfo(recordDefinition, recordHeader);
            case 26 -> new FitWorkout(recordDefinition, recordHeader);
            case 31 -> new FitCourse(recordDefinition, recordHeader);
            case 34 -> new FitActivity(recordDefinition, recordHeader);
            case 49 -> new FitFileCreator(recordDefinition, recordHeader);
            case 55 -> new FitMonitoring(recordDefinition, recordHeader);
            case 103 -> new FitMonitoringInfo(recordDefinition, recordHeader);
            case 127 -> new FitConnectivity(recordDefinition, recordHeader);
            case 128 -> new FitWeather(recordDefinition, recordHeader);
            case 140 -> new FitPhysiologicalMetrics(recordDefinition, recordHeader);
            case 159 -> new FitWatchfaceSettings(recordDefinition, recordHeader);
            case 160 -> new FitGpsMetadata(recordDefinition, recordHeader);
            case 162 -> new FitTimestampCorrelation(recordDefinition, recordHeader);
            case 206 -> new FitFieldDescription(recordDefinition, recordHeader);
            case 207 -> new FitDeveloperData(recordDefinition, recordHeader);
            case 211 -> new FitMonitoringHrData(recordDefinition, recordHeader);
            case 216 -> new FitTimeInZone(recordDefinition, recordHeader);
            case 222 -> new FitAlarmSettings(recordDefinition, recordHeader);
            case 225 -> new FitSet(recordDefinition, recordHeader);
            case 227 -> new FitStressLevel(recordDefinition, recordHeader);
            case 229 -> new FitMaxMetData(recordDefinition, recordHeader);
            case 259 -> new FitDiveGas(recordDefinition, recordHeader);
            case 268 -> new FitDiveSummary(recordDefinition, recordHeader);
            case 269 -> new FitSpo2(recordDefinition, recordHeader);
            case 273 -> new FitSleepDataInfo(recordDefinition, recordHeader);
            case 274 -> new FitSleepDataRaw(recordDefinition, recordHeader);
            case 275 -> new FitSleepStage(recordDefinition, recordHeader);
            case 297 -> new FitRespirationRate(recordDefinition, recordHeader);
            case 336 -> new FitEcgSummary(recordDefinition, recordHeader);
            case 337 -> new FitEcgRawSample(recordDefinition, recordHeader);
            case 338 -> new FitEcgSmoothSample(recordDefinition, recordHeader);
            case 346 -> new FitSleepStats(recordDefinition, recordHeader);
            case 370 -> new FitHrvSummary(recordDefinition, recordHeader);
            case 371 -> new FitHrvValue(recordDefinition, recordHeader);
            case 378 -> new FitTrainingLoad(recordDefinition, recordHeader);
            case 397 -> new FitSkinTempRaw(recordDefinition, recordHeader);
            case 398 -> new FitSkinTempOvernight(recordDefinition, recordHeader);
            case 412 -> new FitNap(recordDefinition, recordHeader);
             default -> new RecordData(recordDefinition, recordHeader);
        };
    }
}
