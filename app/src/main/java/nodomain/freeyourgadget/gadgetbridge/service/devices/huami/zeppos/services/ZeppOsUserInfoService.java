/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_REGION;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_DATE_OF_BIRTH;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_GENDER;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_HEIGHT_CM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_NAME;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_WEIGHT_KG;
import static nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions.fromUint16;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ZeppOsUserInfoService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsUserInfoService.class);

    private static final short ENDPOINT = 0x0017;

    public static final byte USER_INFO_CMD_SET = 0x01;
    public static final byte USER_INFO_CMD_SET_ACK = 0x02;

    public ZeppOsUserInfoService(final ZeppOsSupport support) {
        super(support, true);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (payload[0]) {
            case USER_INFO_CMD_SET_ACK:
                LOG.info("Got user info set ack, status = {}", payload[1]);
                return;
        }

        LOG.warn("Unexpected user info payload byte {}", String.format("0x%02x", payload[0]));
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        setUserInfo(builder);
    }

    /** @noinspection EnhancedSwitchMigration*/
    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case PREF_USER_DATE_OF_BIRTH:
            case PREF_USER_NAME:
            case PREF_USER_WEIGHT_KG:
            case PREF_USER_HEIGHT_CM:
            case PREF_USER_GENDER:
            case PREF_DEVICE_REGION:
                withTransactionBuilder("set user info", this::setUserInfo);
                return true;
        }

        return false;
    }

    public void setUserInfo(final ZeppOsTransactionBuilder builder) {
        LOG.info("Attempting to set user info...");

        final Prefs prefs = GBApplication.getPrefs();
        final Prefs devicePrefs = getDevicePrefs();

        final String alias = prefs.getString(PREF_USER_NAME, null);
        final ActivityUser activityUser = new ActivityUser();
        final int height = activityUser.getHeightCm();
        final int weight = activityUser.getWeightKg();
        final LocalDate dateOfBirth = activityUser.getDateOfBirth();
        final int birthYear = dateOfBirth.getYear();
        final byte birthMonth = (byte) dateOfBirth.getMonthValue();
        final byte birthDay = (byte) dateOfBirth.getDayOfMonth();
        final String region = devicePrefs.getString(PREF_DEVICE_REGION, "unknown");

        if (alias == null || weight == 0 || height == 0 || birthYear == 0) {
            LOG.warn("Unable to set user info, make sure it is set up");
            return;
        }

        byte genderByte = 2; // other
        switch (activityUser.getGender()) {
            case ActivityUser.GENDER_MALE:
                genderByte = 0;
                break;
            case ActivityUser.GENDER_FEMALE:
                genderByte = 1;
        }
        final int userid = alias.hashCode();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(USER_INFO_CMD_SET);
            baos.write(new byte[]{0x4f, 0x07, 0x00, 0x00});
            baos.write(fromUint16(birthYear));
            baos.write(birthMonth);
            baos.write(birthDay);
            baos.write(genderByte);
            baos.write(fromUint16(height));
            baos.write(fromUint16(weight * 200));
            baos.write(BLETypeConversions.fromUint64(userid));
            baos.write(region.getBytes(StandardCharsets.UTF_8));
            baos.write(0);
            baos.write(0x09); // TODO ?
            baos.write(alias.getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0);

            write(builder, baos.toByteArray());
        } catch (final Exception e) {
            LOG.error("Failed to send user info", e);
        }
    }
}
