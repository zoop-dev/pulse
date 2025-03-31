/*  Copyright (C) 2024 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Contacts;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendSetContactsRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendSetContactsRequest.class);

    private final ArrayList<? extends Contact> contacts;
    int maxCount;

    public SendSetContactsRequest(HuaweiSupportProvider support,ArrayList<? extends Contact> contacts, int maxCount) {
        super(support);
        this.serviceId = Contacts.id;
        this.commandId = Contacts.ContactsSet.id;
        this.contacts = contacts;
        this.maxCount = maxCount;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Contacts.ContactsSet.Request(paramsProvider, this.contacts, this.maxCount).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        if (receivedPacket instanceof Contacts.ContactsSet.Response) {
            if (((Contacts.ContactsSet.Response) receivedPacket).isOk) {
                LOG.debug("Contacts set");
            } else {
                LOG.warn("Error set contacts");
            }
        } else {
            LOG.error("Set Contacts response is not of type ContactsSet response");
        }
    }
}
