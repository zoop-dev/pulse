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

public abstract class IGPSportAbstractCoordinator extends AbstractBLEDeviceCoordinator {
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
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
//        Long deviceId = device.getId();
//        QueryBuilder<?> qb = session.getIGPSportSampleDao().queryBuilder();
//        qb.where(IGPSportSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAppListFetching() {
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
