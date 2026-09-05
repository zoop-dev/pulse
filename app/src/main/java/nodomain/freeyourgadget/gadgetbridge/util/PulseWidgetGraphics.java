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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.appcompat.content.res.AppCompatResources;

public final class PulseWidgetGraphics {
    private PulseWidgetGraphics() {
    }

    public static int dp(final Context context, final float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    public static Bitmap ring(final int sizePx, final float strokePx, final float pct,
                              final int trackColor, final int fillColor) {
        final Bitmap bmp = Bitmap.createBitmap(Math.max(sizePx, 1), Math.max(sizePx, 1), Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bmp);
        final float pad = strokePx / 2f + 1f;
        final RectF r = new RectF(pad, pad, sizePx - pad, sizePx - pad);
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(strokePx);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(trackColor);
        c.drawArc(r, 0f, 360f, false, p);
        final float sweep = Math.max(0f, Math.min(1f, pct)) * 360f;
        if (sweep > 0f) {
            p.setColor(fillColor);
            c.drawArc(r, -90f, sweep, false, p);
        }
        return bmp;
    }

    public static Bitmap bar(final int wPx, final int hPx, final float pct,
                             final int trackColor, final int fillColor) {
        final Bitmap bmp = Bitmap.createBitmap(Math.max(wPx, 1), Math.max(hPx, 1), Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bmp);
        final float radius = hPx / 2f;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(trackColor);
        c.drawRoundRect(new RectF(0, 0, wPx, hPx), radius, radius, p);
        final float w = Math.max(0f, Math.min(1f, pct)) * wPx;
        if (w > 0f) {
            p.setColor(fillColor);
            c.drawRoundRect(new RectF(0, 0, Math.max(w, hPx), hPx), radius, radius, p);
        }
        return bmp;
    }

    public static Bitmap spark(final int wPx, final int hPx, final int[] data,
                               final int lineColor, final boolean fillArea, final float strokePx) {
        final Bitmap bmp = Bitmap.createBitmap(Math.max(wPx, 1), Math.max(hPx, 1), Bitmap.Config.ARGB_8888);
        if (data == null || data.length < 2) {
            return bmp;
        }
        final Canvas c = new Canvas(bmp);
        int min = data[0];
        int max = data[0];
        for (final int v : data) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (max <= min) max = min + 1;
        final float pad = strokePx + 1f;
        final float usableW = wPx - pad * 2f;
        final float usableH = hPx - pad * 2f;
        final Path line = new Path();
        for (int i = 0; i < data.length; i++) {
            final float x = pad + usableW * i / (data.length - 1);
            final float y = pad + usableH * (1f - (data[i] - min) / (float) (max - min));
            if (i == 0) {
                line.moveTo(x, y);
            } else {
                line.lineTo(x, y);
            }
        }
        if (fillArea) {
            final Path area = new Path(line);
            area.lineTo(pad + usableW, hPx);
            area.lineTo(pad, hPx);
            area.close();
            final Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
            fp.setStyle(Paint.Style.FILL);
            fp.setColor((lineColor & 0x00FFFFFF) | 0x1F000000);
            c.drawPath(area, fp);
        }
        final Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setStyle(Paint.Style.STROKE);
        lp.setStrokeWidth(strokePx);
        lp.setStrokeCap(Paint.Cap.ROUND);
        lp.setStrokeJoin(Paint.Join.ROUND);
        lp.setColor(lineColor);
        c.drawPath(line, lp);
        return bmp;
    }

    public static Bitmap chip(final Context context, final int sizePx, final float cornerPx,
                              final int glyphRes, final int tint) {
        final Bitmap bmp = Bitmap.createBitmap(Math.max(sizePx, 1), Math.max(sizePx, 1), Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bmp);
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor((tint & 0x00FFFFFF) | 0x24000000);
        c.drawRoundRect(new RectF(0, 0, sizePx, sizePx), cornerPx, cornerPx, p);
        final Drawable glyph = AppCompatResources.getDrawable(context, glyphRes);
        if (glyph != null) {
            final int inset = Math.round(sizePx * 0.24f);
            glyph.setBounds(inset, inset, sizePx - inset, sizePx - inset);
            glyph.setColorFilter(new PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN));
            glyph.draw(c);
        }
        return bmp;
    }

    public static Bitmap glyph(final Context context, final int sizePx, final int glyphRes, final int tint) {
        final Bitmap bmp = Bitmap.createBitmap(Math.max(sizePx, 1), Math.max(sizePx, 1), Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bmp);
        final Drawable glyph = AppCompatResources.getDrawable(context, glyphRes);
        if (glyph != null) {
            glyph.setBounds(0, 0, sizePx, sizePx);
            glyph.setColorFilter(new PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN));
            glyph.draw(c);
        }
        return bmp;
    }
}
