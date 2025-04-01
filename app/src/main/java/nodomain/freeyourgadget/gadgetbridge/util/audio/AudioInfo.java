package nodomain.freeyourgadget.gadgetbridge.util.audio;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.util.GBToStringBuilder;

public class AudioInfo {
    private final String fileName;
    private final long fileSize;
    private final String title;
    private final String artist;
    private final String mimeType;
    private final String extension;

    private long duration;
    private int sampleRate;
    private int bitrate;
    private byte channels;

    public AudioInfo(final String fileName,
                     final long fileSize,
                     final String title,
                     final String artist,
                     final String mimeType,
                     final String extension) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.title = title;
        this.artist = artist;
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setCharacteristics(final long duration, final int sampleRate, final int bitrate, final byte channels) {
        this.duration = duration;
        this.sampleRate = sampleRate;
        this.bitrate = bitrate;
        this.channels = channels;
    }

    public long getDuration() {
        return duration;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getBitrate() {
        return bitrate;
    }

    public byte getChannels() {
        return channels;
    }

    @NonNull
    @Override
    public String toString() {
        final GBToStringBuilder tsb = new GBToStringBuilder(this);
        tsb.append("fileName", fileName);
        tsb.append("fileSize", fileSize);
        tsb.append("title", title);
        tsb.append("artist", artist);
        tsb.append("mimeType", mimeType);
        return tsb.toString();
    }
}
