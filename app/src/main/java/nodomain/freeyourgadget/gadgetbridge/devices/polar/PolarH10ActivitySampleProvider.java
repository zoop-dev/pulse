package nodomain.freeyourgadget.gadgetbridge.devices.polar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Optional;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.PolarH10ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.PolarH10ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class PolarH10ActivitySampleProvider extends AbstractSampleProvider<PolarH10ActivitySample> {
    public PolarH10ActivitySampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<PolarH10ActivitySample, ?> getSampleDao() {
        return getSession().getPolarH10ActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return null;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return PolarH10ActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return PolarH10ActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        return ActivityKind.fromCode(rawType);
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        return activityKind.getCode();
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity;
    }

    /**
     * Factory method to creates an empty sample of the correct type for this sample provider
     *
     * @return the newly created "empty" sample
     */
    @Override
    public PolarH10ActivitySample createActivitySample() {
        return new PolarH10ActivitySample();
    }

    public Optional<PolarH10ActivitySample> getSampleForTimestamp(int timestamp) {
        List<PolarH10ActivitySample> foundSamples = this.getGBActivitySamples(timestamp, timestamp);
        if (foundSamples.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(foundSamples.get(0));
    }
}
