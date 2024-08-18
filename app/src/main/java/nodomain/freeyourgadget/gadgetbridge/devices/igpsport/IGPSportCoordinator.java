package nodomain.freeyourgadget.gadgetbridge.devices.igpsport;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
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

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return true;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return AppManagerActivity.class;
    }


    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {

//        final IGPSportAgpsInstallHandler agpsInstallHandler = new IGPSportAgpsInstallHandler(uri, context);
//        if (agpsInstallHandler.isValid()) {
//            return agpsInstallHandler;
//        }

        final IGPSportRouteInstallHandler routeInstallHandler = new IGPSportRouteInstallHandler(uri, context);
        if (routeInstallHandler.isValid()) {
            return routeInstallHandler;
        }

        return null;
    }


}
