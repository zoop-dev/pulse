/*  Copyright (C) 2026 Baptiste Debut

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiNotificationService;

public class XiaomiNotificationServiceTest {
    /// "Open on phone" must not be mistaken for a canned messages reply. The two cases are
    /// adjacent in the switch, and without a break the empty CannedMessages of an open-on-phone
    /// command used to overwrite the canned reply counters with zero, which silently removed the
    /// canned replies setting screen until the next reconnection.
    @Test
    public void testOpenOnPhoneDoesNotClobberCannedMessages() {
        final List<GBDeviceEvent> events = new ArrayList<>();
        final XiaomiSupport support = mock(XiaomiSupport.class);
        doAnswer(invocation -> {
            events.add(invocation.getArgument(0));
            return null;
        }).when(support).evaluateGBDeviceEvent(any(GBDeviceEvent.class));

        final XiaomiNotificationService service = new XiaomiNotificationService(support);

        service.handleCommand(XiaomiProto.Command.newBuilder()
                .setType(XiaomiNotificationService.COMMAND_TYPE)
                .setSubtype(XiaomiNotificationService.CMD_OPEN_ON_PHONE)
                .setNotification(XiaomiProto.Notification.newBuilder()
                        .setOpenOnPhone(XiaomiProto.NotificationId.newBuilder().setId(1234)))
                .build());

        assertEquals("open on phone should raise exactly one event", 1, events.size());
        assertTrue("expected a notification control event, got " + events.get(0),
                events.get(0) instanceof GBDeviceEventNotificationControl);
        assertEquals(1234, ((GBDeviceEventNotificationControl) events.get(0)).handle);
    }
}
