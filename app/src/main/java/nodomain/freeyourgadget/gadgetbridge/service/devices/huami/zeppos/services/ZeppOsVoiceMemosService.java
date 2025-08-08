/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import android.content.Intent;
import android.os.Handler;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.activities.audiorecordings.AudioRecordingsActivity;
import nodomain.freeyourgadget.gadgetbridge.database.repository.AudioRecordingsRepository;
import nodomain.freeyourgadget.gadgetbridge.entities.AudioRecording;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsVoiceMemosService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsVoiceMemosService.class);

    private static final short ENDPOINT = 0x0033;

    private static final byte CMD_LIST_REQUEST = 0x05;
    private static final byte CMD_LIST_RESPONSE = 0x06;
    private static final byte CMD_DOWNLOAD_START_REQUEST = 0x07;
    private static final byte CMD_DOWNLOAD_START_ACK = 0x08;
    private static final byte CMD_DOWNLOAD_FINISH_REQUEST = 0x0a;
    private static final byte CMD_DOWNLOAD_FINISH_ACK = 0x09;

    private final Map<String, AudioRecording> downloadingRecordings = new HashMap<>();
    private final Queue<String> downloadQueue = new LinkedList<>();
    private boolean downloading = false;
    private final Handler handler = new Handler();

    public ZeppOsVoiceMemosService(final ZeppOsSupport support) {
        super(support, true);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void dispose() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void handlePayload(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        switch (buf.get()) {
            case CMD_LIST_RESPONSE:
                final short count = buf.getShort();
                LOG.info("Got list with {} voice memos", count);

                for (int i = 0; i < count; i++) {
                    final String filename = Objects.requireNonNull(StringUtils.untilNullTerminator(buf));
                    final int size = buf.getInt();
                    final int duration = buf.getInt();
                    final long timestamp = buf.getLong();

                    final AudioRecording existingRecording = AudioRecordingsRepository.getByTimestamp(getSupport().getDevice(), timestamp);

                    LOG.debug(
                            "Voice memo: filename={}, size={}b, duration={}ms, timestamp={}",
                            filename, size, duration, DateTimeUtils.formatIso8601(new Date(timestamp))
                    );

                    if (existingRecording != null) {
                        LOG.debug("Ignoring known local recording {}", filename);
                        continue;
                    }

                    final AudioRecording audioRecording = new AudioRecording();
                    audioRecording.setRecordingId(UUID.randomUUID().toString());
                    audioRecording.setLabel(filename.replace(".opus", ""));
                    audioRecording.setTimestamp(timestamp);
                    audioRecording.setDuration(duration);
                    downloadingRecordings.put(filename, audioRecording);

                    downloadStart(Objects.requireNonNull(filename));
                }

                if (!downloading) {
                    broadcastDownloadFinished();
                }

                return;
            case CMD_DOWNLOAD_START_ACK:
                LOG.info("Download start ACK, status = {}", payload[1]);
                return;
            case CMD_DOWNLOAD_FINISH_ACK:
                LOG.info("Download finish ACK, status = {}", payload[1]);
                return;
        }

        LOG.warn("Unexpected voice memos byte {}", String.format("0x%02x", payload[0]));
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        downloadingRecordings.clear();
        downloadQueue.clear();
        downloading = false;
        handler.removeCallbacksAndMessages(null);
    }

    public void requestList() {
        write("get voice memos list", CMD_LIST_REQUEST);
    }

    public void downloadStart(final String filename) {
        LOG.debug("Queuing voice memo download for {}", filename);

        downloadQueue.add(filename);
        if (!downloading) {
            downloading = true;
            downloadNext();
        }
    }

    public void onFileDownloadFinish(final String url, final String filename, final byte[] data) {
        final AudioRecording audioRecording = downloadingRecordings.get(filename);
        if (audioRecording == null) {
            LOG.error("Received file {} for unknown audio recording", filename);
            downloadNext();
            return;
        }

        final File targetFile;
        try {
            final File exportDirectory = getCoordinator().getWritableExportDirectory(getSupport().getDevice(), true);
            final File voiceMemosDirectory = new File(exportDirectory, "voicememo");
            //noinspection ResultOfMethodCallIgnored
            voiceMemosDirectory.mkdirs();

            final String validFilename = FileUtils.makeValidFileName(filename);
            targetFile = new File(voiceMemosDirectory, validFilename);
        } catch (final IOException e) {
            LOG.error("Failed create folder to save voice memo", e);
            downloadNext();
            return;
        }

        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            outputStream.write(data);
        } catch (final IOException e) {
            LOG.error("Failed to save voice memo bytes", e);
            downloadNext();
            return;
        }

        audioRecording.setPath(targetFile.getPath());
        AudioRecordingsRepository.insertOrReplace(getSupport().getDevice(), audioRecording);

        downloadNext();
    }

    private void downloadNext() {
        handler.removeCallbacksAndMessages(null);

        final String filename = downloadQueue.poll();

        if (filename != null) {
            // Timeout after a while so we do not get stuck
            handler.postDelayed(() -> {
                LOG.warn("Timed out waiting for voice memo download, triggering next");
                downloadNext();
            }, 5000L);

            LOG.debug("Will download voice memo {}", filename);
            write(
                    "voice memo download " + filename,
                    ArrayUtils.addAll(new byte[]{CMD_DOWNLOAD_START_REQUEST}, filename.getBytes())
            );
        } else {
            LOG.debug("Voice memo downloads finished");
            downloading = false;
            write("voice memo download finish", CMD_DOWNLOAD_FINISH_REQUEST);

            broadcastDownloadFinished();
        }
    }

    private void broadcastDownloadFinished() {
        final Intent intent = new Intent(AudioRecordingsActivity.ACTION_FETCH_FINISH);
        intent.setPackage(BuildConfig.APPLICATION_ID);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }
}
