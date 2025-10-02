package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.PendingFileProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminFitFileInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminGpxRouteInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPrgFileInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiCore;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiDeviceStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiFileSyncService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiFindMyWatch;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiInstalledAppsService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSettingsService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.ICommunicator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v1.CommunicatorV1;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v2.CommunicatorV2;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.CapabilitiesDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.FileDownloadedDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.NotificationSubscriptionDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.SupportedFileTypesDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.WeatherRequestDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitAsyncProcessor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitImporter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GpxRouteFileConverter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.PredefinedLocalMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarmLabel;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitAlarmSettings;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitDeviceSettings;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ConfigurationMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.DownloadRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MusicControlEntityUpdateMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ProtobufMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SetDeviceSettingsMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SetFileFlagsMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SupportedFileTypesMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SystemEventMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.NotificationSubscriptionStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.CompressionUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.notifications.GBProgressNotification;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALLOW_HIGH_MTU;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS;


public class GarminSupport extends AbstractBTLESingleDeviceSupport implements ICommunicator.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(GarminSupport.class);
    private final ProtocolBufferHandler protocolBufferHandler;
    private final NotificationsHandler notificationsHandler;
    private final FileTransferHandler fileTransferHandler;
    private final Queue<FileToDownload> filesToDownload;
    private FileToDownload currentlyDownloading;
    private final List<MessageHandler> messageHandlers;
    private final List<FileType> supportedFileTypeList = new ArrayList<>();
    private ICommunicator communicator;
    private MediaManager mediaManager;
    private boolean mFirstConnect = false;
    private boolean isBusyFetching;

    private GBProgressNotification transferNotification;

    final Map<UUID, GdiInstalledAppsService.InstalledAppsService.InstalledApp> installedApps = new HashMap<>();

    public GarminSupport() {
        super(LOG);
        addSupportedService(CommunicatorV1.UUID_SERVICE_GARMIN_GFDI_V0);
        addSupportedService(CommunicatorV1.UUID_SERVICE_GARMIN_GFDI_V1);
        addSupportedService(CommunicatorV2.UUID_SERVICE_GARMIN_ML_GFDI);
        protocolBufferHandler = new ProtocolBufferHandler(this);
        fileTransferHandler = new FileTransferHandler(this);
        filesToDownload = new LinkedList<>();
        messageHandlers = new ArrayList<>();
        notificationsHandler = new NotificationsHandler();
        messageHandlers.add(fileTransferHandler);
        messageHandlers.add(protocolBufferHandler);
        messageHandlers.add(notificationsHandler);
    }

    @Override
    public void setContext(final GBDevice gbDevice, final BluetoothAdapter btAdapter, final Context context) {
        super.setContext(gbDevice, btAdapter, context);
        this.mediaManager = new MediaManager(context);
        this.transferNotification = new GBProgressNotification(context, GB.NOTIFICATION_CHANNEL_ID_TRANSFER);
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            LOG.info("Garmin dispose()");
            GBLocationService.stop(getContext(), getDevice());
            super.dispose();
        }
    }

    public void onFileDownloadProgress(final int progress) {
        transferNotification.setChunkProgress(progress);
    }

    public void addFileToDownloadList(FileTransferHandler.DirectoryEntry directoryEntry) {
        if (newSyncProtocol()) {
            if (directoryEntry.getFiletype() == FileType.FILETYPE.DIRECTORY) {
                LOG.debug("Got directory entry, syncing with new protocol");
                sendOutgoingMessage(
                        "request file list",
                        protocolBufferHandler.prepareProtobufRequest(
                                GdiSmartProto.Smart.newBuilder().setFileSyncService(
                                        protocolBufferHandler.getFileSyncServiceHandler().requestFileList()
                                ).build()
                        )
                );
                return;
            }
            LOG.warn("Ignoring directory entry {} in new sync protocol", directoryEntry.getFileName());
            return;
        }
        filesToDownload.add(new FileToDownload(directoryEntry));
        if (directoryEntry.getFiletype() != FileType.FILETYPE.DIRECTORY) {
            transferNotification.incrementTotalSize(directoryEntry.getFileSize());
        }
    }

    public void addFileToDownloadList(GdiFileSyncService.File file) {
        filesToDownload.add(new FileToDownload(file));
        if (file.hasSize()) {
            transferNotification.incrementTotalSize(file.getSize());
        }
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public GarminPrefs getDevicePrefs() {
        return new GarminPrefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()), gbDevice);
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);

        if (getDevicePrefs().getBoolean(PREF_ALLOW_HIGH_MTU, true)) {
            builder.requestMtu(515);
        }

        final CommunicatorV2 communicatorV2 = new CommunicatorV2(this);
        if (communicatorV2.initializeDevice(builder)) {
            communicator = communicatorV2;
        } else {
            // V2 did not manage to initialize, attempt V1
            final CommunicatorV1 communicatorV1 = new CommunicatorV1(this);
            if (!communicatorV1.initializeDevice(builder)) {
                // Neither V1 nor V2 worked, not a Garmin device?
                LOG.warn("Failed to find a known Garmin service");
                builder.setDeviceState(GBDevice.State.NOT_CONNECTED);
                return builder;
            }

            communicator = communicatorV1;
        }

        return builder;
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        super.onMtuChanged(gatt, mtu, status);
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }

        if (mtu < 23) {
            LOG.warn("Ignoring mtu of {}, too low", mtu);
            return;
        }
        if (!getDevicePrefs().getBoolean(PREF_ALLOW_HIGH_MTU, true)) {
            LOG.warn("Ignoring mtu change to {} - high mtu is disabled", mtu);
            return;
        }

        communicator.onMtuChanged(mtu);
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] value) {
        final UUID characteristicUUID = characteristic.getUuid();
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            LOG.debug("Change of characteristic {} handled by parent", characteristicUUID);
            return true;
        }

        return communicator.onCharacteristicChanged(gatt, characteristic, value);
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (communicator != null) {
            communicator.onConnectionStateChange(gatt, status, newState);
        }
    }

    @Override
    public void onMessage(final byte[] message) {
        if (null == message) {
            return; //message is not complete yet TODO check before calling
        }
//        LOG.debug("COBS decoded MESSAGE: {}", GB.hexdump(message));

        GFDIMessage parsedMessage = GFDIMessage.parseIncoming(message);

        if (null == parsedMessage) {
            LOG.error("GFDIMessage is null - this should never happen");
            return; //message cannot be handled
        }

        LOG.debug("Got GFDIMessage {} ({} bytes)", parsedMessage.getClass().getSimpleName(), message.length);

        /*
        the handler elaborates the followup message but might change the status message since it does
        check the integrity of the incoming message payload. Hence we let the handlers elaborate the
        incoming message, then we send the status message of the incoming message, then the response
        and finally we send the followup.
         */

        GFDIMessage followup = null;
        for (MessageHandler han : messageHandlers) {
            followup = han.handle(parsedMessage);
            if (followup != null) {
                break;
            }
        }

        final List<GBDeviceEvent> events = parsedMessage.getGBDeviceEvent();
        for (final GBDeviceEvent event : events) {
            evaluateGBDeviceEvent(event);
        }

        communicator.sendMessage("send status", parsedMessage.getAckBytestream()); //send status message

        sendOutgoingMessage("send reply", parsedMessage); //send reply if any

        sendOutgoingMessage("send followup", followup); //send followup message if any

        if (parsedMessage instanceof ConfigurationMessage) { //the last forced message exchange
            completeInitialization();
        }

        processDownloadQueue();

    }

    protected String getNotificationAttachmentPath(int notificationId) {
        return notificationsHandler.getNotificationAttachmentPath(notificationId);
    }

    protected Bitmap getNotificationAttachmentBitmap(int notificationId) {
        final String picturePath = getNotificationAttachmentPath(notificationId);
        final Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
        if (bitmap == null) {
            LOG.warn("Failed to load bitmap for {} from {}", notificationId, picturePath);
        }
        return bitmap;
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        LOG.info("INCOMING CALLSPEC: {}", callSpec.command);
        sendOutgoingMessage("send call", notificationsHandler.onSetCallState(callSpec));
    }

    @Override
    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {
        if (deviceEvent instanceof WeatherRequestDeviceEvent) {
            WeatherSpec weather = Weather.getWeatherSpec();
            if (weather != null) {
                sendWeatherConditions(weather);
            }
        } else if (deviceEvent instanceof CapabilitiesDeviceEvent) {
            final Set<GarminCapability> capabilities = ((CapabilitiesDeviceEvent) deviceEvent).capabilities;
            if (capabilities.contains(GarminCapability.REALTIME_SETTINGS)) {
                final String language = Locale.getDefault().getLanguage();
                final String country = Locale.getDefault().getCountry();
                final String localeString = language + "_" + country.toUpperCase();
                final ProtobufMessage realtimeSettingsInit = protocolBufferHandler.prepareProtobufRequest(GdiSmartProto.Smart.newBuilder()
                        .setSettingsService(
                                GdiSettingsService.SettingsService.newBuilder()
                                        .setInitRequest(
                                                GdiSettingsService.InitRequest.newBuilder()
                                                        .setLanguage(localeString.length() == 5 ? localeString : "en_US")
                                                        .setRegion("us") // FIXME choose region
                                        )
                        )
                        .build());
                sendOutgoingMessage("init realtime settings", realtimeSettingsInit);
            }
        } else if (deviceEvent instanceof NotificationSubscriptionDeviceEvent) {
            final boolean enable = ((NotificationSubscriptionDeviceEvent) deviceEvent).enable;
            notificationsHandler.setEnabled(enable);

            final NotificationSubscriptionStatusMessage.NotificationStatus finalStatus;
            if (getDevicePrefs().getBoolean(PREF_SEND_APP_NOTIFICATIONS, true)) {
                finalStatus = NotificationSubscriptionStatusMessage.NotificationStatus.ENABLED;
            } else {
                finalStatus = NotificationSubscriptionStatusMessage.NotificationStatus.DISABLED;
            }

            LOG.info("NOTIFICATIONS ARE NOW enabled={}, status={}", enable, finalStatus);

            sendOutgoingMessage("toggle notification subscription", new NotificationSubscriptionStatusMessage(
                    GFDIMessage.Status.ACK,
                    finalStatus,
                    enable,
                    0
            ));
        } else if (deviceEvent instanceof SupportedFileTypesDeviceEvent) {
            this.supportedFileTypeList.clear();
            this.supportedFileTypeList.addAll(((SupportedFileTypesDeviceEvent) deviceEvent).getSupportedFileTypes());
        } else if (deviceEvent instanceof FileDownloadedDeviceEvent fileDownloadedDeviceEvent) {
            final FileTransferHandler.DirectoryEntry entry = fileDownloadedDeviceEvent.directoryEntry;
            if (!fileDownloadedDeviceEvent.success) {
                LOG.warn("FILE DOWNLOAD FAILED");
                // Continue to the next one
                currentlyDownloading = null;
                return;
            }

            if (entry != null) {
                final String filename = entry.getFileName();
                LOG.debug("FILE DOWNLOAD COMPLETE {}", filename);
                transferNotification.incrementTotalProgress(entry.getFileSize());

                if (entry.getFiletype().isFitFile()) {
                    try (DBHandler handler = GBApplication.acquireDB()) {
                        final DaoSession session = handler.getDaoSession();

                        final PendingFileProvider pendingFileProvider = new PendingFileProvider(gbDevice, session);
                        pendingFileProvider.addPendingFile(fileDownloadedDeviceEvent.localPath);
                    } catch (final Exception e) {
                        GB.toast(getContext(), "Error saving pending file", Toast.LENGTH_LONG, GB.ERROR, e);
                    }
                }

                if (!getKeepActivityDataOnDevice()) { // delete file from watch upon successful download
                    sendOutgoingMessage("archive file " + entry.getFileIndex(), new SetFileFlagsMessage(entry.getFileIndex(), SetFileFlagsMessage.FileFlags.ARCHIVE));
                }
            } else if (fileDownloadedDeviceEvent.localPath != null) {
                LOG.debug("ZIP DOWNLOAD COMPLETE {}", fileDownloadedDeviceEvent.localPath);
                final File zipFile = new File(fileDownloadedDeviceEvent.localPath);
                if (gbDevice.isBusy()) {
                    transferNotification.incrementTotalProgress(zipFile.length());
                }

                try (DBHandler handler = GBApplication.acquireDB()) {
                    final DaoSession session = handler.getDaoSession();

                    final PendingFileProvider pendingFileProvider = new PendingFileProvider(gbDevice, session);
                    pendingFileProvider.addPendingFile(fileDownloadedDeviceEvent.localPath);
                } catch (final Exception e) {
                    GB.toast(getContext(), "Error saving pending file", Toast.LENGTH_LONG, GB.ERROR, e);
                }
            } else {
                LOG.error("Got invalid FileDownloadedDeviceEvent");
            }

            currentlyDownloading = null;
        } else {
            super.evaluateGBDeviceEvent(deviceEvent);
        }
    }

    /**
     * @noinspection BooleanMethodIsAlwaysInverted
     */
    private boolean getKeepActivityDataOnDevice() {
        return getDevicePrefs().getBoolean("keep_activity_data_on_device", false);
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        if (dataTypes == RecordedDataTypes.TYPE_DEBUGLOGS) {
            sendOutgoingMessage("fetch debug data", fileTransferHandler.initiateDebugDownload());
            return;
        }

        if (this.supportedFileTypeList.isEmpty() && !newSyncProtocol()) {
            LOG.warn("No known supported file types");
            return;
        }

        // FIXME respect dataTypes?

        // We initiate download here even in the new sync protocol so that the watch "flushes" the data
        // otherwise we might get incomplete monitor files
        sendOutgoingMessage("fetch recorded data", fileTransferHandler.initiateDownload());
    }

    public boolean newSyncProtocol() {
        return getDevicePrefs().getBoolean("new_sync_protocol", false);
    }

    public boolean mlrEnabled() {
        return getDevicePrefs().getBoolean("garmin_mlr", false);
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        sendOutgoingMessage("send notification " + notificationSpec.getId(), notificationsHandler.onNotification(notificationSpec));
    }

    @Override
    public void onDeleteNotification(int id) {
        sendOutgoingMessage("delete notification " + id, notificationsHandler.onDeleteNotification(id));
    }

    @Override
    public void onAppInfoReq() {
        sendOutgoingMessage(
                "request apps",
                protocolBufferHandler.prepareProtobufRequest(
                        GdiSmartProto.Smart.newBuilder().setInstalledAppsService(
                                GdiInstalledAppsService.InstalledAppsService.newBuilder().setGetInstalledAppsRequest(
                                        GdiInstalledAppsService.InstalledAppsService.GetInstalledAppsRequest.newBuilder()
                                                .setAppType(GdiInstalledAppsService.InstalledAppsService.AppType.ALL)
                                )
                        ).build()
                )
        );
    }

    @Override
    public void onAppStart(final UUID uuid, final boolean start) {

    }

    @Override
    public void onAppDelete(final UUID uuid) {
        final GdiInstalledAppsService.InstalledAppsService.InstalledApp app = installedApps.get(uuid);

        if (app == null) {
            LOG.warn("Unknown app {}", uuid);
            return;
        }

        sendOutgoingMessage(
                "delete app",
                protocolBufferHandler.prepareProtobufRequest(
                        GdiSmartProto.Smart.newBuilder().setInstalledAppsService(
                                GdiInstalledAppsService.InstalledAppsService.newBuilder().setDeleteAppRequest(
                                        GdiInstalledAppsService.InstalledAppsService.DeleteAppRequest.newBuilder()
                                                .setStoreAppId(app.getStoreAppId())
                                                .setAppType(app.getType())
                                )
                        ).build()
                )
        );
    }

    public void onAppListReceived(final List<GdiInstalledAppsService.InstalledAppsService.InstalledApp> apps) {
        installedApps.clear();

        final List<GBDeviceApp> gbApps = new ArrayList<>(apps.size());

        for (final GdiInstalledAppsService.InstalledAppsService.InstalledApp installedApp : apps) {
            GBDeviceApp.Type type;

            switch (installedApp.getType()) {
                case WATCH_FACE:
                    type = GBDeviceApp.Type.WATCHFACE;
                    break;
                case DATA_FIELD:
                case ACTIVITY:
                    type = GBDeviceApp.Type.APP_ACTIVITYTRACKER;
                    break;
                default:
                    // FIXME we set everything else as app generic otherwise they get filtered, add new types
                    type = GBDeviceApp.Type.APP_GENERIC;
            }

            final UUID uuid = UUID.nameUUIDFromBytes(installedApp.getStoreAppId().toByteArray());
            installedApps.put(uuid, installedApp);
            gbApps.add(new GBDeviceApp(
                    uuid,
                    installedApp.getName() + " (" + installedApp.getType() + ")",
                    "",
                    String.valueOf(installedApp.getVersion()),
                    type
            ));
            gbApps.sort(Comparator.comparing(GBDeviceApp::getName));
        }

        final GBDeviceEventAppInfo appInfoCmd = new GBDeviceEventAppInfo();
        appInfoCmd.apps = gbApps.toArray(new GBDeviceApp[0]);
        evaluateGBDeviceEvent(appInfoCmd);
    }

    @Override
    public void onSendWeather() { //todo: find the closest one relative to the requested lat/long
        WeatherSpec weatherSpec = Weather.getWeatherSpec();
        if (weatherSpec == null) {
            LOG.warn("No weather found in singleton");
            return;
        }
        sendWeatherConditions(weatherSpec);
    }

    private void sendOutgoingMessage(final String taskName, final GFDIMessage message) {
        if (message == null)
            return;
        communicator.sendMessage(taskName, message.getOutgoingMessage());
    }

    private void sendWeatherConditions(WeatherSpec weather) {
        if (!getCoordinator().supports(getDevice(), GarminCapability.WEATHER_CONDITIONS)) {
            // Device does not support sending weather as fit
            return;
        }

        List<RecordData> weatherData = new ArrayList<>();

        final RecordDefinition recordDefinitionToday = PredefinedLocalMessage.TODAY_WEATHER_CONDITIONS.getRecordDefinition();
        final RecordDefinition recordDefinitionHourly = PredefinedLocalMessage.HOURLY_WEATHER_FORECAST.getRecordDefinition();
        final RecordDefinition recordDefinitionDaily = PredefinedLocalMessage.DAILY_WEATHER_FORECAST.getRecordDefinition();

        List<RecordDefinition> weatherDefinitions = new ArrayList<>(3);
        weatherDefinitions.add(recordDefinitionToday);
        weatherDefinitions.add(recordDefinitionHourly);
        weatherDefinitions.add(recordDefinitionDaily);

        sendOutgoingMessage("send weather definitions", new nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDefinitionMessage(weatherDefinitions));

        RecordData today = new RecordData(recordDefinitionToday, recordDefinitionToday.getRecordHeader());
        today.setFieldByName("weather_report", 0); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
        today.setFieldByName("timestamp", weather.getTimestamp());
        today.setFieldByName("observed_at_time", weather.getTimestamp());
        today.setFieldByName("temperature", weather.getCurrentTemp());
        today.setFieldByName("low_temperature", weather.getTodayMinTemp());
        today.setFieldByName("high_temperature", weather.getTodayMaxTemp());
        today.setFieldByName("condition", weather.getCurrentConditionCode());
        today.setFieldByName("wind_direction", weather.getWindDirection());
        today.setFieldByName("precipitation_probability", weather.getPrecipProbability());
        today.setFieldByName("wind_speed", Math.round(weather.getWindSpeed()));
        today.setFieldByName("temperature_feels_like", weather.getFeelsLikeTemp());
        today.setFieldByName("relative_humidity", weather.getCurrentHumidity());
        today.setFieldByName("observed_location_lat", weather.getLatitude());
        today.setFieldByName("observed_location_long", weather.getLongitude());
        today.setFieldByName("dew_point", weather.getDewPoint());
        if (null != weather.getAirQuality()) {
            today.setFieldByName("air_quality", weather.getAirQuality().getAqi());
        }
        today.setFieldByName("location", weather.getLocation());
        weatherData.add(today);

        for (int hour = 0; hour <= 11; hour++) {
            if (hour < weather.getHourly().size()) {
                WeatherSpec.Hourly hourly = weather.getHourly().get(hour);
                RecordData weatherHourlyForecast = new RecordData(recordDefinitionHourly, recordDefinitionHourly.getRecordHeader());
                weatherHourlyForecast.setFieldByName("weather_report", 1); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                weatherHourlyForecast.setFieldByName("timestamp", hourly.getTimestamp());
                weatherHourlyForecast.setFieldByName("temperature", hourly.getTemp());
                weatherHourlyForecast.setFieldByName("condition", hourly.getConditionCode());
                weatherHourlyForecast.setFieldByName("temperature_feels_like", hourly.getTemp()); //TODO: switch to actual feels like field once Hourly contains this information
                weatherHourlyForecast.setFieldByName("wind_direction", hourly.getWindDirection());
                weatherHourlyForecast.setFieldByName("wind_speed", Math.round(hourly.getWindSpeed()));
                weatherHourlyForecast.setFieldByName("precipitation_probability", hourly.getPrecipProbability());
                weatherHourlyForecast.setFieldByName("relative_humidity", hourly.getHumidity());
//                    weatherHourlyForecast.setFieldByName("dew_point", 0); // TODO: add once Hourly contains this information
                weatherHourlyForecast.setFieldByName("uv_index", hourly.getUvIndex());
//                    weatherHourlyForecast.setFieldByName("air_quality", 0); // TODO: add once Hourly contains this information
                weatherData.add(weatherHourlyForecast);
            }
        }
//
        RecordData todayDailyForecast = new RecordData(recordDefinitionDaily, recordDefinitionDaily.getRecordHeader());
        todayDailyForecast.setFieldByName("weather_report", 2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
        todayDailyForecast.setFieldByName("timestamp", weather.getTimestamp());
        todayDailyForecast.setFieldByName("low_temperature", weather.getTodayMinTemp());
        todayDailyForecast.setFieldByName("high_temperature", weather.getTodayMaxTemp());
        todayDailyForecast.setFieldByName("condition", weather.getCurrentConditionCode());
        todayDailyForecast.setFieldByName("precipitation_probability", weather.getPrecipProbability());
        todayDailyForecast.setFieldByName("day_of_week", weather.getTimestamp());
        if (null != weather.getAirQuality()) {
            todayDailyForecast.setFieldByName("air_quality", weather.getAirQuality().getAqi());
        }
        weatherData.add(todayDailyForecast);


        for (int day = 0; day < 4; day++) {
            if (day < weather.getForecasts().size()) {
                //noinspection ExtractMethodRecommender
                WeatherSpec.Daily daily = weather.getForecasts().get(day);
                int ts = weather.getTimestamp() + (day + 1) * 24 * 60 * 60;
                RecordData weatherDailyForecast = new RecordData(recordDefinitionDaily, recordDefinitionDaily.getRecordHeader());
                weatherDailyForecast.setFieldByName("weather_report", 2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                weatherDailyForecast.setFieldByName("timestamp", weather.getTimestamp());
                weatherDailyForecast.setFieldByName("low_temperature", daily.getMinTemp());
                weatherDailyForecast.setFieldByName("high_temperature", daily.getMaxTemp());
                weatherDailyForecast.setFieldByName("condition", daily.getConditionCode());
                weatherDailyForecast.setFieldByName("precipitation_probability", daily.getPrecipProbability());
                if (null != daily.getAirQuality()) {
                    weatherDailyForecast.setFieldByName("air_quality", daily.getAirQuality().getAqi());
                }
                weatherDailyForecast.setFieldByName("day_of_week", ts);
                weatherData.add(weatherDailyForecast);
            }
        }

        sendOutgoingMessage("send weather data", new nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDataMessage(weatherData));

    }

    private void completeInitialization() {
        if (GBApplication.getPrefs().syncTime()) {
            onSetTime();
        }
        enableWeather();

        //following is needed for vivomove style
        sendOutgoingMessage("set sync ready", new SystemEventMessage(SystemEventMessage.GarminSystemEventType.SYNC_READY, 0));

        enableBatteryLevelUpdate();

        gbDevice.setUpdateState(GBDevice.State.INITIALIZED, getContext());

        sendOutgoingMessage("request supported file types", new SupportedFileTypesMessage());

        if (mFirstConnect) {
            sendOutgoingMessage("set sync complete", new SystemEventMessage(SystemEventMessage.GarminSystemEventType.SYNC_COMPLETE, 0));
            this.mFirstConnect = false;
        }
    }

    @Override
    public void onSendConfiguration(final String config) {
        if (config.startsWith("protobuf:")) {
            try {
                final GdiSmartProto.Smart smart = GdiSmartProto.Smart.parseFrom(GB.hexStringToByteArray(config.replaceFirst("protobuf:", "")));
                sendOutgoingMessage("send config", protocolBufferHandler.prepareProtobufRequest(smart));
            } catch (final Exception e) {
                LOG.error("Failed to send {} as protobuf", config, e);
            }

            return;
        }

        //noinspection SwitchStatementWithTooFewBranches
        switch (config) {
            case PREF_SEND_APP_NOTIFICATIONS:
                NotificationSubscriptionDeviceEvent notificationSubscriptionDeviceEvent = new NotificationSubscriptionDeviceEvent();
                notificationSubscriptionDeviceEvent.enable = true; // actual status is fetched from preferences
                evaluateGBDeviceEvent(notificationSubscriptionDeviceEvent);
                return;
        }
    }

    private void processDownloadQueue() {
        if (!filesToDownload.isEmpty() && currentlyDownloading == null) {
            if (!gbDevice.isBusy()) {
                isBusyFetching = true;
                transferNotification.start(
                        R.string.busy_task_fetch_activity_data,
                        0,
                        filesToDownload.stream().mapToLong(FileToDownload::getSize).sum()
                );
                getDevice().setBusyTask(R.string.busy_task_fetch_activity_data, getContext());
                getDevice().sendDeviceUpdateIntent(getContext());
            }

            while (!filesToDownload.isEmpty()) {
                currentlyDownloading = filesToDownload.remove();
                if (currentlyDownloading.getDirectoryEntry() != null) {
                    FileTransferHandler.DirectoryEntry directoryEntry = currentlyDownloading.getDirectoryEntry();
                    if (alreadyDownloaded(directoryEntry)) {
                        LOG.debug("File: {} already downloaded, not downloading again.", directoryEntry.getFileName());
                        if (!getKeepActivityDataOnDevice()) { // delete file from watch if already downloaded
                            currentlyDownloading = null;
                            sendOutgoingMessage("archive file " + directoryEntry.getFileIndex(), new SetFileFlagsMessage(directoryEntry.getFileIndex(), SetFileFlagsMessage.FileFlags.ARCHIVE));
                        }
                        if (directoryEntry.getFiletype() != FileType.FILETYPE.DIRECTORY) {
                            transferNotification.incrementTotalProgress(directoryEntry.getFileSize());
                        }
                        continue;
                    }

                    final DownloadRequestMessage downloadRequestMessage = fileTransferHandler.downloadDirectoryEntry(directoryEntry);
                    LOG.debug("Will download file: {}", directoryEntry.getOutputPath());
                    if (directoryEntry.getFiletype() != FileType.FILETYPE.DIRECTORY) {
                        transferNotification.setChunkProgress(0);
                    }
                    sendOutgoingMessage("download file " + directoryEntry.getFileIndex(), downloadRequestMessage);
                } else if (currentlyDownloading.getSyncFile() != null) {
                    LOG.debug("Will download file: {}/{}", currentlyDownloading.getSyncFile().getId().getId1(), currentlyDownloading.getSyncFile().getId().getId2());

                    sendOutgoingMessage(
                            "request file",
                            protocolBufferHandler.prepareProtobufRequest(
                                    GdiSmartProto.Smart.newBuilder().setFileSyncService(
                                            protocolBufferHandler.getFileSyncServiceHandler().requestFile(currentlyDownloading.getSyncFile())
                                    ).build()
                            )
                    );
                }

                return;
            }
        }

        if (filesToDownload.isEmpty() && currentlyDownloading == null && isBusyFetching) {
            final List<File> filesToProcess;
            try (DBHandler handler = GBApplication.acquireDB()) {
                final DaoSession session = handler.getDaoSession();

                final PendingFileProvider pendingFileProvider = new PendingFileProvider(gbDevice, session);

                filesToProcess = pendingFileProvider.getAllPendingFiles()
                        .stream()
                        .map(pf -> new File(pf.getPath()))
                        .collect(Collectors.toList());
            } catch (final Exception e) {
                LOG.error("Failed to get pending files", e);
                return;
            }

            if (filesToProcess.isEmpty()) {
                LOG.debug("No pending files to process");
                // No downloaded fit files to process
                if (gbDevice.isBusy() && isBusyFetching) {
                    getDevice().unsetBusyTask();
                    GB.signalActivityDataFinish(getDevice());
                    transferNotification.finish();
                    getDevice().sendDeviceUpdateIntent(getContext());
                }
                isBusyFetching = false;
                return;
            }

            // Keep the device marked as busy while we process the files asynchronously, but unset
            // isBusyFetching so we do not start multiple processors
            isBusyFetching = false;

            transferNotification.start(R.string.busy_task_processing_files, 0, filesToProcess.size());

            final FitAsyncProcessor fitAsyncProcessor = new FitAsyncProcessor(getContext(), getDevice());
            fitAsyncProcessor.process(filesToProcess, new FitAsyncProcessor.Callback() {
                @Override
                public void onProgress(final int i) {
                    transferNotification.setTotalProgress(i);
                }

                @Override
                public void onFinish() {
                    getDevice().unsetBusyTask();
                    GB.signalActivityDataFinish(getDevice());
                    transferNotification.finish();
                    getDevice().sendDeviceUpdateIntent(getContext());
                }
            });
        }
    }

    private void enableBatteryLevelUpdate() {
        final ProtobufMessage batteryLevelProtobufRequest = protocolBufferHandler.prepareProtobufRequest(GdiSmartProto.Smart.newBuilder()
                .setDeviceStatusService(
                        GdiDeviceStatus.DeviceStatusService.newBuilder()
                                .setRemoteDeviceBatteryStatusRequest(
                                        GdiDeviceStatus.DeviceStatusService.RemoteDeviceBatteryStatusRequest.newBuilder()
                                )
                )
                .build());
        sendOutgoingMessage("enable battery updates", batteryLevelProtobufRequest);
    }

    private void enableWeather() {
        final Map<SetDeviceSettingsMessage.GarminDeviceSetting, Object> settings = new LinkedHashMap<>(3);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.AUTO_UPLOAD_ENABLED, false);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.WEATHER_CONDITIONS_ENABLED, true);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.WEATHER_ALERTS_ENABLED, false);
        sendOutgoingMessage("enable weather", new SetDeviceSettingsMessage(settings));
    }

    @Override
    public void onSetTime() {
        sendOutgoingMessage("set time", new SystemEventMessage(SystemEventMessage.GarminSystemEventType.TIME_UPDATED, 0));
    }

    @Override
    public void onFindDevice(boolean start) {
        final GdiFindMyWatch.FindMyWatchService.Builder a = GdiFindMyWatch.FindMyWatchService.newBuilder();
        if (start) {
            a.setFindRequest(
                    GdiFindMyWatch.FindMyWatchService.FindMyWatchRequest.newBuilder()
                            .setTimeout(60)
            );
        } else {
            a.setCancelRequest(
                    GdiFindMyWatch.FindMyWatchService.FindMyWatchCancelRequest.newBuilder()
            );
        }
        final ProtobufMessage findMyWatch = protocolBufferHandler.prepareProtobufRequest(
                GdiSmartProto.Smart.newBuilder()
                        .setFindMyWatchService(a).build());

        sendOutgoingMessage("find device", findMyWatch);
    }

    @Override
    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        final int alarmSlotCount = getCoordinator().getAlarmSlotCount(getDevice());

        final List<RecordData> dataRecords = new ArrayList<>(1 + alarmSlotCount);

        final int currentTime = (int) (System.currentTimeMillis() / 1000);

        dataRecords.add(new FitFileId.Builder()
                .setType(FileType.FILETYPE.SETTINGS)
                .setManufacturer(1) // garmin
                .setProduct(65534) // connect
                .setTimeCreated((long) currentTime)
                .setSerialNumber(1L)
                .setNumber(1)
                .build());

        final List<Number> deviceSettingsTimes = new ArrayList<>();
        final List<Number> deviceSettingsUnk5 = new ArrayList<>();
        final List<Number> deviceSettingsEnabled = new ArrayList<>();
        final List<Number> deviceSettingsRepeat = new ArrayList<>();

        int numberEnabledAlarms = 0;
        for (Alarm alarm : alarms) {
            if (alarm.getUnused()) {
                continue;
            }

            final int soundCode = switch (Alarm.ALARM_SOUND.values()[alarm.getSoundCode()]) {
                case OFF -> 0;
                case TONE -> 1;
                case VIBRATION -> 2;
                case UNSET, TONE_AND_VIBRATION -> 3;
            };
            final FieldDefinitionAlarmLabel.Label label;

            final String alarmTitle = alarm.getTitle();
            if (StringUtils.isBlank(alarmTitle)) {
                label = FieldDefinitionAlarmLabel.Label.NONE;
            } else {
                FieldDefinitionAlarmLabel.Label alarmLabel;
                try {
                    alarmLabel = FieldDefinitionAlarmLabel.Label.valueOf(alarmTitle);
                } catch (final Exception e) {
                    LOG.error("Invalid alarm label {}", alarmTitle, e);
                    alarmLabel = FieldDefinitionAlarmLabel.Label.NONE;
                }
                label = alarmLabel;
            }

            final long repetitionCode = alarm.getRepetition() != 0 ? alarm.getRepetition() : 128L;

            final FitAlarmSettings.Builder alarmBuilder = new FitAlarmSettings.Builder()
                    .setTime(LocalTime.of(alarm.getHour(), alarm.getMinute()))
                    .setRepeat(repetitionCode)
                    .setEnabled(alarm.getEnabled() ? 1 : 0)
                    .setSound(soundCode)
                    .setBacklight(alarm.getBacklight() ? 1 : 0)
                    .setSomeTimestamp((long) currentTime)
                    .setUnknown7(0)
                    .setLabel(label)
                    .setMessageIndex(numberEnabledAlarms);

            dataRecords.add(alarmBuilder.build());

            deviceSettingsTimes.add(alarm.getHour() * 60 + alarm.getMinute());
            deviceSettingsUnk5.add(5);
            deviceSettingsEnabled.add(alarm.getEnabled() ? 1 : 0);
            deviceSettingsRepeat.add(repetitionCode);

            numberEnabledAlarms++;
        }

        if (numberEnabledAlarms > 0) {
            final FitDeviceSettings.Builder deviceSettingsBuilder = new FitDeviceSettings.Builder()
                    .setAlarmsTime(deviceSettingsTimes.toArray(new Number[0]))
                    .setAlarmsUnk5(deviceSettingsUnk5.toArray(new Number[0]))
                    .setAlarmsEnabled(deviceSettingsEnabled.toArray(new Number[0]))
                    .setAlarmsRepeat(deviceSettingsRepeat.toArray(new Number[0]));

            dataRecords.add(deviceSettingsBuilder.build());
        }

        final FitFile fitFile = new FitFile(dataRecords);
        communicator.sendMessage(
                "set alarms",
                fileTransferHandler.initiateUpload(
                        fitFile.getOutgoingMessage(),
                        fitFile.getFileType()
                ).getOutgoingMessage()
        );
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        sendOutgoingMessage("set canned messages", protocolBufferHandler.setCannedMessages(cannedMessagesSpec));
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        if (!mediaManager.onSetMusicInfo(musicSpec)) {
            return;
        }

        LOG.debug("onSetMusicInfo: {}", musicSpec.toString());

        Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();

        attributes.put(MusicControlEntityUpdateMessage.TRACK.ARTIST, musicSpec.artist);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.ALBUM, musicSpec.album);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.TITLE, musicSpec.track);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.DURATION, String.valueOf(musicSpec.duration));

        sendOutgoingMessage("set music info", new MusicControlEntityUpdateMessage(attributes));

        // Update the music state spec as well
        final MusicStateSpec bufferMusicStateSpec = mediaManager.getBufferMusicStateSpec();
        if (bufferMusicStateSpec != null) {
            sendMusicState(bufferMusicStateSpec, bufferMusicStateSpec.position);
        }
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        if (!mediaManager.onSetMusicState(stateSpec)) {
            return;
        }

        LOG.debug("onSetMusicState: {}", stateSpec.toString());

        sendMusicState(stateSpec, stateSpec.position);
    }

    private void sendMusicState(final MusicStateSpec stateSpec, final int progress) {
        final int playing;
        final float playRate;
        if (stateSpec.state == MusicStateSpec.STATE_PLAYING) {
            playing = 1;
            playRate = stateSpec.playRate > 0 ? stateSpec.playRate / 100f : 1.0f;
        } else {
            playing = 0;
            playRate = 0;
        }
        final Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();
        attributes.put(
                MusicControlEntityUpdateMessage.PLAYER.PLAYBACK_INFO,
                String.format(Locale.ROOT, "%d,%.1f,%.3f", playing, playRate, (float) progress)
        );
        sendOutgoingMessage("set music state", new MusicControlEntityUpdateMessage(attributes));
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        final Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();

        attributes.put(MusicControlEntityUpdateMessage.PLAYER.VOLUME, String.format(Locale.ROOT, "%.2f", volume / 100f));

        sendOutgoingMessage("set phone volume", new MusicControlEntityUpdateMessage(attributes));
    }

    private boolean alreadyDownloaded(final FileTransferHandler.DirectoryEntry entry) {
        // Current filename
        final Optional<File> file = getFile(entry.getOutputPath());
        if (file.isPresent()) {
            if (file.get().length() == 0) {
                LOG.warn("File {} is empty", entry.getOutputPath());
                return false;
            }
            return true;
        }

        // Legacy filename 1, before we had per-type/year folder
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat legacyDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
        final StringBuilder sbLegacy1 = new StringBuilder(entry.getFiletype().name());
        if (entry.getFileDate().getTime() != GarminTimeUtils.GARMIN_TIME_EPOCH * 1000L) {
            sbLegacy1.append("_").append(legacyDateFormat.format(entry.getFileDate()));
        }
        sbLegacy1.append("_").append(entry.getFileIndex()).append(entry.getFiletype().isFitFile() ? ".fit" : ".bin");
        final String legacyName1 = sbLegacy1.toString();
        final Optional<File> legacyFile1 = getFile(legacyName1);
        if (legacyFile1.isPresent()) {
            if (legacyFile1.get().length() == 0) {
                LOG.warn("Legacy file 1 {} is empty", legacyName1);
                return false;
            }
            return true;
        }

        // Legacy filename 2
        final String legacyName2 = entry.getFiletype().name() + "_" +
                entry.getFileIndex() + "_" +
                legacyDateFormat.format(entry.getFileDate()) +
                (entry.getFiletype().isFitFile() ? ".fit" : ".bin");
        final Optional<File> legacyFile2 = getFile(legacyName2);
        if (legacyFile2.isPresent()) {
            if (legacyFile2.get().length() == 0) {
                LOG.warn("Legacy file 2 {} is empty", legacyName2);
                return false;
            }
            return true;
        }

        return false;
    }

    private Optional<File> getFile(final String fileName) {
        File dir;
        try {
            dir = getWritableExportDirectory();
            File outputFile = new File(dir, fileName);
            if (outputFile.exists())
                return Optional.of(outputFile);
        } catch (IOException e) {
            LOG.error("Failed to get file", e);
        }
        return Optional.empty();
    }

    public File getWritableExportDirectory() throws IOException {
        return getDevice().getDeviceCoordinator().getWritableExportDirectory(getDevice(), true);
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        final GdiCore.CoreService.LocationUpdatedNotification.Builder locationUpdatedNotification = GdiCore.CoreService.LocationUpdatedNotification.newBuilder()
                .addLocationData(
                        GarminUtils.toLocationData(location, GdiCore.CoreService.DataType.REALTIME_TRACKING)
                );

        final ProtobufMessage locationUpdatedNotificationRequest = protocolBufferHandler.prepareProtobufRequest(
                GdiSmartProto.Smart.newBuilder().setCoreService(
                        GdiCore.CoreService.newBuilder().setLocationUpdatedNotification(locationUpdatedNotification)
                ).build()
        );
        sendOutgoingMessage("set gps location", locationUpdatedNotificationRequest);
    }

    @Nullable
    public DocumentFile getAgpsFile(final String url) {
        final Prefs prefs = getDevicePrefs();
        final String filename = prefs.getString(GarminPreferences.agpsFilename(url), "");
        if (filename.isEmpty()) {
            LOG.debug("agps file not configured for {}", url);
            return null;
        }

        final String folderUri = prefs.getString(GarminPreferences.PREF_GARMIN_AGPS_FOLDER, "");
        if (folderUri.isEmpty()) {
            LOG.debug("agps folder not set");
            return null;
        }
        final DocumentFile folder = DocumentFile.fromTreeUri(getContext(), Uri.parse(folderUri));
        if (folder == null) {
            LOG.warn("Failed to find agps folder on {}", folderUri);
            return null;
        }

        final DocumentFile localFile = folder.findFile(filename);
        if (localFile == null) {
            LOG.warn("Failed to find agps file '{}' for '{}' on '{}'", filename, url, folderUri);
            return null;
        }
        if (!localFile.isFile()) {
            LOG.warn("Local agps file {} for {} can't be read: isFile={} canRead={}", folderUri, url, localFile.isFile(), localFile.canRead());
            return null;
        }
        return localFile;
    }

    public GarminCoordinator getCoordinator() {
        return (GarminCoordinator) getDevice().getDeviceCoordinator();
    }

    @Override
    public boolean connectFirstTime() {
        mFirstConnect = true;
        return super.connect();
    }

    @Override
    public void onReadConfiguration(final String config) {
        if (config.startsWith("screenId:")) {
            final int screenId = Integer.parseInt(config.replaceFirst("screenId:", ""));

            LOG.debug("Requesting screen {}", screenId);

            final String language = Locale.getDefault().getLanguage();
            final String country = Locale.getDefault().getCountry();
            final String localeString = language + "_" + country.toUpperCase();

            sendOutgoingMessage("get settings screen " + screenId, protocolBufferHandler.prepareProtobufRequest(
                    GdiSmartProto.Smart.newBuilder()
                            .setSettingsService(GdiSettingsService.SettingsService.newBuilder()
                                    .setDefinitionRequest(
                                            GdiSettingsService.ScreenDefinitionRequest.newBuilder()
                                                    .setScreenId(screenId)
                                                    .setUnk2(0)
                                                    .setLanguage(localeString.length() == 5 ? localeString : "en_US")
                                    )
                            ).build()
            ));

            sendOutgoingMessage("get settings state " + screenId, protocolBufferHandler.prepareProtobufRequest(
                    GdiSmartProto.Smart.newBuilder()
                            .setSettingsService(GdiSettingsService.SettingsService.newBuilder()
                                    .setStateRequest(
                                            GdiSettingsService.ScreenStateRequest.newBuilder()
                                                    .setScreenId(screenId)
                                    )
                            ).build()
            ));
        }
    }

    @Override
    public void onInstallApp(Uri uri, @NonNull final Bundle options) {
        final GarminFitFileInstallHandler fitFileInstallHandler = new GarminFitFileInstallHandler(uri, getContext());
        if (fitFileInstallHandler.isValid()) {
            communicator.sendMessage(
                    "upload fit file",
                    fileTransferHandler.initiateUpload(
                            fitFileInstallHandler.getRawBytes(),
                            fitFileInstallHandler.getFileType()
                    ).getOutgoingMessage()
            );
        }

        final GarminGpxRouteInstallHandler garminGpxRouteInstallHandler = new GarminGpxRouteInstallHandler(uri, getContext());
        if (garminGpxRouteInstallHandler.isValid()) {
            final String trackName = options.getString(GarminGpxRouteInstallHandler.EXTRA_TRACK_NAME);
            final GpxRouteFileConverter gpxRouteFileConverter = new GpxRouteFileConverter(
                    garminGpxRouteInstallHandler.getGpxFile(),
                    trackName
            );
            final FitFile convertedFile = gpxRouteFileConverter.getConvertedFile();
            final FileType.FILETYPE fileType = convertedFile.getFileType();
            communicator.sendMessage("upload " + fileType + " file", fileTransferHandler.initiateUpload(convertedFile.getOutgoingMessage(), fileType).getOutgoingMessage());
        }

        final GarminPrgFileInstallHandler prgFileInstallHandler = new GarminPrgFileInstallHandler(uri, getContext());
        if (prgFileInstallHandler.isValid()) {
            communicator.sendMessage(
                    "upload prg file",
                    fileTransferHandler.initiateUpload(
                            prgFileInstallHandler.getRawBytes(),
                            FileType.FILETYPE.PRG
                    ).getOutgoingMessage()
            );
        }
    }

    @Override
    public void onHeartRateTest() {
        communicator.onHeartRateTest();
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        communicator.onEnableRealtimeHeartRateMeasurement(enable);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        communicator.onEnableRealtimeSteps(enable);
    }

    public void downloadFileFromServiceV2(final int fileHandle) {
        LOG.info("Requesting file service V2 handle={}", fileHandle);
        if (!(communicator instanceof CommunicatorV2 communicatorV2)) {
            LOG.error("Communicator is not V2");
            return;
        }
        communicatorV2.startTransfer(new CommunicatorV2.ServiceCallback() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            private boolean started = false;
            private CommunicatorV2.ServiceWriter writer;

            @Override
            public void onConnect(final CommunicatorV2.ServiceWriter writer) {
                this.writer = writer;

                final ByteBuffer buf = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
                buf.put((byte) 0x00);
                buf.put((byte) 0x00);
                buf.putShort((short) fileHandle);
                buf.put((byte) 0x00);
                buf.put((byte) 0x00);
                writer.write("request file", buf.array());
            }

            @Override
            public void onClose() {
                if (baos.size() == 0) {
                    LOG.warn("File transfer closed with 0 bytes in the buffer");
                    return;
                }

                LOG.debug("Attempting to inflate {} bytes", baos.size());

                final byte[] inflated = CompressionUtils.INSTANCE.inflate(baos.toByteArray());
                if (inflated == null) {
                    if (currentlyDownloading != null && currentlyDownloading.getSyncFile() != null) {
                        currentlyDownloading = null;
                    }
                    return;
                }
                LOG.debug("Inflated to {} bytes", inflated.length);

                final File file;
                try {
                    final File cacheDir = getContext().getExternalCacheDir();
                    final File inflateDir = new File(cacheDir, "garmin-inflated");
                    //noinspection ResultOfMethodCallIgnored
                    inflateDir.mkdirs();
                    file = File.createTempFile("activity-files-import", ".fit", inflateDir);
                    file.deleteOnExit();
                    FileUtils.copyStreamToFile(new ByteArrayInputStream(inflated), file);
                } catch (final IOException e) {
                    LOG.error("Failed to create temp file for activity file", e);
                    if (currentlyDownloading != null && currentlyDownloading.getSyncFile() != null) {
                        currentlyDownloading = null;
                    }
                    return;
                }

                LOG.debug("Dumped inflated bytes to {}", file.getAbsolutePath());

                try {
                    final FitImporter fitImporter = new FitImporter(getContext(), gbDevice);
                    fitImporter.importFile(file);
                } catch (final Exception e) {
                    LOG.error("Failed to parse file as fit", e);
                    if (currentlyDownloading != null && currentlyDownloading.getSyncFile() != null) {
                        currentlyDownloading = null;
                    }
                    return;
                }

                if (!getKeepActivityDataOnDevice()) { // delete file from watch upon successful download
                    final GdiFileSyncService.FileSyncService syncedCommand = protocolBufferHandler.getFileSyncServiceHandler()
                            .markSynced(currentlyDownloading.getSyncFile());
                    if (syncedCommand != null) {
                        sendOutgoingMessage(
                                "mark file as synced",
                                protocolBufferHandler.prepareProtobufRequest(
                                        GdiSmartProto.Smart.newBuilder().setFileSyncService(syncedCommand).build()
                                )
                        );
                    }
                }

                LOG.debug("New file sync success");
                if (currentlyDownloading != null && currentlyDownloading.getSyncFile() != null) {
                    transferNotification.incrementTotalProgress(currentlyDownloading.getSyncFile().getSize());
                    currentlyDownloading = null;
                }
            }

            @Override
            public void onMessage(final byte[] value) {
                if (!started) {
                    if (!ArrayUtils.equals(new byte[]{0,0,0}, value, 0)) {
                        LOG.error("Got unexpected first message");
                        if (currentlyDownloading != null && currentlyDownloading.getSyncFile() != null) {
                            transferNotification.incrementTotalProgress(currentlyDownloading.getSyncFile().getSize());
                            currentlyDownloading = null;
                        }
                        return;
                    }
                    started = true;
                    return;
                }
                LOG.debug("Buffering {} bytes", value.length);
                baos.write(value, 0, value.length);
                transferNotification.setChunkProgress(baos.size());
            }
        });
    }

    @Override
    public void onTestNewFunction() {
        parseAllFitFilesFromStorage();
    }

    boolean parsingFitFilesFromStorage = false;

    private void parseAllFitFilesFromStorage() {
        if (parsingFitFilesFromStorage) {
            GB.toast(getContext(), "Already parsing!", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        parsingFitFilesFromStorage = true;

        LOG.info("Parsing all fit files from storage");

        final List<File> fitFiles;
        try {
            final File exportDir = getWritableExportDirectory();

            if (!exportDir.exists() || !exportDir.isDirectory()) {
                LOG.error("export directory {} not found", exportDir);
                GB.toast(getContext(), "export directory " + exportDir + " not found", Toast.LENGTH_LONG, GB.ERROR);
                return;
            }

            fitFiles = FileUtils.listRecursive(exportDir, (dir, name) -> name.endsWith(".fit"));
            if (fitFiles.isEmpty()) {
                LOG.error("No fit files found in {}", exportDir);
                GB.toast(getContext(), "No fit files found in " + exportDir, Toast.LENGTH_LONG, GB.ERROR);
                return;
            }
        } catch (final Exception e) {
            LOG.error("Failed to parse from storage", e);
            GB.toast(getContext(), "Failed to parse from storage", Toast.LENGTH_LONG, GB.ERROR, e);
            return;
        }

        LOG.debug("Got {} fit files to parse", fitFiles.size());

        GB.toast(getContext(), "Check notification for progress", Toast.LENGTH_LONG, GB.INFO);

        transferNotification.start(R.string.busy_task_processing_files, 0, fitFiles.size());

        //try (DBHandler handler = GBApplication.acquireDB()) {
        //    final DaoSession session = handler.getDaoSession();
        //    final Device device = DBHelper.getDevice(gbDevice, session);
        //    //getCoordinator().deleteAllActivityData(device, session);
        //} catch (final Exception e) {
        //    GB.toast(getContext(), "Error deleting activity data", Toast.LENGTH_LONG, GB.ERROR, e);
        //}

        final FitAsyncProcessor fitAsyncProcessor = new FitAsyncProcessor(getContext(), getDevice());
        fitAsyncProcessor.process(fitFiles, new FitAsyncProcessor.Callback() {
            @Override
            public void onProgress(final int i) {
                transferNotification.setTotalProgress(i);
            }

            @Override
            public void onFinish() {
                parsingFitFilesFromStorage = false;
                transferNotification.finish();
                GB.signalActivityDataFinish(getDevice());
            }
        });
    }

}
