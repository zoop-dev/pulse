package nodomain.freeyourgadget.gadgetbridge.devices.keephealth;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.KeephealthTemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.KeephealthTemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class KeephealthTemperatureSampleProvider extends AbstractTimeSampleProvider<KeephealthTemperatureSample> {
    public KeephealthTemperatureSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<KeephealthTemperatureSample, ?> getSampleDao() {
        return getSession().getKeephealthTemperatureSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return KeephealthTemperatureSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return KeephealthTemperatureSampleDao.Properties.DeviceId;
    }

    @Override
    public KeephealthTemperatureSample createSample() {
        return new KeephealthTemperatureSample();
    }
}