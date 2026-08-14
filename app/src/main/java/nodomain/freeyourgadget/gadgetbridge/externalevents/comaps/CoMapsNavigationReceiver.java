package nodomain.freeyourgadget.gadgetbridge.externalevents.comaps;

import android.app.Application;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.util.NavigationUtils;

public final class CoMapsNavigationReceiver extends ContentObserver {

    private static final Logger LOG = LoggerFactory.getLogger(CoMapsNavigationReceiver.class);

    private final Application app;

    private final Uri dataUri;

    private final Map<String, Integer> carDirectionToNavSpecInfo = Map.ofEntries(
            Map.entry("GO_STRAIGHT", NavigationInfoSpec.ACTION_CONTINUE),
            Map.entry("TURN_RIGHT", NavigationInfoSpec.ACTION_TURN_RIGHT),
            Map.entry("TURN_SHARP_RIGHT", NavigationInfoSpec.ACTION_TURN_RIGHT_SHARPLY),
            Map.entry("TURN_SLIGHT_RIGHT", NavigationInfoSpec.ACTION_TURN_RIGHT_SLIGHTLY),
            Map.entry("TURN_LEFT", NavigationInfoSpec.ACTION_TURN_LEFT),
            Map.entry("TURN_SHARP_LEFT", NavigationInfoSpec.ACTION_TURN_LEFT_SHARPLY),
            Map.entry("TURN_SLIGHT_LEFT", NavigationInfoSpec.ACTION_TURN_LEFT_SLIGHTLY),
            Map.entry("U_TURN_LEFT", NavigationInfoSpec.ACTION_UTURN_LEFT),
            Map.entry("U_TURN_RIGHT", NavigationInfoSpec.ACTION_UTURN_RIGHT),
            Map.entry("ENTER_ROUND_ABOUT", NavigationInfoSpec.ACTION_ROUNDABOUT_STRAIGHT),
            Map.entry("LEAVE_ROUND_ABOUT", NavigationInfoSpec.ACTION_ROUNDABOUT_STRAIGHT),
            Map.entry("STAY_ON_ROUND_ABOUT", NavigationInfoSpec.ACTION_ROUNDABOUT_STRAIGHT),
            Map.entry("START_AT_END_OF_STREET", NavigationInfoSpec.ACTION_OFFROUTE),
            Map.entry("REACHED_YOUR_DESTINATION", NavigationInfoSpec.ACTION_FINISH),
            Map.entry("EXIT_HIGHWAY_TO_LEFT", NavigationInfoSpec.ACTION_TURN_LEFT_SLIGHTLY),
            Map.entry("EXIT_HIGHWAY_TO_RIGHT", NavigationInfoSpec.ACTION_TURN_RIGHT_SLIGHTLY)
    );
    private final Map<String, Integer> pedestrianDirectionToNavSpecInfo = Map.ofEntries(
            Map.entry("NO_TURN", NavigationInfoSpec.ACTION_CONTINUE),
            Map.entry("GO_STRAIGHT", NavigationInfoSpec.ACTION_CONTINUE),
            Map.entry("TURN_RIGHT", NavigationInfoSpec.ACTION_TURN_RIGHT),
            Map.entry("TURN_LEFT", NavigationInfoSpec.ACTION_TURN_LEFT),
            Map.entry("REACHED_YOUR_DESTINATION", NavigationInfoSpec.ACTION_FINISH)
    );

    CoMapsNavigationReceiver(Handler handler, Application application, Uri dataUri) {
        super(handler);
        this.app = application;
        this.dataUri = dataUri;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        if (!NavigationUtils.shouldSendNavigation(app, "comaps")) {
            return;
        }

        queryNavigationData();
    }

    private void queryNavigationData() {
        final ContentResolver resolver = app.getContentResolver();
        try (Cursor cursor = resolver.query(dataUri, new String[]{
                NavigationContract.Live.Columns.CAR_DIRECTION,
                NavigationContract.Live.Columns.PEDESTRIAN_DIRECTION,
                NavigationContract.Live.Columns.NEXT_STREET,
                NavigationContract.Live.Columns.DIST_TO_TURN,
                NavigationContract.Live.Columns.DIST_TO_TARGET,
                NavigationContract.Live.Columns.TOTAL_TIME_SECONDS,
                NavigationContract.Live.Columns.COMPLETION_PERCENT,
                NavigationContract.Live.Columns.EXIT_NUM
        }, null, null, null)) {
            final NavigationInfoSpec navInfo = new NavigationInfoSpec();

            if (cursor == null || !cursor.moveToFirst()) {
                GBApplication.deviceService().onSetNavigationInfo(navInfo);
                return;
            }

            final String carDirection = cursor.getString(cursor.getColumnIndexOrThrow(NavigationContract.Live.Columns.CAR_DIRECTION));
            final String pedestrianDirection = cursor.getString(cursor.getColumnIndexOrThrow(NavigationContract.Live.Columns.PEDESTRIAN_DIRECTION));

            navInfo.setNextAction(carDirectionToNavSpecInfo.getOrDefault(carDirection, pedestrianDirectionToNavSpecInfo.getOrDefault(pedestrianDirection, 0)));

            final int exitNumCol = cursor.getColumnIndex(NavigationContract.Live.Columns.EXIT_NUM);
            if (exitNumCol >= 0 && navInfo.getNextAction() == NavigationInfoSpec.ACTION_ROUNDABOUT_STRAIGHT) {
                int exitNum = cursor.getInt(exitNumCol);
                if (exitNum == 1) {
                    navInfo.setNextAction(NavigationInfoSpec.ACTION_ROUNDABOUT_LEFT);
                } else if (exitNum >= 3) {
                    navInfo.setNextAction(NavigationInfoSpec.ACTION_ROUNDABOUT_RIGHT);
                }
            }

            final int distToTurnFormattedCol = cursor.getColumnIndex(NavigationContract.Live.Columns.DIST_TO_TURN);
            if (distToTurnFormattedCol >= 0) {
                navInfo.setDistanceToTurn(cursor.getString(distToTurnFormattedCol));
            }

            final int distToTargetFormattedCol = cursor.getColumnIndex(NavigationContract.Live.Columns.DIST_TO_TARGET);
            if (distToTargetFormattedCol >= 0) {
                navInfo.setDistanceToTarget(cursor.getString(distToTargetFormattedCol));
            }

            final int nextStreetCol = cursor.getColumnIndex(NavigationContract.Live.Columns.NEXT_STREET);
            if (nextStreetCol >= 0) {
                navInfo.setInstruction(cursor.getString(nextStreetCol));
            }

            final int timeLeftCol = cursor.getColumnIndex(NavigationContract.Live.Columns.TOTAL_TIME_SECONDS);
            if (timeLeftCol >= 0 && !cursor.isNull(timeLeftCol)) {
                navInfo.setETA(cursor.getString(timeLeftCol));
            }

            final int completionPercentCol = cursor.getColumnIndex(NavigationContract.Live.Columns.COMPLETION_PERCENT);
            if (completionPercentCol >= 0 && !cursor.isNull(completionPercentCol)) {
                navInfo.setCompletionPercent((int) cursor.getDouble(completionPercentCol));
            }

            LOG.debug("CoMaps navigation data unmarshalled: {}", navInfo);

            GBApplication.deviceService().onSetNavigationInfo(navInfo);
        } catch (SecurityException e) {
            LOG.debug("Permission to read CoMaps navigation data has not been granted");
        } catch (Exception e) {
            LOG.error("Error querying CoMaps navigation data", e);
        }
    }

    protected static final class NavigationContract {
        public static final class Live {
            public static final class Columns {
                public static final String SESSION_STATE = "session_state";

                public static final String CAR_DIRECTION = "car_direction";
                public static final String PEDESTRIAN_DIRECTION = "pedestrian_direction";

                public static final String DIST_TO_TURN = "dist_to_turn";
                public static final String DIST_TO_TARGET = "dist_to_target";
                public static final String DIST_TO_NEXT_STOP = "dist_to_next_stop";

                public static final String TOTAL_TIME_SECONDS = "total_time_seconds";
                public static final String TIME_TO_NEXT_STOP = "time_to_next_stop";

                public static final String CURRENT_STREET = "current_street";
                public static final String NEXT_STREET = "next_street";

                public static final String COMPLETION_PERCENT = "completion_percent";

                public static final String EXIT_NUM = "exit_num";
            }
        }
    }
}