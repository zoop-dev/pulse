/*  Copyright (C) 2025 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.atcbleoepl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.nQuant.PnnLABQuantizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class ATCBLEOEPLDeviceSupport extends AbstractBTLESingleDeviceSupport {
    public static final UUID UUID_SERVICE_MAIN = UUID.fromString("00001337-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_MAIN = UUID.fromString("00001337-0000-1000-8000-00805f9b34fb");
    private static final Logger LOG = LoggerFactory.getLogger(ATCBLEOEPLDeviceSupport.class);
    private static final byte[] COMMAND_GET_CONFIGURATION = new byte[]{0x00, 0x05};
    private static final byte[] COMMAND_ENABLE_OEPL = new byte[]{0x00, 0x06};
    private static final byte[] COMMAND_DISABLE_OEPL = new byte[]{0x00, 0x07};
    private static final byte[] COMMAND_CONFIGURE_HS_154_BWRY_JD = new byte[]{0x00, 0x10, 0x26, 0x00, 0x6C, 0x00, 0x05, 0x00, 0x01, 0x01, 0x00, (byte) 0xC8, 0x00, (byte) 0xC8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x01, 0x02, 0x01, 0x10, 0x03, (byte) 0x80, 0x01, 0x02, 0x00, 0x00, 0x00, 0x04, 0x03, 0x00, 0x00, (byte) 0x80, 0x03, 0x40, 0x01, 0x20, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x02, 0x01, 0x10, 0x01, 0x08, 0x03, (byte) 0x80, 0x00, 0x00, 0x01, 0x02, 0x02, 0x02, 0x40, 0x02, 0x10, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    private int epaper_width = 0;
    private int epaper_height = 0;
    private int epaper_colors = 0;
    private int model;
    private byte[] image_payload = null;
    private byte[] block_data = null;
    private int blocks_total = -1;
    private int current_block = -1;
    private int current_chunk = -1;


    public ATCBLEOEPLDeviceSupport() {
        super(LOG);
        addSupportedService(UUID_SERVICE_MAIN);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");
        builder.requestMtu(512);
        builder.notify(UUID_CHARACTERISTIC_MAIN, true);
        builder.wait(300);
        builder.write(UUID_CHARACTERISTIC_MAIN, COMMAND_GET_CONFIGURATION);
        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        super.onCharacteristicChanged(gatt, characteristic, value);

        UUID characteristicUUID = characteristic.getUuid();

        LOG.info("Characteristic changed UUID: {}", characteristicUUID);
        LOG.info("Characteristic changed value: {}", StringUtils.bytesToHex(value));
        if (characteristicUUID.equals(UUID_CHARACTERISTIC_MAIN)) {
            if (value[0] == 0x00) {
                switch (value[1]) {
                    case 0x05:
                        handleConfigResponse(value);
                        return true;
                    case (byte) 0xc4:
                        handleNextChunkRequest(false);
                        return true;
                    case (byte) 0xc5:
                        handleNextChunkRequest(true);
                        return true;
                    case (byte) 0xc6:
                        handleNextBlockRequest(value);
                        return true;
                    case (byte) 0xc7:
                        handleFinishUploadRequest(false, true);
                        return true;
                    case (byte) 0xc8:
                        handleFinishUploadRequest(false, false);
                        return true;
                    case (byte) 0xc9:
                        handleFinishUploadRequest(true, true);
                        return true;

                }
            }
        }
        return false;
    }

    private int getCheckSum(byte[] buffer) {
        int sum = 0;
        for (byte val : buffer) {
            sum += val & 0xff;
        }
        return sum;
    }

    private byte[] encodeBlock(byte[] image, int block_nr) {
        int block_length = 4096;
        if (image.length < block_nr * 4096) {
            return null;
        }
        if (image.length < (block_nr + 1) * 4096) {
            block_length = image.length % 4096;
        }
        byte[] buffer = new byte[block_length + 4];
        buffer[0] = 0; // length filled in later
        buffer[1] = 0; // length filled in later
        buffer[2] = 0; // checksum filled in later
        buffer[3] = 0; // checksum filled in later
        System.arraycopy(image, block_nr * 4096, buffer, 4, block_length);
        long checksum = getCheckSum(buffer) & 0xffffffffL;
        buffer[0] = (byte) (block_length & 0xff);
        buffer[1] = (byte) ((block_length >> 8) & 0xff);
        buffer[2] = (byte) (checksum & 0xff);
        buffer[3] = (byte) ((checksum >> 8) & 0xff);
        return buffer;

    }

    private byte[] encodeImageChunk(byte[] block, int block_nr, int chunk_nr) {
        int chunk_length = 230;
        if (block.length < chunk_nr * 230) {
            return null;
        }
        if (block.length < (chunk_nr + 1) * 230) {
            chunk_length = block.length % 230;
        }
        byte[] buffer = new byte[235];
        buffer[0] = 0; // command filled in later
        buffer[1] = 0; // command filled in later
        buffer[2] = 0; // checksum filled in later
        buffer[3] = (byte) block_nr;
        buffer[4] = (byte) chunk_nr;
        System.arraycopy(block, chunk_nr * 230, buffer, 5, chunk_length);
        buffer[2] = (byte) getCheckSum(buffer);
        buffer[1] = 0x65; // command
        return buffer;
    }

    private void handleFinishUploadRequest(boolean is_firmware, boolean success) {
        if (!success) {
            GB.toast(getContext().getString(R.string.same_image_already_on_device), Toast.LENGTH_LONG, GB.WARN);
        }
        final TransactionBuilder builder = createTransactionBuilder("finish upload");
        builder.setProgress(R.string.sending_image, false, 100);
        if (!is_firmware) {
            builder.write(UUID_CHARACTERISTIC_MAIN, new byte[]{0x00, 0x03});
        }
        builder.queue();

        if (getDevice().isBusy()) {
            getDevice().unsetBusyTask();
            getDevice().sendDeviceUpdateIntent(getContext());
        }
        image_payload = null;
        current_chunk = -1;
        current_block = -1;
        blocks_total = -1;
    }

    private void handleNextBlockRequest(byte[] value) {
        final TransactionBuilder builder = createTransactionBuilder("send image start");
        builder.write(UUID_CHARACTERISTIC_MAIN, new byte[]{0x00, 0x02});
        current_chunk = 0;
        current_block = value[11];
        block_data = encodeBlock(image_payload, current_block);
        builder.write(UUID_CHARACTERISTIC_MAIN, encodeImageChunk(block_data, current_block, current_chunk));
        int progressPercent = (int) ((((float) current_block) / blocks_total) * 100);
        builder.setProgress(R.string.sending_image, true, progressPercent);
        builder.queue();
    }

    private void handleNextChunkRequest(boolean success) {
        final TransactionBuilder builder = createTransactionBuilder("send image chunk");
        if (success) {
            current_chunk++; // only send next chunk when successful, else resend last chunk
        }
        builder.write(UUID_CHARACTERISTIC_MAIN, encodeImageChunk(block_data, current_block, current_chunk));
        builder.queue();
    }

    private void handleConfigResponse(byte[] value) {
        ByteBuffer buf = ByteBuffer.wrap(value);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.position(6);
        int version = buf.getInt();

        buf.position(17);
        model = buf.getShort();

        buf.position(21);
        boolean is_wh_swapped = buf.get() == 0x01;

        buf.position(24);
        epaper_width = buf.getShort();
        epaper_height = buf.getShort();

        buf.position(32);
        epaper_colors = buf.get();

        buf.position(35);
        boolean oepl_enabled = buf.get() == 0x01;

        buf.position(41);
        int ble_adv_interval = buf.getShort() & 0xffff;

        if (epaper_width == 200 && epaper_height == 200 && model == 0x26) {
            model = 10000; //HACK for unsupported HS 154 BWRY JD
        }


        LOG.info("decoded data: version={}, width={}, height={}, nr_colors={}, w/h swapped={}, model={} ble_adv_interval={}, oepl_enabled={}", version, epaper_width, epaper_height, epaper_colors, is_wh_swapped, model, ble_adv_interval, oepl_enabled);

        SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).edit();
        editor.putString(DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_MODEL, String.valueOf(model));
        editor.putString(DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_BLE_ADV_INTERVAL, String.valueOf(ble_adv_interval));
        editor.putBoolean(DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_OEPL_PROTOCOL_ENABLE, oepl_enabled);
        editor.apply();

        final TransactionBuilder builder = createTransactionBuilder("set initialized");
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        builder.queue();
    }

    @Override
    public void onSendConfiguration(String config) {
        if (DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_MODEL.equals(config)
                || DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_BLE_ADV_INTERVAL.equals(config)
                || DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_OEPL_PROTOCOL_ENABLE.equals(config)
        ) {
            TransactionBuilder builder;
            SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
            try {
                builder = performInitialized("Sending configuration for option: " + config);
                if (DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_MODEL.equals(config)) {
                    String display_model = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_MODEL, "1");
                    int model = Integer.parseInt(display_model);
                    if (model == 10000) {// HACK: this is for the HS 213 BWRY JD type
                        builder.write(UUID_CHARACTERISTIC_MAIN, COMMAND_CONFIGURE_HS_154_BWRY_JD);
                    } else {
                        builder.write(UUID_CHARACTERISTIC_MAIN, new byte[]{0x00, 0x04, 0x00, (byte) model});
                    }
                }
                if (DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_BLE_ADV_INTERVAL.equals(config)) {
                    String bt_adv_interval = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_BLE_ADV_INTERVAL, "1000");
                    int interval = Integer.parseInt(bt_adv_interval);
                    builder.write(UUID_CHARACTERISTIC_MAIN, new byte[]{0x00, 0x08, (byte) ((interval >> 8) & 0xff), (byte) (interval & 0xff)});
                }
                if (DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_OEPL_PROTOCOL_ENABLE.equals(config)) {
                    boolean enable_oepl_protocol = sharedPrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_ATC_BLE_OEPL_OEPL_PROTOCOL_ENABLE, true);
                    if (enable_oepl_protocol) {
                        builder.write(UUID_CHARACTERISTIC_MAIN, COMMAND_ENABLE_OEPL);
                    } else {
                        builder.write(UUID_CHARACTERISTIC_MAIN, COMMAND_DISABLE_OEPL);
                    }
                }

                builder.queue();
            } catch (IOException e) {
                GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
            }
        }
    }

    private int[] getPalette() {
        // check for black/white/yellow models
        if (epaper_colors == 1) {
            return new int[]{
                    0x000000, // black
                    0xffffff, // white
            };
        }
        switch (model) {
            case 1:
            case 2:
            case 3:
            case 5:
            case 6:
            case 31:
            case 35:
                return new int[]{
                        0x000000, // black
                        0xffff00, // yellow
                        0xffffff, // white
                };
            case 17:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 10000:
                return new int[]{
                        0x000000, // black
                        0xff0000, // red
                        0xffff00, // yellow
                        0xffffff, // white
                };
            default:
                return new int[]{
                        0x000000, // black
                        0xff0000, // red
                        0xffffff, // white
                };
        }
    }

    @Override
    public void onInstallApp(Uri uri, @NonNull final Bundle options) {
        try {
            boolean is_firmware = false;
            UriHelper uriHelper = UriHelper.get(uri, getContext());
            if (uriHelper.getFileName().endsWith(".bin")) {
                InputStream in = new BufferedInputStream(uriHelper.openInputStream());
                image_payload = FileUtils.readAll(in, 262144);
                if (image_payload[8] == 0x4b && image_payload[9] == 0x4e && image_payload[10] == 0x4c && image_payload[11] == 0x54) {
                    is_firmware = true;
                } else {
                    image_payload = null;
                    LOG.error("firmware magic not found");
                    return;
                }
            } else {
                if (epaper_width == 0 || epaper_height == 0 || epaper_colors == 0) {
                    LOG.error("epaper config unknown");
                    return;
                }

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(GBApplication.getContext().getContentResolver(), uri);
                int src_width = bitmap.getWidth();
                int src_height = bitmap.getHeight();
                float src_aspect = (float) src_width / src_height;
                float dst_aspect = (float) epaper_width / epaper_height;
                int dst_height;
                int dst_width;
                if (dst_aspect > src_aspect) {
                    // scale to height
                    dst_height = epaper_height;
                    dst_width = (int) (epaper_height * src_aspect);
                } else {
                    // scale to width
                    dst_width = epaper_width;
                    dst_height = (int) (epaper_width * (1/src_aspect));
                }
                final Bitmap bmpResized = Bitmap.createBitmap(epaper_width, epaper_height, Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(bmpResized);

                // FIXME: make rotation optional
                canvas.rotate(180, canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f);

                // Fill background white
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                canvas.drawRect(0, 0, epaper_width, epaper_height, paint);

                // Draw centered
                final Rect rect = new Rect((epaper_width - dst_width) / 2, (epaper_height - dst_height) / 2, epaper_width - (epaper_width - dst_width) / 2, epaper_height - (epaper_height - dst_height) / 2);
                canvas.drawBitmap(bitmap, null, rect, null);
                Matrix matrix = new Matrix();
                matrix.postRotate(180);

                int[] palette = getPalette();

                // dither and convert to colors in palette
                int[] pixels = new int[epaper_height * epaper_width];
                bmpResized.getPixels(pixels, 0, epaper_width, 0, 0, epaper_width, epaper_height);
                final PnnLABQuantizer pnnLABQuantizer = new PnnLABQuantizer(bmpResized);
                int[] dithered_pixels = pnnLABQuantizer.dither(pixels, palette, epaper_width, epaper_height, false);

                image_payload = new byte[((epaper_width * epaper_height) / 8) * epaper_colors];
                int nr_planes = (epaper_colors > 1) ? 2 : 1;
                for (int plane = 0; plane < nr_planes; plane++) {
                    int byte_offset = 0;
                    int buffer_offset = plane * epaper_width * epaper_height / 8;
                    int color = palette[plane];
                    for (int y = 0; y < epaper_height; y++) {
                        int bit_pos = 0;
                        int packed_byte = 0;
                        for (int x = 0; x < epaper_width; x++) {
                            int pixel = dithered_pixels[y * epaper_width + x];
                            // put the third color in all planes;
                            if ((pixel & 0xffffff) == color || (epaper_colors > 2 && (pixel & 0xffffff) == palette[2])) {
                                packed_byte |= (1 << (7 - bit_pos));
                            }
                            bit_pos++;
                            if (bit_pos == 8) {
                                image_payload[buffer_offset + byte_offset++] = (byte) (packed_byte & 0xff);
                                bit_pos = 0;
                                packed_byte = 0;
                            }
                        }
                    }
                }
            }

            int crc32 = is_firmware ? 123 : CheckSums.getCRC32(image_payload);
            ByteBuffer buf = ByteBuffer.allocate(19);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put((byte) 0x00);
            buf.put((byte) 0x64);
            buf.put((byte) 0xff);
            buf.putLong(crc32 & 0xffffffffL);
            buf.putInt(image_payload.length);
            if (is_firmware)
                buf.put((byte) 0x03);
            else
                buf.put((byte) ((epaper_colors == 1) ? 0x20 : 0x21));

            buf.put((byte) 0x00);
            buf.putShort((byte) 0x00);

            final TransactionBuilder builder = createTransactionBuilder("send image prepare");
            builder.write(UUID_CHARACTERISTIC_MAIN, buf.array());
            builder.setBusyTask(R.string.sending_image);
            builder.queue();
            blocks_total = image_payload.length / 4096 + 1;

        } catch (FileNotFoundException e) {
            LOG.error("could not find file");
        } catch (IOException e) {
            LOG.error("error decoding file");
        }
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
