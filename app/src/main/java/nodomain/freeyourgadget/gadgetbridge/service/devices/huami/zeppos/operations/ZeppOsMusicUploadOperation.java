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
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFileTransferService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.audio.AudioInfo;

public class ZeppOsMusicUploadOperation extends AbstractZeppOsOperation<ZeppOsSupport>
        implements ZeppOsFileTransferService.UploadCallback {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsMusicUploadOperation.class);

    private final AudioInfo audioInfo;
    private final byte[] fileBytes;

    private final ZeppOsFileTransferService fileTransferService;

    public ZeppOsMusicUploadOperation(final ZeppOsSupport support,
                                      final AudioInfo audioInfo,
                                      final byte[] fileBytes,
                                      final ZeppOsFileTransferService fileTransferService) {
        super(support);
        this.audioInfo = audioInfo;
        this.fileBytes = fileBytes;
        this.fileTransferService = fileTransferService;
    }

    @Override
    protected void doPerform() throws IOException {
        fileTransferService.sendFile(
                String.format(
                        Locale.ROOT,
                        "music://file?songName=%s&singer=%s&end=1",
                        audioInfo.getTitle(),
                        audioInfo.getArtist()
                ),
                audioInfo.getFileName(),
                fileBytes,
                false,
                this
        );
    }

    @Override
    public void onFileUploadFinish(final boolean success) {
        LOG.info("Finished music upload operation, success={}", success);

        final String notificationMessage = success ?
                getContext().getString(R.string.music_upload_complete) :
                getContext().getString(R.string.music_upload_failed);

        GB.updateInstallNotification(notificationMessage, false, 100, getContext());

        operationFinished();
    }

    @Override
    public void onFileUploadProgress(final int progress) {
        LOG.trace("Music upload operation progress: {}", progress);

        final int progressPercent = (int) ((((float) (progress)) / fileBytes.length) * 100);
        updateProgress(progressPercent);
    }

    private void updateProgress(final int progressPercent) {
        try {
            final ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("send music upload progress");
            builder.setProgress(R.string.music_upload_in_progress, true, progressPercent);
            builder.queue();
        } catch (final Exception e) {
            LOG.error("Failed to update progress notification", e);
        }
    }
}
