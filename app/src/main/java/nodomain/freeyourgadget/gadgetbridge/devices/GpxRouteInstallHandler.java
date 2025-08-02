package nodomain.freeyourgadget.gadgetbridge.devices;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.install.GpxRouteInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.install.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParser;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrack;

public abstract class GpxRouteInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GpxRouteInstallHandler.class);

    public static final String EXTRA_TRACK_NAME = "gpx_track_name";

    private static final int MAX_EXPECTED_SIZE = 10 * 1024 * 1024; // 10MB

    protected final Context mContext;
    private GpxFile gpxFile;

    public GpxRouteInstallHandler(final Uri uri, final Context context) {
        this.mContext = context;

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri", e);
            return;
        }

        if (uriHelper.getFileSize() > MAX_EXPECTED_SIZE) {
            LOG.debug("Not gpx - file too large");
            return;
        }

        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            final byte[] rawBytes = FileUtils.readAll(in, MAX_EXPECTED_SIZE);
            this.gpxFile = GpxParser.parseGpx(rawBytes);
        } catch (final Exception e) {
            LOG.error("Failed to read gpx file", e);
        }
    }

    protected abstract boolean isCompatible(final GBDevice device);

    @NonNull
    @Override
    public Class<? extends Activity> getInstallActivity() {
        return GpxRouteInstallerActivity.class;
    }

    @Override
    public boolean isValid() {
        return gpxFile != null;
    }

    @Override
    public void validateInstallation(final InstallActivity installActivity, final GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!isCompatible(device)) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!device.isInitialized()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_ready));
            installActivity.setInstallEnabled(false);
            return;
        }

        final GenericItem fwItem = createInstallItem(device);
        fwItem.setIcon(device.getDeviceCoordinator().getDefaultIconResource());

        if (gpxFile == null) {
            fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_incompatible_version));
            installActivity.setInfoText(mContext.getString(R.string.fwinstaller_firmware_not_compatible_to_device));
            installActivity.setInstallEnabled(false);
            return;
        }

        final StringBuilder builder = new StringBuilder();
        final String gpxRoute = mContext.getString(R.string.kind_gpx_route);
        builder.append(mContext.getString(R.string.fw_upgrade_notice, gpxRoute));
        installActivity.setInfoText(builder.toString());
        installActivity.setInstallItem(fwItem);
        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall(final GBDevice device) {
    }

    @Nullable
    public GpxFile getGpxFile() {
        return gpxFile;
    }

    public String getName() {
        if (gpxFile == null) {
            return "";
        }

        // Prioritize metadata name
        if (StringUtils.isNotBlank(gpxFile.getName())) {
            return gpxFile.getName();
        }

        // Fallback to the first track that has a name
        for (final GpxTrack track : gpxFile.getTracks()) {
            if (StringUtils.isNotBlank(track.getName())) {
                return track.getName();
            }
        }

        return "";
    }

    private GenericItem createInstallItem(final GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final String firmwareName = mContext.getString(
                R.string.installhandler_firmware_name,
                mContext.getString(coordinator.getDeviceNameResource()),
                mContext.getString(R.string.kind_gpx_route),
                getName()
        );
        return new GenericItem(firmwareName);
    }
}
