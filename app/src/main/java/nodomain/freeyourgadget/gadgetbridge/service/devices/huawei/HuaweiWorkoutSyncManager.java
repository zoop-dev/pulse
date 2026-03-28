/*  Copyright (C) 2026 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.query.DeleteQuery;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutPaceSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutPaceSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSectionsSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSectionsSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSpO2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSpO2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummaryAdditionalValuesSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummaryAdditionalValuesSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSwimSegmentsSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSwimSegmentsSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.RequestBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class HuaweiWorkoutSyncManager {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiWorkoutSyncManager.class);

    /*
     * TODO: We add the workout totals first, if at any point after that a sync fails, the workout
     *  will not be synchronised again automatically. Might want to fix that.
     */

    /*
     * General dataflow in this class:
     *  - Sync function sends a request to the device
     *  - Response gets into handle function, which calls the next sync function
     *
     * If we get an error or something we don't expect from the band, we generally try it three
     * times, and then move on to the next normal step for the synchronisation.
     * There's quite some exceptions that do not fall into this category, but they should be much
     * more rare.
     */

    private record WorkoutSyncRequest(int start, int end) { }

    public interface WorkoutSyncCallback {
        void syncComplete();
        void handleTimeout();
        void handleException();
    }

    /*
     * For all the packets that we know can return an error (from the watch, not the parsing),
     * we retry up to this many times. If they still error, we continue with the next step in the
     * sync process.
     */
    private static final int MAX_RETRIES = 3;

    /*
     * If you draw a tree from the smaller times we try, this is the max depth
     */
    private static final int MAX_SMALLER_TIMESLOT_DEPTH = 5;

    private final HuaweiSupportProvider supportProvider;
    private final ArrayList<WorkoutSyncRequest> syncRequests;
    private WorkoutSyncRequest currentSyncRequest;
    /// Used for the callback, but also to see if there's still a sync running
    private WorkoutSyncCallback callback = null;
    private int smallerTimeslotDepth;

    private List<Workout.WorkoutCount.Response.WorkoutNumbers> workoutNumbersList;
    private Workout.WorkoutCount.Response.WorkoutNumbers currentWorkoutNumbers;

    private Long currentDatabaseId;
    private int currentNumber; // Used for data, pace, etc...
    private int retryCounter;

    private boolean currentWorkoutMissingData;

    public HuaweiWorkoutSyncManager(HuaweiSupportProvider supportProvider) {
        this.supportProvider = supportProvider;
        this.syncRequests = new ArrayList<>();
    }

    public void performSync(WorkoutSyncCallback callback) {
        int start = getStartTimeFromDb();
        int end = (int) (System.currentTimeMillis() / 1000);
        WorkoutSyncRequest syncRequest = new WorkoutSyncRequest(start, end);

        synchronized (supportProvider) {
            if (this.callback != null) {
                callback.handleException();
                return;
            }
            this.callback = callback;
            syncRequests.add(syncRequest);
        }
        syncNextFromQueue();
    }

    private void resetState() {
        synchronized (this.supportProvider) {
            this.callback = null;

            if (!this.syncRequests.isEmpty())
                LOG.error("SyncRequests is not empty yet state is reset");
            this.syncRequests.clear();
            this.workoutNumbersList = null;
            this.currentDatabaseId = null;
            this.smallerTimeslotDepth = 0;
            this.currentWorkoutMissingData = false;
        }
    }

    private void handleException(@Nullable Exception e) {
        GB.toast("Exception synchronizing workout", Toast.LENGTH_SHORT, GB.ERROR, e);
        if (this.currentWorkoutMissingData)
            dataSkippedToast();
        this.callback.handleException();
        resetState();
    }

    private void handleTimeout() {
        GB.toast("Timeout when synchronizing workout", Toast.LENGTH_SHORT, GB.WARN);
        if (this.currentWorkoutMissingData)
            dataSkippedToast();
        this.callback.handleTimeout();
        resetState();
    }

    /**
     * Notify the user that data might be missing
     */
    private void dataSkippedToast() {
        GB.toast("The watch refuses to return some data, which may be missing from the workout", Toast.LENGTH_SHORT, GB.WARN);
    }

    private void syncNextFromQueue() {
        if (this.syncRequests.isEmpty()) {
            this.callback.syncComplete();
            resetState();
            return;
        }
        this.currentSyncRequest = this.syncRequests.remove(0);

        Request getWorkoutCountRequest = new RequestBuilder<Workout.WorkoutCount.Response>(
                    this.supportProvider,
                    Workout.id,
                    Workout.WorkoutCount.id,
                    new Workout.WorkoutCount.Request(
                            supportProvider.getParamsProvider(),
                            this.currentSyncRequest.start,
                            this.currentSyncRequest.end
                    )
            )
            .setTimeout(5000)                           // Set a 5 second timeout
            .onCallback(this::handleWorkoutCount)
            .onException(e -> {
                LOG.error("Exception receiving workout count", e);
                handleException(e);
            })
            .onTimeout(packet -> {
                LOG.error("Timeout on workout count");
                handleTimeout();
            })
            .build();
        try {
            getWorkoutCountRequest.doPerform();
        } catch (IOException e) {
            LOG.error("Exception sending workout count", e);
            handleException(e);
        }
    }

    private void handleWorkoutCount(Workout.WorkoutCount.Response packet) {
        if (packet.error != null) {
            LOG.error("Error when retrieving/parsing workout count: {}", packet.error);
            syncSmallerTimeslot();
            return;
        }
        if (packet.count == 0 || packet.workoutNumbers == null) {
            this.callback.syncComplete();
            resetState();
            return;
        }

        if (packet.count != packet.workoutNumbers.size()) {
            LOG.error("Packet count does not match workoutNumbers size. Attempting smaller timeslot.");
            // If it errors, try to get less data - half the timeframe
            syncSmallerTimeslot();
            return;
        }

        this.workoutNumbersList = packet.workoutNumbers;
        // Has to be sorted for the timestamp-based sync stat that we use
        this.workoutNumbersList.sort(Comparator.comparingInt(o -> o.workoutNumber));

        syncNextWorkout();
    }

    private void syncSmallerTimeslot() {
        if (this.smallerTimeslotDepth >= MAX_SMALLER_TIMESLOT_DEPTH) {
            LOG.error("Requested smaller timeslot too many times - refusing.");
            handleException(null);
            resetState();
            return;
        }
        this.smallerTimeslotDepth += 1;

        int start = this.currentSyncRequest.start;
        int end = this.currentSyncRequest.end;
        int half = start + (end - start) / 2;

        this.syncRequests.add(new WorkoutSyncRequest(start, half));
        this.syncRequests.add(new WorkoutSyncRequest(half + 1, end));

        syncNextFromQueue();
    }

    private void syncNextWorkout() {
        if (this.workoutNumbersList == null || this.workoutNumbersList.isEmpty()) {
            syncNextFromQueue();
            return;
        }
        this.currentWorkoutNumbers = this.workoutNumbersList.remove(0);
        this.retryCounter = 0;
        this.currentWorkoutMissingData = false;
        syncWorkoutTotals();
    }

    private void syncWorkoutTotals() {
        Request getWorkoutTotalsRequest = new RequestBuilder<Workout.WorkoutTotals.Response>(
                this.supportProvider,
                Workout.id,
                Workout.WorkoutTotals.id,
                new Workout.WorkoutTotals.Request(
                        this.supportProvider.getParamsProvider(),
                        this.currentWorkoutNumbers.workoutNumber
                )
            )
            .setTimeout(5000)
            .onCallback(this::handleWorkoutTotals)
            .onException(e -> {
                LOG.error("Exception receiving workout totals", e);
                handleException(e);
            })
            .onTimeout(packet -> {
                LOG.error("Timeout on workout totals");
                handleTimeout();
            })
            .build();
        try {
            getWorkoutTotalsRequest.doPerform();
        } catch (IOException e) {
            LOG.error("Exception sending workout totals", e);
            handleException(e);
        }
    }

    private void handleWorkoutTotals(Workout.WorkoutTotals.Response packet) {
        if (packet.error != null) {
            LOG.error("Error {} occurred during workout totals sync", packet.error);
            if (this.retryCounter >= MAX_RETRIES) {
                LOG.error("Max tries for workout totals exceeded, moving to next workout");
                GB.toast("There was an issue synchronizing one of the workouts. Continuing with then next one.", Toast.LENGTH_SHORT, GB.ERROR);
                this.retryCounter = 0;
                syncNextWorkout();
            } else {
                // Retry getting workout totals
                this.retryCounter += 1;
                syncWorkoutTotals();
            }
            return;
        }
        this.retryCounter = 0;

        if (packet.number != this.currentWorkoutNumbers.workoutNumber) {
            LOG.error("Incorrect workout number for totals response!");
            handleException(null);
            return;
        }

        LOG.info("Workout {} totals:", this.currentWorkoutNumbers.workoutNumber);
        LOG.info("Number  : {}", packet.number);
        LOG.info("Status  : {}", packet.status);
        LOG.info("Start   : {}", packet.startTime);
        LOG.info("End     : {}", packet.endTime);
        LOG.info("Calories: {}", packet.calories);
        LOG.info("Distance: {}", packet.distance);
        LOG.info("Steps   : {}", packet.stepCount);
        LOG.info("Time    : {}", packet.totalTime);
        LOG.info("Duration: {}", packet.duration);
        LOG.info("Type    : {}", packet.type);

        this.currentDatabaseId = addWorkoutTotalsDataToDb(packet);
        if (this.currentDatabaseId == null) {
            handleException(null);
            return;
        }

        this.currentNumber = 0;
        syncData();
    }

    private void syncData() {
        if (this.currentNumber >= this.currentWorkoutNumbers.dataCount) {
            this.currentNumber = 0;
            syncPace();
            return;
        }

        Request getWorkoutDataRequest = new RequestBuilder<Workout.WorkoutData.Response>(
                this.supportProvider,
                Workout.id,
                Workout.WorkoutData.id,
                new Workout.WorkoutData.Request(
                        this.supportProvider.getParamsProvider(),
                        this.currentWorkoutNumbers.workoutNumber,
                        (short) this.currentNumber,
                        this.supportProvider.getDeviceState().isSupportsWorkoutNewSteps()
                )
            )
            .setTimeout(5000)
            .onCallback(this::handleData)
            .onException(e -> {
                LOG.error("Exception receiving workout data", e);
                handleException(e);
            })
            .onTimeout(packet -> {
                LOG.error("Timeout on workout data");
                handleTimeout();
            })
            .build();
        try {
            getWorkoutDataRequest.doPerform();
        } catch (IOException e) {
            LOG.error("Exception sending workout data request", e);
            handleException(e);
        }
    }

    private void handleData(Workout.WorkoutData.Response packet) {
        boolean err = false;
        if (packet.error != null) {
            LOG.error("Error {} occurred during workout data sync", packet.error);
            err = true;
        }
        if (packet.workoutNumber != this.currentWorkoutNumbers.workoutNumber) {
            LOG.error("Incorrect workout number for data response!");
            err = true;
        }
        if (packet.dataNumber != this.currentNumber) {
            LOG.error("Incorrect data number!");
            err = true;
        }
        if (err) {
            if (this.retryCounter >= MAX_RETRIES) {
                LOG.error("Max tries for workout data exceeded, moving to next data");
                this.currentWorkoutMissingData = true;
                this.currentNumber += 1;
                this.retryCounter = 0;
                syncData();
            } else {
                // Retry getting workout data
                this.retryCounter += 1;
                syncData();
            }
            return;
        }
        this.retryCounter = 0;

        LOG.info("Workout {} data {}:", this.currentWorkoutNumbers.workoutNumber, this.currentNumber);
        LOG.info("Workout : {}", packet.workoutNumber);
        LOG.info("Data num: {}", packet.dataNumber);
        LOG.info("Header  : {}", Arrays.toString(packet.rawHeader));
        LOG.info("Header  : {}", packet.header);
        LOG.info("Data    : {}", Arrays.toString(packet.rawData));
        LOG.info("Data    : {}", Arrays.toString(packet.dataList.toArray()));
        LOG.info("Bitmap  : {}", packet.innerBitmap);

        addWorkoutSampleDataToDb(this.currentDatabaseId, packet.dataList);
        this.currentNumber += 1;

        syncData();
    }

    private void syncPace() {
        if (this.currentNumber >= this.currentWorkoutNumbers.paceCount) {
            this.currentNumber = 0;
            syncSwimSegments();
            return;
        }

        Request getWorkoutPaceRequest = new RequestBuilder<Workout.WorkoutPace.Response>(
                this.supportProvider,
                Workout.id,
                Workout.WorkoutPace.id,
                new Workout.WorkoutPace.Request(
                        this.supportProvider.getParamsProvider(),
                        this.currentWorkoutNumbers.workoutNumber,
                        (short) this.currentNumber
                )
            )
            .setTimeout(5000)
            .onCallback(this::handlePace)
            .onException(e -> {
                LOG.error("Exception receiving workout pace", e);
                handleException(e);
            })
            .onTimeout(packet -> {
                LOG.error("Tiemout on workout pace");
                handleTimeout();
            })
            .build();
        try {
            getWorkoutPaceRequest.doPerform();
        } catch (IOException e) {
            LOG.error("Exception sending workout data request", e);
            handleException(e);
        }
    }

    private void handlePace(Workout.WorkoutPace.Response packet) {
        boolean err = false;
        if (packet.error != null) {
            LOG.error("Error {} occurred during workout pace sync", packet.error);
            err = true;
        }
        if (packet.workoutNumber != this.currentWorkoutNumbers.workoutNumber) {
            LOG.error("Incorrect workout number for pace response!");
            err = true;
        }
        if (packet.paceNumber != currentNumber) {
            LOG.error("Incorrect pace number!");
            err = true;
        }
        if (err) {
            if (this.retryCounter >= MAX_RETRIES) {
                LOG.error("Max tries for workout pace exceeded, moving to next data");
                this.currentWorkoutMissingData = true;
                this.currentNumber += 1;
                this.retryCounter = 0;
                syncPace();
            } else {
                // Retry getting workout data
                this.retryCounter += 1;
                syncPace();
            }
            return;
        }
        this.retryCounter = 0;

        LOG.info("Workout {} pace {}:", this.currentWorkoutNumbers.workoutNumber, this.currentNumber);
        LOG.info("Workout  : {}", packet.workoutNumber);
        LOG.info("Pace     : {}", packet.paceNumber);
        LOG.info("Block num: {}", packet.blocks.size());
        LOG.info("Blocks   : {}", Arrays.toString(packet.blocks.toArray()));

        addWorkoutPaceDataToDb(this.currentDatabaseId, packet.blocks, packet.paceNumber);
        currentNumber += 1;

        syncPace();
    }

    private void syncSwimSegments() {
        if (this.currentNumber >= this.currentWorkoutNumbers.segmentsCount) {
            this.currentNumber = 0;
            syncSpO2();
            return;
        }

        Request getWorkoutSwimSegmentsRequest = new RequestBuilder<Workout.WorkoutSwimSegments.Response>(
                this.supportProvider,
                Workout.id,
                Workout.WorkoutSwimSegments.id,
                new Workout.WorkoutSwimSegments.Request(
                        this.supportProvider.getParamsProvider(),
                        this.currentWorkoutNumbers.workoutNumber,
                        (short) this.currentNumber
                )
            )
            .setTimeout(5000)
            .onCallback(this::handleSwimSegment)
            .onException(e -> {
                LOG.error("Exception receiving workout swim segments", e);
                handleException(e);
            })
            .onTimeout(packet -> {
                LOG.error("Timeout on workout swim segments");
                handleTimeout();
            })
            .build();
        try {
            getWorkoutSwimSegmentsRequest.doPerform();
        } catch (IOException e) {
            LOG.error("Exception sending workout swim segment request", e);
            handleException(e);
        }
    }

    private void handleSwimSegment(Workout.WorkoutSwimSegments.Response packet) {
        boolean err = false;
        if (packet.error != null) {
            LOG.error("Error {} occurred during workout swim segments sync", packet.error);
            err = true;
        }
        if (packet.workoutNumber != this.currentWorkoutNumbers.workoutNumber) {
            LOG.error("Incorrect workout number for swim segment response!");
            err = true;
        }
        if (packet.segmentNumber != this.currentNumber) {
            LOG.error("Incorrect swim segment number!");
            err = true;
        }
        if (err) {
            if (this.retryCounter >= MAX_RETRIES) {
                LOG.error("Max tries for workout swim segment exceeded, moving to next data");
                this.currentWorkoutMissingData = true;
                this.currentNumber += 1;
                this.retryCounter = 0;
                syncSwimSegments();
            } else {
                // Retry getting workout data
                this.retryCounter += 1;
                syncSwimSegments();
            }
            return;
        }
        this.retryCounter = 0;

        LOG.info("Workout {} segment {}:", this.currentWorkoutNumbers.workoutNumber, this.currentNumber);
        LOG.info("Workout  : {}", packet.workoutNumber);
        LOG.info("Segments : {}", packet.segmentNumber);
        LOG.info("Block num: {}", packet.blocks.size());
        LOG.info("Blocks   : {}", Arrays.toString(packet.blocks.toArray()));

        addWorkoutSwimSegmentsDataToDb(this.currentDatabaseId, packet.blocks, packet.segmentNumber);
        this.currentNumber += 1;

        syncSwimSegments();
    }

    private void syncSpO2() {
        if (this.currentNumber >= this.currentWorkoutNumbers.spO2Count) {
            this.currentNumber = 0;
            syncSections();
            return;
        }

        Request getWorkoutSpO2Request = new RequestBuilder<Workout.WorkoutSpO2.Response>(
                this.supportProvider,
                Workout.id,
                Workout.WorkoutSpO2.id,
                new Workout.WorkoutSpO2.Request(
                        this.supportProvider.getParamsProvider(),
                        this.currentWorkoutNumbers.workoutNumber,
                        (short) this.currentNumber
                )
            )
            .setTimeout(5000)
            .onCallback(this::handleSpO2)
            .onException(e -> {
                LOG.error("Exception receiving SpO2 data");
                handleException(e);
            })
            .onTimeout(packet -> {
                LOG.error("Timeout on SpO2 data");
                handleTimeout();
            })
            .build();
        try {
            getWorkoutSpO2Request.doPerform();
        } catch (IOException e) {
            LOG.error("Exception sending workout SpO2 request", e);
            handleException(e);
        }
    }

    private void handleSpO2(Workout.WorkoutSpO2.Response packet) {
        if (packet.error != null) {
            LOG.error("Error {} occurred during workout SpO2 sync", packet.error);
            if (this.retryCounter >= MAX_RETRIES) {
                LOG.error("Max tries for workout SpO2 exceeded, moving to next data");
                this.currentWorkoutMissingData = true;
                this.currentNumber += 1;
                this.retryCounter = 0;
                syncSpO2();
            } else {
                // Retry getting workout data
                this.retryCounter += 1;
                syncSpO2();
            }
            return;
        }
        this.retryCounter = 0;

        LOG.info("Workout {} current {}:", this.currentWorkoutNumbers.workoutNumber, this.currentNumber);
        LOG.info("spO2Number1: {}", packet.spO2Number1);
        LOG.info("spO2Number2: {}", packet.spO2Number2);
        LOG.info("Block num  : {}", packet.blocks.size());
        LOG.info("Blocks     : {}", Arrays.toString(packet.blocks.toArray()));

        addWorkoutSpO2DataToDb(this.currentDatabaseId, packet.blocks, (short) this.currentNumber);
        this.currentNumber += 1;

        syncSpO2();
    }

    private void syncSections() {
        if (this.currentNumber >= this.currentWorkoutNumbers.sectionsCount) {
            this.currentNumber = 0;
            finishSingleWorkoutSync();
            return;
        }

        Request getWorkoutSectionsRequest = new RequestBuilder<Workout.WorkoutSections.Response>(
                this.supportProvider,
                Workout.id,
                Workout.WorkoutSections.id,
                new Workout.WorkoutSections.Request(
                        this.supportProvider.getParamsProvider(),
                        this.currentWorkoutNumbers.workoutNumber,
                        (short) this.currentNumber
                )
            )
            .setTimeout(5000)
            .onCallback(this::handleSections)
            .onException(e -> {
                LOG.error("Exception receiving workout sections", e);
                handleException(e);
            })
            .onTimeout(packet -> {
                LOG.error("Timeout on workout sections");
                handleTimeout();
            })
            .build();
        try {
            getWorkoutSectionsRequest.doPerform();
        } catch (IOException e) {
            LOG.error("Exception sending workout sections request", e);
            handleException(e);
        }
    }

    private void handleSections(Workout.WorkoutSections.Response packet) {
        if (packet.error != null) {
            if (this.retryCounter >= MAX_RETRIES || packet.error == 0x0001E079) {
                if (packet.error == 0x0001E079) {
                    LOG.warn("Error 0001E079 occurred during workout swim segments sync. This seems to be normal, so it's ignored.");
                } else {
                    LOG.error("Max tries for workout sections exceeded, moving to next data");
                    this.currentWorkoutMissingData = true;
                }
                this.currentNumber += 1;
                this.retryCounter = 0;
                syncSections();
            } else {
                // Retry getting workout data
                this.retryCounter += 1;
                syncSections();
            }
            return;
        }
        this.retryCounter = 0;

        LOG.info("Workout {} section {}:", this.currentWorkoutNumbers.workoutNumber, this.currentNumber);
        LOG.info("workoutId  : {}", packet.workoutId);
        LOG.info("number     : {}", packet.number);
        LOG.info("Block num  : {}", packet.blocks.size());
        LOG.info("Blocks     : {}", Arrays.toString(packet.blocks.toArray()));

        addWorkoutSectionsDataToDb(this.currentDatabaseId, packet.blocks, (short) this.currentNumber);
        currentNumber += 1;

        syncSections();
    }

    private void finishSingleWorkoutSync() {
        // Show toast if we could not download some data
        if (this.currentWorkoutMissingData)
            dataSkippedToast();

        new HuaweiWorkoutGbParser(
                this.supportProvider.getDevice(),
                this.supportProvider.getContext()
        ).parseWorkout(this.currentDatabaseId);

        supportProvider.downloadWorkoutGpsFiles(
                this.currentWorkoutNumbers.workoutNumber,
                this.currentDatabaseId,
                this::syncNextWorkout
        );

        // We reduce this by one, as it kinda forms a tree, where we want the max depth to be 3
        this.smallerTimeslotDepth -= 1;
    }

    /*
    --------------------------------------------------
                    DATABASE FUNCTIONS
    --------------------------------------------------
     */
    private int getStartTimeFromDb() {
        int start = 0;
        GBDevice gbDevice = supportProvider.getDevice();
        SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
        long prefLastSyncTime = sharedPreferences.getLong("lastSportsActivityTimeMillis", 0);
        if (prefLastSyncTime != 0) {
            start = (int) (prefLastSyncTime / 1000);

            // Reset for next calls
            sharedPreferences.edit().putLong("lastSportsActivityTimeMillis", 0).apply();
        } else {
            try (DBHandler db = GBApplication.acquireDB()) {
                Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                Long deviceId = DBHelper.getDevice(gbDevice, db.getDaoSession()).getId();

                QueryBuilder<HuaweiWorkoutSummarySample> qb1 = db.getDaoSession().getHuaweiWorkoutSummarySampleDao().queryBuilder().where(
                        HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(deviceId),
                        HuaweiWorkoutSummarySampleDao.Properties.UserId.eq(userId)
                ).orderDesc(
                        HuaweiWorkoutSummarySampleDao.Properties.StartTimestamp
                ).limit(1);

                List<HuaweiWorkoutSummarySample> samples1 = qb1.list();
                if (!samples1.isEmpty())
                    start = samples1.get(0).getEndTimestamp();

                QueryBuilder<BaseActivitySummary> qb2 = db.getDaoSession().getBaseActivitySummaryDao().queryBuilder().where(
                        BaseActivitySummaryDao.Properties.DeviceId.eq(deviceId),
                        BaseActivitySummaryDao.Properties.UserId.eq(userId)
                ).orderDesc(
                        BaseActivitySummaryDao.Properties.StartTime
                ).limit(1);

                List<BaseActivitySummary> samples2 = qb2.list();
                if (!samples2.isEmpty())
                    start = Math.min(start, (int) (samples2.get(0).getEndTime().getTime() / 1000L));

                start = start + 1;
            } catch (Exception e) {
                LOG.warn("Exception for getting start time, using 10/06/2022 - 00:00:00.");
                start = 1654819200;
            }

            if (start == 0 || start == 1)
                start = 1654819200;
        }
        return start;
    }

    public Long addWorkoutTotalsDataToDb(Workout.WorkoutTotals.Response packet) {
        try (DBHandler db = GBApplication.acquireDB()) {
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(
                    this.supportProvider.getDevice(),
                    db.getDaoSession()
            ).getId();

            // Avoid duplicates
            QueryBuilder<HuaweiWorkoutSummarySample> qb = db.getDaoSession().getHuaweiWorkoutSummarySampleDao().queryBuilder().where(
                    HuaweiWorkoutSummarySampleDao.Properties.UserId.eq(userId),
                    HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(deviceId),
                    HuaweiWorkoutSummarySampleDao.Properties.WorkoutNumber.eq(packet.number),
                    HuaweiWorkoutSummarySampleDao.Properties.StartTimestamp.eq(packet.startTime),
                    HuaweiWorkoutSummarySampleDao.Properties.EndTimestamp.eq(packet.endTime)
            );
            List<HuaweiWorkoutSummarySample> results = qb.build().list();
            Long workoutId = null;
            if (!results.isEmpty())
                workoutId = results.get(0).getWorkoutId();

            byte[] raw;
            if (packet.rawData == null)
                raw = null;
            else
                raw = StringUtils.bytesToHex(packet.rawData).getBytes(StandardCharsets.UTF_8);


            byte[] recoveryHeartRates;
            if (packet.recoveryHeartRates == null)
                recoveryHeartRates = null;
            else
                recoveryHeartRates = StringUtils.bytesToHex(packet.recoveryHeartRates).getBytes(StandardCharsets.UTF_8);

            HuaweiWorkoutSummarySample summarySample = new HuaweiWorkoutSummarySample(
                    workoutId,
                    deviceId,
                    userId,
                    packet.number,
                    packet.status,
                    packet.startTime,
                    packet.endTime,
                    packet.calories,
                    packet.distance,
                    packet.stepCount,
                    packet.totalTime,
                    packet.duration,
                    packet.type,
                    packet.strokes,
                    packet.avgStrokeRate,
                    packet.poolLength,
                    packet.laps,
                    packet.avgSwolf,
                    raw,
                    null,
                    packet.maxAltitude,
                    packet.minAltitude,
                    packet.elevationGain,
                    packet.elevationLoss,
                    packet.workoutLoad,
                    packet.workoutAerobicEffect,
                    packet.workoutAnaerobicEffect,
                    packet.recoveryTime,
                    packet.minHeartRatePeak,
                    packet.maxHeartRatePeak,
                    recoveryHeartRates,
                    packet.swimType,
                    packet.maxMET,
                    packet.hrZoneType,
                    packet.runPaceZone1Min,
                    packet.runPaceZone2Min,
                    packet.runPaceZone3Min,
                    packet.runPaceZone4Min,
                    packet.runPaceZone5Min,
                    packet.runPaceZone5Max,
                    packet.runPaceZone1Time,
                    packet.runPaceZone2Time,
                    packet.runPaceZone3Time,
                    packet.runPaceZone4Time,
                    packet.runPaceZone5Time,
                    packet.algType,
                    packet.trainingPoints,
                    packet.longestStreak,
                    packet.tripped,
                    this.supportProvider.getDeviceState().isSupportsWorkoutNewSteps(),
                    null
            );

            db.getDaoSession().getHuaweiWorkoutSummarySampleDao().insertOrReplace(summarySample);

            // We should completely replace values. Delete all and insert again.
            final DeleteQuery<HuaweiWorkoutSummaryAdditionalValuesSample> tableDeleteQuery = db.getDaoSession().getHuaweiWorkoutSummaryAdditionalValuesSampleDao().queryBuilder()
                    .where(HuaweiWorkoutSummaryAdditionalValuesSampleDao.Properties.WorkoutId.eq(summarySample.getWorkoutId()))
                    .buildDelete();
            tableDeleteQuery.executeDeleteWithoutDetachingEntities();

            for (Map.Entry<String, String> entry : packet.additionalValues.entrySet()) {
                HuaweiWorkoutSummaryAdditionalValuesSample summarySampleAdditionalValue = new HuaweiWorkoutSummaryAdditionalValuesSample(summarySample.getWorkoutId(), entry.getKey(), entry.getValue());
                db.getDaoSession().getHuaweiWorkoutSummaryAdditionalValuesSampleDao().insertOrReplace(summarySampleAdditionalValue);
            }

            return summarySample.getWorkoutId();
        } catch (Exception e) {
            LOG.error("Failed to add workout totals data to database", e);
            return null;
        }
    }

    public void addWorkoutSampleDataToDb(Long workoutId, List<Workout.WorkoutData.Response.Data> dataList) {
        if (workoutId == null)
            return;

        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiWorkoutDataSampleDao dao = db.getDaoSession().getHuaweiWorkoutDataSampleDao();

            for (Workout.WorkoutData.Response.Data data : dataList) {
                byte[] unknown;
                if (data.unknownData == null)
                    unknown = null;
                else
                    unknown = StringUtils.bytesToHex(data.unknownData).getBytes(StandardCharsets.UTF_8);

                HuaweiWorkoutDataSample dataSample = new HuaweiWorkoutDataSample(
                        workoutId,
                        data.timestamp,
                        data.heartRate,
                        data.speed,
                        data.stepRate,
                        data.cadence,
                        data.stepLength,
                        data.groundContactTime,
                        data.impact,
                        data.swingAngle,
                        data.foreFootLanding,
                        data.midFootLanding,
                        data.backFootLanding,
                        data.eversionAngle,
                        data.swolf,
                        data.strokeRate,
                        unknown,
                        data.calories,
                        data.cyclingPower,
                        data.frequency,
                        data.altitude,
                        data.hangTime,
                        data.impactHangRate,
                        data.rideCadence,
                        data.ap,
                        data.vo,
                        data.gtb,
                        data.vr,
                        data.ceiling,
                        data.temp,
                        data.spo2,
                        data.cns
                );
                dao.insertOrReplace(dataSample);
            }
        } catch (Exception e) {
            LOG.error("Failed to add workout data to database", e);
        }
    }

    public void addWorkoutPaceDataToDb(Long workoutId, List<Workout.WorkoutPace.Response.Block> paceList, short number) {
        if (workoutId == null)
            return;

        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiWorkoutPaceSampleDao dao = db.getDaoSession().getHuaweiWorkoutPaceSampleDao();

            if (number == 0) {
                final DeleteQuery<HuaweiWorkoutPaceSample> tableDeleteQuery = dao.queryBuilder()
                        .where(HuaweiWorkoutPaceSampleDao.Properties.WorkoutId.eq(workoutId))
                        .buildDelete();
                tableDeleteQuery.executeDeleteWithoutDetachingEntities();
            }

            int paceIndex = (int) dao.queryBuilder().where(HuaweiWorkoutPaceSampleDao.Properties.WorkoutId.eq(workoutId)).count();
            for (Workout.WorkoutPace.Response.Block block : paceList) {

                Integer correction = block.hasCorrection ? (int) block.correction : null;
                HuaweiWorkoutPaceSample paceSample = new HuaweiWorkoutPaceSample(
                        workoutId,
                        paceIndex++,
                        block.distance,
                        block.type,
                        block.pace,
                        block.pointIndex,
                        correction
                );
                dao.insertOrReplace(paceSample);
            }
        } catch (Exception e) {
            LOG.error("Failed to add workout pace data to database", e);
        }
    }


    public void addWorkoutSwimSegmentsDataToDb(Long workoutId, List<Workout.WorkoutSwimSegments.Response.Block> paceList, short number) {
        if (workoutId == null)
            return;

        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiWorkoutSwimSegmentsSampleDao dao = db.getDaoSession().getHuaweiWorkoutSwimSegmentsSampleDao();

            if (number == 0) {
                final DeleteQuery<HuaweiWorkoutSwimSegmentsSample> tableDeleteQuery = dao.queryBuilder()
                        .where(HuaweiWorkoutSwimSegmentsSampleDao.Properties.WorkoutId.eq(workoutId))
                        .buildDelete();
                tableDeleteQuery.executeDeleteWithoutDetachingEntities();
            }

            int paceIndex = (int) dao.queryBuilder().where(HuaweiWorkoutSwimSegmentsSampleDao.Properties.WorkoutId.eq(workoutId)).count();
            for (Workout.WorkoutSwimSegments.Response.Block block : paceList) {
                HuaweiWorkoutSwimSegmentsSample swimSectionSample = new HuaweiWorkoutSwimSegmentsSample(
                        workoutId,
                        paceIndex++,
                        block.distance,
                        block.type,
                        block.pace,
                        block.pointIndex,
                        block.segment,
                        block.swimType,
                        block.strokes,
                        block.avgSwolf,
                        block.time
                );
                dao.insertOrReplace(swimSectionSample);
            }
        } catch (Exception e) {
            LOG.error("Failed to add workout swim section data to database", e);
        }
    }

    public void addWorkoutSpO2DataToDb(Long workoutId, List<Workout.WorkoutSpO2.Response.Block> spO2List, short number) {
        if (workoutId == null)
            return;

        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiWorkoutSpO2SampleDao dao = db.getDaoSession().getHuaweiWorkoutSpO2SampleDao();

            if (number == 0) {
                final DeleteQuery<HuaweiWorkoutSpO2Sample> tableDeleteQuery = dao.queryBuilder()
                        .where(HuaweiWorkoutSpO2SampleDao.Properties.WorkoutId.eq(workoutId))
                        .buildDelete();
                tableDeleteQuery.executeDeleteWithoutDetachingEntities();
            }

            for (Workout.WorkoutSpO2.Response.Block block : spO2List) {
                HuaweiWorkoutSpO2Sample spO2Sample = new HuaweiWorkoutSpO2Sample(
                        workoutId,
                        block.interval,
                        block.value
                );
                dao.insertOrReplace(spO2Sample);
            }
        } catch (Exception e) {
            LOG.error("Failed to add workout SpO2 data to database", e);
        }
    }

    public void addWorkoutSectionsDataToDb(Long workoutId, List<Workout.WorkoutSections.Response.Block> sectionsList, short number) {
        if (workoutId == null)
            return;

        // NOTE: All fields of this data is optional. At this point I don't all workouts that this data used.
        // I decided to add two additional fields dataIdx and rowIdx as primary keys that should identify each row
        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiWorkoutSectionsSampleDao dao = db.getDaoSession().getHuaweiWorkoutSectionsSampleDao();

            if (number == 0) {
                final DeleteQuery<HuaweiWorkoutSectionsSample> tableDeleteQuery = dao.queryBuilder()
                        .where(HuaweiWorkoutSectionsSampleDao.Properties.WorkoutId.eq(workoutId))
                        .buildDelete();
                tableDeleteQuery.executeDeleteWithoutDetachingEntities();
            }

            int i = 0;
            for (Workout.WorkoutSections.Response.Block block : sectionsList) {
                HuaweiWorkoutSectionsSample huaweiWorkoutSectionsSample = new HuaweiWorkoutSectionsSample(
                        workoutId,
                        number,
                        i++,
                        block.num,
                        block.time,
                        block.distance,
                        block.pace,
                        block.heartRate,
                        block.cadence,
                        block.stepLength,
                        block.totalRise,
                        block.totalDescend,
                        block.groundContactTime,
                        block.groundImpact,
                        block.swingAngle,
                        block.eversion,
                        block.avgCadence,
                        block.divingUnderwaterTime,
                        block.divingMaxDepth,
                        block.divingUnderwaterTime,
                        block.divingBreakTime
                );
                dao.insertOrReplace(huaweiWorkoutSectionsSample);
            }
        } catch (Exception e) {
            LOG.error("Failed to add workout sections data to database", e);
        }
    }
}
