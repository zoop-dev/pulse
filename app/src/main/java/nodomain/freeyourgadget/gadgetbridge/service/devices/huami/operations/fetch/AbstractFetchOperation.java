/*  Copyright (C) 2018-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFetcher;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data.
 */
public abstract class AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFetchOperation.class);

    protected final HuamiFetcher fetcher;
    protected final HuamiFetchDataType dataType;
    protected final String name;

    protected Calendar startTimestamp;

    protected int fetchCount;
    protected byte lastPacketCounter;
    protected int expectedDataLength = 0;
    protected final ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);

    protected boolean operationValid = true; // to mark operation failed midway (eg. out of sync)

    public AbstractFetchOperation(final HuamiFetcher fetcher, final HuamiFetchDataType dataType) {
        this.fetcher = fetcher;
        this.dataType = dataType;
        this.name = "fetching " + dataType.name();
    }

    protected Context getContext() {
        return fetcher.getContext();
    }

    public String getName() {
        return this.name;
    }

    protected GBDevice getDevice() {
        return fetcher.getDevice();
    }

    public void doPerform() {
        expectedDataLength = 0;
        lastPacketCounter = -1;
        fetchCount++;

        startFetching();
    }

    /**
     * A task description, to display in notifications and device card.
     */
    @StringRes
    public abstract int taskDescription();

    protected abstract void startFetching();

    protected abstract String getLastSyncTimeKey();

    /**
     * Handles the finishing of fetching the activity. This signals the actual end of this operation.
     */
    protected final void onOperationFinished() {
        fetcher.triggerNextOperation();
    }

    /**
     * Validates that the received data has the expected checksum. Only
     * relevant for ZeppOsSupport devices.
     *
     * @param crc32 the expected checksum
     * @return whether the checksum was valid
     */
    protected boolean validChecksum(int crc32) {
        return crc32 == CheckSums.getCRC32(buffer.toByteArray());
    }

    protected abstract boolean processBufferedData();

    public void handleActivityData(final byte[] value) {
        LOG.debug("{} data: {}", getName(), Logging.formatBytes(value));

        if (!operationValid) {
            LOG.error("Ignoring {} notification because operation is not valid. Data length: {}", getName(), value.length);
            return;
        }

        if ((byte) (lastPacketCounter + 1) == value[0]) {
            // TODO we should handle skipped or repeated bytes more gracefully
            lastPacketCounter++;
            bufferActivityData(value);
        } else {
            GB.toast("Error " + getName() + ", invalid package counter: " + value[0] + ", last was: " + lastPacketCounter, Toast.LENGTH_LONG, GB.ERROR);
            operationValid = false;
        }
    }

    protected void bufferActivityData(byte[] value) {
        buffer.write(value, 1, value.length - 1); // skip the counter
    }

    protected void startFetching(final byte fetchType, final GregorianCalendar sinceWhen) {
        final byte[] fetchBytes = BLETypeConversions.join(
                new byte[]{HuamiService.COMMAND_ACTIVITY_DATA_START_DATE, fetchType},
                fetcher.getTimeBytes(sinceWhen, fetcher.getFetchOperationsTimeUnit())
        );
        fetcher.writeControl("fetch", fetchBytes);
    }

    public void handleActivityMetadata(byte[] value) {
        if (value.length < 3) {
            LOG.warn("Activity metadata too short: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        if (value[0] != HuamiService.RESPONSE) {
            LOG.warn("Activity metadata not a response: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        switch (value[1]) {
            case HuamiService.COMMAND_ACTIVITY_DATA_START_DATE:
                handleStartDateResponse(value);
                return;
            case HuamiService.COMMAND_FETCH_DATA:
                handleFetchDataResponse(value);
                return;
            case HuamiService.COMMAND_ACK_ACTIVITY_DATA:
                LOG.info("Got reply to COMMAND_ACK_ACTIVITY_DATA");
                onOperationFinished();
                return;
            default:
                LOG.warn("Unexpected activity metadata: {}", Logging.formatBytes(value));
                onOperationFinished();
        }
    }

    private void handleStartDateResponse(final byte[] value) {
        if (value[2] != HuamiService.SUCCESS) {
            LOG.warn("Start date unsuccessful response: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        // it's 16 on the MB7, with a 0 at the end
        if (value.length != 15 && (value.length != 16 && value[15] != 0x00)) {
            LOG.warn("Start date response length: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        // the third byte (0x01 on success) = ?
        // the 4th - 7th bytes represent the number of bytes/packets to expect, excluding the counter bytes
        expectedDataLength = BLETypeConversions.toUint32(Arrays.copyOfRange(value, 3, 7));

        // last 8 bytes are the start date
        Calendar startTimestamp = BLETypeConversions.rawBytesToCalendar(Arrays.copyOfRange(value, 7, value.length));

        if (expectedDataLength == 0) {
            LOG.info("No data to fetch since {}", DateTimeUtils.formatIso8601(startTimestamp.getTime()));
            sendAck(true);
            // do not finish the operation - do it in the ack response
            return;
        }

        setStartTimestamp(startTimestamp);
        LOG.info("Will transfer {} packets since {}", expectedDataLength, DateTimeUtils.formatIso8601(startTimestamp.getTime()));

        GB.updateTransferNotification(getContext().getString(taskDescription()),
                getContext().getString(R.string.FetchActivityOperation_about_to_transfer_since,
                        DateFormat.getDateTimeInstance().format(startTimestamp.getTime())), true, 0, getContext());

        // Trigger the actual data fetch
        fetcher.setNotifications(true, true);
        fetcher.writeControl(getName() + " step 2", new byte[]{HuamiService.COMMAND_FETCH_DATA});
    }

    private void handleFetchDataResponse(final byte[] value) {
        if (value[2] != HuamiService.SUCCESS) {
            LOG.warn("Fetch data unsuccessful response: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        if (value.length != 3 && value.length != 7) {
            LOG.warn("Fetch data unexpected metadata length: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        if (value.length == 7 && !validChecksum(BLETypeConversions.toUint32(value, 3))) {
            LOG.warn("Data checksum invalid");
            // If we're on Zepp OS, ack but keep data on device
            if (fetcher.isZeppOs()) {
                sendAck(true);
                // do not finish the operation - do it in the ack response
                return;
            }
            onOperationFinished();
            return;
        }

        final boolean success = operationValid && processBufferedData();

        final boolean keepActivityDataOnDevice = !success || HuamiCoordinator.getKeepActivityDataOnDevice(getDevice().getAddress());
        if (fetcher.isZeppOs() || !keepActivityDataOnDevice) {
            sendAck(keepActivityDataOnDevice);
            // do not finish the operation - do it in the ack response
            return;
        }

        onOperationFinished();
    }

    protected void sendAck(final boolean keepDataOnDevice) {
        final byte[] ackBytes;

        if (fetcher.isZeppOs()) {
            LOG.debug("Sending ack, keepDataOnDevice = {}", keepDataOnDevice);

            // 0x01 to ACK, mark as saved on phone (drop from band)
            // 0x09 to ACK, but keep it marked as not saved
            // If 0x01 is sent, detailed information seems to be discarded, and is not sent again anymore
            final byte ackByte = (byte) (keepDataOnDevice ? 0x09 : 0x01);
            ackBytes = new byte[]{HuamiService.COMMAND_ACK_ACTIVITY_DATA, ackByte};
        } else {
            LOG.debug("Sending ack, simple");
            ackBytes = new byte[]{HuamiService.COMMAND_ACK_ACTIVITY_DATA};
        }

        fetcher.writeControl(getName() + " end", ackBytes);
    }

    private void setStartTimestamp(final Calendar startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    protected Calendar getLastStartTimestamp() {
        return startTimestamp;
    }

    protected void saveLastSyncTimestamp(@NonNull final GregorianCalendar timestamp) {
        final SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(fetcher.getDevice().getAddress()).edit();
        editor.putLong(getLastSyncTimeKey(), timestamp.getTimeInMillis());
        editor.apply();
    }

    protected GregorianCalendar getLastSuccessfulSyncTime() {
        final long timeStampMillis = GBApplication.getDeviceSpecificSharedPrefs(fetcher.getDevice().getAddress()).getLong(getLastSyncTimeKey(), 0);
        if (timeStampMillis != 0) {
            GregorianCalendar calendar = BLETypeConversions.createCalendar();
            calendar.setTimeInMillis(timeStampMillis);
            return calendar;
        }
        final GregorianCalendar calendar = BLETypeConversions.createCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, -100);
        return calendar;
    }
}
