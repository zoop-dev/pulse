package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.Notification;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;

import org.junit.Test;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilter;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotificationListenerTest extends TestBase {

    private NotificationListener mNotificationListener;
    private List<String> wordList = Arrays.asList("Hello", "world", "test");

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
    public void dissectNotificationTo_prefersMessagingMetadataForSenderAndBody() {
        final NotificationListener listener = new NotificationListener();
        final NotificationSpec spec = new NotificationSpec();

        final Person sender = new Person.Builder().setName("Alice").build();
        final Person user = new Person.Builder().setName("Me").build();
        final Notification notification = new NotificationCompat.Builder(getContext(), "test")
                .setContentTitle("Fallback title")
                .setContentText("Fallback body")
                .setStyle(new NotificationCompat.MessagingStyle(user)
                        .setConversationTitle("Family Group")
                        .addMessage("Latest message", System.currentTimeMillis(), sender))
                .build();

        listener.dissectNotificationTo(notification, spec, true);

        assertEquals("Alice", spec.sender);
        assertEquals("Family Group", spec.title);
        assertEquals("Latest message", spec.body);
    }

    @Test
    public void dissectNotificationTo_usesConversationTitleWhenTitleMissing() {
        final NotificationListener listener = new NotificationListener();
        final NotificationSpec spec = new NotificationSpec();

        final Bundle extras = new Bundle();
        extras.putCharSequence(NotificationCompat.EXTRA_CONVERSATION_TITLE, "Chat Room");
        final Notification notification = new NotificationCompat.Builder(getContext(), "test")
                .setExtras(extras)
                .setStyle(new NotificationCompat.MessagingStyle(new Person.Builder().setName("Me").build())
                        .setConversationTitle("Chat Room")
                        .addMessage("Body text", System.currentTimeMillis(), new Person.Builder().setName("Bob").build()))
                .build();

        listener.dissectNotificationTo(notification, spec, true);

        assertEquals("Chat Room", spec.title);
        assertEquals("Bob", spec.sender);
        assertEquals("Body text", spec.body);
    }
}
