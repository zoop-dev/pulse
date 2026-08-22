package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiNotificationService;
import nodomain.freeyourgadget.gadgetbridge.test.GBTestApplication;

/**
 * Dismissing a notification looks its key up from what was posted. Producers other than
 * NotificationListener never set one, and protobuf rejects a null string.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, application = GBTestApplication.class)
public class XiaomiNotificationDismissTest {

    private static final int COMMAND_TYPE = 7;
    private static final int CMD_NOTIFICATION_DISMISS = 1;

    private static final String ALARM_PACKAGE = "com.android.deskclock";

    private XiaomiSupport support;
    private XiaomiNotificationService notificationService;

    @Before
    public void setUp() {
        support = Mockito.mock(XiaomiSupport.class);
        Mockito.when(support.getDevice()).thenReturn(
                new GBDevice("00:11:22:33:44:55", "Testie", "Testie", "Test Folder", DeviceType.TEST));
        notificationService = new XiaomiNotificationService(support);
    }

    private XiaomiProto.NotificationId postAndDismiss(final NotificationSpec spec) {
        notificationService.onNotification(spec);
        notificationService.onDeleteNotification(spec.getId());

        final ArgumentCaptor<XiaomiProto.Command> captor = ArgumentCaptor.forClass(XiaomiProto.Command.class);
        Mockito.verify(support, Mockito.atLeastOnce()).sendCommand(Mockito.anyString(), captor.capture());

        XiaomiProto.NotificationDismiss dismiss = null;
        for (final XiaomiProto.Command command : captor.getAllValues()) {
            if (command.getType() == COMMAND_TYPE && command.getSubtype() == CMD_NOTIFICATION_DISMISS) {
                dismiss = command.getNotification().getNotificationDismiss();
            }
        }

        Assert.assertNotNull("dismiss command was sent", dismiss);
        final List<XiaomiProto.NotificationId> ids = dismiss.getNotificationIdList();
        Assert.assertEquals(1, ids.size());
        return ids.get(0);
    }

    @Test
    public void dismissesNotificationWithoutKey() {
        final NotificationSpec spec = new NotificationSpec();
        spec.sourceAppId = ALARM_PACKAGE;

        final XiaomiProto.NotificationId notificationId = postAndDismiss(spec);

        Assert.assertEquals(spec.getId(), notificationId.getId());
        Assert.assertEquals(ALARM_PACKAGE, notificationId.getPackage());
        Assert.assertFalse("key is omitted when the spec had none", notificationId.hasKey());
    }

    @Test
    public void dismissesNotificationWithKey() {
        final NotificationSpec spec = new NotificationSpec();
        spec.sourceAppId = ALARM_PACKAGE;
        spec.key = "0|" + ALARM_PACKAGE + "|42|null|12345";

        final XiaomiProto.NotificationId notificationId = postAndDismiss(spec);

        Assert.assertEquals(spec.getId(), notificationId.getId());
        Assert.assertEquals(ALARM_PACKAGE, notificationId.getPackage());
        Assert.assertEquals(spec.key, notificationId.getKey());
    }
}
