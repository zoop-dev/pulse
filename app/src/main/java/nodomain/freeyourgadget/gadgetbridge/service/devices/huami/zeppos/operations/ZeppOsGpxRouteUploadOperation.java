/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFileTransferService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;

public class ZeppOsGpxRouteUploadOperation extends AbstractZeppOsOperation<ZeppOsSupport>
        implements ZeppOsFileTransferService.UploadCallback {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsGpxRouteUploadOperation.class);

    private final ZeppOsGpxRouteFile file;
    private final byte[] fileBytes;

    private final ZeppOsFileTransferService fileTransferService;

    public ZeppOsGpxRouteUploadOperation(final ZeppOsSupport support,
                                         final GpxFile gpxFile,
                                         final String trackName,
                                         final ZeppOsFileTransferService fileTransferService) {
        super(support);
        this.file = new ZeppOsGpxRouteFile(gpxFile, trackName);
        this.fileBytes = file.getEncodedBytes();
        this.fileTransferService = fileTransferService;
    }

    @Override
    protected void doPerform() throws IOException {
        fileTransferService.sendFile(
                "sport://file_transfer?appId=7073283073&params={}",
                "track_" + file.getTimestamp() + ".dat",
                fileBytes,
                false,
                this
        );
    }

    @Override
    public void onFileUploadFinish(final boolean success) {
        LOG.info("Finished gpx route upload operation, success={}", success);

        final String notificationMessage = success ?
                getContext().getString(R.string.gpx_route_upload_complete) :
                getContext().getString(R.string.gpx_route_upload_failed);

        GB.updateInstallNotification(notificationMessage, false, 100, getContext());

        operationFinished();
    }

    @Override
    public void onFileUploadProgress(final int progress) {
        LOG.trace("Gpx route upload operation progress: {}", progress);

        final int progressPercent = (int) ((((float) (progress)) / fileBytes.length) * 100);
        updateProgress(progressPercent);
    }

    private void updateProgress(final int progressPercent) {
        try {
            final ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("send gpx route upload progress");
            builder.setProgress(R.string.gpx_route_upload_in_progress, true, progressPercent);
            builder.queue();
        } catch (final Exception e) {
            LOG.error("Failed to update progress notification", e);
        }
    }
}
