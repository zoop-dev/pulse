/*
    Copyright (C) 2026 Christian Breiteneder

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import androidx.core.content.FileProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.BloodPressureSample;

public class BloodPressureExportHelper {
    private static final Logger LOG = LoggerFactory.getLogger(BloodPressureExportHelper.class);

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String FILE_DATE_PATTERN = "yyyy-MM-dd";

    private static SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
    }

    private static SimpleDateFormat getFileDateFormat() {
        return new SimpleDateFormat(FILE_DATE_PATTERN, Locale.getDefault());
    }

    public static void exportCsv(Context context, List<? extends BloodPressureSample> samples) {
        try {
            File exportDir = new File(context.getCacheDir(), "csv");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                LOG.error("Failed to create export directory for csv file");
                return;
            }

            Date lastSampleDate = new Date(samples.get(samples.size() - 1).getTimestamp());
            String fileName = "blood_pressure_" + getFileDateFormat().format(lastSampleDate) + ".csv";
            File file = new File(exportDir, fileName);

            String colDate = context.getString(R.string.blood_pressure_export_date);
            String colTime = context.getString(R.string.pref_header_time);
            String colSystolic = context.getString(R.string.blood_pressure_systolic) + " (mmHg)";
            String colDiastolic = context.getString(R.string.blood_pressure_diastolic) + " (mmHg)";

            SimpleDateFormat dateFormat = getDateFormat();

            FileWriter writer = new FileWriter(file);
            writer.append(colDate).append(",")
                    .append(colTime).append(",")
                    .append(colSystolic).append(",")
                    .append(colDiastolic).append("\n");

            for (BloodPressureSample sample : samples) {
                String dateTime = dateFormat.format(new Date(sample.getTimestamp()));
                String[] parts = dateTime.split(" ");
                writer.append(parts[0])
                        .append(",")
                        .append(parts[1])
                        .append(",")
                        .append(String.valueOf(sample.getBpSystolic()))
                        .append(",")
                        .append(String.valueOf(sample.getBpDiastolic()))
                        .append("\n");
            }

            writer.flush();
            writer.close();

            shareFile(context, file, "text/csv");
        } catch (IOException e) {
            LOG.error("Error exporting CSV", e);
        }
    }

    public static void exportPdf(Context context, List<? extends BloodPressureSample> samples, String dateLabel, Bitmap chartBitmap) {
        try {
            File exportDir = new File(context.getCacheDir(), "pdf");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                LOG.error("Failed to create export directory for pdf file");
                return;
            }

            Date lastSampleDate = new Date(samples.get(samples.size() - 1).getTimestamp());
            String fileName = "blood_pressure_" + getFileDateFormat().format(lastSampleDate) + ".pdf";
            File file = new File(exportDir, fileName);

            // Localized strings
            String reportTitle = context.getString(R.string.blood_pressure);
            String colDate = context.getString(R.string.blood_pressure_export_date);
            String colTime = context.getString(R.string.pref_header_time);
            String colSystolic = context.getString(R.string.blood_pressure_systolic) + " (mmHg)";
            String colDiastolic = context.getString(R.string.blood_pressure_diastolic) + " (mmHg)";

            // User info
            ActivityUser user = new ActivityUser();
            String userName = user.getName();
            String userDob = user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "";

            SimpleDateFormat dateFormat = getDateFormat();

            PdfDocument document = new PdfDocument();

            int pageWidth = 595;  // A4
            int pageHeight = 842;
            int margin = 40;
            int rowHeight = 18;

            Paint titlePaint = new Paint();
            titlePaint.setTextSize(18);
            titlePaint.setFakeBoldText(true);
            titlePaint.setAntiAlias(true);

            Paint subtitlePaint = new Paint();
            subtitlePaint.setTextSize(11);
            subtitlePaint.setAntiAlias(true);
            subtitlePaint.setColor(0xFF666666);

            Paint userPaint = new Paint();
            userPaint.setTextSize(11);
            userPaint.setAntiAlias(true);

            Paint headerPaint = new Paint();
            headerPaint.setTextSize(10);
            headerPaint.setFakeBoldText(true);
            headerPaint.setAntiAlias(true);

            Paint cellPaint = new Paint();
            cellPaint.setTextSize(9);
            cellPaint.setAntiAlias(true);

            Paint linePaint = new Paint();
            linePaint.setColor(0xFFCCCCCC);
            linePaint.setStrokeWidth(0.5f);

            // === PAGE 1: Header + Chart + beginning of table ===

            // Calculate chart height
            int chartHeight = 0;
            int chartTop;
            if (chartBitmap != null) {
                float aspectRatio = (float) chartBitmap.getHeight() / chartBitmap.getWidth();
                int chartWidth = pageWidth - 2 * margin;
                chartHeight = (int) (chartWidth * aspectRatio);
                chartHeight = Math.min(chartHeight, 250);
            }

            // Layout positions
            float yPos = margin;

            // Title
            yPos += 20;
            float titleY = yPos;
            yPos += 8;

            // Date label
            yPos += 14;
            float dateLabelY = yPos;
            yPos += 6;

            // User info
            if (userName != null && !userName.isEmpty()) {
                yPos += 16;
            }
            if (!userDob.isEmpty()) {
                yPos += 14;
            }

            // Chart
            yPos += 10;
            chartTop = (int) yPos;
            if (chartBitmap != null) {
                yPos += chartHeight + 10;
            }

            // Table starts here
            float tableStartY = yPos + 10;
            float usableForTable = pageHeight - tableStartY - margin;
            int rowsOnFirstPage = Math.max(0, (int) (usableForTable / rowHeight) - 1);

            int totalRows = samples.size();
            int remainingRows = totalRows - rowsOnFirstPage;
            int rowsPerFullPage = (pageHeight - margin * 2 - 30) / rowHeight;
            int fullTablePages = remainingRows > 0 ? (int) Math.ceil((double) remainingRows / rowsPerFullPage) : 0;
            int totalPages = 1 + fullTablePages;

            float col2 = margin + 130;
            float col3 = margin + 260;
            float col4 = margin + 390;

            // --- Draw page 1 ---
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page pdfPage = document.startPage(pageInfo);
            Canvas canvas = pdfPage.getCanvas();

            // Title
            canvas.drawText(reportTitle, margin, titleY, titlePaint);

            // Date label
            canvas.drawText(dateLabel, margin, dateLabelY, subtitlePaint);

            // User info (right-aligned)
            float rightX = pageWidth - margin;
            if (userName != null && !userName.isEmpty()) {
                float nameWidth = userPaint.measureText(userName);
                canvas.drawText(userName, rightX - nameWidth, titleY, userPaint);
            }
            if (!userDob.isEmpty()) {
                String dobText = context.getString(R.string.blood_pressure_export_dob, userDob);
                float dobWidth = subtitlePaint.measureText(dobText);
                canvas.drawText(dobText, rightX - dobWidth, dateLabelY, subtitlePaint);
            }

            // Chart bitmap
            if (chartBitmap != null) {
                int chartWidth = pageWidth - 2 * margin;
                Rect destRect = new Rect(margin, chartTop, margin + chartWidth, chartTop + chartHeight);

                // White background (chart may use dark theme)
                Bitmap whiteBgBitmap = Bitmap.createBitmap(chartBitmap.getWidth(), chartBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas bmpCanvas = new Canvas(whiteBgBitmap);
                bmpCanvas.drawColor(0xFFFFFFFF);
                bmpCanvas.drawBitmap(chartBitmap, 0, 0, null);

                canvas.drawBitmap(whiteBgBitmap, null, destRect, null);
                whiteBgBitmap.recycle();
            }

            // Table header
            float tableHeaderY = tableStartY + 12;
            canvas.drawText(colDate, margin, tableHeaderY, headerPaint);
            canvas.drawText(colTime, col2, tableHeaderY, headerPaint);
            canvas.drawText(colSystolic, col3, tableHeaderY, headerPaint);
            canvas.drawText(colDiastolic, col4, tableHeaderY, headerPaint);
            canvas.drawLine(margin, tableHeaderY + 4, pageWidth - margin, tableHeaderY + 4, linePaint);

            // Table rows on page 1
            int sampleIndex = 0;
            int rowsToDraw = Math.min(rowsOnFirstPage, totalRows);
            for (int row = 0; row < rowsToDraw; row++) {
                BloodPressureSample sample = samples.get(sampleIndex);
                float y = tableHeaderY + 20 + (row * rowHeight);

                String dateTime = dateFormat.format(new Date(sample.getTimestamp()));
                String[] parts = dateTime.split(" ");

                canvas.drawText(parts[0], margin, y, cellPaint);
                canvas.drawText(parts[1], col2, y, cellPaint);
                canvas.drawText(String.valueOf(sample.getBpSystolic()), col3, y, cellPaint);
                canvas.drawText(String.valueOf(sample.getBpDiastolic()), col4, y, cellPaint);
                canvas.drawLine(margin, y + 4, pageWidth - margin, y + 4, linePaint);

                sampleIndex++;
            }

            drawPageNumber(canvas, subtitlePaint, 1, totalPages, pageWidth, pageHeight, margin);
            document.finishPage(pdfPage);

            // === ADDITIONAL PAGES ===
            while (sampleIndex < totalRows) {
                int currentPage = (sampleIndex - rowsOnFirstPage) / rowsPerFullPage + 2;
                PdfDocument.PageInfo nextPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create();
                PdfDocument.Page nextPage = document.startPage(nextPageInfo);
                Canvas nextCanvas = nextPage.getCanvas();

                float nextHeaderY = margin + 16;
                nextCanvas.drawText(colDate, margin, nextHeaderY, headerPaint);
                nextCanvas.drawText(colTime, col2, nextHeaderY, headerPaint);
                nextCanvas.drawText(colSystolic, col3, nextHeaderY, headerPaint);
                nextCanvas.drawText(colDiastolic, col4, nextHeaderY, headerPaint);
                nextCanvas.drawLine(margin, nextHeaderY + 4, pageWidth - margin, nextHeaderY + 4, linePaint);

                int rowsThisPage = Math.min(rowsPerFullPage, totalRows - sampleIndex);

                for (int row = 0; row < rowsThisPage; row++) {
                    BloodPressureSample sample = samples.get(sampleIndex);
                    float y = nextHeaderY + 20 + (row * rowHeight);

                    String dateTime = dateFormat.format(new Date(sample.getTimestamp()));
                    String[] parts = dateTime.split(" ");

                    nextCanvas.drawText(parts[0], margin, y, cellPaint);
                    nextCanvas.drawText(parts[1], col2, y, cellPaint);
                    nextCanvas.drawText(String.valueOf(sample.getBpSystolic()), col3, y, cellPaint);
                    nextCanvas.drawText(String.valueOf(sample.getBpDiastolic()), col4, y, cellPaint);
                    nextCanvas.drawLine(margin, y + 4, pageWidth - margin, y + 4, linePaint);

                    sampleIndex++;
                }

                drawPageNumber(nextCanvas, subtitlePaint, currentPage, totalPages, pageWidth, pageHeight, margin);
                document.finishPage(nextPage);
            }

            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            shareFile(context, file, "application/pdf");
        } catch (IOException e) {
            LOG.error("Error exporting PDF", e);
        }
    }

    private static void drawPageNumber(Canvas canvas, Paint paint, int current, int total, int pageWidth, int pageHeight, int margin) {
        String pageNum = current + " / " + total;
        float pageNumWidth = paint.measureText(pageNum);
        canvas.drawText(pageNum, pageWidth - margin - pageNumWidth, pageHeight - margin + 10, paint);
    }

    private static void shareFile(Context context, File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".screenshot_provider",
                file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, null));
    }
}