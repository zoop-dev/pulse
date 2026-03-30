/* Copyright (C) 2019-2024 Arjan Schrijver, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;

public class TimePicker extends MaterialAlertDialogBuilder {
    ImageView pickerView;
    Canvas pickerCanvas;
    Bitmap pickerBitmap;

    NotificationConfiguration settings;

    int height, width, radius;
    int radius1, radius2, radius3;
    int controlledHand = 0;
    int handRadius;

    AlertDialog dialog;

    OnFinishListener finishListener;
    OnHandsSetListener handsListener;
    OnVibrationSetListener vibrationListener;

    protected TimePicker(@NonNull Context context, PackageInfo info) {
        super(context);

        settings = new NotificationConfiguration(info.packageName, context.getApplicationContext().getPackageManager().getApplicationLabel(info.applicationInfo).toString());
        initGraphics(context);
    }

    protected TimePicker(Context context, NotificationConfiguration config){
        super(context);

        settings = config;
        initGraphics(context);
    }

    private void initGraphics(Context context){
        int w = (int) (((WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth() * 0.8);
        height = w;
        width = w;
        radius = (int) (w * 0.06);
        radius1 = 0;
        radius2 = (int) (radius * 2.3);
        radius3 = (int)(radius2 * 2.15);
        int offset = (int) (w * 0.1);
        radius1 += offset;
        radius2 += offset;
        radius3 += offset;

        pickerView = new ImageView(context);
        pickerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        pickerCanvas = new Canvas(pickerBitmap);

        drawClock();

        // Main container for all UI elements
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(pickerView);

        CheckBox box = new CheckBox(context);
        box.setText("Respect silent mode");
        box.setChecked(settings.getRespectSilentMode());
        box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                settings.setRespectSilentMode(b);
            }
        });
        layout.addView(box);

        RadioGroup group = new RadioGroup(context);
        for(PlayNotificationRequest.VibrationType vibe: PlayNotificationRequest.VibrationType.values()){
            RadioButton button = new RadioButton(context);
            button.setText(vibe.toString());
            button.setId(vibe.getValue());
            group.addView(button);
        }

        group.check(settings.getVibration().getValue());
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                settings.setVibration(PlayNotificationRequest.VibrationType.fromValue((byte)i));
                if(TimePicker.this.vibrationListener != null) TimePicker.this.vibrationListener.onVibrationSet(settings);
            }
        });

        // Add the RadioGroup directly to the LinearLayout
        layout.addView(group);

        // Wrap the entire layout in a ScrollView so the whole dialog scrolls
        ScrollView mainScrollView = new ScrollView(context);
        mainScrollView.addView(layout);

        // Set the ScrollView as the dialog's view
        setView(mainScrollView);



        setNegativeButton("cancel", null);
        setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(finishListener == null) return;
                finishListener.onFinish(true, settings);
            }
        });
        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if(finishListener == null) return;
                finishListener.onFinish(false, settings);
            }
        });
        setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if(finishListener == null) return;
                finishListener.onFinish(false, settings);
            }
        });
        dialog = show();

        pickerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // Request the parent ScrollView not to intercept touch events
                // while the user is actively dragging the clock hands.
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                handleTouch(dialog, motionEvent);
                return true;
            }
        });
    }

    public NotificationConfiguration getSettings() {
        return settings;
    }

    private void handleTouch(AlertDialog dialog, MotionEvent event) {
        int centerX = width / 2;
        int centerY = height / 2;
        int difX = centerX - (int) event.getX();
        int difY = (int) event.getY() - centerY;
        int dist = (int) Math.sqrt(Math.abs(difX) * Math.abs(difX) + Math.abs(difY) * Math.abs(difY));
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                int radiusHalf = radius;
                if (dist < (radius1 + radiusHalf) && dist > (radius1 - radiusHalf)) {
                    Log.d("Settings", "hit sub");
                    handRadius = (int) (height / 2f - radius1);
                    controlledHand = 3;
                } else if (dist < (radius2 + radiusHalf) && dist > (radius2 - radiusHalf)) {
                    Log.d("Settings", "hit hour");
                    controlledHand = 1;
                    handRadius = (int) (height / 2f - radius2);
                } else if (dist < (radius3 + radiusHalf) && dist > (radius3 - radiusHalf)) {
                    Log.d("Settings", "hit minute");
                    controlledHand = 2;
                    handRadius = (int) (height / 2f - radius3);
                } else {
                    Log.d("Settings", "hit nothing");
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (controlledHand == 0) return;
                double degree = difY == 0 ? (difX < 0 ? 90 : 270) : Math.toDegrees(Math.atan((float) difX / (float) difY));
                if (difY > 0) degree = 180 + degree;
                if (degree < 0) degree = 360 + degree;
                switch (controlledHand) {
                    case 1: {
                        settings.setHour((short) (((int)(degree + 15) / 30) * 30 % 360));
                        break;
                    }
                    case 2: {
                        settings.setMin((short) (((int)(degree + 15) / 30) * 30 % 360));
                        break;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(true);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAlpha(1f);
                if(handsListener != null) handsListener.onHandsSet(settings);
                break;
            }
        }
        drawClock();
    }


    private void drawClock() {
        // Clear background
        Paint white = new Paint();
        white.setColor(Color.WHITE);
        white.setStyle(Paint.Style.FILL);
        pickerCanvas.drawCircle(width / 2f, height / 2f, width / 2f, white);

        int centerX = width / 2;
        int centerY = height / 2;

        // --- DRAW CLOCK LABELS ---
        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Calculate radii for the text rings
        float minuteLabelRadius = (width / 2f) * 0.88f;
        float hourLabelRadius = (width / 2f) * 0.65f;

        // Draw outer minute labels (0, 5, 10... 55)
        labelPaint.setTextSize(width * 0.045f);
        labelPaint.setColor(Color.DKGRAY);
        float minuteTextShiftY = (labelPaint.descent() + labelPaint.ascent()) / 2;

        for (int i = 0; i < 60; i += 5) {
            float angle = i * 6; // 360 degrees / 60 minutes
            float x = (float) (centerX + Math.sin(Math.toRadians(angle)) * minuteLabelRadius);
            float y = (float) (centerY - Math.cos(Math.toRadians(angle)) * minuteLabelRadius);
            pickerCanvas.drawText(String.valueOf(i), x, y - minuteTextShiftY, labelPaint);
        }

        // Draw inner hour labels (1 to 12)
        labelPaint.setTextSize(width * 0.065f);
        labelPaint.setColor(Color.BLACK);
        labelPaint.setFakeBoldText(true);
        float hourTextShiftY = (labelPaint.descent() + labelPaint.ascent()) / 2;

        for (int i = 1; i <= 12; i++) {
            float angle = i * 30; // 360 degrees / 12 hours
            float x = (float) (centerX + Math.sin(Math.toRadians(angle)) * hourLabelRadius);
            float y = (float) (centerY - Math.cos(Math.toRadians(angle)) * hourLabelRadius);
            pickerCanvas.drawText(String.valueOf(i), x, y - hourTextShiftY, labelPaint);
        }
        // -------------------------

        // --- DRAW HANDS ---
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.BLUE);

        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setStyle(Paint.Style.FILL);
        text.setTextSize(radius * 1.5f);
        text.setColor(Color.BLACK);
        text.setTextAlign(Paint.Align.CENTER);
        int textShiftY = (int) ((text.descent() + text.ascent()) / 2);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(10);
        linePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        linePaint.setColor(Color.BLACK);

        // Draw Minute Hand
        if (settings.getMin() != -1) {
            paint.setAlpha(255);
            float x = (float) (centerX + Math.sin(Math.toRadians(settings.getMin())) * (float) radius3);
            float y = (float) (centerY - Math.cos(Math.toRadians(settings.getMin())) * (float) radius3);
            linePaint.setAlpha(255);
            pickerCanvas.drawLine(centerX, centerY, x, y, linePaint);
            pickerCanvas.drawCircle(x, y, radius, paint);

            // Floating exact minute text
            pickerCanvas.drawText(String.valueOf(settings.getMin() / 6), x, y - textShiftY, text);
        }

        // Draw Hour Hand
        if (settings.getHour() != -1) {
            paint.setAlpha(255);
            float x = (float) (centerX + Math.sin(Math.toRadians(settings.getHour())) * (float) radius2);
            float y = (float) (centerY - Math.cos(Math.toRadians(settings.getHour())) * (float) radius2);
            linePaint.setAlpha(255);
            pickerCanvas.drawLine(centerX, centerY, x, y, linePaint);
            pickerCanvas.drawCircle(x, y, radius, paint);

            // Floating exact hour text
            pickerCanvas.drawText(settings.getHour() == 0 ? "12" : String.valueOf(settings.getHour() / 30), x, y - textShiftY, text);
        }

        // Draw Center Pin
        Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint2.setColor(Color.BLACK);
        paint2.setStyle(Paint.Style.FILL_AND_STROKE);
        pickerCanvas.drawCircle(centerX, centerY, 8, paint2);

        pickerView.setImageBitmap(pickerBitmap);
    }

    interface OnFinishListener{
        public void onFinish(boolean success, NotificationConfiguration config);
    }

    interface OnHandsSetListener{
        public void onHandsSet(NotificationConfiguration config);
    }

    interface OnVibrationSetListener{
        public void onVibrationSet(NotificationConfiguration config);
    }
}
