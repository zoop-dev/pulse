/*  Copyright (C) 2021-2024 Arjan Schrijver, José Rebelo, x29a

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
package nodomain.freeyourgadget.gadgetbridge.devices.smaq2oss;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.proto.SMAQ2OSSProtos;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SMAQ2OSSSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(SMAQ2OSSSupport.class);

    public BluetoothGattCharacteristic normalWriteCharacteristic = null;

    public SMAQ2OSSSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(SMAQ2OSSConstants.UUID_SERVICE_SMAQ2OSS);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        normalWriteCharacteristic = getCharacteristic(SMAQ2OSSConstants.UUID_CHARACTERISTIC_WRITE_NORMAL);
        normalWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        builder.setDeviceState(GBDevice.State.INITIALIZING);

        setTime(builder)
                .setInitialized(builder);

        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        builder.notify(SMAQ2OSSConstants.UUID_CHARACTERISTIC_NOTIFY_NORMAL, true);

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] value) {
        super.onCharacteristicChanged(gatt, characteristic, value);

        UUID characteristicUUID = characteristic.getUuid();
        if (SMAQ2OSSConstants.UUID_CHARACTERISTIC_NOTIFY_NORMAL.equals(characteristicUUID)) {
            handleDeviceEvent(value);
        }
        return true;
    }

    private void handleDeviceEvent(byte[] value){
        if (value == null || value.length == 0) {
            return;
        }

        switch (value[0]) {
            case SMAQ2OSSConstants.MSG_MUSIC_EVENT:
                LOG.info("got music control");
                handleMusicEvent(value[1]);
                break;
            case SMAQ2OSSConstants.MSG_CALL_COMMAND:
                LOG.info("got call control");
                handleCallCommand(value[1]);
                break;

        }

    }

    private void handleMusicEvent(byte value){
        GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();

        switch (value){
            case SMAQ2OSSConstants.EVT_PLAY_PAUSE:
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
                break;
            case SMAQ2OSSConstants.EVT_FWD:
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                break;
            case SMAQ2OSSConstants.EVT_REV:
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                break;
            case SMAQ2OSSConstants.EVT_VOL_UP:
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                break;
            case SMAQ2OSSConstants.EVT_VOL_DOWN:
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                break;
        }
        evaluateGBDeviceEvent(deviceEventMusicControl);

    }

    private void handleCallCommand(byte command){
        GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();

        switch (command){
            case CallSpec.CALL_ACCEPT:
                callCmd.event = GBDeviceEventCallControl.Event.ACCEPT;
                evaluateGBDeviceEvent(callCmd);
                break;
            case CallSpec.CALL_REJECT:
                callCmd.event = GBDeviceEventCallControl.Event.REJECT;
                evaluateGBDeviceEvent(callCmd);
                break;
        }

    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {

        SMAQ2OSSProtos.MessageNotification.Builder notification = SMAQ2OSSProtos.MessageNotification.newBuilder();


        notification.setTimestamp(getTimestamp());
        String sender = StringUtils.getFirstOf(StringUtils.getFirstOf(notificationSpec.sender,notificationSpec.phoneNumber),notificationSpec.title);
        notification.setSender(truncateUTF8(sender,SMAQ2OSSConstants.NOTIFICATION_SENDER_MAX_LEN));
//        notification.setSubject(truncateUTF8(notificationSpec.subject,SMAQ2OSSConstants.NOTIFICATION_SUBJECT_MAX_LEN));
        notification.setBody(truncateUTF8(notificationSpec.body,SMAQ2OSSConstants.NOTIFICATION_BODY_MAX_LEN));


        try {
            TransactionBuilder builder;
            builder = performInitialized("Sending notification");

            builder.write(normalWriteCharacteristic,createMessage(SMAQ2OSSConstants.MSG_NOTIFICATION,notification.build().toByteArray()));

            builder.queue();
        } catch (Exception ex) {
            LOG.error("Error sending notification", ex);
        }
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("time");
            setTime(builder);
            builder.queueConnected();
        } catch(IOException e) {
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {

        SMAQ2OSSProtos.CallNotification.Builder callnotif = SMAQ2OSSProtos.CallNotification.newBuilder();

        callnotif.setName(truncateUTF8(callSpec.name,SMAQ2OSSConstants.CALL_NAME_MAX_LEN));
        callnotif.setNumber(truncateUTF8(callSpec.number,SMAQ2OSSConstants.CALL_NUMBER_MAX_LEN));
        callnotif.setCommand(callSpec.command);

        try {
            TransactionBuilder builder;
            builder = performInitialized("Sending call state");

            builder.write(normalWriteCharacteristic,createMessage(SMAQ2OSSConstants.MSG_CALL_NOTIFICATION,callnotif.build().toByteArray()));

            builder.queue();
        } catch (Exception ex) {
            LOG.error("Error sending call state", ex);
        }
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        SMAQ2OSSProtos.MusicInfo.Builder musicInfo = SMAQ2OSSProtos.MusicInfo.newBuilder();

        musicInfo.setArtist(truncateUTF8(musicSpec.artist,SMAQ2OSSConstants.MUSIC_ARTIST_MAX_LEN));
        musicInfo.setAlbum(truncateUTF8(musicSpec.album,SMAQ2OSSConstants.MUSIC_ALBUM_MAX_LEN));
        musicInfo.setTrack(truncateUTF8(musicSpec.track,SMAQ2OSSConstants.MUSIC_TRACK_MAX_LEN));

        try {
            TransactionBuilder builder;
            builder = performInitialized("Sending music info");
            LOG.info(musicInfo.getArtist());
            LOG.info(musicInfo.getAlbum());
            LOG.info(musicInfo.getTrack());

            builder.write(normalWriteCharacteristic,createMessage(SMAQ2OSSConstants.MSG_SET_MUSIC_INFO,musicInfo.build().toByteArray()));

            builder.queue();
        } catch (Exception ex) {
            LOG.error("Error sending music info", ex);
        }
    }

    @Override
    public void onReset(int flags) {
//        try {
//            getQueue().clear();
//
//            TransactionBuilder builder = performInitialized("reboot");
//            builder.write(normalWriteCharacteristic, new byte[] {
//                    SMAQ2OSSConstants.CMD_ID_DEVICE_RESTART, SMAQ2OSSConstants.CMD_KEY_REBOOT
//            });
//            performConnected(builder.getTransaction());
//        } catch(Exception e) {
//        }
    }

    @Override
    public void onSendWeather() {
        final WeatherSpec weatherSpec = Weather.getWeatherSpec();
        if (weatherSpec == null) {
            LOG.warn("No weather found in singleton");
            return;
        }
        try {
            TransactionBuilder builder;
            builder = performInitialized("Sending current weather");

            SMAQ2OSSProtos.SetWeather.Builder setWeather= SMAQ2OSSProtos.SetWeather.newBuilder();

            setWeather.setTimestamp(weatherSpec.getTimestamp());
            setWeather.setCondition(weatherSpec.getCurrentConditionCode());
            setWeather.setTemperature(weatherSpec.getCurrentTemp() -273);
            setWeather.setTemperatureMin(weatherSpec.getTodayMinTemp() -273);
            setWeather.setTemperatureMax(weatherSpec.getTodayMaxTemp() -273);
            setWeather.setHumidity(weatherSpec.getCurrentHumidity());

            for (WeatherSpec.Daily f: weatherSpec.getForecasts()) {

                SMAQ2OSSProtos.Forecast.Builder fproto = SMAQ2OSSProtos.Forecast.newBuilder();

                fproto.setCondition(f.getConditionCode());
                fproto.setTemperatureMin(f.getMinTemp() -273);
                fproto.setTemperatureMax(f.getMaxTemp() -273);

                setWeather.addForecasts(fproto);
            }

            builder.write(normalWriteCharacteristic,createMessage(SMAQ2OSSConstants.MSG_SET_WEATHER,setWeather.build().toByteArray()));
            builder.queue();
        } catch (Exception ex) {
            LOG.error("Error sending current weather", ex);
        }
    }

    private void setInitialized(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZED);
    }

    byte[] createMessage(byte msgid, byte[] data){

        ByteBuffer buf=ByteBuffer.allocate(data.length+1);
        buf.put(msgid);
        buf.put(data);

        return buf.array();
    }

    String truncateUTF8(String str, int len){

        if (str ==null)
            return new String();

        int currLen = str.getBytes(UTF_8).length;

        while (currLen>len-1){

            str=str.substring(0,str.length()-1);
            currLen = str.getBytes(UTF_8).length;
        }

        return str;
    }

    private int getTimestamp(){

        Calendar c = GregorianCalendar.getInstance();

        long offset = (c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET));
        long ts =  (c.getTimeInMillis()+ offset)/1000 ;

        return (int)ts;

    }

    SMAQ2OSSSupport setTime(TransactionBuilder builder) {

        SMAQ2OSSProtos.SetTime.Builder settime = SMAQ2OSSProtos.SetTime.newBuilder();
        settime.setTimestamp(getTimestamp());
        builder.write(normalWriteCharacteristic,createMessage(SMAQ2OSSConstants.MSG_SET_TIME,settime.build().toByteArray()));
        return this;
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}
