package nodomain.freeyourgadget.gadgetbridge.devices.thermalprinter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter.BitmapToBitSet;
import nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter.GenericThermalPrinterSupport;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;


public class SendToPrinterActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(SendToPrinterActivity.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Bitmap bitmap;
    private ImageView previewImage;
    private ImageView incomingImage;
    private MaterialSwitch dithering;
    private BitmapToBitSet bitmapToBitSet;
    private File printPicture = null;

    ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Uri uri = result.getData().getData();
                    processUriAsync(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_print_image);
        incomingImage = findViewById(R.id.incomingImage);
        previewImage = findViewById(R.id.convertedImage);
        Button sendToPrinter = findViewById(R.id.sendToPrinterButton);
        dithering = findViewById(R.id.switchDithering);
        final TextView warning = findViewById(R.id.warning_devices);

        final List<GBDevice> devices = ((GBApplication) getApplicationContext()).getDeviceManager().getSelectedDevices();
        GBDevice device = devices.get(0);

        switch (devices.size()) {
            case 0:
                warning.setText(R.string.open_fw_installer_connect_minimum_one_device);
                sendToPrinter.setEnabled(false);
                break;
            case 1:
                warning.setText(String.format(getString(R.string.open_fw_installer_select_file), device.getAliasOrName()));
                sendToPrinter.setEnabled(true);
                break;
            default:
                warning.setText(R.string.open_fw_installer_connect_maximum_one_device);
                sendToPrinter.setEnabled(false);
        }

        dithering.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOG.info("dithering is : {}", dithering.isChecked());
                updatePreview();
            }
        });

        sendToPrinter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToPrinter();
            }
        });

        printPicture = new File(getCacheDir(), "temp_bitmap.png");
        final Uri uri = getIntent().getParcelableExtra(GenericThermalPrinterSupport.INTENT_EXTRA_URI);
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
                Bitmap incoming;

                try (InputStream stream = uriHelper.openInputStream()) {
                    incoming = BitmapFactory.decodeStream(stream);
                }

                Bitmap scaledBitmap;
                if (incoming.getWidth() > 384) {
                    float aspectRatio = (float) incoming.getHeight() / incoming.getWidth();
                    scaledBitmap = Bitmap.createScaledBitmap(incoming, 384, (int) (384 * aspectRatio), true);
                } else {
                    scaledBitmap = incoming;
                }

                try (FileOutputStream out = new FileOutputStream(printPicture)) {
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    LOG.error("Failed to save picture to print: {}", e.getMessage());
                }
                runOnUiThread(() -> {
                    bitmap = scaledBitmap;
                    updatePreview();
                });

            } catch (IOException e) {
                LOG.error("Failed to load or process bitmap", e);
            }
        });
    }

    private void updatePreview() {
        incomingImage.setImageBitmap(bitmap);

        bitmapToBitSet = new BitmapToBitSet(bitmap);
        bitmapToBitSet.toBlackAndWhite(dithering.isChecked());

        previewImage.setImageBitmap(bitmapToBitSet.getPreview());
    }

    private void sendToPrinter() {
        Intent intent = new Intent(GenericThermalPrinterSupport.INTENT_ACTION_PRINT_BITMAP);
        intent.putExtra(GenericThermalPrinterSupport.INTENT_EXTRA_BITMAP_CACHE_FILE_PATH, printPicture.getAbsolutePath());
        intent.putExtra(GenericThermalPrinterSupport.INTENT_EXTRA_APPLY_DITHERING, dithering.isChecked());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void cleanUpPrintPictureCache() {
        if (printPicture == null)
            return;
        printPicture.delete();
    }

}
