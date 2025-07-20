package nodomain.freeyourgadget.gadgetbridge.devices.thermalprinter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter.BitmapToBitSet;
import nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter.GenericThermalPrinterSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;


public class SendToPrinterActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(SendToPrinterActivity.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    GenericThermalPrinterSupport.PrintAlignment alignment = GenericThermalPrinterSupport.PrintAlignment.ALIGN_CENTER;
    private Bitmap bitmap;
    private Bitmap incoming;
    private ImageView previewImage;
    private MaterialSwitch dithering;
    private MaterialSwitch upscale;
    private GBDevice device;

    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        private static final int SWIPE_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true; // needed to enable fling detection
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();

            if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY())) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD) {
                    if (diffX > 0) {
                        alignment = alignment == GenericThermalPrinterSupport.PrintAlignment.ALIGN_LEFT ? GenericThermalPrinterSupport.PrintAlignment.ALIGN_CENTER : GenericThermalPrinterSupport.PrintAlignment.ALIGN_RIGHT;
                    } else {
                        alignment = alignment == GenericThermalPrinterSupport.PrintAlignment.ALIGN_RIGHT ? GenericThermalPrinterSupport.PrintAlignment.ALIGN_CENTER : GenericThermalPrinterSupport.PrintAlignment.ALIGN_LEFT;
                    }
                    updatePreview();
                    return true;
                }
            }
            return false;
        }
    };
    private File printPicture = null;
    private Uri uri;

    ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    uri = result.getData().getData();
                    processUriAsync(uri);
                }
            }
    );
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_print_image);
        previewImage = findViewById(R.id.convertedImage);
        Button sendToPrinter = findViewById(R.id.sendToPrinterButton);
        dithering = findViewById(R.id.switchDithering);
        final TextView warning = findViewById(R.id.warning_devices);
        upscale = findViewById(R.id.switchUpscale);

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            // This activity does not support auto-discovery - see FileInstallerActivity for that
            GB.toast("No device provided to SendToPrinterActivity", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        gestureDetector = new GestureDetector(this, gestureListener);

        previewImage.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        dithering.setOnClickListener(view -> {
            LOG.info("dithering is : {}", dithering.isChecked());
            updatePreview();
        });

        sendToPrinter.setOnClickListener(v -> sendToPrinter(device));

        upscale.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                float aspectRatio = (float) incoming.getHeight() / incoming.getWidth();
                bitmap = Bitmap.createScaledBitmap(incoming, GenericThermalPrinterSupport.IMAGE_WIDTH, (int) (GenericThermalPrinterSupport.IMAGE_WIDTH * aspectRatio), true);
            } else {
                bitmap = incoming;
            }

            updatePreview();
        });

        printPicture = new File(getCacheDir(), "temp_bitmap.png");
        uri = getIntent().getData();
        if (uri == null) { // For "share" intent
            uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (uri != null) {
            processUriAsync(uri);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void processUriAsync(Uri uri) {
        executor.execute(() -> {
            cleanUpPrintPictureCache();
            try {
                UriHelper uriHelper = UriHelper.get(uri, getApplicationContext());

                try (InputStream stream = uriHelper.openInputStream()) {
                    incoming = BitmapFactory.decodeStream(stream);
                }

                Bitmap scaledBitmap;
                if (incoming.getWidth() > GenericThermalPrinterSupport.IMAGE_WIDTH) {
                    float aspectRatio = (float) incoming.getHeight() / incoming.getWidth();
                    scaledBitmap = Bitmap.createScaledBitmap(incoming, GenericThermalPrinterSupport.IMAGE_WIDTH, (int) (GenericThermalPrinterSupport.IMAGE_WIDTH * aspectRatio), true);
                } else {
                    scaledBitmap = incoming;
                }

                try (FileOutputStream out = new FileOutputStream(printPicture)) {
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    LOG.error("Failed to save picture to print: {}", e.getMessage());
                }
                runOnUiThread(() -> {
                    upscale.setEnabled(incoming.getWidth() <= GenericThermalPrinterSupport.IMAGE_WIDTH);
                    upscale.setChecked(false);
                    bitmap = scaledBitmap;
                    updatePreview();
                });

            } catch (IOException e) {
                LOG.error("Failed to load or process bitmap", e);
            }
        });
    }

    private void updatePreview() {
        final BitmapToBitSet bitmapToBitSet = new BitmapToBitSet(GenericThermalPrinterSupport.createAlignedBitmap(bitmap, alignment));
        bitmapToBitSet.toBlackAndWhite(dithering.isChecked());

        TextView imageSizeText = findViewById(R.id.imageSizeText);
        imageSizeText.setText(getString(R.string.activity_print_label_print_size, bitmap.getWidth(), bitmap.getHeight()));

        previewImage.setImageBitmap(bitmapToBitSet.getPreview());
    }

    private void sendToPrinter(final GBDevice device) {
        final Bundle options = new Bundle();
        options.putString(GenericThermalPrinterSupport.INTENT_EXTRA_BITMAP_CACHE_FILE_PATH, printPicture.getAbsolutePath());
        options.putBoolean(GenericThermalPrinterSupport.INTENT_EXTRA_APPLY_DITHERING, dithering.isChecked());
        options.putBoolean(GenericThermalPrinterSupport.INTENT_EXTRA_UPSCALE, upscale.isChecked());
        options.putSerializable(GenericThermalPrinterSupport.INTENT_EXTRA_ALIGNMENT, alignment);
        GBApplication.deviceService(device).onInstallApp(uri, options);
    }

    private void cleanUpPrintPictureCache() {
        if (printPicture == null)
            return;
        printPicture.delete();
    }

}
