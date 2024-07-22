package nodomain.freeyourgadget.gadgetbridge.devices.igpsport;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MakibesHR3ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport.IGPSportDeviceSupport;

public class IGPSportCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_igpsport_bsc200;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_default;
    }

    @Override
    public String getManufacturer() {
        return "IGPSPORT";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return IGPSportDeviceSupport.class;
    }

    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_ASK;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        /* return Pattern.compile("Amazfit T-Rex", Pattern.CASE_INSENSITIVE); */
        /* return Pattern.compile("Xiaomi Smart Band 7.*");  */
      /* return Pattern.compile("Bangle\\.js.*|Pixl\\.js.*|Puck\\.js.*|MDBT42Q.*|Espruino.*"); /*
      /* return Pattern.compile("M6.*|M4.*|LH716|Sunset 6|Watch7|Fit1900"); */
        return Pattern.compile("BSC200");
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
//        Long deviceId = device.getId();
//        QueryBuilder<?> qb = session.getIGPSportSampleDao().queryBuilder();
//        qb.where(IGPSportSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

}
