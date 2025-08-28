package nodomain.freeyourgadget.gadgetbridge.activities.charts.marker;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;

public class ValueMarker extends MarkerView {
    private TextView markerContent;
    private List<ValueFormatter> valueFormatters;
    private List<String> valueUnits;
    private CombinedData lineData;

    public ValueMarker(Context context) {
        super(context, R.layout.value_marker);
        this.markerContent = findViewById(R.id.marker_content);
    }

    public ValueMarker(Context context, CombinedData lineData, List<ValueFormatter> valueFormatters, List<String> valueUnits) {
        super(context, R.layout.value_marker);
        this.markerContent = findViewById(R.id.marker_content);
        this.valueFormatters = valueFormatters;
        this.lineData = lineData;
        this.valueUnits = valueUnits;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        float xVal = e.getX();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < lineData.getDataSetCount(); i++) {
            IBarLineScatterCandleBubbleDataSet dataSet = lineData.getDataSetByIndex(i);
            if (dataSet == null || !dataSet.isVisible()) {
                continue;
            }
            Entry entryForX = dataSet.getEntryForXValue(xVal, Float.NaN);
            if (entryForX != null) {
                if (valueFormatters.get(i) != null) {
                    content.append(valueFormatters.get(i).getFormattedValue(entryForX.getY()));
                } else {
                    content.append(entryForX.getY());
                }
                if (valueUnits.get(i) != null) {
                    content.append(" ");
                    content.append(valueUnits.get(i));
                }
                content.append("\n");
            }
        }
        markerContent.setText(content.toString().trim());
        super.refreshContent(e, highlight);
    }

    @Override
    public void draw(Canvas canvas, float posX, float posY) {
        // Stick the value marker box to the top.
        MPPointF offset = getOffset();
        float markerWidth = getWidth();
        float offsetX = offset.x;

        if ((posX + offsetX) < 0) {
            posX = -offsetX;
        } else if ((posX + offsetX + markerWidth) > canvas.getWidth()) {
            posX = canvas.getWidth() - markerWidth - offsetX;
        }

        float fixedTopY = 10f;
        canvas.translate(posX + offsetX, fixedTopY);
        draw(canvas);
        canvas.translate(-posX - offsetX, -fixedTopY);
    }

    @Override
    public MPPointF getOffset() {
        // Center horizontally and place above the marker point
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }

}

