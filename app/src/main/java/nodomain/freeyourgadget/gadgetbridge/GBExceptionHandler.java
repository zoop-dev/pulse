/*  Copyright (C) 2015-2025 Carsten Pfeiffer, José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge;


import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_ID_ERROR;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Catches otherwise uncaught exceptions, logs them and terminates the app.
 */
public class GBExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GBExceptionHandler.class);
    private final Thread.UncaughtExceptionHandler mDelegate;
    private final boolean mNotifyOnCrash;

    public GBExceptionHandler(Thread.UncaughtExceptionHandler delegate, final boolean notifyOnCrash) {
        mDelegate = delegate;
        mNotifyOnCrash = notifyOnCrash;
    }

    /// Log and notify the unhandled exception
    /// Flushing and closing the log is handled by {@link GBApplication.ShutdownHook}
    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        // This method is only called if something is seriously wrong so be very generous
        // with try-catch.
        try {
            LOG.error("Uncaught exception in {}", thread.getName(), ex);
        } catch (Throwable ignored) {
        }

        if (mNotifyOnCrash) {
            try {
                showNotification(ex);
            } catch (Throwable ignored) {
            }
        }

        // Heap dump on OOM in debug builds
        if (BuildConfig.DEBUG && (ex.getClass().equals(OutOfMemoryError.class)
                || (ex.getCause() != null && ex.getCause().getClass().equals(OutOfMemoryError.class)))) {
            try {
                final File cacheDir = GBApplication.getContext().getExternalCacheDir();
                if (cacheDir != null) {
                    final File oomDir = new File(cacheDir.getAbsolutePath() + File.separator + "oom");
                    //noinspection ResultOfMethodCallIgnored
                    oomDir.mkdirs();
                    final String dumpPath = oomDir.getAbsolutePath() + File.separator + "oom-" + System.currentTimeMillis() + ".hprof";
                    LOG.debug("Dumping hprof data to: {}", dumpPath);
                    android.os.Debug.dumpHprofData(dumpPath);
                }
            } catch (final Throwable t) {
                LOG.error("Failed to dump hprof on oom", t);
            }
        }

        if (mDelegate != null) {
            try {
                mDelegate.uncaughtException(thread, ex);
            } catch (Throwable ignored) {
            }
        } else {
            System.exit(1);
        }
    }

    private void showNotification(final Throwable e) {
        final Context context = GBApplication.getContext();

        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, Log.getStackTraceString(e));
        shareIntent.setType("text/plain");

        Intent intent = Intent.createChooser(shareIntent, context.getString(R.string.app_crash_share_stacktrace));
        final PendingIntent pendingShareIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        final NotificationCompat.Action shareAction = new NotificationCompat.Action.Builder(android.R.drawable.ic_menu_share, context.getString(R.string.share), pendingShareIntent).build();

        final Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(
                        R.string.app_crash_notification_title,
                        context.getString(R.string.app_name)
                ))
                .setContentText(e.getLocalizedMessage())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(shareAction)
                .build();

        GB.notify(NOTIFICATION_ID_ERROR, notification, context);
    }
}
