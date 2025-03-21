package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import android.graphics.Bitmap;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.android.nQuant.PnnLABQuantizer;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;

public enum ZeppOsBitmapFormat {
    TGA_RGB565_GCNANOLITE(0x04, "SOMHP".getBytes()) {
        @Override
        public byte[] encode(final Bitmap bmp, final int width, final int height) {
            return BitmapUtil.convertToTgaRGB565(bmp, width, height, getTgaIdBytes());
        }
    },

    // Zepp OS 4
    TGA_L8_ARGB8888_GCNANOLITE(0x05, new byte[]{'S', 'O', 'M', 'H', (byte) 0x80}) {
        @Override
        public byte[] encode(final Bitmap bmp, final int width, final int height) {
            final Bitmap resizedBmp = BitmapUtil.convert(bmp, Bitmap.Config.ARGB_8888, width, height);

            final PnnLABQuantizer pnnLABQuantizer = new PnnLABQuantizer(resizedBmp);
            final Pair<Bitmap, int[]> converted = pnnLABQuantizer.convert(256, true);

            final Bitmap ditheredBmp = converted.first;
            final int[] palette = converted.second;

            // we need to allocate 256b even if we don't use all of them, otherwise
            // the watches misbehave
            final byte[] paletteBytes = new byte[256 * 4];
            for (int i = 0; i < palette.length; i++) {
                // ARGB -> RGBA
                final int color = palette[i];
                paletteBytes[i * 4] = (byte) ((color >> 16) & 0xff);
                paletteBytes[i * 4 + 1] = (byte) ((color >> 8) & 0xff);
                paletteBytes[i * 4 + 2] = (byte) (color & 0xff);
                paletteBytes[i * 4 + 3] = (byte) ((color >> 24) & 0xff);
            }

            final ByteBuffer imageDataBuf = ByteBuffer.allocate(ditheredBmp.getHeight() * ditheredBmp.getWidth());
            for (int y = 0; y < ditheredBmp.getHeight(); y++) {
                for (int x = 0; x < ditheredBmp.getWidth(); x++) {
                    final int pixel = ditheredBmp.getPixel(x, y);
                    boolean foundInPalette = false;
                    for (int i = 0; i < palette.length; i++) {
                        if (palette[i] == pixel) {
                            imageDataBuf.put((byte) i);
                            foundInPalette = true;
                            break;
                        }
                    }
                    if (!foundInPalette) {
                        imageDataBuf.put((byte) 0);
                    }
                }
            }

            return BitmapUtil.buildTga(imageDataBuf.array(), 8, paletteBytes, width, height, getTgaIdBytes());
        }
    },

    TGA_RGB565_DAVE2D(0x08, "SOMH6".getBytes()) {
        @Override
        public byte[] encode(final Bitmap bmp, final int width, final int height) {
            return BitmapUtil.convertToTgaRGB565(bmp, width, height, getTgaIdBytes());
        }
    },

    ;

    private final byte code;
    private final byte[] tgaId;

    ZeppOsBitmapFormat(final int code, final byte[] tgaId) {
        this.code = (byte) code;
        this.tgaId = tgaId;
    }

    public byte getCode() {
        return code;
    }

    public byte[] getTgaIdBytes() {
        // Without the expected tga id and format string they seem to get corrupted,
        // but the encoding seems to actually be the same...?
        // The TGA needs to have this ID, or the band does not accept it
        final byte[] tgaIdBytes = new byte[46];
        System.arraycopy(tgaId, 0, tgaIdBytes, 0, tgaId.length);
        return tgaIdBytes;
    }

    @Nullable
    public abstract byte[] encode(final Bitmap bmp, final int width, final int height);

    @Nullable
    public static ZeppOsBitmapFormat fromCode(final byte code) {
        for (final ZeppOsBitmapFormat format : ZeppOsBitmapFormat.values()) {
            if (format.code == code) {
                return format;
            }
        }

        return null;
    }
}
