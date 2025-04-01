package nodomain.freeyourgadget.gadgetbridge.util.audio;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;

public class MusicUtils {
    private static final Logger LOG = LoggerFactory.getLogger(MusicUtils.class);

    @Nullable
    public static AudioInfo audioInfoFromUri(final Context context, final Uri uri) {
        final ContentResolver contentResolver = context.getContentResolver();
        final String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null || !mimeType.startsWith("audio/")) {
            LOG.warn("Mime type is not an audio file: {}", mimeType);
            return null;
        }

        final String[] filePathColumn = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};

        final Cursor cursor = contentResolver.query(uri, filePathColumn, null, null, null);
        if (cursor == null)
            return null;
        cursor.moveToFirst();

        final int fileNameIndex = cursor.getColumnIndex(filePathColumn[0]);
        final String fileName = cursor.getString(fileNameIndex);
        final int fileSizeIndex = cursor.getColumnIndex(filePathColumn[1]);
        final long fileSize = cursor.getLong(fileSizeIndex);
        cursor.close();

        final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, uri);

        String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        if (TextUtils.isEmpty(title)) {
            title = getNameWithoutExtension(fileName);
        }
        if (TextUtils.isEmpty(artist)) {
            artist = context.getString(R.string.unknown);
        }

        String extension = getExtension(fileName);
        if (!TextUtils.isEmpty(extension)) {
            extension = extension.toLowerCase();
        }

        final AudioInfo audioInfo = new AudioInfo(fileName, fileSize, title, artist, mimeType, extension);

        try {
            final MediaExtractor mex = new MediaExtractor();
            mex.setDataSource(context, uri, null);

            final MediaFormat mf = mex.getTrackFormat(0);

            int bitrate = -1; // TODO: calculate or get bitrate
            int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channels = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            long duration = mf.getLong(MediaFormat.KEY_DURATION);

            LOG.trace("bitRate: {}", bitrate);
            LOG.trace("sampleRate: {}", sampleRate);
            LOG.trace("channelCount: {}", channels);
            LOG.trace("duration: {}", duration);

            audioInfo.setCharacteristics(duration, sampleRate, bitrate, (byte) channels);
        } catch (final IOException e) {
            LOG.error("Failed to set data source on media extractor", e);
        }

        return audioInfo;
    }

    private static String getNameWithoutExtension(final String fileName) {
        return fileName.indexOf(".") > 0 ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
    }

    @Nullable
    private static String getExtension(final String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        final int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf >= 0 && lastIndexOf + 1 < fileName.length()) {
            return fileName.substring(lastIndexOf + 1);
        }
        return null;
    }
}
