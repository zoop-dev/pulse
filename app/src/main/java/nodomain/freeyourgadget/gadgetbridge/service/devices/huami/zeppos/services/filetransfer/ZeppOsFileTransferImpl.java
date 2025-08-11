package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.filetransfer;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.zip.Deflater;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFileTransferService;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public abstract class ZeppOsFileTransferImpl {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFileTransferImpl.class);

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    protected static final byte CMD_TRANSFER_REQUEST = 0x03;
    protected static final byte CMD_TRANSFER_RESPONSE = 0x04;
    protected static final byte CMD_DATA_SEND = 0x10;
    protected static final byte CMD_DATA_ACK = 0x11;
    protected static final byte CMD_DATA_V3_SEND = 0x12;
    protected static final byte CMD_DATA_V3_ACK = 0x13;

    protected static final byte FLAG_FIRST_CHUNK = 0x01;
    protected static final byte FLAG_LAST_CHUNK = 0x02;
    protected static final byte FLAG_CRC = 0x04;

    public static final String PREF_SUPPORTED_SERVICES = "zepp_os_file_transfer_supported_service";

    protected final ZeppOsFileTransferService mFileTransferService;
    protected final ZeppOsSupport mSupport;

    protected int mVersion = -1;
    protected int mChunkSize = -1;
    protected int mCompressedChunkSize = -1;
    protected final List<String> supportedServices = new ArrayList<>();

    public ZeppOsFileTransferImpl(final ZeppOsFileTransferService fileTransferService,
                                  final ZeppOsSupport support) {
        this.mFileTransferService = fileTransferService;
        this.mSupport = support;
    }

    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                handleCapabilitiesResponse(payload);
                return;
            case CMD_TRANSFER_REQUEST:
                handleFileTransferRequest(payload);
                return;
            default:
                LOG.warn("Unexpected file transfer payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    public abstract void uploadFile(final FileTransferRequest request);

    public abstract void handleFileDownloadRequest(final byte session, final FileTransferRequest request);

    public abstract void onCharacteristicChanged(final UUID characteristicUUID, final byte[] value);

    public void uploadFile(final String url,
                           final String filename,
                           final byte[] bytes,
                           final boolean compress,
                           final ZeppOsFileTransferService.Callback callback) {
        LOG.info("Sending {} bytes to {} in {}", bytes.length, filename, url);

        final FileTransferRequest request = new FileTransferRequest(
                url,
                filename,
                bytes.length,
                compress && mCompressedChunkSize > 0 ? compress(bytes) : bytes,
                compress && mCompressedChunkSize > 0,
                CheckSums.getCRC32(bytes),
                compress && mCompressedChunkSize > 0 ? mCompressedChunkSize : mChunkSize,
                callback
        );

        uploadFile(request);
    }

    private void handleCapabilitiesResponse(final byte[] payload) {
        supportedServices.clear();

        final ByteBuffer buf = ByteBuffer.wrap(payload)
                .order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // discard command byte

        mVersion = buf.get() & 0xff;
        mChunkSize = buf.getShort() & 0xffff;
        if (mVersion >= 3) {
            mCompressedChunkSize = buf.getInt();
            final int numServices = buf.getShort();
            for (int i = 0; i < numServices; i++) {
                // gtr 4:    terminal agps notification jsapp sticky_notification nfc sport httpproxy
                // active 2: terminal agps notification jsapp sticky_notification nfc sport httpproxy readiness voicememo
                supportedServices.add(StringUtils.untilNullTerminator(buf));
            }

            // TODO: 3 unknown bytes for v3

            final ZeppOsTransactionBuilder builder = mSupport.createZeppOsTransactionBuilder("enable file transfer v3 notifications");
            builder.notify(HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3_SEND, true);
            builder.notify(HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3_RECEIVE, true);
            builder.queue();
        }

        mSupport.evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_SUPPORTED_SERVICES, new HashSet<>(supportedServices)));

        LOG.info(
                "Got file transfer service: version={}, chunkSize={}, compressedChunkSize={}, supportedServices=[{}]",
                mVersion,
                mChunkSize,
                mCompressedChunkSize,
                String.join(",", supportedServices)
        );
    }

    private void handleFileTransferRequest(final byte[] payload) {
        // File transfer request initialized from watch
        final ByteBuffer buf = ByteBuffer.wrap(payload)
                .order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // discard command byte

        final byte session = buf.get();
        final String url = StringUtils.untilNullTerminator(buf);
        if (url == null) {
            LOG.error("Unable to parse url from transfer request");
            return;
        }
        final String filename = StringUtils.untilNullTerminator(buf);
        if (filename == null) {
            LOG.error("Unable to parse filename from transfer request");
            return;
        }
        final int length = buf.getInt();
        final int crc32 = buf.getInt();

        final boolean compressed;
        if (buf.hasRemaining()) {
            final byte compressedByte = buf.get();
            final Boolean compressedBoolean = booleanFromByte(compressedByte);
            if (compressedBoolean == null) {
                LOG.warn("Unknown compression type {}", String.format("0x%02x", compressedByte));
                return;
            }
            compressed = compressedBoolean;
        } else {
            compressed = false;
        }

        LOG.info(
                "Got transfer request: session={}, url={}, filename={}, length={}, compressed={}",
                session,
                url,
                filename,
                length,
                compressed
        );

        final FileTransferRequest request = new FileTransferRequest(
                url,
                filename,
                length,
                new byte[length],
                compressed,
                crc32,
                compressed ? mCompressedChunkSize : mChunkSize,
                mSupport
        );

        handleFileDownloadRequest(session, request);
    }

    public static byte[] compress(final byte[] data) {
        final Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        final byte[] buf = new byte[8096];
        int read;
        while ((read = deflater.deflate(buf)) > 0) {
            baos.write(buf, 0, read);
        }

        return baos.toByteArray();
    }

    @Nullable
    protected static Boolean booleanFromByte(final byte b) {
        return switch (b) {
            case 0x00 -> false;
            case 0x01 -> true;
            default -> null;
        };
    }
}
