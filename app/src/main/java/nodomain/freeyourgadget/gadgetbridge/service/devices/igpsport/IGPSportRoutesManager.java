package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport;

import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportRouteInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Common;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.GeneralFileOperation;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.RoutePlan;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class IGPSportRoutesManager {

    Logger LOG = LoggerFactory.getLogger(IGPSportRoutesManager.class);
    private IGPSportDeviceSupport support = null;
    private HashMap<UUID, RouteInfo> installedRouteHash = new HashMap<>();

    public class RouteInfo {
        private int id=0;
        private String name="";
        private int type=0;
        private UUID uuid;
        public RouteInfo(RoutePlan.route_plan_info_message message, UUID uuid) {
            id = message.getId();
            name = message.getName();
            type = message.getFileType().getNumber();
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public String getExtension() {
            switch (type) {
                case RoutePlan.ROUTE_PLAN_FILE_TYPE.enum_ROUTE_PLAN_FILE_TYPE_CNX_VALUE:
                    return "cnx";
                case RoutePlan.ROUTE_PLAN_FILE_TYPE.enum_ROUTE_PLAN_FILE_TYPE_GPX_VALUE:
                    return "gpx";
                case RoutePlan.ROUTE_PLAN_FILE_TYPE.enum_ROUTE_PLAN_FILE_TYPE_FIT_VALUE:
                    return "fit";
                case RoutePlan.ROUTE_PLAN_FILE_TYPE.enum_ROUTE_PLAN_FILE_TYPE_TCX_VALUE:
                    return "tcx";
                case RoutePlan.ROUTE_PLAN_FILE_TYPE.enum_ROUTE_PLAN_FILE_TYPE_XML_VALUE:
                    return "xml";
                default:
                    return "";
            }
        }

        public RoutePlan.route_plan_info_message.Builder toMessage() {
            RoutePlan.route_plan_info_message.Builder  message = RoutePlan.route_plan_info_message.newBuilder();
            message.setFileType(RoutePlan.ROUTE_PLAN_FILE_TYPE.forNumber(type));
            message.setId(id);
            message.setName(name);
            return message;
        }
    }


    public IGPSportRoutesManager(IGPSportDeviceSupport support) {

        this.support = support;

    }

    public void uploadRoute(IGPSportRouteInstallHandler handler) {
        try {
            TransactionBuilder builder = support.performInitialized("prepare upload gpx");

            Random random = new Random();
            int ran = random.nextInt() & Integer.MAX_VALUE;
            GeneralFileOperation.general_file_operation.Builder fileOperationbuilder = GeneralFileOperation.general_file_operation.newBuilder();
            fileOperationbuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_FILE_OPERATION)
                    .setOperateType(Common.SERVICE_OPERATE_TYPE.enum_SERVICE_OPERATE_TYPE_ADD)
                    .setFileType(GeneralFileOperation.file_operation_type.enum_FILE_TYPE_ROUTE_PLAN)
                    .setFileId(ran)
                    .setFileExtension(handler.getExtension())
                    .setFileName(handler.getFilename())
                    .setFileSize(handler.getSize());

            byte[] fileOperationBytes = support.craftFileData(fileOperationbuilder.getServiceType().getNumber(),
                    0xff,
                    fileOperationbuilder.getOperateType().getNumber(),
                    fileOperationbuilder.build().toByteArray(),
                    handler.getBytes());
            builder.writeChunkedData(support.writeCharacteristicFourth, fileOperationBytes, support.getMTU());
            support.getDevice().setBusyTask("Installing route");
            support.getDevice().sendDeviceUpdateIntent(support.getContext());
            builder.queue(support.getQueue());

        } catch (final Exception e) {
            GB.toast(support.getContext(), "Gpx install error: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }

    }

    public void requestRouteList() {
        try {
            TransactionBuilder builder = support.performInitialized("get gpx routes");

            RoutePlan.route_plan_data_msg.Builder routePlanBuilder = RoutePlan.route_plan_data_msg.newBuilder();
            routePlanBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_ROUTE_PLAN);
            routePlanBuilder.setRoutePlanOperateType(RoutePlan.ROUTE_PLAN_OPERATE_TYPE.enum_ROUTE_PLAN_OPERATE_TYPE_LIST_NUM_GET);
            byte[] routePlanBytes = support.craftData(routePlanBuilder.getServiceType().getNumber(), 0xff, routePlanBuilder.getRoutePlanOperateType().getNumber(), routePlanBuilder.build().toByteArray());
            builder.write(support.writeCharacteristicFourth, routePlanBytes);

            builder.queue(support.getQueue());

        } catch (IOException e) {
            GB.toast(support.getContext(), "Gpx get list error: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }

    }

    public static UUID toRouteUUID(final String id) {
        // Watchface IDs are numbers as strings - pad them to the right with F
        // and encode as UUID
        final String padded = String.format("%-32s", id).replace(' ', 'F');
        return UUID.fromString(
                padded.substring(0, 8) + "-" +
                        padded.substring(8, 12) + "-" +
                        padded.substring(12, 16) + "-" +
                        padded.substring(16, 20) + "-" +
                        padded.substring(20, 32)
        );
    }

    public static String toRouteId(final UUID uuid) {
        return uuid.toString()
                .replaceAll("-", "")
                .replaceAll("f", "")
                .replaceAll("F", "");
    }

    public void handleRouteList(byte[] pbData) throws InvalidProtocolBufferException {
        installedRouteHash.clear();
        RoutePlan.route_plan_data_msg routeplatMsg = RoutePlan.route_plan_data_msg.parseFrom(pbData);

        List<RoutePlan.route_plan_info_message> routeList = routeplatMsg.getRoutePlanInfoMsgList();
        final List<GBDeviceApp> gbDeviceApps = new ArrayList<>();

        for (final RoutePlan.route_plan_info_message routeMsg : routeList) {

            final UUID uuid = toRouteUUID(String.valueOf(routeMsg.getId()));

            installedRouteHash.put(uuid, new RouteInfo(routeMsg, uuid));
            GBDeviceApp gbDeviceApp = new GBDeviceApp(
                    uuid,
                    routeMsg.getName(),
                    "",
                    "",
                    GBDeviceApp.Type.APP_GENERIC
            );
            gbDeviceApps.add(gbDeviceApp);
        }

        final GBDeviceEventAppInfo appInfoCmd = new GBDeviceEventAppInfo();
        appInfoCmd.apps = gbDeviceApps.toArray(new GBDeviceApp[0]);
        support.evaluateGBDeviceEvent(appInfoCmd);

    }

    public void handleRouteNumber(byte[] pbData) throws InvalidProtocolBufferException {
        RoutePlan.route_plan_data_msg routeplanMsg = RoutePlan.route_plan_data_msg.parseFrom(pbData);
        int fileNumber = 0;
        if (routeplanMsg.getRouteListGetMsg().getFileNum() > 0)
            fileNumber = routeplanMsg.getRouteListGetMsg().getFileNum();

        TransactionBuilder builder = new TransactionBuilder("get files list");
        RoutePlan.route_plan_data_msg.Builder routePlan2ndBuilder = RoutePlan.route_plan_data_msg.newBuilder();
        routePlan2ndBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_ROUTE_PLAN);
        routePlan2ndBuilder.setRoutePlanOperateType(RoutePlan.ROUTE_PLAN_OPERATE_TYPE.enum_ROUTE_PLAN_OPERATE_TYPE_LIST_GET);
        routePlan2ndBuilder.setRouteListGetMsg(Common.file_list_get_message.newBuilder().setFileIndexEnd(0).setFileIndexEnd(fileNumber));
        byte[] routePlan2ndBytes = support.craftData(routePlan2ndBuilder.getServiceType().getNumber(), 0xff, routePlan2ndBuilder.getRoutePlanOperateType().getNumber(), routePlan2ndBuilder.build().toByteArray());
        builder.write(support.writeCharacteristicFourth, routePlan2ndBytes);
        builder.queue(support.getQueue());

    }

    public void activateRoute(final UUID uuid) {
        try {
            TransactionBuilder builder = support.performInitialized("start gpx route");
            RoutePlan.route_plan_data_msg.Builder routePlanBuilder = RoutePlan.route_plan_data_msg.newBuilder();
            routePlanBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_ROUTE_PLAN);
            routePlanBuilder.setRoutePlanOperateType(RoutePlan.ROUTE_PLAN_OPERATE_TYPE.enum_ROUTE_PLAN_OPERATE_TYPE_FILE_USE);
            routePlanBuilder.addRoutePlanInfoMsg(installedRouteHash.get(uuid).toMessage());
            byte[] routePlanBytes = support.craftData(routePlanBuilder.getServiceType().getNumber(), 0xff, routePlanBuilder.getRoutePlanOperateType().getNumber(), routePlanBuilder.build().toByteArray());
            builder.write(support.writeCharacteristicFourth, routePlanBytes);
            builder.queue(support.getQueue());
        } catch (IOException e) {
            GB.toast(support.getContext(), "Failed to start gpx navigation: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    public void deleteRoute(final UUID uuid) {
        try {
            TransactionBuilder builder = support.performInitialized("delete gpx route");
            RoutePlan.route_plan_data_msg.Builder routePlanBuilder = RoutePlan.route_plan_data_msg.newBuilder();
            routePlanBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_ROUTE_PLAN);
            routePlanBuilder.setRoutePlanOperateType(RoutePlan.ROUTE_PLAN_OPERATE_TYPE.enum_ROUTE_PLAN_OPERATE_TYPE_FILE_DEL);
            routePlanBuilder.addRoutePlanInfoMsg(installedRouteHash.get(uuid).toMessage());
            byte[] routePlanBytes = support.craftData(routePlanBuilder.getServiceType().getNumber(), 0xff, routePlanBuilder.getRoutePlanOperateType().getNumber(), routePlanBuilder.build().toByteArray());
            builder.write(support.writeCharacteristicFourth, routePlanBytes);
            builder.queue(support.getQueue());
        } catch (IOException e) {
            GB.toast(support.getContext(), "Failed to delete gpx navigation: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

}
