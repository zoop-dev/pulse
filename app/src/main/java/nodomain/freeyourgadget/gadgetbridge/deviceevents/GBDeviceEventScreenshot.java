/*  Copyright (C) 2015-2024 Andreas Shimokawa, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GBDeviceEventScreenshot extends GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventScreenshot.class);

    private static final int NOTIFICATION_ID_SCREENSHOT = 8000;

    private final byte[] data;

    public GBDeviceEventScreenshot(final byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        if (getData() == null) {
            LOG.warn("Screenshot data is null");
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-hhmmss", Locale.US);
        String filename = "screenshot_" + dateFormat.format(new Date()) + ".bmp";

        try {
            final String fullpath = GB.writeScreenshot(this, filename);
            final Bitmap bmp = BitmapFactory.decodeFile(fullpath);
            final Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            final Uri screenshotURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".screenshot_provider", new File(fullpath));
            intent.setDataAndType(screenshotURI, "image/*");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            int flags1 = 0;
            flags1 |= PendingIntent.FLAG_IMMUTABLE;
            final PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, flags1);

            final Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, screenshotURI);

            Intent intent1 = Intent.createChooser(shareIntent, context.getString(R.string.share_screenshot));
            final PendingIntent pendingShareIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent1,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            final NotificationCompat.Action action = new NotificationCompat.Action.Builder(android.R.drawable.ic_menu_share, context.getString(R.string.share), pendingShareIntent).build();

            final Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID)
                    .setContentTitle(context.getString(R.string.screenshot_taken))
                    .setTicker(context.getString(R.string.screenshot_taken))
                    .setContentText(filename)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(bmp))
                    .setContentIntent(pIntent)
                    .addAction(action)
                    .setAutoCancel(true)
                    .build();

            GB.notify(NOTIFICATION_ID_SCREENSHOT, notification, context);
        } catch (IOException ex) {
            LOG.error("Error writing screenshot", ex);
        }
    }
}
