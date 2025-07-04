package nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.android.nQuant.PnnLABQuantizer;

import java.util.Arrays;
import java.util.BitSet;

public class BitmapToBitSet {
    private final int width;
    private final int height;
    private final Bitmap bitmap;
    private final BitSet bwPixels;

    public BitmapToBitSet(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
        this.bwPixels = new BitSet(this.width * this.height);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public Bitmap getPreview() {
        final Bitmap bwBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final int[] pixels = new int[width * height];
        Arrays.fill(pixels, Color.WHITE);

        for (int i = bwPixels.nextSetBit(0); i >= 0; i = bwPixels.nextSetBit(i + 1)) {
            pixels[i] = Color.BLACK;
        }

        bwBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bwBitmap;
    }

    public BitSet toBlackAndWhite(final boolean applyDithering) {
        bwPixels.clear();
        int[] pixels = new int[width * height];

        final PnnLABQuantizer pnnLABQuantizer = new PnnLABQuantizer(bitmap);
        pnnLABQuantizer.convert(2, applyDithering).first.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == Color.BLACK)
                bwPixels.set(i);
        }

        return bwPixels;
    }

}
