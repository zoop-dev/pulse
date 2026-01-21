package nodomain.freeyourgadget.internethelper.aidl.wifi;

import nodomain.freeyourgadget.internethelper.aidl.wifi.IWifiCallback;

interface IWifiService {
    int version();
    String getCurrentSsid();
}
