package nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GenericThermalPrinterSupport extends AbstractBTLESingleDeviceSupport {
    public static final int IMAGE_WIDTH = 384;
    public static final String INTENT_EXTRA_BITMAP_CACHE_FILE_PATH = "bitmap_cache_file_path";
    public static final String INTENT_EXTRA_APPLY_DITHERING = "apply_dithering";
    public static final String INTENT_EXTRA_UPSCALE = "upscale";
    public static final String INTENT_EXTRA_ALIGNMENT = "alignment";
    public static final UUID discoveryService = UUID.fromString("0000af30-0000-1000-8000-00805f9b34fb");
    private static final Logger LOG = LoggerFactory.getLogger(GenericThermalPrinterSupport.class);
    private final UUID writeCharUUID = UUID.fromString("0000ae01-0000-1000-8000-00805f9b34fb");
    private final UUID notifCharUUID = UUID.fromString("0000ae02-0000-1000-8000-00805f9b34fb");
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable batteryRunner = () -> {
        LOG.info("Running retrieving battery through runner.");
        send("getDevState", PrinterCommand.getDevState.message(new byte[]{0x00}));
    };
    private final int PRINT_INTENSITY = 8000;
    private final int PRINT_SPEED = 10;
    private final int PRINT_TYPE = 0; //1 also observed
    private boolean useRunLengthEncoding = false;
    private boolean canPrint = false;

    public GenericThermalPrinterSupport() {
        super(LOG);
        addSupportedService(UUID.fromString("0000ae30-0000-1000-8000-00805f9b34fb"));

    }

    private static Bitmap createBitmapFromString(String text) {
        float textSize = 24f; // Approx 10pt at 200 dpi

        // Prepare the paint for drawing text
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF000000); // Black color
        textPaint.setTextSize(textSize);
        textPaint.setTypeface(Typeface.DEFAULT);

        // Create StaticLayout to handle text wrapping
        StaticLayout staticLayout = new StaticLayout(text, textPaint, IMAGE_WIDTH,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f, 0.0f, false);

        // Calculate required height for the bitmap
        int bitmapHeight = staticLayout.getHeight();

        // Create the bitmap
        final Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw the text onto the canvas using the StaticLayout
        staticLayout.draw(canvas);

        return bitmap;
    }

    private static byte[] byteBufferToArray(final ByteBuffer input) {
        input.flip();
        final byte[] result = new byte[input.limit()];
        input.get(result);
        return result;
    }

    private static int getCrc8(final byte[] seq) {
        int crc = 0x00;

        for (byte b : seq) {
            crc ^= (b & 0xFF);

            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc = (crc << 1) ^ 0x07;
                } else {
                    crc <<= 1;
                }
            }
        }

        return (byte) (crc & 0xFF);
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        if(notificationSpec.type.equals(NotificationType.GADGETBRIDGE_TEXT_RECEIVER)) {
            final SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
            if(!prefs.getBoolean("pref_printer_print_received_text", true)) {
                LOG.info("Not printing received text, device preference forbids this.");
                return;
            }

            //title is appName
            //body is the shared text
            // see TextReceiverActivity.java
            printImage(createBitmapFromString(notificationSpec.body), false, PrintAlignment.ALIGN_LEFT);
        }
//        printImage(createNotificationBitmap(notificationSpec), true, PrintAlignment.ALIGN_LEFT);
    }

    public Bitmap createNotificationBitmap(NotificationSpec spec) {

        StringBuilder textBuilder = new StringBuilder();
        if (spec.sender != null) {
            textBuilder.append("Sender: ").append(spec.sender).append("\n");
        }
        if (spec.title != null) {
            textBuilder.append("Title: ").append(spec.title).append("\n");
        }
        if (spec.subject != null) {
            textBuilder.append("Subject: ").append(spec.subject).append("\n\n");
        }
        if (spec.body != null) {
            textBuilder.append(spec.body);
        }

        return createBitmapFromString(textBuilder.toString());
    }

    public void printImage(Bitmap bitmap, boolean applyDithering, PrintAlignment alignment) {
        if (!canPrint) {
            LOG.error("Printer cannot print.");
            return;
        }

        final BitmapToBitSet bitmapToBitSet = new BitmapToBitSet(createAlignedBitmap(bitmap, alignment));

        final ByteBuffer result = ByteBuffer.allocate(500 + (int) (Math.ceil(bitmap.getWidth() / 8) + 8) * bitmap.getHeight()); //TODO: approximate better?

        result.put(PrinterCommand.intensity(PRINT_INTENSITY));
        result.put(PrinterCommand.printSpeed.message(new byte[]{PRINT_SPEED}));
        result.put(PrinterCommand.printType.message(new byte[]{PRINT_TYPE}));
        result.put(PrinterCommand.quality(5));

        result.put(encodePictureToPrinterCommands(bitmapToBitSet.toBlackAndWhite(applyDithering), bitmapToBitSet.getWidth(), bitmapToBitSet.getHeight()));
        result.put(PrinterCommand.feedPaper(72));

        result.put(PrinterCommand.getDevState.message(new byte[]{0})); //to get back the current device status

        send("Print...", byteBufferToArray(result));
    }

    private byte[] encodePictureToPrinterCommands(BitSet imageBits, int imageWidth, int imageHeight) {
        final int maxOctetsPerRow = (imageWidth + 7) / 8; //always round up
        final ByteBuffer result = ByteBuffer.allocate(imageBits.length() + 1000);

        for (int row = 0; row < imageHeight; row++) {
            boolean rowRLE = useRunLengthEncoding;
            final ByteBuffer rowContent = ByteBuffer.allocate(maxOctetsPerRow);
            final BitSet rowBits = imageBits.get(row * imageWidth, row * imageWidth + imageWidth);
            if (useRunLengthEncoding) {
                try {
                    rowContent.put(EncodingUtils.encodeRowRLE(rowBits, maxOctetsPerRow));
                } catch (BufferOverflowException e) {
                    //if compressed data is bigger than uncompressed, we use uncompressed
                    rowRLE = false;
                    rowContent.clear();
                    rowContent.put(EncodingUtils.encodeRowPlain(rowBits));
                }
            } else {
                rowContent.put(EncodingUtils.encodeRowPlain(rowBits));
            }

            result.put(rowRLE ? PrinterCommand.printRowRLE.message(byteBufferToArray(rowContent)) :
                    PrinterCommand.printRowPlain.message(byteBufferToArray(rowContent))
            );
        }
        return byteBufferToArray(result);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    public static Bitmap createAlignedBitmap(Bitmap bitmap, PrintAlignment alignment) {
        if (bitmap.getWidth() >= IMAGE_WIDTH) {
            return bitmap;
        } else {//handle alignment
            Bitmap alignedBitmap = Bitmap.createBitmap(IMAGE_WIDTH, bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(alignedBitmap);
            canvas.drawColor(Color.WHITE);

            switch (alignment) {
                case ALIGN_LEFT -> canvas.drawBitmap(bitmap, 0, 0, new Paint());
                case ALIGN_CENTER ->
                        canvas.drawBitmap(bitmap, (alignedBitmap.getWidth() - bitmap.getWidth()) / 2, 0, new Paint());
                case ALIGN_RIGHT ->
                        canvas.drawBitmap(bitmap, alignedBitmap.getWidth() - bitmap.getWidth(), 0, new Paint());
            }
            return alignedBitmap;
        }
    }

    private void sendTestPrint() {
        printImage(createBitmapFromString(ZonedDateTime.now().toString()), false, PrintAlignment.ALIGN_CENTER);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }
        ByteBuffer data = ByteBuffer.wrap(value);

        if (data.get() != PrinterCommand.preamble[0] || data.get() != PrinterCommand.preamble[1]) {
            LOG.error("Incoming message does not start with preamble: {}", data.array());
            return true;
        }

        final int cmdId = data.get();
        PrinterCommand command = PrinterCommand.fromId(cmdId);
        if (command == null) {
            LOG.error("Unknown incoming command{} in message: {}", cmdId, GB.hexdump(data.array()));
            return true;
        }
        int response = data.get();
        if (0x01 != response) {
            LOG.error("Incoming message is not a response: {}", GB.hexdump(data.array()));
            return true;
        }
        int payloadLength = data.get();
        if (data.remaining() < payloadLength + 3) {
            LOG.error("Incoming message is incomplete: {}", GB.hexdump(data.array()));
            return true;
        }
        data.get();
        byte[] payload = new byte[payloadLength];
        data.get(payload);
        int checksum = data.get();
        if (checksum != getCrc8(payload)) {
            LOG.error("Incoming message CRC error does not match: {}", GB.hexdump(data.array()));
            return true;
        }
        if (((byte) 0xff) != data.get()) {
            LOG.error("Incoming message does not have correct terminator: {}", GB.hexdump(data.array()));
            return true;
        }

        LOG.debug("Incoming message: {}", GB.hexdump(data.array()));
        final ByteBuffer payloadBB = ByteBuffer.wrap(payload);
        switch (command) {
            case flowControl:
                //5178AE010100 10 70FF -> buffer full
                //5178AE010100 00 00FF -> buffer empty
                final int code = payloadBB.get();
                if (code == 0x10) {
                    LOG.info("Printer buffer is full, will stop the queue");
                    getQueue().setPaused(true);
                } else if (code == 0x00) {
                    LOG.info("Printer buffer is empty, will resume the queue");
                    getQueue().setPaused(false);
                }

                break;
            case getDevInfo:
                if (payloadBB.get() != 0) {
                    LOG.info("Setting the device supports Run Length Encoding");
                    this.useRunLengthEncoding = true;
                } else {
                    LOG.info("Device does not support Run Length Encoding");
                }
                payloadBB.get();//unk
                payloadBB.get();//unk
                byte[] version = new byte[payloadBB.remaining()];
                payloadBB.get(version);
                String versionString = new String(version);
                LOG.info("Device version(??): {}", versionString);
                return true;
            case getDevState:
                //5178A3010300 000E28 0EFF observed values
                //5178A3010300 000226 D8FF
                //5178A3010300 000526 B3FF
                //5178A3010300 000C25 07FF
                //5178A3010300 000D24 15FF
                //5178A3010300 080D23 51FF (here starts blinking red)
                int status = payloadBB.get();
                if (status == 0) {
                    LOG.info("Enabling print");
                    canPrint = true;
                } else if (status == 1) {
                    GB.toast("Printer out of paper", Toast.LENGTH_LONG, GB.ERROR);
                    canPrint = false;
                } else if (status == 8) {
                    GB.toast("Printer battery almost empty", Toast.LENGTH_LONG, GB.ERROR);
                    canPrint = true;
                } else {
                    LOG.warn("Status != 0, disabling print functionality. Status: {}", status);
                    canPrint = false;
                }
                if (payloadBB.hasRemaining()) {
                    int unk = payloadBB.get();
                    int battery = payloadBB.get(); //possibly

                    final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
                    batteryCmd.level = ((short) battery - 35) * 18 + 10;
                    handleGBDeviceEvent(batteryCmd);
                }
                return true;
            default:
                LOG.info("Incoming message: {}", GB.hexdump(data.array()));
        }
        return true;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        builder.setCallback(this);
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");
        builder.requestMtu(512);
        builder.notify(notifCharUUID, true);

        builder.write(writeCharUUID, PrinterCommand.getDevInfo.message(new byte[]{0x00}));

        builder.write(writeCharUUID, PrinterCommand.getDevState.message(new byte[]{0x00}));

        builder.setDeviceState(GBDevice.State.INITIALIZED);

        LOG.debug("Connected to: {}", gbDevice.getName());

        return builder;
    }

    public void send(String taskname, byte[] command) {
        TransactionBuilder builder = createTransactionBuilder(taskname);
        builder.writeChunkedData(getCharacteristic(writeCharUUID), command, getMTU() - 3);
        builder.queue();
    }


    @Override
    public void onTestNewFunction() {
//        send(PrinterCommand.feedPaper.message(new byte[]{1}));
        sendTestPrint();
    }

    @Override
    public void onSendConfiguration(String config) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_ENABLE:
                if (!GBApplication.getDevicePrefs(gbDevice).getBatteryPollingEnabled()) {
                    stopBatteryRunnerDelayed();
                    break;
                }
                // Fall through if enabled
            case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_INTERVAL:
                if (!startBatteryRunnerDelayed()) {
                    GB.toast(getContext(), R.string.battery_polling_failed_start, Toast.LENGTH_SHORT, GB.ERROR);
                    LOG.error("Failed to start the battery polling");
                }
                break;
        }
    }

    public boolean startBatteryRunnerDelayed() {
        int interval_minutes = GBApplication.getDevicePrefs(gbDevice).getBatteryPollingIntervalMinutes();
        int interval = interval_minutes * 60 * 1000;
        LOG.debug("Starting battery runner delayed by {} ({} minutes)", interval, interval_minutes);
        handler.removeCallbacks(batteryRunner);
        return handler.postDelayed(batteryRunner, interval);
    }

    public void stopBatteryRunnerDelayed() {
        LOG.debug("Stopping battery runner delayed");
        handler.removeCallbacks(batteryRunner);
    }

    @Override
    public void onInstallApp(Uri uri, @NonNull final Bundle options) {
        String picturePath = options.getString(INTENT_EXTRA_BITMAP_CACHE_FILE_PATH, "");
        if (picturePath.isEmpty()) {
            LOG.error("Cannot print: picturePath is empty");
            return;
        }
        final Bitmap incoming = BitmapFactory.decodeFile(picturePath);
        final Bitmap bitmap = options.getBoolean(INTENT_EXTRA_UPSCALE, false) ?
                Bitmap.createScaledBitmap(incoming, IMAGE_WIDTH, (int) (IMAGE_WIDTH * (float) incoming.getHeight() / incoming.getWidth()), true)
                : incoming;

        if (bitmap == null) {
            LOG.error("Cannot print: bitmap is null");
            return;
        }
        boolean dither = options.getBoolean(INTENT_EXTRA_APPLY_DITHERING, false);
        PrintAlignment alignment = (PrintAlignment) options.getSerializable(INTENT_EXTRA_ALIGNMENT);
        printImage(bitmap, dither, alignment);
    }

    public enum PrintAlignment {
        ALIGN_LEFT,
        ALIGN_CENTER,
        ALIGN_RIGHT
    }

    private enum PrinterCommand {
        feedPaper(0xa1),
        printRowPlain(0xa2),
        getDevState(0xa3),
        quality(0xa4),
        printHeadSetup(0xa6), //possibly
        getDevInfo(0xa8),
        flowControl(0xae),
        intensity(0xaf),
        printSpeed(0xbd),
        printType(0xbe),
        printRowRLE(0xbf),
        ;
        private final int cmdId;
        public static final byte[] preamble = new byte[]{0x51, 0x78};

        //Following messages observed in dumps before and after the actual print, seem to make no difference hence we're not sending them ATM
        public static final byte[] setupPrintBegin = printHeadSetup.message(new byte[]{-86, 85, 23, 56, 68, 95, 95, 95, 68, 56, 44});
        public static final byte[] setupPrintEnd = printHeadSetup.message(new byte[]{-86, 85, 23, 0, 0, 0, 0, 0, 0, 0, 23});


        PrinterCommand(int cmdId) {
            this.cmdId = cmdId;
        }

        public static PrinterCommand fromId(int id) {
            for (final PrinterCommand printerCommand : PrinterCommand.values()) {
                if (printerCommand.cmdId == (id & 0xff)) {
                    return printerCommand;
                }
            }
            return null;
        }

        private static byte[] feedPaper(final int lines) {
            return feedPaper.message(new byte[]{(byte) (lines & 0xff), (byte) ((lines >> 8) & 0xFF)});
        }

        private static byte[] intensity(final int value) {
            return intensity.message(new byte[]{(byte) (value & 0xff), (byte) ((value >> 8) & 0xFF)});
        }

        private static byte[] quality(final int level) {
            if (level < 2)
                return quality.message(new byte[]{49});
            if (level > 4)
                return quality.message(new byte[]{53});
            return quality.message(new byte[]{(byte) (48 + level)});
        }

        public byte[] message(byte[] payload) {
            final ByteBuffer buf = ByteBuffer.allocate(preamble.length + 4 + payload.length + 1 + 1);
            buf.put(preamble);
            buf.put((byte) this.cmdId);
            buf.put((byte) 0x00); //0x00 phone->printer (request), 0x01 printer->phone (response)
            buf.put((byte) payload.length);
            buf.put((byte) 0x00);
            buf.put(payload);
            buf.put((byte) getCrc8(payload));
            buf.put((byte) 0xff);

            return buf.array();
        }
    }

    private static class EncodingUtils {

        private static ByteBuffer encodeRowRLE(BitSet binaryData, int maxEncodedLength) throws BufferOverflowException {
            final ByteBuffer encodedData = ByteBuffer.allocate(maxEncodedLength);

            boolean currentValue = binaryData.get(0);
            int count = 0;

            for (int i = 0; i < binaryData.length(); i++) {
                boolean bit = binaryData.get(i);
                if (bit == currentValue) {
                    count++;
                    if (count == 128) {
                        encodedData.put(rleCountToByte(currentValue, 127));
                        count = 1;
                    }
                } else {
                    encodedData.put(rleCountToByte(currentValue, count));
                    currentValue = bit;
                    count = 1;
                }
            }
            encodedData.put(rleCountToByte(currentValue, count));
            encodedData.flip();

            return encodedData;
        }

        private static byte rleCountToByte(boolean value, int count) {
            return (byte) ((value ? 1 : 0) << 7 | (count & 0x7F));
        }

        private static byte readBitsAsInt(BitSet bitSet) {
            int result = 0;

            for (int i = 7; i >= 0; i--) {
                if (bitSet.get(i)) {
                    result |= (1 << (i));
                }
            }

            return (byte) (result & 0xff);
        }

        private static ByteBuffer encodeRowPlain(BitSet rowBits) {
            final ByteBuffer rowContent = ByteBuffer.allocate((rowBits.length() + 7) / 8);
            for (int octetIdx = 0; octetIdx < (rowBits.length() + 7) / 8; octetIdx++) {
                final int bitIdx = octetIdx * 8;
                rowContent.put(readBitsAsInt(rowBits.get(bitIdx, bitIdx + 8)));
            }
            rowContent.flip();
            return rowContent;
        }
    }
}
