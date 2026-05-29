package nodomain.freeyourgadget.gadgetbridge.devices.keephealth;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.KeephealthBloodPressureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.KeephealthBloodPressureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class KeephealthBloodPressureSampleProvider extends AbstractTimeSampleProvider<KeephealthBloodPressureSample> {
    public KeephealthBloodPressureSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<KeephealthBloodPressureSample, ?> getSampleDao() {
        return getSession().getKeephealthBloodPressureSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return KeephealthBloodPressureSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return KeephealthBloodPressureSampleDao.Properties.DeviceId;
    }

    @Override
    public KeephealthBloodPressureSample createSample() {
        return new KeephealthBloodPressureSample();
    }
}