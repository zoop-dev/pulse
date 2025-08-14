/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.AbstractBTBRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsBtbrSupport extends AbstractBTBRDeviceSupport implements ZeppOsCommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsBtbrSupport.class);

    /**
     * The highest MTU we have seen is 32768. However, there is some bug in the BtbrQueue where it
     * will lose some bytes if we read less than the available bytes, so we double the buffer size.
     */
    private static final int MAX_MTU = 2 * 32768;

    private static final byte PACKET_PREAMBLE = 0x55;
    private static final byte PACKET_TRAILER = (byte) 0xaa;

    private static final byte CMD_CHANNELS_GET = 0x01;
    private static final byte CMD_CHANNELS_RET = 0x02;
    private static final byte CMD_SESSION_START = 0x03;
    private static final byte CMD_SESSION_START_ACK = 0x04;
    private static final byte CMD_SESSION_END = 0x05;
    private static final byte CMD_SESSION_END_ACK = 0x06;
    private static final byte CMD_CHANNEL_DATA = 0x07;
    private static final byte CMD_CHANNEL_ACK = 0x08;
    private static final byte CMD_PING = 0x09;
    private static final byte CMD_PONG = 0x0a;

    private byte seqNumTx = 0x00;
    private byte seqNumRx = 0x5a;

    private byte sessionNumber;
    private int sessionNonce;

    private final ByteBuffer packetBuffer = ByteBuffer.allocate(MAX_MTU).order(ByteOrder.LITTLE_ENDIAN);

    private final ZeppOsSupport zeppOsSupport = new ZeppOsSupport(this);

    private final SparseArray<UUID> channelToCharacteristic = new SparseArray<>();
    private final Map<UUID, Short> characteristicToChannel = new HashMap<>();

    private final Handler pingHandler = new Handler();
    private long lastWrite = -1;

    public ZeppOsBtbrSupport() {
        super(LOG, MAX_MTU);
        addSupportedService(HuamiService.UUID_BT_SERIAL_SERVICE);
    }

    @Override
    public void setContext(final GBDevice gbDevice, final BluetoothAdapter btAdapter, final Context context) {
        super.setContext(gbDevice, btAdapter, context);
        zeppOsSupport.setContext(gbDevice, btAdapter, context);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            zeppOsSupport.dispose();
            pingHandler.removeCallbacksAndMessages(null);
            super.dispose();
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        packetBuffer.clear();

        write(builder, CMD_CHANNELS_GET, new byte[]{});
        scheduleNextPing(25 * 60 * 1000L);
        return builder;
    }

    @Override
    public void onSocketRead(final byte[] data) {
        packetBuffer.put(data);
        packetBuffer.flip();

        while (packetBuffer.hasRemaining()) {
            packetBuffer.mark();
            if (packetBuffer.remaining() < 8) {
                // not enough bytes for min packet
                packetBuffer.reset();
                break;
            }

            final byte preamble = packetBuffer.get();
            if (preamble != PACKET_PREAMBLE) {
                LOG.warn("Unexpected byte {} is not preamble, skipping 1b", String.format("0x%02x", preamble));
                continue;
            }

            final byte cmd = packetBuffer.get();
            final byte seqNum = packetBuffer.get();
            // TODO check seqNum
            seqNumRx = seqNum;

            final int length = packetBuffer.getShort();
            if (packetBuffer.remaining() < length + 3) {
                // not enough bytes for payload + crc + trailer
                packetBuffer.reset();
                break;
            }
            final byte[] payload = new byte[length];
            packetBuffer.get(payload);

            final short expectedCrc = packetBuffer.getShort();

            final byte trailer = packetBuffer.get();
            if (trailer != PACKET_TRAILER) {
                LOG.warn("Unexpected byte {} is not trailer, skipping 1b", String.format("0x%02x", trailer));
                // if we made it this far, just rewind 1 byte and continue
                packetBuffer.position(packetBuffer.position() - 1);
                continue;
            }

            final short actualCrc = (short) crc16(cmd, seqNum, payload);
            if (expectedCrc != actualCrc) {
                LOG.warn(
                        "Packet has invalid crc, got {}, expected {}",
                        String.format("%04x", actualCrc),
                        String.format("%04x", expectedCrc)
                );
                continue;
            }

            handlePacket(cmd, payload);
        }

        packetBuffer.compact();
    }

    public void write(final TransactionBuilder builder, final byte cmd, final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.allocate(payload.length + 8).order(ByteOrder.LITTLE_ENDIAN);

        final byte seqNum = seqNumTx++;
        buf.put(PACKET_PREAMBLE);
        buf.put(cmd);
        buf.put(seqNum);
        buf.putShort((short) payload.length);
        if (payload.length > 0) {
            buf.put(payload);
        }
        buf.putShort((short) crc16(cmd, seqNum, payload));
        buf.put(PACKET_TRAILER);

        builder.write(buf.array());

        // FIXME we haven't written here yet, but a few milliseconds / seconds shouldn't be a big problem
        if (cmd == CMD_CHANNEL_DATA) {
            lastWrite = System.currentTimeMillis();
        }
    }

    protected void write(final TransactionBuilder builder,
                         final UUID characteristic,
                         final byte[] value,
                         final boolean requestAck) {
        final Short channel = characteristicToChannel.get(characteristic);
        if (channel == null) {
            LOG.error("Unknown characteristic {}", characteristic);
            return;
        }
        final ByteBuffer buf = ByteBuffer.allocate(value.length + 4).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(sessionNumber);
        buf.putShort(channel);
        buf.put((byte) (requestAck ? 1 : 0));
        buf.put(value);
        write(builder, CMD_CHANNEL_DATA, buf.array());
    }

    private void handlePacket(final byte cmd, final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        switch (cmd) {
            case CMD_CHANNELS_RET: {
                final int version = buf.get() & 0xff; // ?
                if (version > 1) {
                    LOG.error("Protocol version {} is not supported", version);
                    GB.toast(getContext(), "Unsupported protocol version " + version, Toast.LENGTH_LONG, GB.WARN);
                    GBApplication.deviceService(getDevice()).disconnect();
                    return;
                }
                final int numCharacteristics = buf.getShort();
                LOG.debug("Got channels version={}, numCharacteristics={}", version, numCharacteristics);
                channelToCharacteristic.clear();
                characteristicToChannel.clear();
                for (int i = 0; i < numCharacteristics; i++) {
                    final String characteristicUuid = StringUtils.untilNullTerminator(buf);
                    final short characteristicNumber = buf.getShort();

                    LOG.debug("Got characteristic uuid={} num={}", characteristicUuid, characteristicNumber);
                    channelToCharacteristic.put(characteristicNumber, UUID.fromString(characteristicUuid));
                    characteristicToChannel.put(UUID.fromString(characteristicUuid), characteristicNumber);
                }

                sessionNonce = RandomUtils.insecure().randomInt();
                final TransactionBuilder builder = createTransactionBuilder("session start");
                write(builder, CMD_SESSION_START, BLETypeConversions.fromUint32(sessionNonce));
                builder.queue();
                return;
            }
            case CMD_SESSION_START_ACK: {
                final int nonce = buf.getInt();
                if (nonce != sessionNonce) {
                    LOG.warn("Got unexpected session nonce {}, expected {}", nonce, sessionNonce);
                    return;
                }
                final int status = buf.get() & 0xff;
                if (status != 1) {
                    LOG.error("Got unexpected status {}", status);
                    return;
                }
                sessionNumber = buf.get();
                final int mtu = buf.getShort() & 0xFFFF;
                zeppOsSupport.setMtu(mtu);

                if (mtu > MAX_MTU) {
                    LOG.error("MTU is larger than {}, there may be issues", MAX_MTU);
                }

                // TODO 3 bytes - 08:07:01?

                LOG.debug("Got session start ack, sessionNumber={}, mtu={}", sessionNumber, mtu);

                final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder("auth phase 1");
                zeppOsSupport.initializeDevice(builder);
                builder.queue();

                return;
            }
            case CMD_SESSION_END: {
                final byte session = buf.get();
                final byte status = buf.get(); // 3
                LOG.debug("Got session end, session={}, status={}", session, status);
                if (session == sessionNumber) {
                    // FIXME: the watch will disconnect the btrfcomm socket ~2s after this msg is received.
                    //  The btbr implementation should recover by itself, but does not, so we
                    //  force a disconnect and reconnect 5s after
                    LOG.warn("Main session ended - will disconnect and re-connect");
                    GBApplication.deviceService(getDevice()).disconnect();
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> {
                        LOG.debug("Triggering re-connect due to main session end");
                        GBApplication.deviceService(getDevice()).connect();
                    }, 5000L);
                }
                return;
            }
            case CMD_SESSION_END_ACK: {
                final byte session = buf.get(); // 0xff on unk session
                final byte status = buf.get(); // 1 for ack, 0x10 for unk session
                LOG.debug("Got session end ack, session={}, status={}", session, status);
                // TODO reconnect if we lose main session?
                return;
            }
            case CMD_CHANNEL_DATA: {
                final byte session = buf.get();
                if (session != sessionNumber) {
                    LOG.error("Got data for unknown session {}, expected {}", session, sessionNumber);
                    return;
                }
                final short channel = buf.getShort();
                final byte mustAck = buf.get();
                final byte[] dataPayload = new byte[buf.remaining()];
                buf.get(dataPayload);
                final UUID uuid = channelToCharacteristic.get(channel);
                if (uuid == null) {
                    LOG.error("Channel {} does not match any known characteristic", channel);
                    return;
                }
                zeppOsSupport.onCharacteristicChanged(uuid, dataPayload);
                if (mustAck != 0) {
                    // untested, based on what we see in the watch->phone ack when requested
                    final TransactionBuilder builder = createTransactionBuilder("channel ack");
                    write(builder, CMD_CHANNEL_ACK, new byte[]{session, seqNumRx, 0x01 /* ok */, 0x00 /* ? */});
                    builder.queue();
                }
                return;
            }
            case CMD_CHANNEL_ACK: {
                final byte session = buf.get(); // 0xff on unk session
                final byte seqNum = buf.get();
                final byte status = buf.get(); // 1 for ack, 2 for unk session
                final byte unk = buf.get(); // 0?
                LOG.debug("Got ack for session={}, seqNum={}, status={}, unk={}", session, seqNum, status, unk);
                return;
            }
            case CMD_PING: {
                final byte session = buf.get();
                final byte status = buf.get(); // 0
                final byte unk1 = buf.get(); // 0
                final byte unk2 = buf.get(); // 0
                if (session != sessionNumber) {
                    LOG.warn("Got ping for unknown session {}, expected {}", session, sessionNumber);
                    return;
                }
                LOG.debug("Got ping, session={}, status={}, unk1={}, unk2={}", session, status, unk1, unk2);

                final TransactionBuilder builder = createTransactionBuilder("pong");
                write(builder, CMD_PONG, new byte[]{session, 0x01, 0x00, 0x00});
                builder.queue();

                // When we send a ping, watch replies with a pong. However, we never actually saw it happen
                // in the other direction, and sending the pong is not enough. The official app will
                // constantly request the apps list every once in a while.

                return;
            }
            case CMD_PONG: {
                final byte session = buf.get();
                final byte status = buf.get(); // 1
                final byte unk1 = buf.get(); // 0
                final byte unk2 = buf.get(); // 0
                LOG.debug("Got pong, session={}, status={}, unk1={}, unk2={}", session, status, unk1, unk2);
                return;
            }
        }

        LOG.warn("Unexpected cmd={}, payload={}", String.format("0x%02x", cmd), GB.hexdump(payload));
    }

    private int crc16(final byte command, final byte seqNum, final byte[] payload) {
        int crc = CheckSums.getCRC16(new byte[]{command});
        crc = CheckSums.getCRC16(new byte[]{seqNum}, crc);
        crc = CheckSums.getCRC16(BLETypeConversions.fromUint16(payload.length), crc);
        crc = CheckSums.getCRC16(payload, crc);
        return crc;
    }

    private void scheduleNextPing(final long delayMillis) {
        LOG.debug("Scheduling next ping for {} ms in the future", delayMillis);
        pingHandler.postDelayed(() -> {
            final long timeSinceLastWrite = System.currentTimeMillis() - lastWrite;
            // Only send the ping if there were > 24 min since the last one
            if (timeSinceLastWrite > 24 * 60 * 1000L) {
                LOG.debug("Sending ping");
                sendPing();
                scheduleNextPing(25 * 60 * 1000L);
            } else {
                LOG.debug("Not enough time since last write, scheduling another ping");
                scheduleNextPing(25 * 60 * 1000L - timeSinceLastWrite);
            }
        }, delayMillis);
    }

    private void sendPing() {
        final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder("ping with request apps");
        zeppOsSupport.requestApps(builder);
        builder.queue();
    }

    // =============================================================================================
    // ZeppOsCommunicator
    // =============================================================================================

    @Override
    public ZeppOsTransactionBuilder createZeppOsTransactionBuilder(final String taskName) {
        return new ZeppOsBtbrTransactionBuilder(this, taskName);
    }

    @Override
    public void setCurrentTime(final ZeppOsTransactionBuilder builder) {
        LOG.error("Set current time not supported on Btbr");
    }

    @Override
    public void requestDeviceInfo(final ZeppOsTransactionBuilder builder) {
        LOG.error("Request device info not supported on Btbr");
    }

    @Override
    public void onAuthenticationSuccess(final ZeppOsTransactionBuilder builder) {
        // Nothing to do
    }

    // =============================================================================================
    // Pass-through to zeppOsSupport
    // =============================================================================================

    @Override
    public void onSendConfiguration(final String config) {
        zeppOsSupport.onSendConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {
        zeppOsSupport.onTestNewFunction();
    }

    @Override
    public void onFindDevice(final boolean start) {
        zeppOsSupport.onFindDevice(start);
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        zeppOsSupport.onFetchRecordedData(dataTypes);
    }

    @Override
    public void onFindPhone(final boolean start) {
        zeppOsSupport.onFindPhone(start);
    }

    @Override
    public void onScreenshotReq() {
        zeppOsSupport.onScreenshotReq();
    }

    @Override
    public void onSetHeartRateMeasurementInterval(final int seconds) {
        zeppOsSupport.onSetHeartRateMeasurementInterval(seconds);
    }

    @Override
    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        zeppOsSupport.onAddCalendarEvent(calendarEventSpec);
    }

    @Override
    public void onDeleteCalendarEvent(final byte type, final long id) {
        zeppOsSupport.onDeleteCalendarEvent(type, id);
    }

    @Override
    public void onHeartRateTest() {
        zeppOsSupport.onHeartRateTest();
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        zeppOsSupport.onEnableRealtimeHeartRateMeasurement(enable);
    }

    @Override
    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        zeppOsSupport.onSetAlarms(alarms);
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        zeppOsSupport.onSetCallState(callSpec);
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        zeppOsSupport.onNotification(notificationSpec);
    }

    @Override
    public void onSetReminders(final ArrayList<? extends Reminder> reminders) {
        zeppOsSupport.onSetReminders(reminders);
    }

    @Override
    public void onSetWorldClocks(ArrayList<? extends WorldClock> clocks) {
        zeppOsSupport.onSetWorldClocks(clocks);
    }

    @Override
    public void onSetLoyaltyCards(final ArrayList<LoyaltyCard> cards) {
        zeppOsSupport.onSetLoyaltyCards(cards);
    }

    @Override
    public void onSetContacts(ArrayList<? extends Contact> contacts) {
        zeppOsSupport.onSetContacts(contacts);
    }

    @Override
    public void onDeleteNotification(final int id) {
        zeppOsSupport.onDeleteNotification(id);
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        zeppOsSupport.onSetGpsLocation(location);
    }

    @Override
    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        zeppOsSupport.onSetCannedMessages(cannedMessagesSpec);
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        zeppOsSupport.onSetPhoneVolume(volume);
    }

    @Override
    public void onSetMusicState(final MusicStateSpec stateSpec) {
        zeppOsSupport.onSetMusicState(stateSpec);
    }

    @Override
    public void onSetMusicInfo(final MusicSpec musicSpec) {
        zeppOsSupport.onSetMusicInfo(musicSpec);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        zeppOsSupport.onEnableRealtimeSteps(enable);
    }

    @Override
    public void onInstallApp(final Uri uri, @NonNull final Bundle options) {
        zeppOsSupport.onInstallApp(uri, options);
    }

    @Override
    public void onAppInfoReq() {
        zeppOsSupport.onAppInfoReq();
    }

    @Override
    public void onAppStart(final UUID uuid, final boolean start) {
        zeppOsSupport.onAppStart(uuid, start);
    }

    @Override
    public void onAppDelete(final UUID uuid) {
        zeppOsSupport.onAppDelete(uuid);
    }

    @Override
    public void onEnableHeartRateSleepSupport(final boolean enable) {
        zeppOsSupport.onEnableHeartRateSleepSupport(enable);
    }

    @Override
    public void onSetTime() {
        zeppOsSupport.onSetTime();
    }

    @Override
    public void onSendWeather() {
        zeppOsSupport.onSendWeather();
    }

    @Override
    public void onSleepAsAndroidAction(final String action, final Bundle extras) {
        zeppOsSupport.onSleepAsAndroidAction(action, extras);
    }
}
