package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.P2P;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;

public class HuaweiP2PDirection extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PDirection.class);
    private final AtomicBoolean isRegistered = new AtomicBoolean(false);

    public static final String MODULE = "com.huawei.maps.app";

    // NOTE: Gadgetbridge and OsmAnd AIDL API does not return events for navigation starts and ends
    // There are a little workarounds to keep it working with huawei watch
    // Also the watch wait actions at least every 5 second(according to my tests), but I send it each 3 seconds,

    Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateCallback = this::sendNavigationInfo;

    private final AtomicBoolean isInProgress = new AtomicBoolean(false);
    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    private final AtomicReference<String> lastInstruction = new AtomicReference<>();

    public final int SEND_DELAY = 3000;
    public final int SEND_COUNT_BEFORE_END = 10;

    private final AtomicInteger currCount = new AtomicInteger(0);


    public HuaweiP2PDirection(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("HuaweiP2PDirection");
    }

    public static HuaweiP2PDirection getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PDirection) manager.getRegisteredService(MODULE);
    }

    @Override
    public String getModule() {
        return MODULE;
    }

    @Override
    public String getPackage() {
        return "com.huawei.ohos.maps.app"; // can be com.huawei.maps.watch.app. In this case Fingerprint can be different too.
    }

    @Override
    public String getFingerprint() {
        return "com.huawei.ohos.maps.app_BE6sofwTFkNMSom4aaR6KF0TAtbNfZpqQ7iHI3XSqEdR1oxlZ2qYfJRBzidRW6M3D885bZdLAFgD08c9ODFDX6E=";
    }

    @Override
    public String getLocalFingerprint() {
        return "DDAB0C1DB857B116AB1F7A41583F8B452A609CDBD9A2EF088F5592D6FADB5CFF";
    }

    @Override
    public String getPingPackage() {
        return MODULE;
    }

    @Override
    public void registered() {
        isRegistered.set(true);
        checkAvailability();
    }

    @Override
    public void unregister() {
        handler.removeCallbacks(updateCallback);
        clearNavigationState();
        isRegistered.set(false);
    }

    public static String actionToIconId(int action) {
        return switch (action) {
            case NavigationInfoSpec.ACTION_CONTINUE -> "straight";
            case NavigationInfoSpec.ACTION_TURN_LEFT -> "turn_left";
            case NavigationInfoSpec.ACTION_TURN_LEFT_SLIGHTLY -> "turn_slight_left";
            case NavigationInfoSpec.ACTION_TURN_LEFT_SHARPLY -> "turn_sharp_left";
            case NavigationInfoSpec.ACTION_TURN_RIGHT -> "turn_right";
            case NavigationInfoSpec.ACTION_TURN_RIGHT_SLIGHTLY -> "turn_slight_right";
            case NavigationInfoSpec.ACTION_TURN_RIGHT_SHARPLY -> "turn_sharp_right";
            case NavigationInfoSpec.ACTION_KEEP_LEFT -> "fork_left_3";
            case NavigationInfoSpec.ACTION_KEEP_RIGHT -> "fork_right_3";
            case NavigationInfoSpec.ACTION_UTURN_LEFT -> "uturn_left";
            case NavigationInfoSpec.ACTION_UTURN_RIGHT -> "uturn_right";
            case NavigationInfoSpec.ACTION_OFFROUTE -> "default";
            case NavigationInfoSpec.ACTION_ROUNDABOUT_RIGHT -> "roundabout_right_5";
            case NavigationInfoSpec.ACTION_ROUNDABOUT_LEFT -> "roundabout_right_4";
            case NavigationInfoSpec.ACTION_ROUNDABOUT_STRAIGHT -> "roundabout_right_1";
            case NavigationInfoSpec.ACTION_ROUNDABOUT_UTURN -> "roundabout_right_8";
            case NavigationInfoSpec.ACTION_FINISH -> "end";
            case NavigationInfoSpec.ACTION_MERGE -> "merge";
            default -> "unknown";
        };
    }

    private void checkAvailability() {
        // NOTE: Get version command may not be supported by some old devices.
        // app availability can be checked by querying app list from the watch (service 0x2a).
        sendGetVersion((version, data) -> {
            LOG.info("HuaweiP2PDirection App version: {}", version);
            manager.getSupportProvider().getDeviceState().setNavigationAvailability(version != -1);
        });
    }

    public void startNavigation() {
        sendPing((code, data) -> {
            if (code != 0xca && code != 0xc9) {
                LOG.error("Invalid ping response");
                isInProgress.set(false);
                return;
            }
            sendCommand("start navigation".getBytes(), (code3, data3) -> {
                if (code3 != 0xcf) {
                    LOG.error("Error start navigation");
                    isInProgress.set(false);
                    return;
                }
                isStarted.set(true);
            });
        });
    }

    public void endNavigation() {
        sendCommand("end navigation".getBytes(), (code, data) -> {
            if (code != 0xcf) {
                LOG.error("Error end navigation");
            }
        });
    }

    public void sendInstruction(String dt) {
        sendCommand(dt.getBytes(), (code2, data2) -> {
            if (code2 != (byte) 0xcf) {
                isInProgress.set(false);
            }
        });
    }

    private void clearNavigationState() {
        currCount.set(0);
        isInProgress.set(false);
        isStarted.set(false);
        lastInstruction.set(null);
    }

    private void sendNavigationInfo() {
        if(!isInProgress.get()) {
            return;
        }
        if (currCount.incrementAndGet() < SEND_COUNT_BEFORE_END) {
            handler.postDelayed(updateCallback, SEND_DELAY);
            sendInstruction(lastInstruction.get());
        } else {
            endNavigation();
            clearNavigationState();
        }
    }

    public void updateInstruction(String distance, String icon, String name) {
        if(!isRegistered.get()) {
            return;
        }
        currCount.set(0);
        handler.removeCallbacks(updateCallback);
        JsonArray roadName = new JsonArray();
        roadName.add(name);

        JsonObject syncData = new JsonObject();
        syncData.addProperty("distance", distance);
        syncData.addProperty("iconId", icon);
        syncData.add("roadName", roadName);
        String dt = new Gson().toJson(syncData);

        lastInstruction.set(dt);

        if (!isInProgress.get()) {
            isInProgress.set(true);
            startNavigation();
            handler.postDelayed(updateCallback, 1000);
        } else {
            if (isStarted.get()) {
                handler.postDelayed(updateCallback, 100);
            }
        }
    }

    @Override
    protected int onPingPacket(P2P.P2PCommand.Response packet) {
        return 0xcd;
    }

    @Override
    public void handleData(byte[] data) {
        LOG.info("HuaweiP2PDirection handleData");
    }
}
