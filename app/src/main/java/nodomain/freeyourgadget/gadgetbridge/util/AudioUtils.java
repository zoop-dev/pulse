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
package nodomain.freeyourgadget.gadgetbridge.util;

import org.concentus.OpusDecoder;
import org.concentus.OpusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioUtils {
    public static byte[] opusToPcm(final byte[] opusBytes) throws OpusException {
        final int SAMPLE_RATE = 16000;
        final int NUM_CHANNELS = 1;
        final int FRAME_SIZE = 320;

        final OpusDecoder opusDecoder = new OpusDecoder(SAMPLE_RATE, NUM_CHANNELS);
        final ByteBuffer buf = ByteBuffer.wrap(opusBytes).order(ByteOrder.BIG_ENDIAN);

        final byte[] pcm = new byte[FRAME_SIZE * 2];

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while (buf.hasRemaining()) {
            final int len = buf.getInt();
            final int unk = buf.getInt();
            final byte[] arr = new byte[len];
            buf.get(arr);

            final int decoded = opusDecoder.decode(arr, 0, arr.length, pcm, 0, FRAME_SIZE, false);

            // ffmpeg -f s16le -ar 16k -ac 1 -i memo.pcm memo.wav
            baos.write(pcm, 0, decoded * 2 /* 16-bit */);
        }

        return baos.toByteArray();
    }

    public static void writeWavHeader(final int pcmLength, final OutputStream out) throws IOException {
        final int totalLength = pcmLength + 36;
        final ByteBuffer buf = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        final int sampleRate = 16000;
        final int channels = 1;
        final int bitsPerSample = 16;

        buf.put("RIFF".getBytes());
        buf.putInt(totalLength);
        buf.put("WAVE".getBytes()); // file type header

        buf.put("fmt ".getBytes());
        buf.putInt(16); // length of format data
        buf.putShort((short) 1); // 1 = pcm
        buf.putShort((short) channels); // 1 channel
        buf.putInt(sampleRate); // sample rate
        buf.putInt((sampleRate * bitsPerSample * channels) / 8);
        buf.putShort((short) ((bitsPerSample * channels) / 8)); // 1 - 8 bit mono, 2 - 8 bit stereo/16 bit mono, 4 - 16 bit stereo
        buf.putShort((short) bitsPerSample);

        buf.put("data".getBytes());
        buf.putInt(pcmLength);

        out.write(buf.array());
    }
}
