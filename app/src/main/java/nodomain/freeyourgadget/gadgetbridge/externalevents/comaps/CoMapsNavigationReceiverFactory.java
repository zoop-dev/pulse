package nodomain.freeyourgadget.gadgetbridge.externalevents.comaps;

import android.app.Application;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoMapsNavigationReceiverFactory {

    public static final String[] APP_VERSIONS = {
            "app.comaps",
            "app.comaps.fdroid",
            "app.comaps.google"
    };

    public static final String PERMISSION_SUFFIX = ".permission.READ_NAVIGATION_DATA";
    public static final String AUTHORITY_SUFFIX = ".provider.navigation";

    private CoMapsNavigationReceiverFactory() { }

    public static List<Pair<Uri, CoMapsNavigationReceiver>> createCoMapsNavigationReceiversForApplication(
            Application application, Handler handler) {
        return discoverInstalledVersions(application.getPackageManager()).stream()
                .map(app -> {
                    Uri uri = Uri.parse("content://" + app + AUTHORITY_SUFFIX + "/live");
                    CoMapsNavigationReceiver receiver = new CoMapsNavigationReceiver(handler, application, uri);
                    return new Pair<>(uri, receiver);
                }).toList();
    }

    public static List<String> discoverInstalledVersions(PackageManager packageManager) {
        List<String> installed = new ArrayList<>();

        for (String app : APP_VERSIONS) {
            if (packageManager.resolveContentProvider(app + AUTHORITY_SUFFIX, 0) != null) {
                installed.add(app);
            }
        }

        return Collections.unmodifiableList(installed);
    }

}
