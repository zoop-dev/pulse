package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v2;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.calcMaxWriteChunk;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.CobsCoDec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.ICommunicator;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CommunicatorV2 implements ICommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicatorV2.class);

    public static final String BASE_UUID = "6A4E%04X-667B-11E3-949A-0800200C9A66";
    public static final UUID UUID_SERVICE_GARMIN_ML_GFDI = UUID.fromString(String.format(BASE_UUID, 0x2800));

    private static final long GADGETBRIDGE_CLIENT_ID = 2L;

    private BluetoothGattCharacteristic characteristicSend;
    private BluetoothGattCharacteristic characteristicReceive;

    private final GarminSupport mSupport;

    private final Map<Integer, Service> serviceByHandle = new HashMap<>();
    private final Map<Service, Integer> handleByService = new HashMap<>();
    private final Map<Service, ServiceCallback> serviceCallbacks = new HashMap<>();

    public int maxWriteSize = 20;

    private boolean realtimeHrOneShot = false;
    private int previousSteps = -1;

    // MLR support
    private final Map<Integer, MlrCommunicator> mlrCommunicators = new HashMap<>();

    public CommunicatorV2(final GarminSupport garminSupport) {
        this.mSupport = garminSupport;
    }

    @Override
    public void onMtuChanged(final int mtu) {
        maxWriteSize = calcMaxWriteChunk(mtu);
        for (MlrCommunicator communicator : mlrCommunicators.values()) {
            communicator.setMaxPacketSize(maxWriteSize);
        }
    }

    @Override
    public boolean initializeDevice(final TransactionBuilder builder) {
        // Iterate through the known ML characteristics until we find a known pair
        // send characteristic = read characteristic + 0x10 (eg. 2810 / 2820)
        for (int i = 0x2810; i <= 0x2814; i++) {
            characteristicReceive = mSupport.getCharacteristic(UUID.fromString(String.format(BASE_UUID, i)));
            characteristicSend = mSupport.getCharacteristic(UUID.fromString(String.format(BASE_UUID, i + 0x10)));

            if (characteristicSend != null && characteristicReceive != null) {
                LOG.debug("Using characteristics receive/send = {}/{}", characteristicReceive.getUuid(), characteristicSend.getUuid());

                builder.notify(characteristicReceive, true);
                builder.write(characteristicSend, closeAllServices());

                return true;
            }
        }

        LOG.warn("Failed to find any known ML characteristics");

        return false;
    }

    @Override
    public void dispose() {
        // Close all MLR communicators
        for (MlrCommunicator mlrComm : mlrCommunicators.values()) {
            mlrComm.close();
        }
        mlrCommunicators.clear();
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        for (MlrCommunicator mlrComm : mlrCommunicators.values()) {
            mlrComm.onConnectionStateChange(gatt, status, newState);
        }
    }

    @Override
    public void sendMessage(final String taskName, final byte[] message) {
        if (null == message)
            return;
        final Integer gfdiHandle = handleByService.get(Service.GFDI);
        if (gfdiHandle == null) {
            LOG.error("CANNOT SENT GFDI MESSAGE, HANDLE NOT YET SET. MESSAGE {}", message);
            return;
        }
        final byte[] payload = CobsCoDec.encode(message);
        final MlrCommunicator mlr = mlrCommunicators.get(gfdiHandle);
        if (mlr != null) {
            mlr.sendMessage(taskName, payload);
            return;
        }
        final TransactionBuilder builder = mSupport.createTransactionBuilder(taskName);
        int remainingBytes = payload.length;
        if (remainingBytes > maxWriteSize - 1) {
            int position = 0;
            while (remainingBytes > 0) {
                final byte[] fragment = Arrays.copyOfRange(payload, position, position + Math.min(remainingBytes, maxWriteSize - 1));
                builder.write(characteristicSend, ArrayUtils.addAll(new byte[]{gfdiHandle.byteValue()}, fragment));
                position += fragment.length;
                remainingBytes -= fragment.length;
            }
        } else {
            builder.write(characteristicSend, ArrayUtils.addAll(new byte[]{gfdiHandle.byteValue()}, payload));
        }
        builder.queue();
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] value) {
        if (!characteristic.getUuid().equals(characteristicReceive.getUuid())) {
            // Not ML
            return false;
        }

        if ((value[0] & MlrCommunicator.MLR_FLAG_MASK) != 0) {
            // MLR packet - extract handle and forward to appropriate MLR communicator, but keep the mlr flag
            final int handle = ((value[0] & MlrCommunicator.HANDLE_MASK) >> 4) | MlrCommunicator.MLR_FLAG_MASK;
            final MlrCommunicator mlrComm = mlrCommunicators.get(handle);
            if (mlrComm == null) {
                LOG.warn("Received MLR packet for unknown handle: {}", handle);
                return true;
            }
            mlrComm.onPacketReceived(value);
            return true;
        }

        final ByteBuffer message = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        final byte handle = message.get();

        if (0x00 == handle) {
            processHandleManagement(message);
            return true;
        }

        final Service service = serviceByHandle.get(handle & 0xff);
        if (service != null) {
            final ServiceCallback serviceCallback = serviceCallbacks.get(service);
            if (serviceCallback != null) {
                serviceCallback.onMessage(Arrays.copyOfRange(value, 1, value.length));
            } else {
                LOG.warn("Got message for {}, but no callback found", service);
            }
        } else {
            LOG.warn("Got message for unknown service on handle {}: {}", handle, GB.hexdump(value));
        }

        return true;
    }

    public void startTransfer(final ServiceCallback callback) {
        final Service service;
        if (!handleByService.containsKey(Service.FILE_TRANSFER_2)) {
            service = Service.FILE_TRANSFER_2;
        } else if (!handleByService.containsKey(Service.FILE_TRANSFER_4)) {
            service = Service.FILE_TRANSFER_4;
        } else if (!handleByService.containsKey(Service.FILE_TRANSFER_6)) {
            service = Service.FILE_TRANSFER_6;
        } else if (!handleByService.containsKey(Service.FILE_TRANSFER_A)) {
            service = Service.FILE_TRANSFER_A;
        } else if (!handleByService.containsKey(Service.FILE_TRANSFER_C)) {
            service = Service.FILE_TRANSFER_C;
        } else if (!handleByService.containsKey(Service.FILE_TRANSFER_E)) {
            service = Service.FILE_TRANSFER_E;
        } else {
            LOG.error("No file transfer services available");
            callback.onClose();
            return;
        }

        serviceCallbacks.put(service, callback);
        mSupport.createTransactionBuilder("start file transfer")
                .write(characteristicSend, registerService(service, mSupport.mlrEnabled()))
                .queue();
    }

    @Override
    public void onHeartRateTest() {
        realtimeHrOneShot = true;
        if (!handleByService.containsKey(Service.REALTIME_HR)) {
            mSupport.createTransactionBuilder("heart rate test")
                    .write(characteristicSend, registerService(Service.REALTIME_HR, false))
                    .queue();
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        toggleService(Service.REALTIME_HR, enable);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        if (toggleService(Service.REALTIME_STEPS, enable)) {
            previousSteps = -1;
        }
    }

    private boolean toggleService(final Service service, final boolean enable) {
        final int currentHandle = Objects.requireNonNull(handleByService.getOrDefault(service, 0));
        if (enable && currentHandle == 0) {
            mSupport.createTransactionBuilder(service + " = true")
                    .write(characteristicSend, registerService(service, false))
                    .queue();
            return true;
        } else if (!enable && currentHandle != 0) {
            mSupport.createTransactionBuilder(service + " = false")
                    .write(characteristicSend, closeService(service, currentHandle))
                    .queue();
            return true;
        }

        return false;
    }

    private void processHandleManagement(final ByteBuffer message) {
        final byte type = message.get();
        final long incomingClientID = message.getLong();

        if (incomingClientID != GADGETBRIDGE_CLIENT_ID) {
            LOG.warn("Ignoring incoming message, client ID {} is not ours. Message: {}", incomingClientID, GB.hexdump(message.array()));
            return;
        }

        final RequestType requestType = RequestType.fromCode(type);
        if (null == requestType) {
            LOG.error("Unknown request type {}. Message: {}", type, message.array());
            return;
        }

        switch (requestType) {
            case REGISTER_ML_REQ:
            case CLOSE_HANDLE_REQ:
            case CLOSE_ALL_REQ:
            case UNK_REQ:
                LOG.warn("Received handle request, expecting responses. Message: {}", message.array());
                return;
            case REGISTER_ML_RESP: {
                final short registeredServiceCode = message.getShort();
                final Service registeredService = Service.fromCode(registeredServiceCode);
                final byte status = message.get();
                if (registeredService == null) {
                    LOG.error("Got register response status={} for unknown service {}", status, registeredServiceCode);
                    return;
                }
                if (status != 0) {
                    LOG.warn("Failed to register {}, status={}", registeredService, status);
                    return;
                }
                final int handle = message.get() & 0xff;
                final int reliable = message.get();
                LOG.debug("Got register response for {}, handle={}, reliable={}", registeredService, handle, reliable);

                serviceByHandle.put(handle, registeredService);
                handleByService.put(registeredService, handle);

                final ServiceCallback serviceCallback = switch (registeredService) {
                    case GFDI:
                        yield new GfdiCallback(mSupport);
                    case REALTIME_HR:
                        yield new RealtimeHeartRateCallback();
                    case REALTIME_STEPS:
                        yield new RealtimeStepsCallback();
                    case REALTIME_ACCELEROMETER:
                        yield new RealtimeAccelerometerCallback();
                    case REALTIME_SPO2:
                        yield new RealtimeSpo2Callback();
                    case REALTIME_RESPIRATION:
                        yield new RealtimeRespirationCallback();
                    case REALTIME_HRV:
                        yield new RealtimeHrvCallback();
                    case FILE_TRANSFER_2:
                    case FILE_TRANSFER_4:
                    case FILE_TRANSFER_6:
                    case FILE_TRANSFER_A:
                    case FILE_TRANSFER_C:
                    case FILE_TRANSFER_E:
                        // For these, the callback should have been provided by the caller in startTransfer
                        yield serviceCallbacks.get(registeredService);
                    default:
                        LOG.error("Got register response for unknown service {}", registeredService);
                        yield null;
                };

                if (serviceCallback == null) {
                    LOG.error("Got service registration, but got no callback");
                    closeService(registeredService, handle);
                    return;
                }

                serviceCallbacks.put(registeredService, serviceCallback);

                if (reliable != 0) {
                    // MLR mode - create reliable communicator
                    final MlrCommunicator mlrComm = createMlrCommunicator(handle, serviceCallback);
                    mlrCommunicators.put(handle, mlrComm);
                    serviceCallback.onConnect(new MlrServiceWriter(mlrComm));
                } else {
                    // Regular ML mode
                    serviceCallback.onConnect(new MlServiceWriter(handle));
                }
                break;
            }
            case CLOSE_HANDLE_RESP: {
                final short serviceCode = message.getShort();
                final Service service = Service.fromCode(serviceCode);
                final int handle = message.get();
                final byte status = message.get();
                LOG.debug("Received close handle response: service={}, handle={}, status={}", service, handle, status);
                if (service != null) {
                    final ServiceCallback serviceCallback = serviceCallbacks.get(service);
                    if (serviceCallback == null) {
                        LOG.error("Got service registration close, but got no callback");
                    } else {
                        serviceCallback.onClose();
                    }
                    // Clean up MLR communicator if it exists
                    final MlrCommunicator mlrComm = mlrCommunicators.get(handle);
                    if (mlrComm != null) {
                        mlrComm.close();
                        mlrCommunicators.remove(handle);
                    }

                    handleByService.remove(service);
                    serviceCallbacks.remove(service);
                }

                serviceByHandle.remove(handle);

                break;
            }
            case CLOSE_ALL_RESP:
                LOG.debug("Received close all handles response. Message: {}", message.array());
                serviceByHandle.clear();
                handleByService.clear();
                for (ServiceCallback callback : serviceCallbacks.values()) {
                    callback.onClose();
                }
                serviceCallbacks.clear();
                mSupport.createTransactionBuilder("open GFDI")
                        .write(characteristicSend, registerService(Service.GFDI, mSupport.mlrEnabled()))
                        .queue();
                break;
            case UNK_RESP:
                LOG.debug("Received unknown. Message: {}", message.array());
                break;
        }
    }

    private static class GfdiCallback implements ServiceCallback {
        private final CobsCoDec cobsCoDec = new CobsCoDec();
        private final GarminSupport mSupport;

        private GfdiCallback(final GarminSupport support) {
            this.mSupport = support;
        }

        @Override
        public void onMessage(final byte[] value) {
            this.cobsCoDec.receivedBytes(value);
            this.mSupport.onMessage(this.cobsCoDec.retrieveMessage());
        }
    }

    private class RealtimeHeartRateCallback implements ServiceCallback {
        @Override
        public void onMessage(final byte[] value) {
            final byte type = value[0]; // 0/2/3? 3 == realtime?
            final int hr = value[0] & 0xff;
            final int resting = value[0] & 0xff;
            // ff ff after
            LOG.debug("Got realtime HR: type={} hr={} resting={}", type, hr, resting);

            if (hr > 0) {
                broadcastRealtimeActivity(hr, -1);

                if (realtimeHrOneShot && handleByService.containsKey(Service.REALTIME_HR)) {
                    onEnableRealtimeHeartRateMeasurement(false);
                }
            }
        }
    }

    private class RealtimeStepsCallback implements ServiceCallback {
        @Override
        public void onMessage(final byte[] value) {
            final int steps = BLETypeConversions.toUint32(value, 0);
            final int goal = BLETypeConversions.toUint32(value, 4);
            LOG.debug("Got realtime steps: steps={} goal={}", steps, goal);

            if (previousSteps == -1) {
                previousSteps = steps;
            }

            broadcastRealtimeActivity(-1, steps - previousSteps);

            previousSteps = steps;
        }
    }

    private static class RealtimeAccelerometerCallback implements ServiceCallback {
        @Override
        public void onConnect(final ServiceWriter writer) {
            writer.write("start realtime accel", new byte[]{0x01});
        }

        @Override
        public void onMessage(final byte[] value) {
            LOG.debug("Got realtime accel: {}", GB.hexdump(value));
        }
    }

    private static class RealtimeSpo2Callback implements ServiceCallback {
        @Override
        public void onMessage(final byte[] value) {
            final int spo2 = value[0]; // -1 when unknown, and the ts is not valid in that case
            final int garminTs = BLETypeConversions.toUint32(value, 1);

            LOG.debug("Got realtime SpO2 at {}: {}", new Date(GarminTimeUtils.garminTimestampToJavaMillis(garminTs)), spo2);
        }
    }

    private static class RealtimeRespirationCallback implements ServiceCallback {
        @Override
        public void onMessage(final byte[] value) {
            final int breathsPerMinute = value[0]; // can be negative if unknown, usually -2

            LOG.debug("Got realtime respiration: {}", breathsPerMinute);
        }
    }

    private static class RealtimeHrvCallback implements ServiceCallback {
        @Override
        public void onMessage(final byte[] value) {
            final int rr = BLETypeConversions.toUint16(value, 0);
            final int unk = BLETypeConversions.toUint32(value, 2);
            LOG.debug("Got realtime HRV: rr={}, unk={}", rr, unk);
        }
    }

    private byte[] closeAllServices() {
        final ByteBuffer toSend = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
        toSend.put((byte) 0); // handle
        toSend.put((byte) RequestType.CLOSE_ALL_REQ.ordinal());
        toSend.putLong(GADGETBRIDGE_CLIENT_ID);
        toSend.putShort((short) 0);
        return toSend.array();
    }

    private byte[] registerService(final Service service, final boolean reliable) {
        final ByteBuffer toSend = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
        toSend.put((byte) 0);
        toSend.put((byte) RequestType.REGISTER_ML_REQ.ordinal());
        toSend.putLong(GADGETBRIDGE_CLIENT_ID);
        toSend.putShort(service.getCode());
        toSend.put((byte) (reliable ? 2 : 0));
        return toSend.array();
    }

    private byte[] closeService(final Service service, final int handle) {
        final ByteBuffer toSend = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
        toSend.put((byte) 0);
        toSend.put((byte) RequestType.CLOSE_HANDLE_REQ.ordinal());
        toSend.putLong(GADGETBRIDGE_CLIENT_ID);
        toSend.putShort(service.getCode());
        toSend.put((byte) handle);
        return toSend.array();
    }

    private void broadcastRealtimeActivity(final int hr, final int steps) {
        final GarminActivitySample sample;
        try (final DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();

            final GBDevice gbDevice = mSupport.getDevice();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);
            final GarminActivitySampleProvider provider = new GarminActivitySampleProvider(gbDevice, session);
            sample = provider.createActivitySample();

            sample.setDeviceId(device.getId());
            sample.setUserId(user.getId());
            sample.setTimestamp((int) (System.currentTimeMillis() / 1000));
            sample.setHeartRate(hr);
            sample.setSteps(steps);
            sample.setRawKind(ActivityKind.UNKNOWN.getCode());
            sample.setRawIntensity(ActivitySample.NOT_MEASURED);
            sample.setRawKind(ActivityKind.UNKNOWN.getCode());
        } catch (final Exception e) {
            LOG.error("Error creating activity sample", e);
            return;
        }

        final Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(GBDevice.EXTRA_DEVICE, mSupport.getDevice())
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
        LocalBroadcastManager.getInstance(mSupport.getContext()).sendBroadcast(intent);
    }

    private enum RequestType {
        REGISTER_ML_REQ,
        REGISTER_ML_RESP,
        CLOSE_HANDLE_REQ,
        CLOSE_HANDLE_RESP,
        UNK_HANDLE,
        CLOSE_ALL_REQ,
        CLOSE_ALL_RESP,
        UNK_REQ,
        UNK_RESP;

        @Nullable
        public static RequestType fromCode(final int code) {
            for (final RequestType requestType : RequestType.values()) {
                if (requestType.ordinal() == code) {
                    return requestType;
                }
            }

            return null;
        }
    }

    private enum Service {
        GFDI(1),
        REGISTRATION(4),
        REALTIME_HR(6),
        REALTIME_STEPS(7),
        REALTIME_CALORIES(8),
        REALTIME_INTENSITY(10),
        REALTIME_HRV(12),
        REALTIME_STRESS(13),
        REALTIME_ACCELEROMETER(16),
        REALTIME_SPO2(19),
        REALTIME_BODY_BATTERY(20),
        REALTIME_RESPIRATION(21),
        FILE_TRANSFER_2(0x2018),
        FILE_TRANSFER_4(0x4018),
        FILE_TRANSFER_6(0x6018),
        FILE_TRANSFER_A(0xa018),
        FILE_TRANSFER_C(0xc018),
        FILE_TRANSFER_E(0xe018),
        ;

        private final short code;

        Service(final int code) {
            this.code = (short) code;
        }

        public short getCode() {
            return code;
        }

        @Nullable
        public static Service fromCode(final int code) {
            for (final Service service : Service.values()) {
                if (service.code == code) {
                    return service;
                }
            }

            return null;
        }
    }

    public interface ServiceCallback {
        default void onConnect(ServiceWriter writer) {

        }

        default void onClose() {

        }

        void onMessage(byte[] value);
    }

    public interface ServiceWriter {
        void write(String taskName, byte[] value);
    }

    public class MlServiceWriter implements ServiceWriter {
        private final int handle;

        private MlServiceWriter(final int handle) {
            this.handle = handle;
        }

        public void write(final String taskName, final byte[] value) {
            final ByteBuffer buf = ByteBuffer.allocate(value.length + 1);
            buf.put((byte) handle);
            buf.put(value);
            mSupport.createTransactionBuilder(taskName)
                    .write(characteristicSend, buf.array())
                    .queue();
        }
    }

    private MlrCommunicator createMlrCommunicator(final int handle, final ServiceCallback callback) {
        final MlrCommunicator.MessageSender messageSender = (taskName, packet) -> mSupport.createTransactionBuilder(taskName)
                .write(characteristicSend, packet)
                .queue();

        return new MlrCommunicator(handle, maxWriteSize, messageSender, callback::onMessage);
    }

    public static class MlrServiceWriter implements ServiceWriter {
        private final MlrCommunicator mlrComm;

        private MlrServiceWriter(final MlrCommunicator mlrComm) {
            this.mlrComm = mlrComm;
        }

        public void write(final String taskName, final byte[] value) {
            mlrComm.sendMessage(taskName, value);
        }
    }
}
