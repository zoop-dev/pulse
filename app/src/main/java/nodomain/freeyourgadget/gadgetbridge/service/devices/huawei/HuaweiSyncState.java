package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class HuaweiSyncState {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiSyncState.class);

    private final HuaweiSupportProvider supportProvider;
    private final List<Integer> syncQueue = new ArrayList<>(2);

    private boolean activitySync = false;
    private boolean p2pSync = false;
    private boolean stressSync = false;
    private boolean workoutSync = false;
    private int workoutGpsDownload = 0;

    public HuaweiSyncState(HuaweiSupportProvider supportProvider) {
        this.supportProvider = supportProvider;
    }

    private boolean isSyncActive() {
        return activitySync || p2pSync || stressSync || workoutSync || workoutGpsDownload != 0;
    }

    private String activeSync() {
        StringBuilder retv = new StringBuilder();
        if (activitySync)
            retv.append("activitySync,");
        if (p2pSync)
            retv.append("p2pSync,");
        if (stressSync)
            retv.append("stressSync,");
        if (workoutSync)
            retv.append("workoutSync,");
        if (workoutGpsDownload != 0) {
            retv.append("workoutGpsDownload:");
            retv.append(workoutGpsDownload);
            retv.append(",");
        }
        return retv.toString();
    }

    public void addActivitySyncToQueue() {
        LOG.debug("Add activity type to sync queue");
        if (syncQueue.contains(RecordedDataTypes.TYPE_ACTIVITY))
            LOG.info("Activity type sync already queued, ignoring");
        else
            syncQueue.add(RecordedDataTypes.TYPE_ACTIVITY);
    }

    public void addWorkoutSyncToQueue() {
        LOG.debug("Add workout type to sync queue");
        if (syncQueue.contains(RecordedDataTypes.TYPE_GPS_TRACKS))
            LOG.info("Workout type sync already queued, ignoring");
        else
            syncQueue.add(RecordedDataTypes.TYPE_GPS_TRACKS);
    }

    public int getCurrentSyncType() {
        if (syncQueue.isEmpty())
            return -1;
        return syncQueue.get(0);
    }

    public boolean startActivitySync() {
        synchronized(this) {
            if (isSyncActive()) {
                LOG.warn("Attempted to start activity sync while another sync is still active: {}", activeSync());
                return false;
            }
            this.activitySync = true;
        }
        LOG.debug("Set activity sync state to true");
        return true;
    }

    public void stopActivitySync() {
        LOG.debug("Set activity sync state to false");
        this.activitySync = false;
        if (!p2pSync && !this.stressSync) {
            this.syncQueue.remove((Integer) RecordedDataTypes.TYPE_ACTIVITY);
            supportProvider.fetchRecodedDataFromQueue();
        }
        updateState();
    }

    public void setP2pSync(boolean state) {
        // We cannot do the syncActive check for the P2P sync as it runs in parallel with the activity sync
        LOG.debug("Set p2p sync state to {}", state);
        this.p2pSync = state;
        if (!state && !this.activitySync && !this.stressSync) {
            this.syncQueue.remove((Integer) RecordedDataTypes.TYPE_ACTIVITY);
            supportProvider.fetchRecodedDataFromQueue();
        }
        updateState();
    }

    public void setStressSync(boolean state) {
        // We cannot do the syncActive check for the stress sync as it runs in parallel with the activity sync (sleep file specifically)
        LOG.debug("Set stress sync state to {}", state);
        this.stressSync = state;
        if (!state && !this.activitySync && !this.p2pSync) {
            this.syncQueue.remove((Integer) RecordedDataTypes.TYPE_ACTIVITY);
            supportProvider.fetchRecodedDataFromQueue();
        }
        updateState();
    }

    public boolean startWorkoutSync() {
        synchronized (this) {
            if (isSyncActive()) {
                LOG.warn("Attempted to start workout sync while another sync is still active: {}", activeSync());
                return false;
            }
            this.workoutSync = true;
        }
        LOG.debug("Set workout sync state to true");
        return true;
    }

    public void stopWorkoutSync() {
        LOG.debug("Set workout sync state to false");
        this.workoutSync = false;
        if (workoutGpsDownload == 0) {
            this.syncQueue.remove((Integer) RecordedDataTypes.TYPE_GPS_TRACKS);
            supportProvider.fetchRecodedDataFromQueue();
        }
        updateState();
    }

    public void startWorkoutGpsDownload() {
        this.workoutGpsDownload += 1;
        LOG.debug("Add GPS download: {}", this.workoutGpsDownload);
    }

    public void stopWorkoutGpsDownload() {
        this.workoutGpsDownload -= 1;
        LOG.debug("Subtract GPS download: {}", this.workoutGpsDownload);
        if (this.workoutGpsDownload == 0 && !this.workoutSync) {
            this.syncQueue.remove((Integer) RecordedDataTypes.TYPE_GPS_TRACKS);
            supportProvider.fetchRecodedDataFromQueue();
        }
        updateState();
    }

    public void updateState() {
        updateState(true);
    }

    public void updateState(boolean needSync) {
        if (!isSyncActive()) {
            if (supportProvider.getDevice().isBusy()) {
                supportProvider.getDevice().unsetBusyTask();
                supportProvider.getDevice().sendDeviceUpdateIntent(supportProvider.getContext());
            }
            if (needSync)
                GB.signalActivityDataFinish(supportProvider.getDevice());
        }
    }
}
