/*  Copyright (C) 2023-2024 Andreas Shimokawa, José Rebelo

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

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_VOICE_SERVICE_LANGUAGE;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import org.concentus.OpusDecoder;
import org.concentus.OpusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsAssistantService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAssistantService.class);

    public static final short ENDPOINT_ALEXA = 0x0011;
    public static final short ENDPOINT_ZEPP_FLOW = 0x004a;

    private static final byte CMD_START = 0x01;
    private static final byte CMD_END = 0x02;
    private static final byte CMD_START_ACK = 0x03;
    private static final byte CMD_VOICE_DATA = 0x05;
    private static final byte CMD_TRIGGERED = 0x06;
    private static final byte CMD_REPLY_COMPLEX = 0x08;
    private static final byte CMD_REPLY_SIMPLE = 0x09;
    private static final byte CMD_REPLY_VOICE = 0x0a;
    private static final byte CMD_REPLY_VOICE_MORE = 0x0b;
    private static final byte CMD_ERROR = 0x0f;
    private static final byte CMD_LANGUAGES_REQUEST = 0x10;
    private static final byte CMD_LANGUAGES_RESPONSE = 0x11;
    private static final byte CMD_SET_LANGUAGE = 0x12;
    private static final byte CMD_SET_LANGUAGE_ACK = 0x13;
    private static final byte CMD_CAPABILITIES_REQUEST = 0x20;
    private static final byte CMD_CAPABILITIES_RESPONSE = 0x21;

    private static final byte COMPLEX_REPLY_WEATHER = 0x01;
    private static final byte COMPLEX_REPLY_REMINDER = 0x02;
    private static final byte COMPLEX_REPLY_RICH_TEXT = 0x06;

    private static final byte ERROR_NO_INTERNET = 0x03;
    private static final byte ERROR_UNAUTHORIZED = 0x06;

    private static final int CHANNELS = 1;
    private static final int MAX_FRAME_SIZE = 6 * 960;

    public static final String PREF_VERSION = "zepp_os_assistant_version";

    private int mVersion = -1;

    private final Handler handler = new Handler();
    private final short endpoint;

    private OpusDecoder opusDecoder;
    private AudioTrack audioTrack;

    private static final boolean DUMP_RAW_VOICE = false;
    private OutputStream rawVoiceOutputStream;

    final ByteBuffer voiceBuffer = ByteBuffer.allocate(4096).order(ByteOrder.BIG_ENDIAN);

    public ZeppOsAssistantService(final ZeppOsSupport support, final short endpoint) {
        super(support, true);
        this.endpoint = endpoint;
    }

    @Override
    public short getEndpoint() {
        return endpoint;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_START:
                handleStart(payload);
                break;
            case CMD_END:
                handleEnd(payload);
                break;
            case CMD_VOICE_DATA:
                handleVoiceData(payload);
                break;
            case CMD_LANGUAGES_RESPONSE:
                handleLanguagesResponse(payload);
                break;
            case CMD_SET_LANGUAGE_ACK:
                LOG.info("Assistant set language ack, status = {}", payload[1]);
                break;
            case CMD_CAPABILITIES_RESPONSE:
                handleCapabilitiesResponse(payload);
                break;
            default:
                LOG.warn("Unexpected assistant byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void dispose() {
        handler.removeCallbacksAndMessages(null);
        opusDecoder = null;
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        if (rawVoiceOutputStream != null) {
            try {
                rawVoiceOutputStream.close();
            } catch (final IOException e) {
                LOG.error("Failed to close raw voice output stream", e);
            }
            rawVoiceOutputStream = null;
        }
        voiceBuffer.clear();
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_VOICE_SERVICE_LANGUAGE:
                final String language = prefs.getString(DeviceSettingsPreferenceConst.PREF_VOICE_SERVICE_LANGUAGE, null);
                LOG.info("Setting assistant language to {}", language);
                setLanguage(language);
                return true;
            case "zepp_os_assistant_btn_trigger":
                GB.toast("Assistant cmd trigger", Toast.LENGTH_SHORT, GB.INFO);
                sendCmdTriggered();
                return true;
            case "zepp_os_assistant_btn_send_simple":
                GB.toast("Assistant simple reply", Toast.LENGTH_SHORT, GB.INFO);
                final String simpleText = prefs.getString("zepp_os_assistant_reply_text", null);
                sendReply(simpleText);
                return true;
            case "zepp_os_assistant_btn_send_complex":
                GB.toast("Assistant complex reply", Toast.LENGTH_SHORT, GB.INFO);
                final String title = prefs.getString("zepp_os_assistant_reply_title", null);
                final String subtitle = prefs.getString("zepp_os_assistant_reply_subtitle", null);
                final String text = prefs.getString("zepp_os_assistant_reply_text", null);
                sendReply(title, subtitle, text);
                return true;
        }

        return false;
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        handler.removeCallbacksAndMessages(null);
        opusDecoder = null;
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        if (rawVoiceOutputStream != null) {
            try {
                rawVoiceOutputStream.close();
            } catch (final IOException e) {
                LOG.error("Failed to close raw voice output stream", e);
            }
            rawVoiceOutputStream = null;
        }
        voiceBuffer.clear();

        if (ZeppOsCoordinator.experimentalFeatures(getSupport().getDevice())) {
            requestCapabilities(builder);
        }
    }

    public void requestCapabilities(final ZeppOsTransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    public void requestLanguages() {
        write("assistant request languages", CMD_LANGUAGES_REQUEST);
    }

    public void sendReply(final String text) {
        LOG.debug("Sending assistant simple text reply '{}'", text);

        final byte[] textBytes = StringUtils.ensureNotNull(text).getBytes(StandardCharsets.UTF_8);

        final ByteBuffer buf = ByteBuffer.allocate(textBytes.length + 2)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_REPLY_SIMPLE);
        buf.put(textBytes);
        buf.put((byte) 0);

        write("send simple text reply", buf.array());
    }

    public void sendReply(final String title, final String subtitle, final String text) {
        LOG.debug("Sending assistant complex text reply '{}', '{}', '{}'", title, subtitle, text);

        final byte[] titleBytes = StringUtils.ensureNotNull(title).getBytes(StandardCharsets.UTF_8);
        final byte[] subtitleBytes = StringUtils.ensureNotNull(subtitle).getBytes(StandardCharsets.UTF_8);
        final byte[] textBytes = StringUtils.ensureNotNull(text).getBytes(StandardCharsets.UTF_8);

        final int messageLength = titleBytes.length + subtitleBytes.length + textBytes.length + 3;

        final ByteBuffer buf = ByteBuffer.allocate(1 + 2 + 4 + messageLength)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_REPLY_COMPLEX);
        buf.putShort(COMPLEX_REPLY_RICH_TEXT);
        buf.putInt(messageLength);
        buf.put(titleBytes);
        buf.put((byte) 0);
        buf.put(subtitleBytes);
        buf.put((byte) 0);
        buf.put(textBytes);
        buf.put((byte) 0);

        write("send complex text reply", buf.array());
    }

    public void sendReply(final WeatherSpec weather) {
        // TODO finish this
        if (true) {
            LOG.warn("Reply with weather not fully implemented");
            return;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(0xfd); // ?
            baos.write(0x03); // ?
            baos.write(0x00); // ?
            baos.write(0x00); // ?

            baos.write(BLETypeConversions.fromUint32(weather.getTimestamp()));

            baos.write(StringUtils.ensureNotNull(weather.getLocation()).getBytes(StandardCharsets.UTF_8));
            baos.write(0);

            // FIXME long date string
            baos.write(0);

            baos.write(StringUtils.ensureNotNull(weather.getCurrentCondition()).getBytes(StandardCharsets.UTF_8));
            baos.write(0);

            // FIXME Second line for the condition
            baos.write(0);

            // FIXME

            baos.write(weather.getForecasts().size());
            for (final WeatherSpec.Daily forecast : weather.getForecasts()) {
                // FIXME
            }
        } catch (final IOException e) {
            LOG.error("Failed to encode weather payload", e);
            return;
        }

        final ByteBuffer buf = ByteBuffer.allocate(1 + 2 + 4 + baos.size())
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_REPLY_COMPLEX);
        buf.putShort(COMPLEX_REPLY_WEATHER);
        buf.putInt(baos.size());
        buf.put(baos.toByteArray());

        write("send weather reply", buf.array());
    }

    public void sendReplyReminder() {
        // TODO implement
    }

    public void sendReplyAlarm() {
        // TODO implement
    }

    public void sendVoiceReply(final List<byte[]> voiceFrames) {
        try {
            final ZeppOsTransactionBuilder builder = createTransactionBuilder("send voice reply");

            for (final byte[] voiceFrame : voiceFrames) {
                // TODO encode
            }

            builder.queue();
        } catch (final Exception e) {
            LOG.error("Failed to send voice reply", e);
        }
    }

    public void setLanguage(final String language) {
        if (language == null) {
            LOG.warn("Assistant language is null");
            return;
        }

        final byte[] languageBytes = language.replace("_", "-").getBytes(StandardCharsets.UTF_8);

        final ByteBuffer buf = ByteBuffer.allocate(languageBytes.length + 2)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_SET_LANGUAGE);
        buf.put(languageBytes);
        buf.put((byte) 0);

        write("set assistant language", buf.array());
    }

    public void sendError(final byte errorCode, final String errorMessage) {
        final byte[] messageBytes = StringUtils.ensureNotNull(errorMessage).getBytes(StandardCharsets.UTF_8);

        final ByteBuffer buf = ByteBuffer.allocate(messageBytes.length + 3)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_ERROR);
        buf.put(errorCode);
        buf.put(messageBytes);
        buf.put((byte) 0);

        write("send assistant error", buf.array());
    }

    public void sendStartAck() {
        write("send assistant start ack", new byte[]{CMD_START_ACK, 0x00});
    }

    public void sendCmdTriggered() {
        write("assistant cmd triggered", CMD_TRIGGERED);
    }

    public void sendVoiceMore() {
        write("assistant request more voice", CMD_REPLY_VOICE_MORE);
    }

    private void handleCapabilitiesResponse(final byte[] payload) {
        mVersion = payload[1] & 0xFF;
        if (mVersion != 3 && mVersion != 5) {
            LOG.warn("Unsupported assistant service version {}", mVersion);
            return;
        }
        final byte var1 = payload[2];
        if (var1 != 1) {
            LOG.warn("Unexpected value for var1 '{}'", var1);
        }
        final byte var2 = payload[3];
        if (var1 != 1) {
            LOG.warn("Unexpected value for var2 '{}'", var2);
        }

        getSupport().evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_VERSION, mVersion));

        LOG.info("Assistant version={}, var1={}, var2={}", mVersion, var1, var2);

        if (mVersion == 3) {
            // only in Alexa (?)
            requestLanguages();
        }
    }

    private void handleStart(final byte[] payload) {
        final byte var1 = payload[1];
        final byte var2 = payload[2];
        final byte var3 = payload[3];
        final byte var4 = payload[4];
        final String params = StringUtils.untilNullTerminator(payload, 5);

        LOG.info("Assistant starting: var1={}, var2={}, var3={}, var4={}, params={}", var1, var2, var3, var4, params);

        final int sampleRate = 16000; // FIXME how to detect sample rate?

        try {
            opusDecoder = new OpusDecoder(sampleRate, 1);
        } catch (final OpusException e) {
            LOG.error("Failed to initialize opus decoder", e);
            write("send assistant start nack", new byte[]{CMD_START_ACK, ERROR_UNAUTHORIZED});
            return;
        }

        if (DUMP_RAW_VOICE) {
            // for decoding debug
            try {
                final File writableExportDirectory = getCoordinator().getWritableExportDirectory(getSupport().getDevice(), true);
                final File targetDir = new File(writableExportDirectory, "assistantRawVoice");
                targetDir.mkdirs();
                final String filename = DateTimeUtils.formatIso8601(new Date()) + ".opus";
                final File outputFile = new File(targetDir, filename);
                rawVoiceOutputStream = new FileOutputStream(outputFile);
                LOG.debug("Started assistant raw voice output to {}", outputFile);
            } catch (final Exception e) {
                LOG.error("Failed to open raw voice output stream", e);
            }
        }

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(
                        16000,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM
        );

        // Send the start ack with a slight delay, to give enough time for the connection to switch to fast mode
        // I can't seem to get the callback for onConnectionUpdated working, and if we reply too soon the watch
        // will just stay stuck "Connecting...". It seems like it takes ~350ms to switch to fast connection.
        handler.postDelayed(this::sendStartAck, 700);
    }

    private void handleEnd(final byte[] payload) {
        voiceBuffer.position(0);

        if (opusDecoder != null) {
            opusDecoder = null;
        }
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }

        if (DUMP_RAW_VOICE && rawVoiceOutputStream != null) {
            try {
                rawVoiceOutputStream.close();
            } catch (final IOException e) {
                LOG.error("Failed to close raw opus output stream");
            }
        }

        // TODO do something else?
    }

    public void handleVoiceData(final byte[] payload) {
        LOG.info("Got {} bytes of voice data ({})", payload.length, BLETypeConversions.toUint32(payload, 1));

        if (DUMP_RAW_VOICE && rawVoiceOutputStream != null) {
            try {
                rawVoiceOutputStream.write(payload, 5, payload.length - 5);
                rawVoiceOutputStream.flush();
            } catch (final IOException e) {
                LOG.error("Failed to dump raw opus payload", e);
            }
        }

        this.voiceBuffer.put(payload, 5, payload.length - 5);
        this.voiceBuffer.flip();

        while (voiceBuffer.remaining() > 0) {
            voiceBuffer.mark();

            final int frameSizeBytes = mVersion >= 5 ? 4 : 1;

            if (voiceBuffer.remaining() < frameSizeBytes) {
                voiceBuffer.reset();
                break;
            }

            final int frameSize;
            switch (frameSizeBytes) {
                case 1:
                    frameSize = voiceBuffer.get() & 0xff;
                    break;
                case 4:
                    frameSize = voiceBuffer.getInt();
                    voiceBuffer.getInt(); // skip 4 unknown bytes
                    break;
                default:
                    throw new IllegalArgumentException("Unknown frame size " + frameSizeBytes);
            }

            if (voiceBuffer.remaining() < frameSize) {
                voiceBuffer.reset(); // Not enough data for full frame
                break;
            }

            if (frameSize == 0) {
                continue;
            }

            final byte[] frame = new byte[frameSize];
            voiceBuffer.get(frame);

            LOG.trace("Voice Data Frame: {}", GB.hexdump(frame));

            if (opusDecoder != null) {
                try {
                    final byte[] pcm = new byte[MAX_FRAME_SIZE * CHANNELS * 2];
                    final int decodedSamples = opusDecoder.decode(frame, 0, frame.length, pcm, 0, MAX_FRAME_SIZE, false);
                    LOG.debug("Opus decode: {}", decodedSamples);
                    if (audioTrack != null) {
                        audioTrack.write(pcm, 0, decodedSamples * 2 /* 16 bit */);
                    }
                } catch (final Exception e) {
                    LOG.error("Failed to decode opus frame", e);
                }
            }
        }

        voiceBuffer.compact();
    }

    private void handleLanguagesResponse(final byte[] payload) {
        if (payload[1] != 1) {
            LOG.warn("Assistant language response status = {}", payload[1]);
            return;
        }

        int pos = 2;
        final String currentLanguage = StringUtils.untilNullTerminator(payload, pos);
        pos = pos + currentLanguage.length() + 1;

        final int numLanguages = payload[pos++] & 0xFF;
        final List<String> allLanguages = new ArrayList<>();

        for (int i = 0; i < numLanguages; i++) {
            final String language = StringUtils.untilNullTerminator(payload, pos);
            allLanguages.add(language);
            pos = pos + language.length() + 1;
        }

        LOG.info("Got assistant language = {}, supported languages = {}", currentLanguage, allLanguages);

        final GBDeviceEventUpdatePreferences evt = new GBDeviceEventUpdatePreferences()
                .withPreference(PREF_VOICE_SERVICE_LANGUAGE, currentLanguage.replace("-", "_"))
                .withPreference(DeviceSettingsUtils.getPrefPossibleValuesKey(PREF_VOICE_SERVICE_LANGUAGE), TextUtils.join(",", allLanguages).replace("-", "_"));
        getSupport().evaluateGBDeviceEvent(evt);
    }

    public static boolean isSupported(final Prefs devicePrefs) {
        final int version = devicePrefs.getInt(PREF_VERSION, 0);
        return version >= 3 && version <= 5;
    }
}
