package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport.fit;

import android.content.Context;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericTrainingLoadAcuteSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericTrainingLoadChronicSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportWorkoutParser;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractTimeSample;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTrainingLoadAcuteSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTrainingLoadChronicSample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.exception.FitParseException;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitMonitoring;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitPhysiologicalMetrics;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSession;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitTimeInZone;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitTrainingLoad;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitUserProfile;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FitImporter {
    private static final Logger LOG = LoggerFactory.getLogger(FitImporter.class);

    private final Context context;
    private final GBDevice gbDevice;

    private final SortedMap<Long, List<FitMonitoring>> activitySamplesPerTimestamp = new TreeMap<>();
    private final List<GenericTrainingLoadAcuteSample> trainingLoadAcuteSamples = new ArrayList<>();
    private final List<GenericTrainingLoadChronicSample> trainingLoadChronicSamples = new ArrayList<>();
    private final Map<Integer, Integer> unknownRecords = new HashMap<>();

    private FitFileId fileId = null;

    private final IGPSportWorkoutParser workoutParser;

    public FitImporter(final Context context, final GBDevice gbDevice) {
        this.context = context;
        this.gbDevice = gbDevice;
        this.workoutParser = new IGPSportWorkoutParser(context);
    }

    /**
     * @noinspection StatementWithEmptyBody
     */
    public void importFile(final File file) throws IOException, FitParseException {
        reset();

        final FitFile fitFile = FitFile.parseIncoming(file);

        Long lastMonitoringTimestamp = null;

        for (final RecordData record : fitFile.getRecords()) {
            if (fileId != null && fileId.getType() == FileType.FILETYPE.ACTIVITY) {
                if (workoutParser.handleRecord(record)) {
                    continue;
                }
            }

            final Long ts = record.getComputedTimestamp();

            if (record instanceof FitFileId) {
                final FitFileId newFileId = (FitFileId) record;
                LOG.debug("File ID: {}", newFileId);
                if (fileId != null) {
                    // Should not happen
                    LOG.warn("Already had a file ID: {}", fileId);
                }
                fileId = newFileId;

            } else if (record instanceof FitMonitoring) {
                LOG.trace("Monitoring at {}: {}", ts, record);
                final FitMonitoring monitoringRecord = (FitMonitoring) record;
                final Long currentMonitoringTimestamp = monitoringRecord.computeTimestamp(lastMonitoringTimestamp);
                if (!activitySamplesPerTimestamp.containsKey(currentMonitoringTimestamp)) {
                    activitySamplesPerTimestamp.put(currentMonitoringTimestamp, new ArrayList<>());
                }
                Objects.requireNonNull(activitySamplesPerTimestamp.get(currentMonitoringTimestamp)).add(monitoringRecord);
                lastMonitoringTimestamp = currentMonitoringTimestamp;

            } else if (record instanceof FitRecord) {
                // handled in workout parser
            } else if (record instanceof FitSession) {
                // handled in workout parser
            } else if (record instanceof FitPhysiologicalMetrics) {
                // handled in workout parser
            } else if (record instanceof FitSport) {
                // handled in workout parser
            } else if (record instanceof FitTimeInZone) {
                // handled in workout parser
            } else if (record instanceof FitUserProfile) {
                // handled in workout parser
            } else if (record instanceof FitTrainingLoad) {
                final FitTrainingLoad trainingLoad = (FitTrainingLoad) record;
                LOG.trace("Training load at {}: {}", ts, record);
                if (trainingLoad.getTrainingLoadAcute() != null) {
                    final GenericTrainingLoadAcuteSample sample = new GenericTrainingLoadAcuteSample();
                    sample.setTimestamp(ts * 1000L);
                    sample.setValue(trainingLoad.getTrainingLoadAcute());
                    trainingLoadAcuteSamples.add(sample);
                }
                if (trainingLoad.getTrainingLoadChronic() != null) {
                    final GenericTrainingLoadChronicSample sample = new GenericTrainingLoadChronicSample();
                    sample.setTimestamp(ts * 1000L);
                    sample.setValue(trainingLoad.getTrainingLoadChronic());
                    trainingLoadChronicSamples.add(sample);
                }
            } else {
                LOG.trace("Unknown record: {}", record);

                if (!unknownRecords.containsKey(record.getGlobalFITMessage().getNumber())) {
                    unknownRecords.put(record.getGlobalFITMessage().getNumber(), 0);
                }
                unknownRecords.put(
                        record.getGlobalFITMessage().getNumber(),
                        Objects.requireNonNull(unknownRecords.get(record.getGlobalFITMessage().getNumber())) + 1
                );
            }
        }

        if (fileId == null) {
            LOG.error("Got no file ID");
            return;
        }
        if (fileId.getType() == null) {
            LOG.error("File has no type");
            return;
        }

        // If the file is not yet on the export directory (eg. we're importing from phone storage), copy it
        File finalExportFile = file;
        try {
            final File exportDirectory = gbDevice.getDeviceCoordinator().getWritableExportDirectory(gbDevice);
            if (!file.getAbsolutePath().startsWith(exportDirectory.getAbsolutePath())) {
                final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
                final StringBuilder sb = new StringBuilder(fileId.getType().name());
                if (fileId.getTimeCreated() != null && fileId.getTimeCreated() != 0) {
                    sb.append("_").append(SDF.format(new Date(fileId.getTimeCreated() * 1000L)));
                }
                sb.append(".fit");

                final File exportFile = new File(exportDirectory, sb.toString());
                if (exportFile.isFile()) {
                    // Prevent overwrite
                    LOG.warn("Fit file {} already exists as {}", file, exportFile);
                } else {
                    LOG.debug("Copying {} to {}", file, exportFile);

                    FileUtils.copyFile(file, exportFile);
                    exportFile.setLastModified(file.lastModified());
                }

                finalExportFile = exportFile;
            }
        } catch (final Exception e) {
            LOG.error("Failed to copy file to export directory", e);
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            switch (fileId.getType()) {
                case ACTIVITY:
                    persistWorkout(finalExportFile, session);
                    break;
                case METRICS:
                    persistAbstractSamples(trainingLoadAcuteSamples, new GenericTrainingLoadAcuteSampleProvider(gbDevice, session));
                    persistAbstractSamples(trainingLoadChronicSamples, new GenericTrainingLoadChronicSampleProvider(gbDevice, session));
                    break;
                default:
                    LOG.warn("Unable to handle fit file of type {}", fileId.getType());
            }
        } catch (final Exception e) {
            GB.toast(context, "Error saving samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }

        for (final Map.Entry<Integer, Integer> e : unknownRecords.entrySet()) {
            LOG.warn("Unknown record of global number {} seen {} times", e.getKey(), e.getValue());
        }
    }

    private void persistWorkout(final File file, final DaoSession session) {
        LOG.debug("Persisting workout for {}", fileId);

        final BaseActivitySummary summary;

        // This ensures idempotency when re-processing
        try {
            summary = ActivitySummaryParser.findOrCreateBaseActivitySummary(
                    session,
                    gbDevice,
                    Objects.requireNonNull(fileId.getTimeCreated()).intValue()
            );
        } catch (final Exception e) {
            GB.toast(context, "Error finding base summary", Toast.LENGTH_LONG, GB.ERROR, e);
            return;
        }

        workoutParser.updateSummary(summary);

        summary.setRawDetailsPath(file.getAbsolutePath());

        try {
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            summary.setDevice(device);
            summary.setUser(user);

            session.getBaseActivitySummaryDao().insertOrReplace(summary);
        } catch (final Exception e) {
            GB.toast(context, "Error saving workout", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void reset() {
        activitySamplesPerTimestamp.clear();
        trainingLoadAcuteSamples.clear();
        trainingLoadChronicSamples.clear();
        unknownRecords.clear();
        fileId = null;
        workoutParser.reset();
    }



    private <T extends AbstractTimeSample> void persistAbstractSamples(final List<T> samples,
                                                                       final AbstractTimeSampleProvider<T> sampleProvider) {
        sampleProvider.persistForDevice(context, gbDevice, samples);
    }
}
