package nodomain.freeyourgadget.gadgetbridge.devices.keephealth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericHeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.KeephealthActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.KeephealthActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class KeephealthSampleProvider extends AbstractSampleProvider<KeephealthActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(KeephealthSampleProvider.class);

    private final GenericHeartRateSampleProvider heartRateProvider;

    public KeephealthSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
        this.heartRateProvider = new GenericHeartRateSampleProvider(device, session);
    }

    @Override
    public AbstractDao<KeephealthActivitySample, ?> getSampleDao() {
        return getSession().getKeephealthActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return KeephealthActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return KeephealthActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return KeephealthActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        switch (rawType) {
            case 1: // fall asleep in vendor app, is there a better kind?
                return ActivityKind.SLEEP_ANY;
            case 2: // LIGHT_SLEEP
                return ActivityKind.LIGHT_SLEEP;
            case 3: // DEEP_SLEEP
                return ActivityKind.DEEP_SLEEP;
            case 4: // awake
                return ActivityKind.AWAKE_SLEEP;
            case 5: // deep sleep in vendor app, but in code its rem
                return ActivityKind.REM_SLEEP;
            default:
                return ActivityKind.UNKNOWN;
        }
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        switch (activityKind) {
            case SLEEP_ANY: // fall asleep
                return 1;
            case LIGHT_SLEEP: // LIGHT_SLEEP
                return 2;
            case DEEP_SLEEP: // DEEP_SLEEP
                return 3;
            case AWAKE_SLEEP: // awake
                return 4;
            default:
                return 0;
        }
    }


    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / 255.0f;
    }

    @Override
    public KeephealthActivitySample createActivitySample() {
        return new KeephealthActivitySample();
    }

    @Override
    protected List<KeephealthActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to) {
        LOG.debug(
                "Getting Keephealth activity samples between {} and {}",
                timestamp_from,
                timestamp_to
        );
        final long nanoStart = System.nanoTime();

        final List<KeephealthActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to);
        final Map<Integer, KeephealthActivitySample> sampleByTs = new HashMap<>();
        for (final KeephealthActivitySample sample : samples) {
            sampleByTs.put(sample.getTimestamp(), sample);
        }

        List<GenericHeartRateSample> hrSamples = this.heartRateProvider.getAllSamples(timestamp_from * 1000L, timestamp_to * 1000L);
        for (GenericHeartRateSample hrSample : hrSamples) {
            int timestamp = (int) (hrSample.getTimestamp() / 1000L);
            if (sampleByTs.containsKey(timestamp)) {
                Objects.requireNonNull(sampleByTs.get(timestamp)).setHeartRate(hrSample.getHeartRate());
            } else {
                KeephealthActivitySample activitySample = new KeephealthActivitySample();
                activitySample.setProvider(this);
                activitySample.setTimestamp(timestamp);
                activitySample.setHeartRate(hrSample.getHeartRate());
                samples.add(activitySample);
                sampleByTs.put(activitySample.getTimestamp(), activitySample);
            }
        }

        final List<KeephealthActivitySample> finalSamples = new ArrayList<>(sampleByTs.values());
        Collections.sort(finalSamples, (a, b) -> Integer.compare(a.getTimestamp(), b.getTimestamp()));

        final long nanoEnd = System.nanoTime();
        final long executionTime = (nanoEnd - nanoStart) / 1000000;
        LOG.debug("Getting Keephealth samples took {}ms", executionTime);

        return finalSamples;
    }

}