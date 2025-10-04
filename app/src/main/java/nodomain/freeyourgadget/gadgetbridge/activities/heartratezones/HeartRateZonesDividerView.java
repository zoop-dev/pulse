package nodomain.freeyourgadget.gadgetbridge.activities.heartratezones;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

public class HeartRateZonesDividerView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int linePx;

    public HeartRateZonesDividerView(Context context) {
        this(context, null);
    }

    public HeartRateZonesDividerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeartRateZonesDividerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.linePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0f, getResources().getDisplayMetrics());
        this.paint.setColor(Color.GRAY);
        this.paint.setStrokeWidth(this.linePx);
        this.paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int horizontalCenter = width / 2;
        int verticalCenter = height / 2;
        canvas.save();
        canvas.drawLine(horizontalCenter, this.linePx,  width, this.linePx, this.paint);
        canvas.drawLine(0.0f, verticalCenter,  horizontalCenter, verticalCenter, this.paint);
        canvas.drawLine(horizontalCenter, this.linePx, horizontalCenter, height - this.linePx, this.paint);
        canvas.drawLine(horizontalCenter, height - this.linePx, width, height - this.linePx, this.paint);
        canvas.restore();
    }
}