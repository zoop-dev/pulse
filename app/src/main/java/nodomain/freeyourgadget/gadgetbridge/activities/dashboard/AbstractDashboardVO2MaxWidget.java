package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.VO2MaxRanges;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.Vo2MaxSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;

public abstract class AbstractDashboardVO2MaxWidget extends AbstractGaugeWidget implements DashboardVO2MaxWidgetInterface {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractDashboardVO2MaxWidget.class);

    public AbstractDashboardVO2MaxWidget(int label, @Nullable String targetActivityTab) {
        super(label, targetActivityTab);
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        final List<GBDevice> devices = getSupportedDevices(dashboardData);
        final VO2MaxData data = new VO2MaxData();

        // Latest vo2max sample.
        Vo2MaxSample sample = null;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                final Vo2MaxSampleProvider sampleProvider = (Vo2MaxSampleProvider) dev.getDeviceCoordinator().getVo2MaxSampleProvider(dev, dbHandler.getDaoSession());
                final Vo2MaxSample latestSample = sampleProvider.getLatestSample(getVO2MaxType(), dashboardData.timeTo * 1000L);
                if (latestSample != null && (sample == null || latestSample.getTimestamp() > sample.getTimestamp())) {
                    sample = latestSample;
                }
            }

            if (sample != null) {
                data.value = sample.getValue();
            }

        } catch (final Exception e) {
            LOG.error("Could not get vo2max for today", e);
        }

        dashboardData.put(getWidgetKey(), data);
    }

    public static int[] getColors() {
        return new int[]{
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_poor_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_fair_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_good_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_excellent_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_superior_color),
        };
    }

    public static float[] getSegments() {
        // Should match the percentiles in VO2MaxRanges
        return new float[] {
                0.40F,
                0.20F,
                0.20F,
                0.15F,
                0.05F,
        };
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        final VO2MaxData vo2MaxData = (VO2MaxData) dashboardData.get(getWidgetKey());
        if (vo2MaxData == null) {
            drawSimpleGauge(0, -1);
            return;
        }

        final int[] colors = getColors();
        final float[] segments = getSegments();
        final ActivityUser activityUser = new ActivityUser();
        final int age = activityUser.getAgeAt(LocalDate.ofInstant(Instant.ofEpochSecond(dashboardData.timeTo), ZoneId.systemDefault()));
        float vo2MaxValue = VO2MaxRanges.INSTANCE.calculateVO2MaxPercentile(vo2MaxData.value != -1 ? vo2MaxData.value : 0, age, activityUser.getGender());
        setText(String.valueOf(vo2MaxData.value != -1 ? Math.round(vo2MaxData.value) : "-"));
        drawSegmentedGauge(
                colors,
                segments,
                vo2MaxValue,
                false,
                true
        );
    }

    private static class VO2MaxData implements Serializable {
        private float value = -1;
    }
}
