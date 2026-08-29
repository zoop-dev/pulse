/*  Copyright (C) 2026 Pulse

    This file is part of Pulse, a Garmin-only fork of Gadgetbridge.

    Pulse is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class PulseLogReporter {
    private static final Logger LOG = LoggerFactory.getLogger(PulseLogReporter.class);

    private static final long MAX_LOG_BYTES = 8L * 1024 * 1024;
    private static final long MAX_FIT_BYTES = 25L * 1024 * 1024;
    private static final int MAX_FIT_FILES = 500;
    private static final int LOGCAT_LINES = 3000;

    public interface Callback {
        void onSuccess(String ref);

        void onFailure(String message);
    }

    private PulseLogReporter() {
    }

    public static boolean fileLoggingActive() {
        return Logging.getInstance().isFileLoggerInitialized()
                && Boolean.TRUE.equals(GBApplication.getPrefs().getBoolean("log_to_file", false));
    }

    public static void submit(final Context context, final String note, final String crashTrace,
                              final boolean includeActivityFiles, final Callback callback) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            File bundle = null;
            try {
                bundle = buildBundle(app, note, crashTrace, includeActivityFiles);
                final String ref = upload(bundle, note, crashTrace != null);
                main.post(() -> callback.onSuccess(ref));
            } catch (final Exception e) {
                LOG.warn("Pulse: log report failed", e);
                final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                main.post(() -> callback.onFailure(msg));
            } finally {
                if (bundle != null && !bundle.delete()) {
                    LOG.debug("Pulse: could not delete report bundle {}", bundle);
                }
            }
        }, "pulse-log-report").start();
    }

    private static File buildBundle(final Context context, final String note, final String crashTrace,
                                    final boolean includeActivityFiles)
            throws IOException {
        final File out = new File(context.getCacheDir(), "pulse-report-" + System.currentTimeMillis() + ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(new java.io.FileOutputStream(out))) {
            writeEntry(zip, "meta.json",
                    collectMeta(context, note, crashTrace, includeActivityFiles).getBytes(StandardCharsets.UTF_8));

            final byte[] logcat = readLogcat();
            if (logcat.length > 0) {
                writeEntry(zip, "logcat.txt", logcat);
            }
            if (crashTrace != null && !crashTrace.isEmpty()) {
                writeEntry(zip, "crash.txt", crashTrace.getBytes(StandardCharsets.UTF_8));
            }

            long budget = MAX_LOG_BYTES;
            for (final File f : logFiles()) {
                if (budget <= 0) {
                    break;
                }
                budget -= copyEntry(zip, "logs/" + f.getName(), f, budget);
            }

            if (includeActivityFiles) {
                long fitBudget = MAX_FIT_BYTES;
                for (final File f : activityFiles()) {
                    if (fitBudget <= 0) {
                        break;
                    }
                    if (f.length() > fitBudget) {
                        continue;
                    }
                    fitBudget -= copyEntry(zip, "activities/" + f.getParentFile().getName() + "/" + f.getName(),
                            f, fitBudget);
                }
            }
        }
        return out;
    }

    private static String collectMeta(final Context context, final String note, final String crashTrace,
                                      final boolean includeActivityFiles) {
        final JSONObject meta = new JSONObject();
        try {
            meta.put("generated", System.currentTimeMillis());
            meta.put("note", note != null ? note : "");
            meta.put("hasCrash", crashTrace != null);
            meta.put("includesActivityFiles", includeActivityFiles);

            final JSONObject appInfo = new JSONObject();
            appInfo.put("id", BuildConfig.APPLICATION_ID);
            appInfo.put("versionName", BuildConfig.VERSION_NAME);
            appInfo.put("versionCode", BuildConfig.VERSION_CODE);
            appInfo.put("flavor", BuildConfig.FLAVOR);
            appInfo.put("debug", BuildConfig.DEBUG);
            appInfo.put("gitHash", BuildConfig.GIT_HASH_SHORT);
            meta.put("app", appInfo);

            final JSONObject dev = new JSONObject();
            dev.put("manufacturer", Build.MANUFACTURER);
            dev.put("model", Build.MODEL);
            dev.put("androidRelease", Build.VERSION.RELEASE);
            dev.put("sdkInt", Build.VERSION.SDK_INT);
            meta.put("phone", dev);

            final JSONObject logging = new JSONObject();
            logging.put("fileLogging", fileLoggingActive());
            logging.put("trace", GBApplication.getPrefs().getBoolean("log_level_trace", false));
            meta.put("logging", logging);

            final JSONArray watches = new JSONArray();
            for (final GBDevice d : GBApplication.app().getDeviceManager().getDevices()) {
                final JSONObject w = new JSONObject();
                w.put("name", nz(d.getName()));
                w.put("model", nz(d.getModel()));
                w.put("firmware", nz(d.getFirmwareVersion()));
                w.put("state", d.getStateString(context));
                w.put("connected", d.isConnected());
                watches.put(w);
            }
            meta.put("watches", watches);
        } catch (final Exception e) {
            LOG.warn("Pulse: meta build failed", e);
        }
        return meta.toString();
    }

    private static String nz(final String s) {
        return s != null ? s : "";
    }

    private static List<File> logFiles() {
        final List<File> files = new ArrayList<>();
        File dir = null;
        try {
            final String current = Logging.getInstance().getLogPath();
            if (current != null) {
                final File cf = new File(current);
                dir = cf.getParentFile();
                if (cf.isFile()) {
                    files.add(cf);
                }
            }
        } catch (final Exception ignored) {
        }
        if (dir == null) {
            try {
                dir = FileUtils.getExternalFilesDir();
            } catch (final IOException ignored) {
            }
        }
        if (dir != null && dir.isDirectory()) {
            final File[] listed = dir.listFiles((d, name) ->
                    name.startsWith("gadgetbridge") && (name.endsWith(".log") || name.contains(".log.")));
            if (listed != null) {
                final List<File> rotated = new ArrayList<>(Arrays.asList(listed));
                rotated.sort(Comparator.comparingLong(File::lastModified).reversed());
                for (final File f : rotated) {
                    if (!files.contains(f)) {
                        files.add(f);
                    }
                }
            }
        }
        return files;
    }

    private static List<File> activityFiles() {
        final List<File> files = new ArrayList<>();
        for (final GBDevice device : GBApplication.app().getDeviceManager().getDevices()) {
            try {
                final File dir = device.getDeviceCoordinator().getWritableExportDirectory(device, false);
                if (dir == null || !dir.isDirectory()) {
                    continue;
                }
                final File[] listed = dir.listFiles((d, name) -> {
                    final String lower = name.toLowerCase(java.util.Locale.ROOT);
                    return lower.endsWith(".fit") || lower.endsWith(".bin");
                });
                if (listed != null) {
                    files.addAll(Arrays.asList(listed));
                }
            } catch (final Exception ignored) {
            }
        }
        files.sort(Comparator.comparingLong(File::lastModified).reversed());
        return files.size() > MAX_FIT_FILES ? files.subList(0, MAX_FIT_FILES) : files;
    }

    public static boolean hasActivityFiles() {
        return !activityFiles().isEmpty();
    }

    private static byte[] readLogcat() {
        Process process = null;
        try {
            process = new ProcessBuilder(
                    "logcat", "-d", "-v", "threadtime", "-t", String.valueOf(LOGCAT_LINES))
                    .redirectErrorStream(true)
                    .start();
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (InputStream in = process.getInputStream()) {
                final byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    bos.write(buf, 0, n);
                }
            }
            process.waitFor();
            return bos.toByteArray();
        } catch (final Exception e) {
            LOG.warn("Pulse: logcat capture failed", e);
            return new byte[0];
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static void writeEntry(final ZipOutputStream zip, final String name, final byte[] data)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static long copyEntry(final ZipOutputStream zip, final String name, final File file,
                                  final long limit) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        long written = 0;
        try (FileInputStream in = new FileInputStream(file)) {
            final byte[] buf = new byte[8192];
            int n;
            while (written < limit && (n = in.read(buf)) != -1) {
                zip.write(buf, 0, n);
                written += n;
            }
        }
        zip.closeEntry();
        return written;
    }

    private static String upload(final File bundle, final String note, final boolean crash)
            throws IOException {
        final OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        final byte[] bytes = readFully(bundle);
        final RequestBody body = RequestBody.create(bytes, MediaType.parse("application/zip"));
        final Request request = new Request.Builder()
                .url(BuildConfig.PULSE_LOG_ENDPOINT)
                .header("Authorization", "Bearer " + BuildConfig.PULSE_LOG_TOKEN)
                .header("X-Pulse-App", ascii(BuildConfig.APPLICATION_ID + " " + BuildConfig.VERSION_NAME
                        + " (" + BuildConfig.VERSION_CODE + ")"))
                .header("X-Pulse-Android", ascii(Build.VERSION.RELEASE + " / " + Build.VERSION.SDK_INT))
                .header("X-Pulse-Device", ascii(Build.MANUFACTURER + " " + Build.MODEL))
                .header("X-Pulse-Crash", crash ? "1" : "0")
                .header("X-Pulse-Note", ascii(note != null ? note : ""))
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            final String payload = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            try {
                final String ref = new JSONObject(payload).optString("ref", "");
                return ref.isEmpty() ? "(sent)" : ref;
            } catch (final Exception e) {
                return "(sent)";
            }
        }
    }

    private static byte[] readFully(final File file) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream((int) Math.max(1024, file.length()));
        try (FileInputStream in = new FileInputStream(file)) {
            final byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
        }
        return bos.toByteArray();
    }

    private static String ascii(final String s) {
        final StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length() && sb.length() < 300; i++) {
            final char c = s.charAt(i);
            sb.append(c >= 0x20 && c < 0x7f ? c : '?');
        }
        return sb.toString();
    }
}
