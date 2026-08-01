package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.Notification;
import android.graphics.drawable.Icon;
import android.os.Build;

import org.junit.Test;
import org.robolectric.util.ReflectionHelpers;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilter;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotificationListenerTest extends TestBase {

    private NotificationListener mNotificationListener;
    private final List<String> wordList = Arrays.asList("Hello", "world", "test");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mNotificationListener = new NotificationListener();
    }

    @Test
    public void shouldContinueAfterFilter_TestBlacklistFindAnyWord_WordFound_MustReturnFalse() {
        String body = "Hello world this is a test";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_BLACKLIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ANY);
        assertFalse(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestWhitelistFindAnyWord_WordFound_MustReturnTrue() {
        String body = "Hello world this is a test";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_WHITELIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ANY);
        assertTrue(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestBlacklistFindAllWords_WordsFound_MustReturnFalse() {
        String body = "Hello world this is a test";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_BLACKLIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ALL);
        assertFalse(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestWhitelistFindAllWords_WordsFound_MustReturnTrue() {
        String body = "Hello world this is a test";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_WHITELIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ALL);
        assertTrue(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));
    }

    @Test
    public void shouldContinueAfterFilter_TestBlacklistFindAnyWord_WordNotFound_MustReturnTrue() {
        String body = "Hallo Welt das ist ein Versuch";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_BLACKLIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ANY);
        assertTrue(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestWhitelistFindAnyWord_WordNotFound_MustReturnFalse() {
        String body = "Hallo Welt das ist ein Versuch";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_WHITELIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ANY);
        assertFalse(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestBlacklistFindAllWords_WordNotFound_MustReturnTrue() {
        String body = "Hallo Welt das ist ein Versuch";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_BLACKLIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ALL);
        assertTrue(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestWhitelistFindAllWords_WordNotFound_MustReturnFalse() {
        String body = "Hallo Welt das ist ein Versuch";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_WHITELIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ALL);
        assertFalse(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));
    }

    @Test
    public void shouldContinueAfterFilter_TestFilterNone_MustReturnTrue() {
        String body = "A text without a meaning";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_NONE);
        assertTrue(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));
    }

    @Test
    public void isOutsideNotificationTimes_samedayWindow_tooEarly_MustReturnTrue() {
        assertTrue(NotificationListener.isOutsideNotificationTimes(
            /* now= */ LocalTime.of(6, 0),
            /* start= */ LocalTime.of(7, 0),
            /* end= */ LocalTime.of(20, 0)
        ));
    }

    @Test
    public void isOutsideNotificationTimes_samedayWindow_withinWindow_MustReturnFalse() {
        assertFalse(NotificationListener.isOutsideNotificationTimes(
            /* now= */ LocalTime.of(10, 0),
            /* start= */ LocalTime.of(7, 0),
            /* end= */ LocalTime.of(20, 0)
        ));
    }

    @Test
    public void isOutsideNotificationTimes_samedayWindow_tooLate_MustReturnTrue() {
        assertTrue(NotificationListener.isOutsideNotificationTimes(
            /* now= */ LocalTime.of(21, 0),
            /* start= */ LocalTime.of(7, 0),
            /* end= */ LocalTime.of(20, 0)
        ));
    }

    @Test
    public void isOutsideNotificationTimes_crossMidnightWindow_tooEarly_MustReturnTrue() {
        assertTrue(NotificationListener.isOutsideNotificationTimes(
            /* now= */ LocalTime.of(18, 0),
            /* start= */ LocalTime.of(20, 0),
            /* end= */ LocalTime.of(7, 0)
        ));
    }

    @Test
    public void isOutsideNotificationTimes_crossMidnightWindow_withinWindow_MustReturnFalse() {
        assertFalse(NotificationListener.isOutsideNotificationTimes(
            /* now= */ LocalTime.of(21, 0),
            /* start= */ LocalTime.of(20, 0),
            /* end= */ LocalTime.of(7, 0)
        ));
        assertFalse(NotificationListener.isOutsideNotificationTimes(
            /* now= */ LocalTime.of(6, 0),
            /* start= */ LocalTime.of(20, 0),
            /* end= */ LocalTime.of(7, 0)
        ));
    }

    @Test
    public void isOutsideNotificationTimes_crossMidnightWindow_tooLate_MustReturnTrue() {
        assertTrue(NotificationListener.isOutsideNotificationTimes(
            /* now= */ LocalTime.of(8, 0),
            /* start= */ LocalTime.of(20, 0),
            /* end= */ LocalTime.of(7, 0)
        ));
    }

    @Test
    public void populateNotificationIcon_preservesResourceOwner() {
        final Object previousVersion = ReflectionHelpers.getStaticField(Build.VERSION.class, "SDK_INT");
        try {
            ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 28);

            final Notification notification = new Notification.Builder(getContext())
                    .setSmallIcon(Icon.createWithResource("com.example.owner", android.R.drawable.ic_dialog_info))
                    .build();
            final NotificationSpec spec = new NotificationSpec();

            NotificationListener.populateNotificationIcon(notification, "com.example.source", spec);

            assertEquals(android.R.drawable.ic_dialog_info, spec.iconId);
            assertEquals("com.example.owner", spec.iconPackageId);
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", previousVersion);
        }
    }

    @Test
    public void populateNotificationIcon_usesSourceForLegacyResource() {
        final Notification notification = new Notification();
        notification.icon = android.R.drawable.ic_dialog_alert;
        final NotificationSpec spec = new NotificationSpec();

        NotificationListener.populateNotificationIcon(notification, "com.example.source", spec);

        assertEquals(android.R.drawable.ic_dialog_alert, spec.iconId);
        assertEquals("com.example.source", spec.iconPackageId);
    }
}
