package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.AbstractFetchOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchActivityOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchDebugLogsOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchHeartRateManualOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchHeartRateMaxOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchHeartRateRestingOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchHrvOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchPaiOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchSleepRespiratoryRateOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchSleepSessionOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchSpo2NormalOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchSportsSummaryOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchStatisticsOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchStressAutoOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchStressManualOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch.FetchTemperatureOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class HuamiFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(HuamiFetcher.class);

    private final HuamiFetchSupport mSupport;

    private final LinkedList<AbstractFetchOperation> fetchOperationQueue = new LinkedList<>();
    private AbstractFetchOperation currentOperation;

    public HuamiFetcher(final HuamiFetchSupport mSupport) {
        this.mSupport = mSupport;
    }

    public void reset() {
        fetchOperationQueue.clear();
        currentOperation = null;
    }

    public void onFetchRecordedData(final int dataTypes) {
        final GBDevice gbDevice = mSupport.getDevice();
        final DeviceCoordinator coordinator = mSupport.getCoordinator();

        if ((dataTypes & RecordedDataTypes.TYPE_ACTIVITY) != 0) {
            this.fetchOperationQueue.add(new FetchActivityOperation(this));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_GPS_TRACKS) != 0 && coordinator.supportsActivityTracks(gbDevice)) {
            this.fetchOperationQueue.add(new FetchSportsSummaryOperation(this, 1));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_DEBUGLOGS) != 0 && coordinator.supportsDebugLogs(gbDevice)) {
            this.fetchOperationQueue.add(new FetchDebugLogsOperation(this));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_STRESS) != 0 && coordinator.supportsStressMeasurement(gbDevice)) {
            this.fetchOperationQueue.add(new FetchStressAutoOperation(this));
            this.fetchOperationQueue.add(new FetchStressManualOperation(this));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_PAI) != 0 && coordinator.supportsPai(gbDevice)) {
            this.fetchOperationQueue.add(new FetchPaiOperation(this));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_SPO2) != 0 && coordinator.supportsSpo2(gbDevice)) {
            this.fetchOperationQueue.add(new FetchSpo2NormalOperation(this));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_HRV) != 0 && coordinator.supportsHrvMeasurement(gbDevice)) {
            this.fetchOperationQueue.add(new FetchHrvOperation(this));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_HEART_RATE) != 0 && coordinator.supportsHeartRateStats(gbDevice)) {
            this.fetchOperationQueue.add(new FetchHeartRateManualOperation(this));
            this.fetchOperationQueue.add(new FetchHeartRateMaxOperation(this));
            this.fetchOperationQueue.add(new FetchHeartRateRestingOperation(this));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_SLEEP_RESPIRATORY_RATE) != 0 && coordinator.supportsSleepRespiratoryRate(gbDevice)) {
            this.fetchOperationQueue.add(new FetchSleepRespiratoryRateOperation(this));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_TEMPERATURE) != 0 && coordinator.supportsTemperatureMeasurement(gbDevice)) {
            this.fetchOperationQueue.add(new FetchTemperatureOperation(this));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_SLEEP) != 0 && coordinator.supportsSleepScore(gbDevice)) {
            this.fetchOperationQueue.add(new FetchSleepSessionOperation(this));
        }

        if ((dataTypes & RecordedDataTypes.TYPE_HUAMI_STATISTICS) != 0) {
            this.fetchOperationQueue.add(new FetchStatisticsOperation(this));
        }

        if (currentOperation == null) {
            triggerNextOperation();
        }
    }

    public void onActivityControl(final byte[] value) {
        if (currentOperation != null) {
            currentOperation.handleActivityMetadata(value);
        } else {
            LOG.warn("Got activity control, but there is no ongoing operation: {}", GB.hexdump(value));
        }
    }

    public void onActivityData(final byte[] value) {
        if (currentOperation != null) {
            currentOperation.handleActivityData(value);
        } else {
            LOG.warn("Got activity data, but there is no ongoing operation: {}", GB.hexdump(value));
        }
    }

    public GBDevice getDevice() {
        return mSupport.getDevice();
    }

    public Context getContext() {
        return mSupport.getContext();
    }

    public int getActivitySampleSize() {
        return mSupport.getActivitySampleSize();
    }

    public byte[] getTimeBytes(final Calendar calendar, final TimeUnit precision) {
        return mSupport.getTimeBytes(calendar, precision);
    }

    public void setNotifications(final boolean control, final boolean data) {
        mSupport.setActivityNotifications(control, data);
    }

    public TimeUnit getFetchOperationsTimeUnit() {
        // This is configurable because using seconds was causing issues on Amazfit GTR 3
        // However, using minutes can cause issues while fetching workouts shorter than 1 minute
        final Prefs devicePrefs = GBApplication.getDevicePrefs(getDevice());
        final boolean truncate = devicePrefs.getBoolean("huami_truncate_fetch_operation_timestamps", true);
        return truncate ? TimeUnit.MINUTES : TimeUnit.SECONDS;
    }

    public void writeControl(final String name, final byte[] value) {
        mSupport.writeActivityControl(name, value);
    }

    public boolean isZeppOs() {
        return mSupport instanceof ZeppOsSupport;
    }

    public void triggerNextOperation() {
        final boolean wasFetching = currentOperation != null;
        currentOperation = this.fetchOperationQueue.poll();

        if (currentOperation != null) {
            LOG.debug("Performing next operation {}", currentOperation.getName());

            getDevice().setBusyTask(currentOperation.taskDescription(), getContext()); // mark as busy quickly to avoid interruptions from the outside
            getDevice().sendDeviceUpdateIntent(getContext());

            if (!wasFetching) {
                setNotifications(true, false);
            }

            currentOperation.doPerform();

            return;
        }

        if (wasFetching) {
            LOG.debug("All operations finished");

            GB.updateTransferNotification(null, "", false, 100, getContext());
            GB.signalActivityDataFinish(getDevice());

            setNotifications(false, false);

            if (getDevice().isBusy()) {
                getDevice().unsetBusyTask();
                getDevice().sendDeviceUpdateIntent(getContext());
            }
        }
    }

    public AbstractFetchOperation getNextFetchOperation() {
        return fetchOperationQueue.poll();
    }

    public LinkedList<AbstractFetchOperation> getFetchOperationQueue() {
        return fetchOperationQueue;
    }

    public interface HuamiFetchSupport {
        DeviceCoordinator getCoordinator();
        GBDevice getDevice();
        Context getContext();
        int getActivitySampleSize();
        byte[] getTimeBytes(final Calendar calendar, final TimeUnit precision);
        void setActivityNotifications(boolean control, boolean data);
        void writeActivityControl(String name, byte[] value);
    }
}
