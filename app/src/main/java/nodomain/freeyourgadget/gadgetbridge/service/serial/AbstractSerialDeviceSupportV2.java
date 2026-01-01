package nodomain.freeyourgadget.gadgetbridge.service.serial;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.Location;
import android.os.ParcelUuid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.AbstractBTBRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;

/**
 * This class serves only to help existing implementations transition from AbstractSerialDeviceSupport,
 * and should not be used for new implementations.
 *
 * @deprecated Use {@link nodomain.freeyourgadget.gadgetbridge.service.btbr.AbstractBTBRDeviceSupport}
 */
@Deprecated
public abstract class AbstractSerialDeviceSupportV2<T extends GBDeviceProtocol> extends AbstractBTBRDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSerialDeviceSupportV2.class);

    protected T mDeviceProtocol;

    public AbstractSerialDeviceSupportV2() {
        this(2048);
    }

    public AbstractSerialDeviceSupportV2(final int bufferSize) {
        super(LOG, bufferSize);
    }

    protected abstract T createDeviceProtocol();

    @Override
    protected UUID getSupportedService() {
        // Preserve the behavior from legacy AbstractSerialDeviceSupport, and return the first UUID
        final UUID supportedService = super.getSupportedService();
        if (supportedService != null) {
            return supportedService;
        }
        final ParcelUuid[] bluetoothDeviceUuids = getBluetoothDeviceUuids();
        if (bluetoothDeviceUuids != null && bluetoothDeviceUuids.length > 0) {
            return bluetoothDeviceUuids[0].getUuid();
        }
        return null;
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void setContext(final GBDevice gbDevice, final BluetoothAdapter btAdapter, final Context context) {
        super.setContext(gbDevice, btAdapter, context);
        this.mDeviceProtocol = createDeviceProtocol();
    }

    @Override
    public void onSocketRead(final byte[] data) {
        final GBDeviceEvent[] deviceEvents = mDeviceProtocol.decodeResponse(data);
        if (deviceEvents == null) {
            LOG.debug("unhandled message");
            return;
        }

        for (GBDeviceEvent deviceEvent : deviceEvents) {
            if (deviceEvent == null) {
                continue;
            }
            evaluateGBDeviceEvent(deviceEvent);
        }
    }

    private void sendToDevice(final byte[] bytes) {
        if (bytes != null) {
            final TransactionBuilder builder = createTransactionBuilder("send bytes");
            builder.write(bytes);
            builder.queue();
        }
    }

    private void handleGBDeviceEvent(final GBDeviceEventSendBytes sendBytes) {
        sendToDevice(sendBytes.encodedBytes);
    }

    @Override
    public void evaluateGBDeviceEvent(final GBDeviceEvent deviceEvent) {
        if (deviceEvent instanceof GBDeviceEventSendBytes) {
            handleGBDeviceEvent((GBDeviceEventSendBytes) deviceEvent);
            return;
        }
        super.evaluateGBDeviceEvent(deviceEvent);
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        byte[] bytes = mDeviceProtocol.encodeNotification(notificationSpec);
        sendToDevice(bytes);
    }

    @Override
    public void onDeleteNotification(final int id) {
        byte[] bytes = mDeviceProtocol.encodeDeleteNotification(id);
        sendToDevice(bytes);
    }

    @Override
    public void onSetTime() {
        byte[] bytes = mDeviceProtocol.encodeSetTime();
        sendToDevice(bytes);
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        byte[] bytes = mDeviceProtocol.encodeSetCallState(callSpec.number, callSpec.name, callSpec.command);
        sendToDevice(bytes);
    }

    @Override
    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        byte[] bytes = mDeviceProtocol.encodeSetCannedMessages(cannedMessagesSpec);
        sendToDevice(bytes);
    }

    @Override
    public void onSetMusicState(final MusicStateSpec stateSpec) {
        byte[] bytes = mDeviceProtocol.encodeSetMusicState(stateSpec.state, stateSpec.position, stateSpec.playRate, stateSpec.shuffle, stateSpec.repeat);
        sendToDevice(bytes);
    }

    @Override
    public void onSetMusicInfo(final MusicSpec musicSpec) {
        byte[] bytes = mDeviceProtocol.encodeSetMusicInfo(musicSpec.artist, musicSpec.album, musicSpec.track, musicSpec.duration, musicSpec.trackCount, musicSpec.trackNr);
        sendToDevice(bytes);
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        byte[] bytes = mDeviceProtocol.encodeVolume(volume);
        sendToDevice(bytes);
    }

    @Override
    public void onAppInfoReq() {
        byte[] bytes = mDeviceProtocol.encodeAppInfoReq();
        sendToDevice(bytes);
    }

    @Override
    public void onAppStart(final UUID uuid, final boolean start) {
        byte[] bytes = mDeviceProtocol.encodeAppStart(uuid, start);
        sendToDevice(bytes);
    }

    @Override
    public void onAppDelete(final UUID uuid) {
        byte[] bytes = mDeviceProtocol.encodeAppDelete(uuid);
        sendToDevice(bytes);
    }

    @Override
    public void onAppReorder(final UUID[] uuids) {
        byte[] bytes = mDeviceProtocol.encodeAppReorder(uuids);
        sendToDevice(bytes);
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        byte[] bytes = mDeviceProtocol.encodeFetchRecordedData(dataTypes);
        sendToDevice(bytes);
    }

    @Override
    public void onReset(final int flags) {
        byte[] bytes = mDeviceProtocol.encodeReset(flags);
        sendToDevice(bytes);
    }

    @Override
    public void onFindDevice(final boolean start) {
        byte[] bytes = mDeviceProtocol.encodeFindDevice(start);
        sendToDevice(bytes);
    }

    @Override
    public void onFindPhone(final boolean start) {
        byte[] bytes = mDeviceProtocol.encodeFindPhone(start);
        sendToDevice(bytes);
    }

    @Override
    public void onScreenshotReq() {
        byte[] bytes = mDeviceProtocol.encodeScreenshotReq();
        sendToDevice(bytes);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        byte[] bytes = mDeviceProtocol.encodeEnableRealtimeSteps(enable);
        sendToDevice(bytes);
    }

    @Override
    public void onEnableHeartRateSleepSupport(final boolean enable) {
        byte[] bytes = mDeviceProtocol.encodeEnableHeartRateSleepSupport(enable);
        sendToDevice(bytes);
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        byte[] bytes = mDeviceProtocol.encodeEnableRealtimeHeartRateMeasurement(enable);
        sendToDevice(bytes);
    }

    @Override
    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        byte[] bytes = mDeviceProtocol.encodeAddCalendarEvent(calendarEventSpec);
        sendToDevice(bytes);
    }

    @Override
    public void onDeleteCalendarEvent(final byte type, final long id) {
        byte[] bytes = mDeviceProtocol.encodeDeleteCalendarEvent(type, id);
        sendToDevice(bytes);
    }

    @Override
    public void onSendConfiguration(final String config) {
        byte[] bytes = mDeviceProtocol.encodeSendConfiguration(config);
        sendToDevice(bytes);
    }

    @Override
    public void onTestNewFunction() {
        byte[] bytes = mDeviceProtocol.encodeTestNewFunction();
        sendToDevice(bytes);
    }

    @Override
    public void onSendWeather() {
        byte[] bytes = mDeviceProtocol.encodeSendWeather();
        sendToDevice(bytes);
    }

    @Override
    public void onSetFmFrequency(final float frequency) {
        byte[] bytes = mDeviceProtocol.encodeFmFrequency(frequency);
        sendToDevice(bytes);
    }

    @Override
    public void onSetLedColor(final int color) {
        byte[] bytes = mDeviceProtocol.encodeLedColor(color);
        sendToDevice(bytes);
    }

    @Override
    public void onPowerOff() {
        byte[] bytes = mDeviceProtocol.encodePowerOff();
        sendToDevice(bytes);
    }

    @Override
    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        byte[] bytes = mDeviceProtocol.encodeSetAlarms(alarms);
        sendToDevice(bytes);
    }

    @Override
    public void onSetReminders(final ArrayList<? extends Reminder> reminders) {
        byte[] bytes = mDeviceProtocol.encodeReminders(reminders);
        sendToDevice(bytes);
    }

    @Override
    public void onSetWorldClocks(final ArrayList<? extends WorldClock> clocks) {
        byte[] bytes = mDeviceProtocol.encodeWorldClocks(clocks);
        sendToDevice(bytes);
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        byte[] bytes = mDeviceProtocol.encodeGpsLocation(location);
        sendToDevice(bytes);
    }
}
