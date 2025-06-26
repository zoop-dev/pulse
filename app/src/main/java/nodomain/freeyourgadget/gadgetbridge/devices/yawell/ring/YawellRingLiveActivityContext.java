/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring;

import java.util.concurrent.ScheduledExecutorService;

public class YawellRingLiveActivityContext {
    private int bufferedSteps = 0;
    private int bufferedCalories = 0;
    private int bufferedDistance = 0;
    private int lastTotalSteps = 0;
    private int lastTotalCalories = 0;
    private int lastTotalDistance = 0;
    private int lastRealtimeHeartRateTimestamp = 0;
    private boolean realtimeHrm = false;
    private int realtimeHrmPacketCount = 0;
    private boolean realtimeSteps = false;
    private ScheduledExecutorService realtimeStepsScheduler;

    public boolean isRealtimeSteps() {
        return realtimeSteps;
    }

    public void setRealtimeSteps(boolean realtimeSteps) {
        this.realtimeSteps = realtimeSteps;
    }

    public ScheduledExecutorService getRealtimeStepsScheduler() {
        return realtimeStepsScheduler;
    }

    public void setRealtimeStepsScheduler(ScheduledExecutorService realtimeStepsScheduler) {
        this.realtimeStepsScheduler = realtimeStepsScheduler;
    }

    public int getBufferedSteps() {
        return bufferedSteps;
    }

    public void setBufferedSteps(int bufferedSteps) {
        this.bufferedSteps = bufferedSteps;
    }

    public int getBufferedCalories() {
        return bufferedCalories;
    }

    public void setBufferedCalories(int bufferedCalories) {
        this.bufferedCalories = bufferedCalories;
    }

    public int getBufferedDistance() {
        return bufferedDistance;
    }

    public void setBufferedDistance(int bufferedDistance) {
        this.bufferedDistance = bufferedDistance;
    }

    public int getLastTotalSteps() {
        return lastTotalSteps;
    }

    public void setLastTotalSteps(int lastTotalSteps) {
        this.lastTotalSteps = lastTotalSteps;
    }

    public int getLastTotalCalories() {
        return lastTotalCalories;
    }

    public void setLastTotalCalories(int lastTotalCalories) {
        this.lastTotalCalories = lastTotalCalories;
    }

    public int getLastTotalDistance() {
        return lastTotalDistance;
    }

    public void setLastTotalDistance(int lastTotalDistance) {
        this.lastTotalDistance = lastTotalDistance;
    }

    public int getLastRealtimeHeartRateTimestamp() {
        return lastRealtimeHeartRateTimestamp;
    }

    public void setLastRealtimeHeartRateTimestamp(int lastRealtimeHeartRateTimestamp) {
        this.lastRealtimeHeartRateTimestamp = lastRealtimeHeartRateTimestamp;
    }

    public boolean isRealtimeHrm() {
        return realtimeHrm;
    }

    public void setRealtimeHrm(boolean realtimeHrm) {
        this.realtimeHrm = realtimeHrm;
    }

    public int getRealtimeHrmPacketCount() {
        return realtimeHrmPacketCount;
    }

    public void setRealtimeHrmPacketCount(int realtimeHrmPacketCount) {
        this.realtimeHrmPacketCount = realtimeHrmPacketCount;
    }
}
