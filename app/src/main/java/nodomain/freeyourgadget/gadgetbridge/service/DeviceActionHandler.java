package nodomain.freeyourgadget.gadgetbridge.service;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.*;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CAMERA_STATUS_CHANGE;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_ENABLE_HEARTRATE_SLEEP_SUPPORT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_ENABLE_REALTIME_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_POWER_OFF;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_READ_CONFIGURATION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_REQUEST_MUSIC_LIST;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_REQUEST_MUSIC_OPERATION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SEND_CONFIGURATION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SEND_WEATHER;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SET_ALARMS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SET_CONTACTS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SET_FM_FREQUENCY;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SET_GPS_LOCATION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SET_HEARTRATE_MEASUREMENT_INTERVAL;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SET_LED_COLOR;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SET_LOYALTY_CARDS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SET_REMINDERS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SET_WORLD_CLOCKS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SLEEP_AS_ANDROID;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_TEST_NEW_FUNCTION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_ALARMS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_BOOLEAN_ENABLE;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_CAMERA_EVENT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_CAMERA_FILENAME;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_CONFIG;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_CONTACTS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_FM_FREQUENCY;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_GPS_LOCATION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_INTERVAL_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_LED_COLOR;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_LOYALTY_CARDS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_OPTIONS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_REMINDERS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_REQUEST_MUSIC_MUSIC_IDS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_REQUEST_MUSIC_OPERATION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_REQUEST_MUSIC_PLAY_LIST_INDEX;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_REQUEST_MUSIC_PLAY_LIST_NAME;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_SLEEP_AS_ANDROID_ACTION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_URI;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_WORLD_CLOCKS;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.util.EmojiConverter;
import nodomain.freeyourgadget.gadgetbridge.util.language.LanguageUtils;
import nodomain.freeyourgadget.gadgetbridge.util.language.Transliterator;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

/**
 * Translates the Intent sent by {GBDeviceService} and calls the corresponding method in the device support class.
 */
@SuppressWarnings({"unchecked", "deprecation"})
public class DeviceActionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceActionHandler.class);

    private DeviceActionHandler() {
        // utility class
    }

    public static void handle(final GBDevice device,
                              final DeviceSupport deviceSupport,
                              final Context context,
                              final Intent intent,
                              final String action) {
        final DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);

        final Transliterator transliterator = LanguageUtils.getTransliterator(device);

        // Copy the incoming intent to make sure we don't modify it before it gets passed to other devices
        final Intent intentCopy = (Intent) intent.clone();

        for (final String extra : GBDeviceService.transliterationExtras) {
            if (intentCopy.hasExtra(extra)) {
                // Ensure the text is sanitized (e.g. emoji converted to ascii) before applying the transliterators
                // otherwise the emoji are removed before converting them
                String sanitizedText = sanitizeNotifText(deviceSupport, device.getDeviceCoordinator(), device, intentCopy.getStringExtra(extra));
                if (transliterator != null) {
                    sanitizedText = transliterator.transliterate(sanitizedText);
                }
                intentCopy.putExtra(extra, sanitizedText);
            }
        }

        switch (action) {
            case ACTION_REQUEST_DEVICEINFO:
                device.sendDeviceUpdateIntent(context, GBDevice.DeviceUpdateSubject.NOTHING);
                break;
            case ACTION_NOTIFICATION: {
                final int desiredId = intentCopy.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
                final NotificationSpec notificationSpec = new NotificationSpec(desiredId);
                notificationSpec.phoneNumber = intentCopy.getStringExtra(EXTRA_NOTIFICATION_PHONENUMBER);
                notificationSpec.sender = intentCopy.getStringExtra(EXTRA_NOTIFICATION_SENDER);
                notificationSpec.subject = intentCopy.getStringExtra(EXTRA_NOTIFICATION_SUBJECT);
                notificationSpec.title = intentCopy.getStringExtra(EXTRA_NOTIFICATION_TITLE);
                if (notificationSpec.title == null) {
                    notificationSpec.title = "";
                }
                notificationSpec.key = intentCopy.getStringExtra(EXTRA_NOTIFICATION_KEY);
                notificationSpec.body = intentCopy.getStringExtra(EXTRA_NOTIFICATION_BODY);
                if (notificationSpec.body == null) {
                    notificationSpec.body = "";
                }
                notificationSpec.sourceName = intentCopy.getStringExtra(EXTRA_NOTIFICATION_SOURCENAME);
                notificationSpec.type = (NotificationType) intentCopy.getSerializableExtra(EXTRA_NOTIFICATION_TYPE);
                notificationSpec.attachedActions = (ArrayList<NotificationSpec.Action>) intentCopy.getSerializableExtra(EXTRA_NOTIFICATION_ACTIONS);
                notificationSpec.flags = intentCopy.getIntExtra(EXTRA_NOTIFICATION_FLAGS, 0);
                notificationSpec.sourceAppId = intentCopy.getStringExtra(EXTRA_NOTIFICATION_SOURCEAPPID);
                notificationSpec.iconId = intentCopy.getIntExtra(EXTRA_NOTIFICATION_ICONID, 0);
                notificationSpec.iconPackageId = intentCopy.getStringExtra(EXTRA_NOTIFICATION_ICONPACKAGEID);
                notificationSpec.picturePath = intent.getStringExtra(NOTIFICATION_PICTURE_PATH);
                notificationSpec.dndSuppressed = intentCopy.getIntExtra(EXTRA_NOTIFICATION_DNDSUPPRESSED, 0);
                notificationSpec.channelId = intentCopy.getStringExtra(EXTRA_NOTIFICATION_CHANNEL_ID);
                notificationSpec.category = intentCopy.getStringExtra(EXTRA_NOTIFICATION_CATEGORY);

                if (notificationSpec.type == NotificationType.GENERIC_SMS && notificationSpec.phoneNumber != null) {
                    GBApplication.getIDSenderLookup().add(notificationSpec.getId(), notificationSpec.phoneNumber);
                }

                //TODO: check if at least one of the attached actions is a reply action instead?
                if ((notificationSpec.attachedActions != null && !notificationSpec.attachedActions.isEmpty())
                        || (notificationSpec.type == NotificationType.GENERIC_SMS && notificationSpec.phoneNumber != null)) {
                    // NOTE: maybe not where it belongs
                    // I would rather like to save that as an array in SharedPreferences
                    // this would work, but I don't know how to do the same in the Settings Activity's xXML
                    ArrayList<String> replies = new ArrayList<>();
                    for (int i = 1; i <= 16; i++) {
                        String reply = devicePrefs.getString("canned_reply_" + i, null);
                        if (reply != null && !reply.isEmpty()) {
                            replies.add(reply);
                        }
                    }
                    notificationSpec.cannedReplies = replies.toArray(new String[0]);
                }

                deviceSupport.onNotification(notificationSpec);
                break;
            }
            case ACTION_DELETE_NOTIFICATION: {
                deviceSupport.onDeleteNotification(intentCopy.getIntExtra(EXTRA_NOTIFICATION_ID, -1));
                break;
            }
            case ACTION_ADD_CALENDAREVENT: {
                final CalendarEventSpec calendarEventSpec = new CalendarEventSpec();
                calendarEventSpec.id = intentCopy.getLongExtra(EXTRA_CALENDAREVENT_ID, -1);
                calendarEventSpec.eventId = intentCopy.getLongExtra(EXTRA_CALENDAREVENT_ID, -1);
                calendarEventSpec.type = intentCopy.getByteExtra(EXTRA_CALENDAREVENT_TYPE, (byte) -1);
                calendarEventSpec.timestamp = intentCopy.getIntExtra(EXTRA_CALENDAREVENT_TIMESTAMP, -1);
                calendarEventSpec.durationInSeconds = intentCopy.getIntExtra(EXTRA_CALENDAREVENT_DURATION, -1);
                calendarEventSpec.allDay = intentCopy.getBooleanExtra(EXTRA_CALENDAREVENT_ALLDAY, false);
                calendarEventSpec.reminders = (ArrayList<Long>) intentCopy.getSerializableExtra(EXTRA_CALENDAREVENT_REMINDERS);
                calendarEventSpec.title = intentCopy.getStringExtra(EXTRA_CALENDAREVENT_TITLE);
                calendarEventSpec.description = intentCopy.getStringExtra(EXTRA_CALENDAREVENT_DESCRIPTION);
                calendarEventSpec.location = intentCopy.getStringExtra(EXTRA_CALENDAREVENT_LOCATION);
                calendarEventSpec.calName = intentCopy.getStringExtra(EXTRA_CALENDAREVENT_CALNAME);
                calendarEventSpec.calendarColor = intentCopy.getIntExtra(EXTRA_CALENDAREVENT_CALENDAR_COLOR, 0);
                calendarEventSpec.color = intentCopy.getIntExtra(EXTRA_CALENDAREVENT_COLOR, 0);
                calendarEventSpec.status = intentCopy.getIntExtra(EXTRA_CALENDAREVENT_STATUS, 0);
                calendarEventSpec.attendingStatus = intentCopy.getIntExtra(EXTRA_CALENDAREVENT_ATTENDING_STATUS, 0);
                deviceSupport.onAddCalendarEvent(calendarEventSpec);
                break;
            }
            case ACTION_DELETE_CALENDAREVENT: {
                final long id = intentCopy.getLongExtra(EXTRA_CALENDAREVENT_ID, -1);
                final byte type = intentCopy.getByteExtra(EXTRA_CALENDAREVENT_TYPE, (byte) -1);
                deviceSupport.onDeleteCalendarEvent(type, id);
                break;
            }
            case ACTION_REBOOT: {
                deviceSupport.onReboot();
                break;
            }
            case ACTION_FACTORY_RESET: {
                deviceSupport.onFactoryReset();
                break;
            }
            case ACTION_HEARTRATE_TEST: {
                deviceSupport.onHeartRateTest();
                break;
            }
            case ACTION_FETCH_RECORDED_DATA: {
                final int dataTypes = intentCopy.getIntExtra(EXTRA_RECORDED_DATA_TYPES, 0);
                deviceSupport.onFetchRecordedData(dataTypes);
                break;
            }
            case ACTION_FIND_DEVICE: {
                final boolean start = intentCopy.getBooleanExtra(EXTRA_FIND_START, false);
                deviceSupport.onFindDevice(start);
                break;
            }
            case ACTION_PHONE_FOUND: {
                final boolean start = intentCopy.getBooleanExtra(EXTRA_FIND_START, false);
                deviceSupport.onFindPhone(start);
                break;
            }
            case ACTION_SET_CONSTANT_VIBRATION: {
                final int intensity = intentCopy.getIntExtra(EXTRA_VIBRATION_INTENSITY, 0);
                deviceSupport.onSetConstantVibration(intensity);
                break;
            }
            case ACTION_CALLSTATE:
                final CallSpec callSpec = new CallSpec();
                callSpec.command = intentCopy.getIntExtra(EXTRA_CALL_COMMAND, CallSpec.CALL_UNDEFINED);
                callSpec.number = intentCopy.getStringExtra(EXTRA_CALL_PHONENUMBER);
                callSpec.name = intentCopy.getStringExtra(EXTRA_CALL_DISPLAYNAME);
                callSpec.sourceName = intentCopy.getStringExtra(EXTRA_CALL_SOURCENAME);
                callSpec.sourceAppId = intentCopy.getStringExtra(EXTRA_CALL_SOURCEAPPID);
                callSpec.key = intentCopy.getStringExtra(EXTRA_CALL_KEY);
                callSpec.channelId = intentCopy.getStringExtra(EXTRA_CALL_CHANNELID);
                callSpec.category = intentCopy.getStringExtra(EXTRA_CALL_CATEGORY);
                callSpec.isVoip = intentCopy.getBooleanExtra(EXTRA_CALL_ISVOIP, false);
                callSpec.dndSuppressed = intentCopy.getIntExtra(EXTRA_CALL_DNDSUPPRESSED, 0);
                deviceSupport.onSetCallState(callSpec);
                break;
            case ACTION_SETCANNEDMESSAGES:
                final int type = intentCopy.getIntExtra(EXTRA_CANNEDMESSAGES_TYPE, -1);
                final String[] cannedMessages = intentCopy.getStringArrayExtra(EXTRA_CANNEDMESSAGES);

                final CannedMessagesSpec cannedMessagesSpec = new CannedMessagesSpec();
                cannedMessagesSpec.type = type;
                cannedMessagesSpec.cannedMessages = cannedMessages;
                deviceSupport.onSetCannedMessages(cannedMessagesSpec);
                break;
            case ACTION_SETTIME:
                deviceSupport.onSetTime();
                break;
            case ACTION_SETMUSICINFO:
                final MusicSpec musicSpec = new MusicSpec();
                musicSpec.artist = intentCopy.getStringExtra(EXTRA_MUSIC_ARTIST);
                musicSpec.album = intentCopy.getStringExtra(EXTRA_MUSIC_ALBUM);
                musicSpec.track = intentCopy.getStringExtra(EXTRA_MUSIC_TRACK);
                musicSpec.duration = intentCopy.getIntExtra(EXTRA_MUSIC_DURATION, 0);
                musicSpec.trackCount = intentCopy.getIntExtra(EXTRA_MUSIC_TRACKCOUNT, 0);
                musicSpec.trackNr = intentCopy.getIntExtra(EXTRA_MUSIC_TRACKNR, 0);
                deviceSupport.onSetMusicInfo(musicSpec);
                break;
            case ACTION_SET_PHONE_VOLUME:
                final float phoneVolume = intentCopy.getFloatExtra(EXTRA_PHONE_VOLUME, 0);
                deviceSupport.onSetPhoneVolume(phoneVolume);
                break;
            case ACTION_SET_PHONE_SILENT_MODE:
                final int ringerMode = intentCopy.getIntExtra(EXTRA_PHONE_RINGER_MODE, -1);
                deviceSupport.onChangePhoneSilentMode(ringerMode);
                break;
            case ACTION_SETMUSICSTATE:
                final MusicStateSpec stateSpec = new MusicStateSpec();
                stateSpec.shuffle = intentCopy.getByteExtra(EXTRA_MUSIC_SHUFFLE, (byte) 0);
                stateSpec.repeat = intentCopy.getByteExtra(EXTRA_MUSIC_REPEAT, (byte) 0);
                stateSpec.position = intentCopy.getIntExtra(EXTRA_MUSIC_POSITION, 0);
                stateSpec.playRate = intentCopy.getIntExtra(EXTRA_MUSIC_RATE, 0);
                stateSpec.state = intentCopy.getByteExtra(EXTRA_MUSIC_STATE, (byte) 0);
                deviceSupport.onSetMusicState(stateSpec);
                break;
            case ACTION_SETNAVIGATIONINFO:
                final NavigationInfoSpec navigationInfoSpec = new NavigationInfoSpec();
                navigationInfoSpec.setInstruction(intentCopy.getStringExtra(EXTRA_NAVIGATION_INSTRUCTION));
                navigationInfoSpec.setNextAction(intentCopy.getIntExtra(EXTRA_NAVIGATION_NEXT_ACTION, 0));
                navigationInfoSpec.setDistanceToTurn(intentCopy.getStringExtra(EXTRA_NAVIGATION_DISTANCE_TO_TURN));
                navigationInfoSpec.setDistanceToTarget(intentCopy.getStringExtra(EXTRA_NAVIGATION_DISTANCE_TO_TARGET));
                // Prefer the time to destination value sent by the receiver, if present, as the ETA has been converted internally
                // in this case
                final int timeToDest = intentCopy.getIntExtra(EXTRA_NAVIGATION_TIME_TO_DESTINATION, 0);
                if( timeToDest != 0) {
                    navigationInfoSpec.setTotalTimeToDestination(timeToDest);
                } else {
                    navigationInfoSpec.setETA(intentCopy.getStringExtra(EXTRA_NAVIGATION_ETA));
                }
                navigationInfoSpec.setCompletionPercent(intentCopy.getIntExtra(EXTRA_NAVIGATION_COMPLETION_PERCENT, 0));
                deviceSupport.onSetNavigationInfo(navigationInfoSpec);
                break;
            case ACTION_REQUEST_APPINFO:
                deviceSupport.onAppInfoReq();
                break;
            case ACTION_REQUEST_SCREENSHOT:
                deviceSupport.onScreenshotReq();
                break;
            case ACTION_STARTAPP: {
                final UUID uuid = (UUID) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                final boolean start = intentCopy.getBooleanExtra(EXTRA_APP_START, true);
                deviceSupport.onAppStart(uuid, start);
                break;
            }
            case ACTION_DOWNLOADAPP: {
                final UUID uuid = (UUID) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                deviceSupport.onAppDownload(uuid);
                break;
            }
            case ACTION_DELETEAPP: {
                final UUID uuid = (UUID) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                deviceSupport.onAppDelete(uuid);
                break;
            }
            case ACTION_APP_CONFIGURE: {
                final UUID uuid = (UUID) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                final String config = intentCopy.getStringExtra(EXTRA_APP_CONFIG);
                Integer id = null;
                if (intentCopy.hasExtra(EXTRA_APP_CONFIG_ID)) {
                    id = intentCopy.getIntExtra(EXTRA_APP_CONFIG_ID, 0);
                }
                deviceSupport.onAppConfiguration(uuid, config, id);
                break;
            }
            case ACTION_APP_CONFIG_REQUEST: {
                final UUID uuid = (UUID) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                deviceSupport.onAppConfigRequest(uuid);
                break;
            }
            case ACTION_APP_CONFIG_SET: {
                final UUID uuid = (UUID) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                final ArrayList<DynamicAppConfig> configs = intentCopy.getParcelableArrayListExtra(EXTRA_APP_CONFIG);
                deviceSupport.onAppConfigSet(uuid, configs);
                break;
            }
            case ACTION_APP_REORDER: {
                final UUID[] uuids = (UUID[]) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                deviceSupport.onAppReorder(uuids);
                break;
            }
            case ACTION_INSTALL: {
                final Uri uri = intentCopy.getParcelableExtra(EXTRA_URI);
                final Bundle options = Objects.requireNonNullElse(intentCopy.getBundleExtra(EXTRA_OPTIONS), Bundle.EMPTY);
                if (uri != null) {
                    LOG.info("will try to install app/fw");
                    deviceSupport.onInstallApp(uri, options);
                } else {
                    LOG.error("Got null uri for app to install");
                }
                break;
            }
            case ACTION_SET_ALARMS:
                final ArrayList<? extends Alarm> alarms = (ArrayList<? extends Alarm>) intentCopy.getSerializableExtra(EXTRA_ALARMS);
                deviceSupport.onSetAlarms(alarms);
                break;
            case ACTION_SET_REMINDERS:
                final ArrayList<? extends Reminder> reminders = (ArrayList<? extends Reminder>) intentCopy.getSerializableExtra(EXTRA_REMINDERS);
                deviceSupport.onSetReminders(reminders);
                break;
            case ACTION_SET_LOYALTY_CARDS:
                final ArrayList<LoyaltyCard> loyaltyCards = (ArrayList<LoyaltyCard>) intentCopy.getSerializableExtra(EXTRA_LOYALTY_CARDS);
                deviceSupport.onSetLoyaltyCards(loyaltyCards);
                break;
            case ACTION_SET_WORLD_CLOCKS:
                final ArrayList<? extends WorldClock> clocks = (ArrayList<? extends WorldClock>) intentCopy.getSerializableExtra(EXTRA_WORLD_CLOCKS);
                deviceSupport.onSetWorldClocks(clocks);
                break;
            case ACTION_SET_CONTACTS:
                final ArrayList<? extends Contact> contacts = (ArrayList<? extends Contact>) intentCopy.getSerializableExtra(EXTRA_CONTACTS);
                deviceSupport.onSetContacts(contacts);
                break;
            case ACTION_ENABLE_REALTIME_STEPS: {
                final boolean enable = intentCopy.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                deviceSupport.onEnableRealtimeSteps(enable);
                break;
            }
            case ACTION_ENABLE_HEARTRATE_SLEEP_SUPPORT: {
                final boolean enable = intentCopy.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                deviceSupport.onEnableHeartRateSleepSupport(enable);
                break;
            }
            case ACTION_SET_HEARTRATE_MEASUREMENT_INTERVAL: {
                final int seconds = intentCopy.getIntExtra(EXTRA_INTERVAL_SECONDS, 0);
                deviceSupport.onSetHeartRateMeasurementInterval(seconds);
                break;
            }
            case ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT: {
                final boolean enable = intentCopy.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                deviceSupport.onEnableRealtimeHeartRateMeasurement(enable);
                break;
            }
            case ACTION_SEND_CONFIGURATION: {
                final String config = intentCopy.getStringExtra(EXTRA_CONFIG);
                deviceSupport.onSendConfiguration(Objects.requireNonNull(config));
                break;
            }
            case ACTION_READ_CONFIGURATION: {
                final String config = intentCopy.getStringExtra(EXTRA_CONFIG);
                deviceSupport.onReadConfiguration(config);
                break;
            }
            case ACTION_TEST_NEW_FUNCTION: {
                final Bundle options = intentCopy.getBundleExtra(EXTRA_OPTIONS);
                deviceSupport.onTestNewFunction(options);
                break;
            }
            case ACTION_SEND_WEATHER: {
                deviceSupport.onSendWeather();
                break;
            }
            case ACTION_SET_LED_COLOR:
                final int color = intentCopy.getIntExtra(EXTRA_LED_COLOR, 0);
                deviceSupport.onSetLedColor(color);
                break;
            case ACTION_POWER_OFF:
                deviceSupport.onPowerOff();
                break;
            case ACTION_SET_FM_FREQUENCY:
                final float frequency = intentCopy.getFloatExtra(EXTRA_FM_FREQUENCY, -1);
                if (frequency != -1) {
                    deviceSupport.onSetFmFrequency(frequency);
                }
                break;
            case ACTION_SET_GPS_LOCATION:
                final Location location = intentCopy.getParcelableExtra(EXTRA_GPS_LOCATION);
                deviceSupport.onSetGpsLocation(location);
                break;
            case ACTION_SLEEP_AS_ANDROID:
                if (device.getDeviceCoordinator().supportsSleepAsAndroid(device) && GBApplication.getPrefs().getString("sleepasandroid_device", "").equals(device.getAddress())) {
                    final String sleepAsAndroidAction = intentCopy.getStringExtra(EXTRA_SLEEP_AS_ANDROID_ACTION);
                    deviceSupport.onSleepAsAndroidAction(sleepAsAndroidAction, intentCopy.getExtras());
                }
                break;
            case ACTION_CAMERA_STATUS_CHANGE:
                final GBDeviceEventCameraRemote.Event event = GBDeviceEventCameraRemote.intToEvent(intentCopy.getIntExtra(EXTRA_CAMERA_EVENT, -1));
                String filename = null;
                if (event == GBDeviceEventCameraRemote.Event.TAKE_PICTURE) {
                    filename = intentCopy.getStringExtra(EXTRA_CAMERA_FILENAME);
                }
                deviceSupport.onCameraStatusChange(event, filename);
                break;
            case ACTION_REQUEST_MUSIC_LIST:
                deviceSupport.onMusicListReq();
                break;
            case ACTION_REQUEST_MUSIC_OPERATION:
                final int operation = intentCopy.getIntExtra(EXTRA_REQUEST_MUSIC_OPERATION, -1);
                final int playlistIndex = intentCopy.getIntExtra(EXTRA_REQUEST_MUSIC_PLAY_LIST_INDEX, -1);
                final String playlistName = intentCopy.getStringExtra(EXTRA_REQUEST_MUSIC_PLAY_LIST_NAME);
                final ArrayList<Integer> musics = (ArrayList<Integer>) intentCopy.getSerializableExtra(EXTRA_REQUEST_MUSIC_MUSIC_IDS);
                deviceSupport.onMusicOperation(operation, playlistIndex, playlistName, musics);
                break;
        }
    }

    /**
     * @param text original text
     * @return 'text' or a new String without non-supported chars like emoticons, etc.
     */
    private static String sanitizeNotifText(final DeviceSupport deviceSupport,
                                            final DeviceCoordinator deviceCoordinator,
                                            final GBDevice device,
                                            String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        text = deviceSupport.customStringFilter(text);

        if (!deviceCoordinator.supportsUnicodeEmojis(device)) {
            return EmojiConverter.convertUnicodeEmojiToAscii(text, GBApplication.getContext());
        }

        return text;
    }
}
