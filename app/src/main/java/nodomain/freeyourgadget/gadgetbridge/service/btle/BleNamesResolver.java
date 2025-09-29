/*  Copyright (C) 2016-2025 Carsten Pfeiffer, João Paulo Barraca, JohnnySun, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

public class BleNamesResolver {
    private static final Map<String, String> mServices = new HashMap<>(100);
    private static final Map<String, String> mCharacteristics = new HashMap<>(600);
    private static final SparseArray<String> mValueFormats = new SparseArray<>(10);
    private static final SparseArray<String> mAppearance = new SparseArray<>(20);
    private static final SparseArray<String> mHeartRateSensorLocation = new SparseArray<>(10);

    static public String resolveServiceName(final String uuid) {
        String result = mServices.get(uuid);
        if (result == null) result = "Unknown Service";
        return result;
    }

    static public String resolveValueTypeDescription(final int format) {
        return mValueFormats.get(format, "Unknown Format");
    }

    static public String resolveCharacteristicName(final String uuid) {
        String result = mCharacteristics.get(uuid);
        if (result == null) result = "Unknown Characteristic";
        return result;
    }

    static public String resolveUuid(final String uuid) {
        String result = mServices.get(uuid);
        if (result != null) return "Service: " + result;

        result = mCharacteristics.get(uuid);
        if (result != null) return "Characteristic: " + result;

        result = "Unknown UUID";
        return result;
    }

    static public String resolveAppearance(int key) {
        return mAppearance.get(key, "Unknown Appearance");
    }

    static public String resolveHeartRateSensorLocation(int key) {
        return mHeartRateSensorLocation.get(key, "Other");
    }

    static public boolean isService(final String uuid) {
        return mServices.containsKey(uuid);
    }

    static public boolean isCharacteristic(final String uuid) {
        return mCharacteristics.containsKey(uuid);
    }

    /// lookup description for numeric BluetoothProfile.STATE_...
    static public String getStateString(int state) {
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return "STATE_DISCONNECTED";
            case BluetoothProfile.STATE_CONNECTING:
                return "STATE_CONNECTING";
            case BluetoothProfile.STATE_CONNECTED:
                return "STATE_CONNECTED";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "STATE_DISCONNECTING";
            default:
                return "state_" + state;
        }
    }

    /// lookup description for numeric BluetoothDevice.BOND_...
    static public String getBondStateString(int state) {
        switch (state) {
            case BluetoothDevice.BOND_NONE:
                return "BOND_NONE";
            case BluetoothDevice.BOND_BONDED:
                return "BOND_BONDED";
            case BluetoothDevice.BOND_BONDING:
                return "BOND_BONDING";
            default:
                return "bond_" + state;
        }
    }

    /// lookup description for numeric status used by {@link android.bluetooth.BluetoothGattCallback}
    static public String getStatusString(int status) {
        // android.bluetooth.BluetoothGatt's numbers are incomplete
        // numeric values taken from GattStatus in https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/main/system/stack/include/gatt_api.h
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "GATT_SUCCESS";
            case 0x01:
                // GATT_CONN_L2C_FAILURE is also 0x01
                return "GATT_INVALID_HANDLE";
            case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                return "GATT_READ_NOT_PERMITTED";
            case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                return "GATT_WRITE_NOT_PERMITTED";
            case 0x04:
                return "GATT_INVALID_PDU";
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                return "GATT_INSUFFICIENT_AUTHENTICATION";
            case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                return "GATT_REQUEST_NOT_SUPPORTED";
            case BluetoothGatt.GATT_INVALID_OFFSET:
                return "GATT_INVALID_OFFSET";
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION:
                // GATT_CONN_TIMEOUT is also 0x08
                return "GATT_INSUFFICIENT_AUTHORIZATION";
            case 0x09:
                return "GATT_PREPARE_Q_FULL";
            case 0x0a:
                return "GATT_NOT_FOUND";
            case 0x0b:
                return "GATT_NOT_LONG";
            case 0x0c:
                return "GATT_INSUF_KEY_SIZE";
            case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                return "GATT_INVALID_ATTRIBUTE_LENGTH";
            case 0x0e:
                return "GATT_ERR_UNLIKELY";
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                return "GATT_INSUFFICIENT_ENCRYPTION";
            case 0x10:
                return "GATT_UNSUPPORT_GRP_TYPE";
            case 0x11:
                return "GATT_INSUF_RESOURCE";
            case 0x12:
                return "GATT_DATABASE_OUT_OF_SYNC";
            case 0x13:
                // GATT_CONN_TERMINATE_PEER_USER is also 0x13
                return "GATT_VALUE_NOT_ALLOWED";
            case 0x15:
                return "GATT_CONN_TERMINATED_POWER_OFF";
            case 0x16:
                return "GATT_CONN_TERMINATE_LOCAL_HOST";
            case 0x22:
                return "GATT_CONN_LMP_TIMEOUT";
            case 0x3e:
                return "GATT_CONN_FAILED_ESTABLISHMENT";
            case 0x87:
                return "GATT_ILLEGAL_PARAMETER";
            case 0x7f:
                return "GATT_TOO_SHORT";
            case 0x80:
                return "GATT_NO_RESOURCES";
            case 0x81:
                return "GATT_INTERNAL_ERROR";
            case 0x82:
                return "GATT_WRONG_STATE";
            case 0x83:
                return "GATT_DB_FULL";
            case 0x84:
                return "GATT_BUSY";
            case 0x85:
                return "GATT_ERROR";
            case 0x86:
                return "GATT_CMD_STARTED";
            case 0x88:
                return "GATT_PENDING";
            case 0x89:
                return "GATT_AUTH_FAIL";
            case 0x8a:
                return "GATT_MORE";
            case 0x8b:
                return "GATT_INVALID_CFG";
            case 0x8c:
                return "GATT_SERVICE_STARTED";
            case 0x8d:
                return "GATT_ENCRYPED_NO_MITM";
            case 0x8e:
                return "GATT_NOT_ENCRYPTED";
            case BluetoothGatt.GATT_CONNECTION_CONGESTED:
                return "GATT_CONNECTION_CONGESTED";
            case 0x90:
                return "GATT_DUP_REG";
            case 0x91:
                return "GATT_ALREADY_OPEN";
            case 0x92:
                return "GATT_CANCEL";
            case BluetoothGatt.GATT_CONNECTION_TIMEOUT:
                return "GATT_CONNECTION_TIMEOUT";
            case 0xFD:
                return "GATT_CCC_CFG_ERR";
            case 0xFE:
                return "GATT_PRC_IN_PROGRESS";
            case 0xFF:
                return "GATT_OUT_OF_RANGE";
            case BluetoothGatt.GATT_FAILURE:
                return "GATT_FAILURE";
            default:
                return "failed_" + status;
        }
    }

    /// lookup description for numeric {@link android.bluetooth.BluetoothStatusCodes}
    public static String getBluetoothStatusString(int status) {
        // many of the values in BluetoothStatusCodes aren't accessible due to @SystemApi
        switch (status) {
            case 0:
                return "SUCCESS";
            case 1:
                return "ERROR_BLUETOOTH_NOT_ENABLED";
            case 2:
                return "ERROR_BLUETOOTH_NOT_ALLOWED";
            case 3:
                return "ERROR_DEVICE_NOT_BONDED";
            case 4:
                return "ERROR_DEVICE_NOT_CONNECTED";
            case 6:
                return "ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION";
            case 7:
                return "ERROR_MISSING_BLUETOOTH_SCAN_PERMISSION";
            case 9:
                return "ERROR_PROFILE_SERVICE_NOT_BOUND";
            case 10:
                return "FEATURE_SUPPORTED";
            case 11:
                return "FEATURE_NOT_SUPPORTED";
            case 12:
                return "ERROR_NOT_ACTIVE_DEVICE";
            case 13:
                return "ERROR_NO_ACTIVE_DEVICES";
            case 14:
                return "ERROR_PROFILE_NOT_CONNECTED";
            case 15:
                return "ERROR_TIMEOUT";
            case 16:
                return "REASON_LOCAL_APP_REQUEST";
            case 17:
                return "REASON_LOCAL_STACK_REQUEST";
            case 18:
                return "REASON_REMOTE_REQUEST";
            case 19:
                return "REASON_SYSTEM_POLICY";
            case 20:
                return "ERROR_HARDWARE_GENERIC";
            case 21:
                return "ERROR_BAD_PARAMETERS";
            case 22:
                return "ERROR_LOCAL_NOT_ENOUGH_RESOURCES";
            case 23:
                return "ERROR_REMOTE_NOT_ENOUGH_RESOURCES";
            case 24:
                return "ERROR_REMOTE_OPERATION_REJECTED";
            case 25:
                return "ERROR_REMOTE_LINK_ERROR";
            case 26:
                return "ERROR_ALREADY_IN_TARGET_STATE";
            case 27:
                return "ERROR_REMOTE_OPERATION_NOT_SUPPORTED";
            case 28:
                return "ERROR_CALLBACK_NOT_REGISTERED";
            case 29:
                return "ERROR_ANOTHER_ACTIVE_REQUEST";
            case 30:
                return "FEATURE_NOT_CONFIGURED";
            case 200:
                return "ERROR_GATT_WRITE_NOT_ALLOWED";
            case 201:
                return "ERROR_GATT_WRITE_REQUEST_BUSY";
            case 400:
                return "ALLOWED";
            case 401:
                return "NOT_ALLOWED";
            case 1000:
                return "ERROR_ANOTHER_ACTIVE_OOB_REQUEST";
            case 1100:
                return "ERROR_DISCONNECT_REASON_LOCAL_REQUEST";
            case 1101:
                return "ERROR_DISCONNECT_REASON_REMOTE_REQUEST";
            case 1102:
                return "ERROR_DISCONNECT_REASON_LOCAL";
            case 1103:
                return "ERROR_DISCONNECT_REASON_REMOTE";
            case 1104:
                return "ERROR_DISCONNECT_REASON_TIMEOUT";
            case 1105:
                return "ERROR_DISCONNECT_REASON_SECURITY";
            case 1106:
                return "ERROR_DISCONNECT_REASON_SYSTEM_POLICY";
            case 1107:
                return "ERROR_DISCONNECT_REASON_RESOURCE_LIMIT_REACHED";
            case 1108:
                return "ERROR_DISCONNECT_REASON_CONNECTION_ALREADY_EXISTS";
            case 1109:
                return "ERROR_DISCONNECT_REASON_BAD_PARAMETERS";
            case 1116:
                return "ERROR_AUDIO_DEVICE_ALREADY_CONNECTED";
            case 1117:
                return "ERROR_AUDIO_DEVICE_ALREADY_DISCONNECTED";
            case 1118:
                return "ERROR_AUDIO_ROUTE_BLOCKED";
            case 1119:
                return "ERROR_CALL_ACTIVE";
            case 1200:
                return "ERROR_LE_BROADCAST_INVALID_BROADCAST_ID";
            case 1201:
                return "ERROR_LE_BROADCAST_INVALID_CODE";
            case 1202:
                return "ERROR_LE_BROADCAST_ASSISTANT_INVALID_SOURCE_ID";
            case 1203:
                return "ERROR_LE_BROADCAST_ASSISTANT_DUPLICATE_ADDITION";
            case 1204:
                return "ERROR_LE_CONTENT_METADATA_INVALID_PROGRAM_INFO";
            case 1205:
                return "ERROR_LE_CONTENT_METADATA_INVALID_LANGUAGE";
            case 1206:
                return "ERROR_LE_CONTENT_METADATA_INVALID_OTHER";
            case 1207:
                return "ERROR_CSIP_INVALID_GROUP_ID";
            case 1208:
                return "ERROR_CSIP_GROUP_LOCKED_BY_OTHER";
            case 1209:
                return "ERROR_CSIP_LOCKED_GROUP_MEMBER_LOST";
            case 1210:
                return "ERROR_HAP_PRESET_NAME_TOO_LONG";
            case 1211:
                return "ERROR_HAP_INVALID_PRESET_INDEX";
            case 1300:
                return "ERROR_NO_LE_CONNECTION";
            case 1301:
                return "ERROR_DISTANCE_MEASUREMENT_INTERNAL";
            case 2000:
                return "RFCOMM_LISTENER_START_FAILED_UUID_IN_USE";
            case 2001:
                return "RFCOMM_LISTENER_OPERATION_FAILED_NO_MATCHING_SERVICE_RECORD";
            case 2002:
                return "RFCOMM_LISTENER_OPERATION_FAILED_DIFFERENT_APP";
            case 2003:
                return "RFCOMM_LISTENER_FAILED_TO_CREATE_SERVER_SOCKET";
            case 2004:
                return "RFCOMM_LISTENER_FAILED_TO_CLOSE_SERVER_SOCKET";
            case 2005:
                return "RFCOMM_LISTENER_NO_SOCKET_AVAILABLE";
            case 3000:
                return "ERROR_NOT_DUAL_MODE_AUDIO_DEVICE";
            default:
                return "status_" + status;
        }
    }

    /// lookup description for numeric {@link BluetoothGatt#requestConnectionPriority} priorities
    public static String getConnectionPriorityString(int priority){
        return switch (priority) {
            case BluetoothGatt.CONNECTION_PRIORITY_BALANCED -> "CONNECTION_PRIORITY_BALANCED";
            case BluetoothGatt.CONNECTION_PRIORITY_HIGH -> "CONNECTION_PRIORITY_HIGH";
            case BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER -> "CONNECTION_PRIORITY_LOW_POWER";
            case BluetoothGatt.CONNECTION_PRIORITY_DCK -> "CONNECTION_PRIORITY_DCK";
            default -> "priority_" + priority;
        };
    }

    /// lookup description for numeric bond loss reasons (introduced in API 36.1)
    public static String getBondLossReasonString(int reason){
        return switch (reason) {
            case 0 -> "BOND_LOSS_REASON_UNKNOWN";
            case 1 -> "BOND_LOSS_REASON_BREDR_AUTH_FAILURE";
            case 2 -> "BOND_LOSS_REASON_BREDR_INCOMING_PAIRING";
            case 3 -> "BOND_LOSS_REASON_LE_ENCRYPT_FAILURE";
            case 4 -> "BOND_LOSS_REASON_LE_INCOMING_PAIRING";
            default -> "reason_" + reason;
        };
    }

    static public String getCharacteristicPropertyString(int property) {
        StringBuilder builder = new StringBuilder();
        if (BluetoothGattCharacteristic.PROPERTY_BROADCAST == (BluetoothGattCharacteristic.PROPERTY_BROADCAST & property)) {
            builder.append("broadcast,");
        }
        if (BluetoothGattCharacteristic.PROPERTY_READ == (BluetoothGattCharacteristic.PROPERTY_READ & property)) {
            builder.append("read,");
        }
        if (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE == (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE & property)) {
            builder.append("writeNoResponse,");
        }
        if (BluetoothGattCharacteristic.PROPERTY_WRITE == (BluetoothGattCharacteristic.PROPERTY_WRITE & property)) {
            builder.append("write,");
        }
        if (BluetoothGattCharacteristic.PROPERTY_NOTIFY == (BluetoothGattCharacteristic.PROPERTY_NOTIFY & property)) {
            builder.append("notify,");
        }
        if (BluetoothGattCharacteristic.PROPERTY_INDICATE == (BluetoothGattCharacteristic.PROPERTY_INDICATE & property)) {
            builder.append("indicate,");
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    static public String getAdapterStateString(int state) {
        return switch (state) {
            case BluetoothAdapter.STATE_OFF -> "STATE_OFF";
            case BluetoothAdapter.STATE_TURNING_ON -> "STATE_TURNING_ON";
            case BluetoothAdapter.STATE_ON -> "STATE_ON";
            case BluetoothAdapter.STATE_TURNING_OFF -> "STATE_TURNING_OFF";
            default -> "state_" + state;
        };
    }

    static {
        // source: https://bitbucket.org/bluetooth-SIG/public/src/main/assigned_numbers/uuids/service_uuids.yaml
        mServices.put("00001801-0000-1000-8000-00805f9b34fb", "GATT");
        mServices.put("00001800-0000-1000-8000-00805f9b34fb", "GAP");
        mServices.put("00001802-0000-1000-8000-00805f9b34fb", "Immediate Alert");
        mServices.put("00001803-0000-1000-8000-00805f9b34fb", "Link Loss");
        mServices.put("00001804-0000-1000-8000-00805f9b34fb", "Tx Power");
        mServices.put("00001805-0000-1000-8000-00805f9b34fb", "Current Time");
        mServices.put("00001806-0000-1000-8000-00805f9b34fb", "Reference Time Update");
        mServices.put("00001807-0000-1000-8000-00805f9b34fb", "Next DST Change");
        mServices.put("00001808-0000-1000-8000-00805f9b34fb", "Glucose");
        mServices.put("00001809-0000-1000-8000-00805f9b34fb", "Health Thermometer");
        mServices.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information");
        mServices.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate");
        mServices.put("0000180e-0000-1000-8000-00805f9b34fb", "Phone Alert Status");
        mServices.put("0000180f-0000-1000-8000-00805f9b34fb", "Battery");
        mServices.put("00001810-0000-1000-8000-00805f9b34fb", "Blood Pressure");
        mServices.put("00001811-0000-1000-8000-00805f9b34fb", "Alert Notification");
        mServices.put("00001812-0000-1000-8000-00805f9b34fb", "Human Interface Device");
        mServices.put("00001813-0000-1000-8000-00805f9b34fb", "Scan Parameters");
        mServices.put("00001814-0000-1000-8000-00805f9b34fb", "Running Speed and Cadence");
        mServices.put("00001815-0000-1000-8000-00805f9b34fb", "Automation IO");
        mServices.put("00001816-0000-1000-8000-00805f9b34fb", "Cycling Speed and Cadence");
        mServices.put("00001818-0000-1000-8000-00805f9b34fb", "Cycling Power");
        mServices.put("00001819-0000-1000-8000-00805f9b34fb", "Location and Navigation");
        mServices.put("0000181a-0000-1000-8000-00805f9b34fb", "Environmental Sensing");
        mServices.put("0000181b-0000-1000-8000-00805f9b34fb", "Body Composition");
        mServices.put("0000181c-0000-1000-8000-00805f9b34fb", "User Data");
        mServices.put("0000181d-0000-1000-8000-00805f9b34fb", "Weight Scale");
        mServices.put("0000181e-0000-1000-8000-00805f9b34fb", "Bond Management");
        mServices.put("0000181f-0000-1000-8000-00805f9b34fb", "Continuous Glucose Monitoring");
        mServices.put("00001820-0000-1000-8000-00805f9b34fb", "Internet Protocol Support");
        mServices.put("00001821-0000-1000-8000-00805f9b34fb", "Indoor Positioning");
        mServices.put("00001822-0000-1000-8000-00805f9b34fb", "Pulse Oximeter");
        mServices.put("00001823-0000-1000-8000-00805f9b34fb", "HTTP Proxy");
        mServices.put("00001824-0000-1000-8000-00805f9b34fb", "Transport Discovery");
        mServices.put("00001825-0000-1000-8000-00805f9b34fb", "Object Transfer");
        mServices.put("00001826-0000-1000-8000-00805f9b34fb", "Fitness Machine");
        mServices.put("00001827-0000-1000-8000-00805f9b34fb", "Mesh Provisioning");
        mServices.put("00001828-0000-1000-8000-00805f9b34fb", "Mesh Proxy");
        mServices.put("00001829-0000-1000-8000-00805f9b34fb", "Reconnection Configuration");
        mServices.put("0000183a-0000-1000-8000-00805f9b34fb", "Insulin Delivery");
        mServices.put("0000183b-0000-1000-8000-00805f9b34fb", "Binary Sensor");
        mServices.put("0000183c-0000-1000-8000-00805f9b34fb", "Emergency Configuration");
        mServices.put("0000183d-0000-1000-8000-00805f9b34fb", "Authorization Control");
        mServices.put("0000183e-0000-1000-8000-00805f9b34fb", "Physical Activity Monitor");
        mServices.put("0000183f-0000-1000-8000-00805f9b34fb", "Elapsed Time");
        mServices.put("00001840-0000-1000-8000-00805f9b34fb", "Generic Health Sensor");
        mServices.put("00001843-0000-1000-8000-00805f9b34fb", "Audio Input Control");
        mServices.put("00001844-0000-1000-8000-00805f9b34fb", "Volume Control");
        mServices.put("00001845-0000-1000-8000-00805f9b34fb", "Volume Offset Control");
        mServices.put("00001846-0000-1000-8000-00805f9b34fb", "Coordinated Set Identification");
        mServices.put("00001847-0000-1000-8000-00805f9b34fb", "Device Time");
        mServices.put("00001848-0000-1000-8000-00805f9b34fb", "Media Control");
        mServices.put("00001849-0000-1000-8000-00805f9b34fb", "Generic Media Control");
        mServices.put("0000184a-0000-1000-8000-00805f9b34fb", "Constant Tone Extension");
        mServices.put("0000184b-0000-1000-8000-00805f9b34fb", "Telephone Bearer");
        mServices.put("0000184c-0000-1000-8000-00805f9b34fb", "Generic Telephone Bearer");
        mServices.put("0000184d-0000-1000-8000-00805f9b34fb", "Microphone Control");
        mServices.put("0000184e-0000-1000-8000-00805f9b34fb", "Audio Stream Control");
        mServices.put("0000184f-0000-1000-8000-00805f9b34fb", "Broadcast Audio Scan");
        mServices.put("00001850-0000-1000-8000-00805f9b34fb", "Published Audio Capabilities");
        mServices.put("00001851-0000-1000-8000-00805f9b34fb", "Basic Audio Announcement");
        mServices.put("00001852-0000-1000-8000-00805f9b34fb", "Broadcast Audio Announcement");
        mServices.put("00001853-0000-1000-8000-00805f9b34fb", "Common Audio");
        mServices.put("00001854-0000-1000-8000-00805f9b34fb", "Hearing Access");
        mServices.put("00001855-0000-1000-8000-00805f9b34fb", "Telephony and Media Audio");
        mServices.put("00001856-0000-1000-8000-00805f9b34fb", "Public Broadcast Announcement");
        mServices.put("00001857-0000-1000-8000-00805f9b34fb", "Electronic Shelf Label");
        mServices.put("00001858-0000-1000-8000-00805f9b34fb", "Gaming Audio");
        mServices.put("00001859-0000-1000-8000-00805f9b34fb", "Mesh Proxy Solicitation");
        mServices.put("0000185a-0000-1000-8000-00805f9b34fb", "Industrial Measurement Device");
        mServices.put("0000185b-0000-1000-8000-00805f9b34fb", "Ranging");
        mServices.put("0000185c-0000-1000-8000-00805f9b34fb", "HID ISO");

        // source: https://bitbucket.org/bluetooth-SIG/public/src/main/assigned_numbers/uuids/service_class.yaml
        mServices.put("00001000-0000-1000-8000-00805f9b34fb", "ServiceDiscoveryServerServiceClassID");
        mServices.put("00001001-0000-1000-8000-00805f9b34fb", "BrowseGroupDescriptorServiceClassID");
        mServices.put("00001101-0000-1000-8000-00805f9b34fb", "SerialPort");
        mServices.put("00001102-0000-1000-8000-00805f9b34fb", "LANAccessUsingPPP");
        mServices.put("00001103-0000-1000-8000-00805f9b34fb", "Dial-Up Networking");
        mServices.put("00001104-0000-1000-8000-00805f9b34fb", "IrMCSync");
        mServices.put("00001105-0000-1000-8000-00805f9b34fb", "OBEXObjectPush");
        mServices.put("00001106-0000-1000-8000-00805f9b34fb", "OBEX File Transfer");
        mServices.put("00001107-0000-1000-8000-00805f9b34fb", "IrMCSyncCommand");
        mServices.put("00001108-0000-1000-8000-00805f9b34fb", "Headset");
        mServices.put("00001109-0000-1000-8000-00805f9b34fb", "CordlessTelephony");
        mServices.put("0000110a-0000-1000-8000-00805f9b34fb", "Audio Source");
        mServices.put("0000110b-0000-1000-8000-00805f9b34fb", "Audio Sink");
        mServices.put("0000110c-0000-1000-8000-00805f9b34fb", "A/V Remote Control Target");
        mServices.put("0000110d-0000-1000-8000-00805f9b34fb", "Advanced Audio Distribution");
        mServices.put("0000110e-0000-1000-8000-00805f9b34fb", "A/V Remote Control");
        mServices.put("0000110f-0000-1000-8000-00805f9b34fb", "A/V Remote Control Controller");
        mServices.put("00001110-0000-1000-8000-00805f9b34fb", "Intercom");
        mServices.put("00001111-0000-1000-8000-00805f9b34fb", "Fax");
        mServices.put("00001112-0000-1000-8000-00805f9b34fb", "Headset Audio Gateway");
        mServices.put("00001113-0000-1000-8000-00805f9b34fb", "WAP");
        mServices.put("00001114-0000-1000-8000-00805f9b34fb", "WAP_CLIENT");
        mServices.put("00001115-0000-1000-8000-00805f9b34fb", "PANU");
        mServices.put("00001116-0000-1000-8000-00805f9b34fb", "NAP");
        mServices.put("00001117-0000-1000-8000-00805f9b34fb", "GN");
        mServices.put("00001118-0000-1000-8000-00805f9b34fb", "DirectPrinting");
        mServices.put("00001119-0000-1000-8000-00805f9b34fb", "ReferencePrinting");
        mServices.put("0000111a-0000-1000-8000-00805f9b34fb", "Imaging");
        mServices.put("0000111b-0000-1000-8000-00805f9b34fb", "Imaging Responder");
        mServices.put("0000111c-0000-1000-8000-00805f9b34fb", "Imaging Automatic Archive");
        mServices.put("0000111d-0000-1000-8000-00805f9b34fb", "Imaging Referenced Objects");
        mServices.put("0000111e-0000-1000-8000-00805f9b34fb", "Hands-Free");
        mServices.put("0000111f-0000-1000-8000-00805f9b34fb", "AG Hands-Free");
        mServices.put("00001120-0000-1000-8000-00805f9b34fb", "DirectPrintingReferencedObjectsService");
        mServices.put("00001121-0000-1000-8000-00805f9b34fb", "ReflectedUI");
        mServices.put("00001122-0000-1000-8000-00805f9b34fb", "BasicPrinting");
        mServices.put("00001123-0000-1000-8000-00805f9b34fb", "PrintingStatus");
        mServices.put("00001124-0000-1000-8000-00805f9b34fb", "HID");
        mServices.put("00001125-0000-1000-8000-00805f9b34fb", "HardcopyCableReplacement");
        mServices.put("00001126-0000-1000-8000-00805f9b34fb", "HCR_Print");
        mServices.put("00001127-0000-1000-8000-00805f9b34fb", "HCR_Scan");
        mServices.put("00001128-0000-1000-8000-00805f9b34fb", "Common_ISDN_Access");
        mServices.put("0000112d-0000-1000-8000-00805f9b34fb", "SIM Access");
        mServices.put("0000112e-0000-1000-8000-00805f9b34fb", "Phonebook Access Client");
        mServices.put("0000112f-0000-1000-8000-00805f9b34fb", "Phonebook Access Server");
        mServices.put("00001130-0000-1000-8000-00805f9b34fb", "Phonebook Access Profile");
        mServices.put("00001131-0000-1000-8000-00805f9b34fb", "Headset - HS");
        mServices.put("00001132-0000-1000-8000-00805f9b34fb", "Message Access Server");
        mServices.put("00001133-0000-1000-8000-00805f9b34fb", "Message Notification Server");
        mServices.put("00001134-0000-1000-8000-00805f9b34fb", "Message Access Profile");
        mServices.put("00001135-0000-1000-8000-00805f9b34fb", "GNSS");
        mServices.put("00001136-0000-1000-8000-00805f9b34fb", "GNSS_Server");
        mServices.put("00001137-0000-1000-8000-00805f9b34fb", "3D Display");
        mServices.put("00001138-0000-1000-8000-00805f9b34fb", "3D Glasses");
        mServices.put("00001139-0000-1000-8000-00805f9b34fb", "3D Synch Profile");
        mServices.put("0000113a-0000-1000-8000-00805f9b34fb", "Multi Profile Specification");
        mServices.put("0000113b-0000-1000-8000-00805f9b34fb", "MPS");
        mServices.put("0000113c-0000-1000-8000-00805f9b34fb", "CTN Access Service");
        mServices.put("0000113d-0000-1000-8000-00805f9b34fb", "CTN Notification Service");
        mServices.put("0000113e-0000-1000-8000-00805f9b34fb", "Calendar Tasks and Notes Profile");
        mServices.put("00001200-0000-1000-8000-00805f9b34fb", "PnPInformation");
        mServices.put("00001201-0000-1000-8000-00805f9b34fb", "Generic Networking");
        mServices.put("00001202-0000-1000-8000-00805f9b34fb", "GenericFileTransfer");
        mServices.put("00001203-0000-1000-8000-00805f9b34fb", "Generic Audio");
        mServices.put("00001204-0000-1000-8000-00805f9b34fb", "GenericTelephony");
        mServices.put("00001205-0000-1000-8000-00805f9b34fb", "UPNP_Service");
        mServices.put("00001206-0000-1000-8000-00805f9b34fb", "UPNP_IP_Service");
        mServices.put("00001300-0000-1000-8000-00805f9b34fb", "ESDP_UPNP_IP_PAN");
        mServices.put("00001301-0000-1000-8000-00805f9b34fb", "ESDP_UPNP_IP_LAP");
        mServices.put("00001302-0000-1000-8000-00805f9b34fb", "ESDP_UPNP_L2CAP");
        mServices.put("00001303-0000-1000-8000-00805f9b34fb", "Video Source");
        mServices.put("00001304-0000-1000-8000-00805f9b34fb", "Video Sink");
        mServices.put("00001305-0000-1000-8000-00805f9b34fb", "Video Distribution");
        mServices.put("00001400-0000-1000-8000-00805f9b34fb", "HDP");
        mServices.put("00001401-0000-1000-8000-00805f9b34fb", "HDP Source");
        mServices.put("00001402-0000-1000-8000-00805f9b34fb", "HDP Sink");

        mServices.put("0000fdab-0000-1000-8000-00805f9b34fb", "(Propr: Xiaomi Proximity Unlock Service)");
        mServices.put("0000fe95-0000-1000-8000-00805f9b34fb", "(Propr: Xiaomi Wear Service)");
        mServices.put("0000fee0-0000-3512-2118-0009af100700", "(Propr: Xiaomi MiLi Service)");
        mServices.put("00001530-0000-3512-2118-0009af100700", "(Propr: Xiaomi Weight Service)");
        mServices.put("0000fee0-0000-1000-8000-00805f9b34fb", "(Propr: Xiaomi Huami Service)");
        mServices.put("14701820-620a-3973-7c78-9cfff0876abd", "(Propr: HPLUS Service)");
        mServices.put("16186f00-0000-1000-8000-00807f9b34fb", "(Propr: Xiaomi Wear Service - Mi Watch Lite/Redmi Watch)");
        mServices.put("16187f00-0000-1000-8000-00807f9b34fb", "(Propr: Xiaomi Wear Service - Mi Smart Watch 4C/Redmi Band)");
        mServices.put("1314f000-1000-9000-7000-301291e21220", "(Propr: Xiaomi Wear Service - Mi Watch/Mi Watch Color/Mi Watch Color Sport)");
        mServices.put("7495fe00-a7f3-424b-92dd-4a006a3aef56", "(Propr: Xiaomi Wear Service - Mi Watch CN)");
        mServices.put("0000fff0-0000-1000-8000-00805f9b34fb", "(Propr: Nothing CMF Command");
        mServices.put("02f00000-0000-0000-0000-00000000ffe0", "(Propr: Nothing CMF Data");
        mServices.put("02f00000-0000-0000-0000-00000000fe00", "(Propr: Nothing CMF Firmware");
        mServices.put("77d4e67c-2fe2-2334-0d35-9ccd078f529c", "(Propr: Nothing CMF Shell");
        mServices.put("000055ff-0000-1000-8000-00805f9b34fb", "(Propr: GloryFit Command");
        mServices.put("000056ff-0000-1000-8000-00805f9b34fb", "(Propr: GloryFit Data");
        mServices.put("9b012401-bc30-ce9a-e111-0f67e491abde", "(Propr: Garmin GFDI V0)");
        mServices.put("6a4e2401-667b-11e3-949a-0800200c9a66", "(Propr: Garmin GFDI V1)");
        mServices.put("6a4e2800-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML)");
        mServices.put("86f61000-f706-58a0-95b2-1fb9261e4dc7", "(Propr: Ultrahuman State)");
        mServices.put("86f65000-f706-58a0-95b2-1fb9261e4dc7", "(Propr: Ultrahuman Request)");
        mServices.put("86f66000-f706-58a0-95b2-1fb9261e4dc7", "(Propr: Ultrahuman Data)");
        mServices.put("8d53dc1d-1db7-4cd3-868b-8a527460aa84", "(Propr: SMP - Simple Management Protocol)");
        mServices.put("6e40fff0-b5a3-f393-e0a9-e50e24dcca9e", "(Propr: NUS - Nordic UART Service)");
        mServices.put("de5bf728-d711-4e47-af26-65e3012a5dc7", "(Propr: Yawell Serial)");

        // source https://bitbucket.org/bluetooth-SIG/public/src/main/assigned_numbers/uuids/characteristic_uuids.yaml
        mCharacteristics.put("00002a00-0000-1000-8000-00805f9b34fb", "Device Name");
        mCharacteristics.put("00002a01-0000-1000-8000-00805f9b34fb", "Appearance");
        mCharacteristics.put("00002a02-0000-1000-8000-00805f9b34fb", "Peripheral Privacy Flag");
        mCharacteristics.put("00002a03-0000-1000-8000-00805f9b34fb", "Reconnection Address");
        mCharacteristics.put("00002a04-0000-1000-8000-00805f9b34fb", "Peripheral Preferred Connection Parameters");
        mCharacteristics.put("00002a05-0000-1000-8000-00805f9b34fb", "Service Changed");
        mCharacteristics.put("00002a06-0000-1000-8000-00805f9b34fb", "Alert Level");
        mCharacteristics.put("00002a07-0000-1000-8000-00805f9b34fb", "Tx Power Level");
        mCharacteristics.put("00002a08-0000-1000-8000-00805f9b34fb", "Date Time");
        mCharacteristics.put("00002a09-0000-1000-8000-00805f9b34fb", "Day of Week");
        mCharacteristics.put("00002a0a-0000-1000-8000-00805f9b34fb", "Day Date Time");
        mCharacteristics.put("00002a0c-0000-1000-8000-00805f9b34fb", "Exact Time 256");
        mCharacteristics.put("00002a0d-0000-1000-8000-00805f9b34fb", "DST Offset");
        mCharacteristics.put("00002a0e-0000-1000-8000-00805f9b34fb", "Time Zone");
        mCharacteristics.put("00002a0f-0000-1000-8000-00805f9b34fb", "Local Time Information");
        mCharacteristics.put("00002a11-0000-1000-8000-00805f9b34fb", "Time with DST");
        mCharacteristics.put("00002a12-0000-1000-8000-00805f9b34fb", "Time Accuracy");
        mCharacteristics.put("00002a13-0000-1000-8000-00805f9b34fb", "Time Source");
        mCharacteristics.put("00002a14-0000-1000-8000-00805f9b34fb", "Reference Time Information");
        mCharacteristics.put("00002a16-0000-1000-8000-00805f9b34fb", "Time Update Control Point");
        mCharacteristics.put("00002a17-0000-1000-8000-00805f9b34fb", "Time Update State");
        mCharacteristics.put("00002a18-0000-1000-8000-00805f9b34fb", "Glucose Measurement");
        mCharacteristics.put("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level");
        mCharacteristics.put("00002a1c-0000-1000-8000-00805f9b34fb", "Temperature Measurement");
        mCharacteristics.put("00002a1d-0000-1000-8000-00805f9b34fb", "Temperature Type");
        mCharacteristics.put("00002a1e-0000-1000-8000-00805f9b34fb", "Intermediate Temperature");
        mCharacteristics.put("00002a21-0000-1000-8000-00805f9b34fb", "Measurement Interval");
        mCharacteristics.put("00002a22-0000-1000-8000-00805f9b34fb", "Boot Keyboard Input Report");
        mCharacteristics.put("00002a23-0000-1000-8000-00805f9b34fb", "System ID");
        mCharacteristics.put("00002a24-0000-1000-8000-00805f9b34fb", "Model Number String");
        mCharacteristics.put("00002a25-0000-1000-8000-00805f9b34fb", "Serial Number String");
        mCharacteristics.put("00002a26-0000-1000-8000-00805f9b34fb", "Firmware Revision String");
        mCharacteristics.put("00002a27-0000-1000-8000-00805f9b34fb", "Hardware Revision String");
        mCharacteristics.put("00002a28-0000-1000-8000-00805f9b34fb", "Software Revision String");
        mCharacteristics.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        mCharacteristics.put("00002a2a-0000-1000-8000-00805f9b34fb", "IEEE 11073-20601 Regulatory Certification Data List");
        mCharacteristics.put("00002a2b-0000-1000-8000-00805f9b34fb", "Current Time");
        mCharacteristics.put("00002a2c-0000-1000-8000-00805f9b34fb", "Magnetic Declination");
        mCharacteristics.put("00002a31-0000-1000-8000-00805f9b34fb", "Scan Refresh");
        mCharacteristics.put("00002a32-0000-1000-8000-00805f9b34fb", "Boot Keyboard Output Report");
        mCharacteristics.put("00002a33-0000-1000-8000-00805f9b34fb", "Boot Mouse Input Report");
        mCharacteristics.put("00002a34-0000-1000-8000-00805f9b34fb", "Glucose Measurement Context");
        mCharacteristics.put("00002a35-0000-1000-8000-00805f9b34fb", "Blood Pressure Measurement");
        mCharacteristics.put("00002a36-0000-1000-8000-00805f9b34fb", "Intermediate Cuff Pressure");
        mCharacteristics.put("00002a37-0000-1000-8000-00805f9b34fb", "Heart Rate Measurement");
        mCharacteristics.put("00002a38-0000-1000-8000-00805f9b34fb", "Body Sensor Location");
        mCharacteristics.put("00002a39-0000-1000-8000-00805f9b34fb", "Heart Rate Control Point");
        mCharacteristics.put("00002a3f-0000-1000-8000-00805f9b34fb", "Alert Status");
        mCharacteristics.put("00002a40-0000-1000-8000-00805f9b34fb", "Ringer Control Point");
        mCharacteristics.put("00002a41-0000-1000-8000-00805f9b34fb", "Ringer Setting");
        mCharacteristics.put("00002a42-0000-1000-8000-00805f9b34fb", "Alert Category ID Bit Mask");
        mCharacteristics.put("00002a43-0000-1000-8000-00805f9b34fb", "Alert Category ID");
        mCharacteristics.put("00002a44-0000-1000-8000-00805f9b34fb", "Alert Notification Control Point");
        mCharacteristics.put("00002a45-0000-1000-8000-00805f9b34fb", "Unread Alert Status");
        mCharacteristics.put("00002a46-0000-1000-8000-00805f9b34fb", "New Alert");
        mCharacteristics.put("00002a47-0000-1000-8000-00805f9b34fb", "Supported New Alert Category");
        mCharacteristics.put("00002a48-0000-1000-8000-00805f9b34fb", "Supported Unread Alert Category");
        mCharacteristics.put("00002a49-0000-1000-8000-00805f9b34fb", "Blood Pressure Feature");
        mCharacteristics.put("00002a4a-0000-1000-8000-00805f9b34fb", "HID Information");
        mCharacteristics.put("00002a4b-0000-1000-8000-00805f9b34fb", "Report Map");
        mCharacteristics.put("00002a4c-0000-1000-8000-00805f9b34fb", "HID Control Point");
        mCharacteristics.put("00002a4d-0000-1000-8000-00805f9b34fb", "Report");
        mCharacteristics.put("00002a4e-0000-1000-8000-00805f9b34fb", "Protocol Mode");
        mCharacteristics.put("00002a4f-0000-1000-8000-00805f9b34fb", "Scan Interval Window");
        mCharacteristics.put("00002a50-0000-1000-8000-00805f9b34fb", "PnP ID");
        mCharacteristics.put("00002a51-0000-1000-8000-00805f9b34fb", "Glucose Feature");
        mCharacteristics.put("00002a52-0000-1000-8000-00805f9b34fb", "Record Access Control Point");
        mCharacteristics.put("00002a53-0000-1000-8000-00805f9b34fb", "RSC Measurement");
        mCharacteristics.put("00002a54-0000-1000-8000-00805f9b34fb", "RSC Feature");
        mCharacteristics.put("00002a55-0000-1000-8000-00805f9b34fb", "SC Control Point");
        mCharacteristics.put("00002a5a-0000-1000-8000-00805f9b34fb", "Aggregate");
        mCharacteristics.put("00002a5b-0000-1000-8000-00805f9b34fb", "CSC Measurement");
        mCharacteristics.put("00002a5c-0000-1000-8000-00805f9b34fb", "CSC Feature");
        mCharacteristics.put("00002a5d-0000-1000-8000-00805f9b34fb", "Sensor Location");
        mCharacteristics.put("00002a5e-0000-1000-8000-00805f9b34fb", "PLX Spot-Check Measurement");
        mCharacteristics.put("00002a5f-0000-1000-8000-00805f9b34fb", "PLX Continuous Measurement");
        mCharacteristics.put("00002a60-0000-1000-8000-00805f9b34fb", "PLX Features");
        mCharacteristics.put("00002a63-0000-1000-8000-00805f9b34fb", "Cycling Power Measurement");
        mCharacteristics.put("00002a64-0000-1000-8000-00805f9b34fb", "Cycling Power Vector");
        mCharacteristics.put("00002a65-0000-1000-8000-00805f9b34fb", "Cycling Power Feature");
        mCharacteristics.put("00002a66-0000-1000-8000-00805f9b34fb", "Cycling Power Control Point");
        mCharacteristics.put("00002a67-0000-1000-8000-00805f9b34fb", "Location and Speed");
        mCharacteristics.put("00002a68-0000-1000-8000-00805f9b34fb", "Navigation");
        mCharacteristics.put("00002a69-0000-1000-8000-00805f9b34fb", "Position Quality");
        mCharacteristics.put("00002a6a-0000-1000-8000-00805f9b34fb", "LN Feature");
        mCharacteristics.put("00002a6b-0000-1000-8000-00805f9b34fb", "LN Control Point");
        mCharacteristics.put("00002a6c-0000-1000-8000-00805f9b34fb", "Elevation");
        mCharacteristics.put("00002a6d-0000-1000-8000-00805f9b34fb", "Pressure");
        mCharacteristics.put("00002a6e-0000-1000-8000-00805f9b34fb", "Temperature");
        mCharacteristics.put("00002a6f-0000-1000-8000-00805f9b34fb", "Humidity");
        mCharacteristics.put("00002a70-0000-1000-8000-00805f9b34fb", "True Wind Speed");
        mCharacteristics.put("00002a71-0000-1000-8000-00805f9b34fb", "True Wind Direction");
        mCharacteristics.put("00002a72-0000-1000-8000-00805f9b34fb", "Apparent Wind Speed");
        mCharacteristics.put("00002a73-0000-1000-8000-00805f9b34fb", "Apparent Wind Direction");
        mCharacteristics.put("00002a74-0000-1000-8000-00805f9b34fb", "Gust Factor");
        mCharacteristics.put("00002a75-0000-1000-8000-00805f9b34fb", "Pollen Concentration");
        mCharacteristics.put("00002a76-0000-1000-8000-00805f9b34fb", "UV Index");
        mCharacteristics.put("00002a77-0000-1000-8000-00805f9b34fb", "Irradiance");
        mCharacteristics.put("00002a78-0000-1000-8000-00805f9b34fb", "Rainfall");
        mCharacteristics.put("00002a79-0000-1000-8000-00805f9b34fb", "Wind Chill");
        mCharacteristics.put("00002a7a-0000-1000-8000-00805f9b34fb", "Heat Index");
        mCharacteristics.put("00002a7b-0000-1000-8000-00805f9b34fb", "Dew Point");
        mCharacteristics.put("00002a7d-0000-1000-8000-00805f9b34fb", "Descriptor Value Changed");
        mCharacteristics.put("00002a7e-0000-1000-8000-00805f9b34fb", "Aerobic Heart Rate Lower Limit");
        mCharacteristics.put("00002a7f-0000-1000-8000-00805f9b34fb", "Aerobic Threshold");
        mCharacteristics.put("00002a80-0000-1000-8000-00805f9b34fb", "Age");
        mCharacteristics.put("00002a81-0000-1000-8000-00805f9b34fb", "Anaerobic Heart Rate Lower Limit");
        mCharacteristics.put("00002a82-0000-1000-8000-00805f9b34fb", "Anaerobic Heart Rate Upper Limit");
        mCharacteristics.put("00002a83-0000-1000-8000-00805f9b34fb", "Anaerobic Threshold");
        mCharacteristics.put("00002a84-0000-1000-8000-00805f9b34fb", "Aerobic Heart Rate Upper Limit");
        mCharacteristics.put("00002a85-0000-1000-8000-00805f9b34fb", "Date of Birth");
        mCharacteristics.put("00002a86-0000-1000-8000-00805f9b34fb", "Date of Threshold Assessment");
        mCharacteristics.put("00002a87-0000-1000-8000-00805f9b34fb", "Email Address");
        mCharacteristics.put("00002a88-0000-1000-8000-00805f9b34fb", "Fat Burn Heart Rate Lower Limit");
        mCharacteristics.put("00002a89-0000-1000-8000-00805f9b34fb", "Fat Burn Heart Rate Upper Limit");
        mCharacteristics.put("00002a8a-0000-1000-8000-00805f9b34fb", "First Name");
        mCharacteristics.put("00002a8b-0000-1000-8000-00805f9b34fb", "Five Zone Heart Rate Limits");
        mCharacteristics.put("00002a8c-0000-1000-8000-00805f9b34fb", "Gender");
        mCharacteristics.put("00002a8d-0000-1000-8000-00805f9b34fb", "Heart Rate Max");
        mCharacteristics.put("00002a8e-0000-1000-8000-00805f9b34fb", "Height");
        mCharacteristics.put("00002a8f-0000-1000-8000-00805f9b34fb", "Hip Circumference");
        mCharacteristics.put("00002a90-0000-1000-8000-00805f9b34fb", "Last Name");
        mCharacteristics.put("00002a91-0000-1000-8000-00805f9b34fb", "Maximum Recommended Heart Rate");
        mCharacteristics.put("00002a92-0000-1000-8000-00805f9b34fb", "Resting Heart Rate");
        mCharacteristics.put("00002a93-0000-1000-8000-00805f9b34fb", "Sport Type for Aerobic and Anaerobic Thresholds");
        mCharacteristics.put("00002a94-0000-1000-8000-00805f9b34fb", "Three Zone Heart Rate Limits");
        mCharacteristics.put("00002a95-0000-1000-8000-00805f9b34fb", "Two Zone Heart Rate Limits");
        mCharacteristics.put("00002a96-0000-1000-8000-00805f9b34fb", "VO2 Max");
        mCharacteristics.put("00002a97-0000-1000-8000-00805f9b34fb", "Waist Circumference");
        mCharacteristics.put("00002a98-0000-1000-8000-00805f9b34fb", "Weight");
        mCharacteristics.put("00002a99-0000-1000-8000-00805f9b34fb", "Database Change Increment");
        mCharacteristics.put("00002a9a-0000-1000-8000-00805f9b34fb", "User Index");
        mCharacteristics.put("00002a9b-0000-1000-8000-00805f9b34fb", "Body Composition Feature");
        mCharacteristics.put("00002a9c-0000-1000-8000-00805f9b34fb", "Body Composition Measurement");
        mCharacteristics.put("00002a9d-0000-1000-8000-00805f9b34fb", "Weight Measurement");
        mCharacteristics.put("00002a9e-0000-1000-8000-00805f9b34fb", "Weight Scale Feature");
        mCharacteristics.put("00002a9f-0000-1000-8000-00805f9b34fb", "User Control Point");
        mCharacteristics.put("00002aa0-0000-1000-8000-00805f9b34fb", "Magnetic Flux Density - 2D");
        mCharacteristics.put("00002aa1-0000-1000-8000-00805f9b34fb", "Magnetic Flux Density - 3D");
        mCharacteristics.put("00002aa2-0000-1000-8000-00805f9b34fb", "Language");
        mCharacteristics.put("00002aa3-0000-1000-8000-00805f9b34fb", "Barometric Pressure Trend");
        mCharacteristics.put("00002aa4-0000-1000-8000-00805f9b34fb", "Bond Management Control Point");
        mCharacteristics.put("00002aa5-0000-1000-8000-00805f9b34fb", "Bond Management Feature");
        mCharacteristics.put("00002aa6-0000-1000-8000-00805f9b34fb", "Central Address Resolution");
        mCharacteristics.put("00002aa7-0000-1000-8000-00805f9b34fb", "CGM Measurement");
        mCharacteristics.put("00002aa8-0000-1000-8000-00805f9b34fb", "CGM Feature");
        mCharacteristics.put("00002aa9-0000-1000-8000-00805f9b34fb", "CGM Status");
        mCharacteristics.put("00002aaa-0000-1000-8000-00805f9b34fb", "CGM Session Start Time");
        mCharacteristics.put("00002aab-0000-1000-8000-00805f9b34fb", "CGM Session Run Time");
        mCharacteristics.put("00002aac-0000-1000-8000-00805f9b34fb", "CGM Specific Ops Control Point");
        mCharacteristics.put("00002aad-0000-1000-8000-00805f9b34fb", "Indoor Positioning Configuration");
        mCharacteristics.put("00002aae-0000-1000-8000-00805f9b34fb", "Latitude");
        mCharacteristics.put("00002aaf-0000-1000-8000-00805f9b34fb", "Longitude");
        mCharacteristics.put("00002ab0-0000-1000-8000-00805f9b34fb", "Local North Coordinate");
        mCharacteristics.put("00002ab1-0000-1000-8000-00805f9b34fb", "Local East Coordinate");
        mCharacteristics.put("00002ab2-0000-1000-8000-00805f9b34fb", "Floor Number");
        mCharacteristics.put("00002ab3-0000-1000-8000-00805f9b34fb", "Altitude");
        mCharacteristics.put("00002ab4-0000-1000-8000-00805f9b34fb", "Uncertainty");
        mCharacteristics.put("00002ab5-0000-1000-8000-00805f9b34fb", "Location Name");
        mCharacteristics.put("00002ab6-0000-1000-8000-00805f9b34fb", "URI");
        mCharacteristics.put("00002ab7-0000-1000-8000-00805f9b34fb", "HTTP Headers");
        mCharacteristics.put("00002ab8-0000-1000-8000-00805f9b34fb", "HTTP Status Code");
        mCharacteristics.put("00002ab9-0000-1000-8000-00805f9b34fb", "HTTP Entity Body");
        mCharacteristics.put("00002aba-0000-1000-8000-00805f9b34fb", "HTTP Control Point");
        mCharacteristics.put("00002abb-0000-1000-8000-00805f9b34fb", "HTTPS Security");
        mCharacteristics.put("00002abc-0000-1000-8000-00805f9b34fb", "TDS Control Point");
        mCharacteristics.put("00002abd-0000-1000-8000-00805f9b34fb", "OTS Feature");
        mCharacteristics.put("00002abe-0000-1000-8000-00805f9b34fb", "Object Name");
        mCharacteristics.put("00002abf-0000-1000-8000-00805f9b34fb", "Object Type");
        mCharacteristics.put("00002ac0-0000-1000-8000-00805f9b34fb", "Object Size");
        mCharacteristics.put("00002ac1-0000-1000-8000-00805f9b34fb", "Object First-Created");
        mCharacteristics.put("00002ac2-0000-1000-8000-00805f9b34fb", "Object Last-Modified");
        mCharacteristics.put("00002ac3-0000-1000-8000-00805f9b34fb", "Object ID");
        mCharacteristics.put("00002ac4-0000-1000-8000-00805f9b34fb", "Object Properties");
        mCharacteristics.put("00002ac5-0000-1000-8000-00805f9b34fb", "Object Action Control Point");
        mCharacteristics.put("00002ac6-0000-1000-8000-00805f9b34fb", "Object List Control Point");
        mCharacteristics.put("00002ac7-0000-1000-8000-00805f9b34fb", "Object List Filter");
        mCharacteristics.put("00002ac8-0000-1000-8000-00805f9b34fb", "Object Changed");
        mCharacteristics.put("00002ac9-0000-1000-8000-00805f9b34fb", "Resolvable Private Address Only");
        mCharacteristics.put("00002acc-0000-1000-8000-00805f9b34fb", "Fitness Machine Feature");
        mCharacteristics.put("00002acd-0000-1000-8000-00805f9b34fb", "Treadmill Data");
        mCharacteristics.put("00002ace-0000-1000-8000-00805f9b34fb", "Cross Trainer Data");
        mCharacteristics.put("00002acf-0000-1000-8000-00805f9b34fb", "Step Climber Data");
        mCharacteristics.put("00002ad0-0000-1000-8000-00805f9b34fb", "Stair Climber Data");
        mCharacteristics.put("00002ad1-0000-1000-8000-00805f9b34fb", "Rower Data");
        mCharacteristics.put("00002ad2-0000-1000-8000-00805f9b34fb", "Indoor Bike Data");
        mCharacteristics.put("00002ad3-0000-1000-8000-00805f9b34fb", "Training Status");
        mCharacteristics.put("00002ad4-0000-1000-8000-00805f9b34fb", "Supported Speed Range");
        mCharacteristics.put("00002ad5-0000-1000-8000-00805f9b34fb", "Supported Inclination Range");
        mCharacteristics.put("00002ad6-0000-1000-8000-00805f9b34fb", "Supported Resistance Level Range");
        mCharacteristics.put("00002ad7-0000-1000-8000-00805f9b34fb", "Supported Heart Rate Range");
        mCharacteristics.put("00002ad8-0000-1000-8000-00805f9b34fb", "Supported Power Range");
        mCharacteristics.put("00002ad9-0000-1000-8000-00805f9b34fb", "Fitness Machine Control Point");
        mCharacteristics.put("00002ada-0000-1000-8000-00805f9b34fb", "Fitness Machine Status");
        mCharacteristics.put("00002adb-0000-1000-8000-00805f9b34fb", "Mesh Provisioning Data In");
        mCharacteristics.put("00002adc-0000-1000-8000-00805f9b34fb", "Mesh Provisioning Data Out");
        mCharacteristics.put("00002add-0000-1000-8000-00805f9b34fb", "Mesh Proxy Data In");
        mCharacteristics.put("00002ade-0000-1000-8000-00805f9b34fb", "Mesh Proxy Data Out");
        mCharacteristics.put("00002ae0-0000-1000-8000-00805f9b34fb", "Average Current");
        mCharacteristics.put("00002ae1-0000-1000-8000-00805f9b34fb", "Average Voltage");
        mCharacteristics.put("00002ae2-0000-1000-8000-00805f9b34fb", "Boolean");
        mCharacteristics.put("00002ae3-0000-1000-8000-00805f9b34fb", "Chromatic Distance from Planckian");
        mCharacteristics.put("00002ae4-0000-1000-8000-00805f9b34fb", "Chromaticity Coordinates");
        mCharacteristics.put("00002ae5-0000-1000-8000-00805f9b34fb", "Chromaticity in CCT and Duv Values");
        mCharacteristics.put("00002ae6-0000-1000-8000-00805f9b34fb", "Chromaticity Tolerance");
        mCharacteristics.put("00002ae7-0000-1000-8000-00805f9b34fb", "CIE 13.3-1995 Color Rendering Index");
        mCharacteristics.put("00002ae8-0000-1000-8000-00805f9b34fb", "Coefficient");
        mCharacteristics.put("00002ae9-0000-1000-8000-00805f9b34fb", "Correlated Color Temperature");
        mCharacteristics.put("00002aea-0000-1000-8000-00805f9b34fb", "Count 16");
        mCharacteristics.put("00002aeb-0000-1000-8000-00805f9b34fb", "Count 24");
        mCharacteristics.put("00002aec-0000-1000-8000-00805f9b34fb", "Country Code");
        mCharacteristics.put("00002aed-0000-1000-8000-00805f9b34fb", "Date UTC");
        mCharacteristics.put("00002aee-0000-1000-8000-00805f9b34fb", "Electric Current");
        mCharacteristics.put("00002aef-0000-1000-8000-00805f9b34fb", "Electric Current Range");
        mCharacteristics.put("00002af0-0000-1000-8000-00805f9b34fb", "Electric Current Specification");
        mCharacteristics.put("00002af1-0000-1000-8000-00805f9b34fb", "Electric Current Statistics");
        mCharacteristics.put("00002af2-0000-1000-8000-00805f9b34fb", "Energy");
        mCharacteristics.put("00002af3-0000-1000-8000-00805f9b34fb", "Energy in a Period of Day");
        mCharacteristics.put("00002af4-0000-1000-8000-00805f9b34fb", "Event Statistics");
        mCharacteristics.put("00002af5-0000-1000-8000-00805f9b34fb", "Fixed String 16");
        mCharacteristics.put("00002af6-0000-1000-8000-00805f9b34fb", "Fixed String 24");
        mCharacteristics.put("00002af7-0000-1000-8000-00805f9b34fb", "Fixed String 36");
        mCharacteristics.put("00002af8-0000-1000-8000-00805f9b34fb", "Fixed String 8");
        mCharacteristics.put("00002af9-0000-1000-8000-00805f9b34fb", "Generic Level");
        mCharacteristics.put("00002afa-0000-1000-8000-00805f9b34fb", "Global Trade Item Number");
        mCharacteristics.put("00002afb-0000-1000-8000-00805f9b34fb", "Illuminance");
        mCharacteristics.put("00002afc-0000-1000-8000-00805f9b34fb", "Luminous Efficacy");
        mCharacteristics.put("00002afd-0000-1000-8000-00805f9b34fb", "Luminous Energy");
        mCharacteristics.put("00002afe-0000-1000-8000-00805f9b34fb", "Luminous Exposure");
        mCharacteristics.put("00002aff-0000-1000-8000-00805f9b34fb", "Luminous Flux");
        mCharacteristics.put("00002b00-0000-1000-8000-00805f9b34fb", "Luminous Flux Range");
        mCharacteristics.put("00002b01-0000-1000-8000-00805f9b34fb", "Luminous Intensity");
        mCharacteristics.put("00002b02-0000-1000-8000-00805f9b34fb", "Mass Flow");
        mCharacteristics.put("00002b03-0000-1000-8000-00805f9b34fb", "Perceived Lightness");
        mCharacteristics.put("00002b04-0000-1000-8000-00805f9b34fb", "Percentage 8");
        mCharacteristics.put("00002b05-0000-1000-8000-00805f9b34fb", "Power");
        mCharacteristics.put("00002b06-0000-1000-8000-00805f9b34fb", "Power Specification");
        mCharacteristics.put("00002b07-0000-1000-8000-00805f9b34fb", "Relative Runtime in a Current Range");
        mCharacteristics.put("00002b08-0000-1000-8000-00805f9b34fb", "Relative Runtime in a Generic Level Range");
        mCharacteristics.put("00002b09-0000-1000-8000-00805f9b34fb", "Relative Value in a Voltage Range");
        mCharacteristics.put("00002b0a-0000-1000-8000-00805f9b34fb", "Relative Value in an Illuminance Range");
        mCharacteristics.put("00002b0b-0000-1000-8000-00805f9b34fb", "Relative Value in a Period of Day");
        mCharacteristics.put("00002b0c-0000-1000-8000-00805f9b34fb", "Relative Value in a Temperature Range");
        mCharacteristics.put("00002b0d-0000-1000-8000-00805f9b34fb", "Temperature 8");
        mCharacteristics.put("00002b0e-0000-1000-8000-00805f9b34fb", "Temperature 8 in a Period of Day");
        mCharacteristics.put("00002b0f-0000-1000-8000-00805f9b34fb", "Temperature 8 Statistics");
        mCharacteristics.put("00002b10-0000-1000-8000-00805f9b34fb", "Temperature Range");
        mCharacteristics.put("00002b11-0000-1000-8000-00805f9b34fb", "Temperature Statistics");
        mCharacteristics.put("00002b12-0000-1000-8000-00805f9b34fb", "Time Decihour 8");
        mCharacteristics.put("00002b13-0000-1000-8000-00805f9b34fb", "Time Exponential 8");
        mCharacteristics.put("00002b14-0000-1000-8000-00805f9b34fb", "Time Hour 24");
        mCharacteristics.put("00002b15-0000-1000-8000-00805f9b34fb", "Time Millisecond 24");
        mCharacteristics.put("00002b16-0000-1000-8000-00805f9b34fb", "Time Second 16");
        mCharacteristics.put("00002b17-0000-1000-8000-00805f9b34fb", "Time Second 8");
        mCharacteristics.put("00002b18-0000-1000-8000-00805f9b34fb", "Voltage");
        mCharacteristics.put("00002b19-0000-1000-8000-00805f9b34fb", "Voltage Specification");
        mCharacteristics.put("00002b1a-0000-1000-8000-00805f9b34fb", "Voltage Statistics");
        mCharacteristics.put("00002b1b-0000-1000-8000-00805f9b34fb", "Volume Flow");
        mCharacteristics.put("00002b1c-0000-1000-8000-00805f9b34fb", "Chromaticity Coordinate");
        mCharacteristics.put("00002b1d-0000-1000-8000-00805f9b34fb", "RC Feature");
        mCharacteristics.put("00002b1e-0000-1000-8000-00805f9b34fb", "RC Settings");
        mCharacteristics.put("00002b1f-0000-1000-8000-00805f9b34fb", "Reconnection Configuration Control Point");
        mCharacteristics.put("00002b20-0000-1000-8000-00805f9b34fb", "IDD Status Changed");
        mCharacteristics.put("00002b21-0000-1000-8000-00805f9b34fb", "IDD Status");
        mCharacteristics.put("00002b22-0000-1000-8000-00805f9b34fb", "IDD Annunciation Status");
        mCharacteristics.put("00002b23-0000-1000-8000-00805f9b34fb", "IDD Features");
        mCharacteristics.put("00002b24-0000-1000-8000-00805f9b34fb", "IDD Status Reader Control Point");
        mCharacteristics.put("00002b25-0000-1000-8000-00805f9b34fb", "IDD Command Control Point");
        mCharacteristics.put("00002b26-0000-1000-8000-00805f9b34fb", "IDD Command Data");
        mCharacteristics.put("00002b27-0000-1000-8000-00805f9b34fb", "IDD Record Access Control Point");
        mCharacteristics.put("00002b28-0000-1000-8000-00805f9b34fb", "IDD History Data");
        mCharacteristics.put("00002b29-0000-1000-8000-00805f9b34fb", "Client Supported Features");
        mCharacteristics.put("00002b2a-0000-1000-8000-00805f9b34fb", "Database Hash");
        mCharacteristics.put("00002b2b-0000-1000-8000-00805f9b34fb", "BSS Control Point");
        mCharacteristics.put("00002b2c-0000-1000-8000-00805f9b34fb", "BSS Response");
        mCharacteristics.put("00002b2d-0000-1000-8000-00805f9b34fb", "Emergency ID");
        mCharacteristics.put("00002b2e-0000-1000-8000-00805f9b34fb", "Emergency Text");
        mCharacteristics.put("00002b2f-0000-1000-8000-00805f9b34fb", "ACS Status");
        mCharacteristics.put("00002b30-0000-1000-8000-00805f9b34fb", "ACS Data In");
        mCharacteristics.put("00002b31-0000-1000-8000-00805f9b34fb", "ACS Data Out Notify");
        mCharacteristics.put("00002b32-0000-1000-8000-00805f9b34fb", "ACS Data Out Indicate");
        mCharacteristics.put("00002b33-0000-1000-8000-00805f9b34fb", "ACS Control Point");
        mCharacteristics.put("00002b34-0000-1000-8000-00805f9b34fb", "Enhanced Blood Pressure Measurement");
        mCharacteristics.put("00002b35-0000-1000-8000-00805f9b34fb", "Enhanced Intermediate Cuff Pressure");
        mCharacteristics.put("00002b36-0000-1000-8000-00805f9b34fb", "Blood Pressure Record");
        mCharacteristics.put("00002b37-0000-1000-8000-00805f9b34fb", "Registered User");
        mCharacteristics.put("00002b38-0000-1000-8000-00805f9b34fb", "BR-EDR Handover Data");
        mCharacteristics.put("00002b39-0000-1000-8000-00805f9b34fb", "Bluetooth SIG Data");
        mCharacteristics.put("00002b3a-0000-1000-8000-00805f9b34fb", "Server Supported Features");
        mCharacteristics.put("00002b3b-0000-1000-8000-00805f9b34fb", "Physical Activity Monitor Features");
        mCharacteristics.put("00002b3c-0000-1000-8000-00805f9b34fb", "General Activity Instantaneous Data");
        mCharacteristics.put("00002b3d-0000-1000-8000-00805f9b34fb", "General Activity Summary Data");
        mCharacteristics.put("00002b3e-0000-1000-8000-00805f9b34fb", "CardioRespiratory Activity Instantaneous Data");
        mCharacteristics.put("00002b3f-0000-1000-8000-00805f9b34fb", "CardioRespiratory Activity Summary Data");
        mCharacteristics.put("00002b40-0000-1000-8000-00805f9b34fb", "Step Counter Activity Summary Data");
        mCharacteristics.put("00002b41-0000-1000-8000-00805f9b34fb", "Sleep Activity Instantaneous Data");
        mCharacteristics.put("00002b42-0000-1000-8000-00805f9b34fb", "Sleep Activity Summary Data");
        mCharacteristics.put("00002b43-0000-1000-8000-00805f9b34fb", "Physical Activity Monitor Control Point");
        mCharacteristics.put("00002b44-0000-1000-8000-00805f9b34fb", "Physical Activity Current Session");
        mCharacteristics.put("00002b45-0000-1000-8000-00805f9b34fb", "Physical Activity Session Descriptor");
        mCharacteristics.put("00002b46-0000-1000-8000-00805f9b34fb", "Preferred Units");
        mCharacteristics.put("00002b47-0000-1000-8000-00805f9b34fb", "High Resolution Height");
        mCharacteristics.put("00002b48-0000-1000-8000-00805f9b34fb", "Middle Name");
        mCharacteristics.put("00002b49-0000-1000-8000-00805f9b34fb", "Stride Length");
        mCharacteristics.put("00002b4a-0000-1000-8000-00805f9b34fb", "Handedness");
        mCharacteristics.put("00002b4b-0000-1000-8000-00805f9b34fb", "Device Wearing Position");
        mCharacteristics.put("00002b4c-0000-1000-8000-00805f9b34fb", "Four Zone Heart Rate Limits");
        mCharacteristics.put("00002b4d-0000-1000-8000-00805f9b34fb", "High Intensity Exercise Threshold");
        mCharacteristics.put("00002b4e-0000-1000-8000-00805f9b34fb", "Activity Goal");
        mCharacteristics.put("00002b4f-0000-1000-8000-00805f9b34fb", "Sedentary Interval Notification");
        mCharacteristics.put("00002b50-0000-1000-8000-00805f9b34fb", "Caloric Intake");
        mCharacteristics.put("00002b51-0000-1000-8000-00805f9b34fb", "TMAP Role");
        mCharacteristics.put("00002b77-0000-1000-8000-00805f9b34fb", "Audio Input State");
        mCharacteristics.put("00002b78-0000-1000-8000-00805f9b34fb", "Gain Settings Attribute");
        mCharacteristics.put("00002b79-0000-1000-8000-00805f9b34fb", "Audio Input Type");
        mCharacteristics.put("00002b7a-0000-1000-8000-00805f9b34fb", "Audio Input Status");
        mCharacteristics.put("00002b7b-0000-1000-8000-00805f9b34fb", "Audio Input Control Point");
        mCharacteristics.put("00002b7c-0000-1000-8000-00805f9b34fb", "Audio Input Description");
        mCharacteristics.put("00002b7d-0000-1000-8000-00805f9b34fb", "Volume State");
        mCharacteristics.put("00002b7e-0000-1000-8000-00805f9b34fb", "Volume Control Point");
        mCharacteristics.put("00002b7f-0000-1000-8000-00805f9b34fb", "Volume Flags");
        mCharacteristics.put("00002b80-0000-1000-8000-00805f9b34fb", "Volume Offset State");
        mCharacteristics.put("00002b81-0000-1000-8000-00805f9b34fb", "Audio Location");
        mCharacteristics.put("00002b82-0000-1000-8000-00805f9b34fb", "Volume Offset Control Point");
        mCharacteristics.put("00002b83-0000-1000-8000-00805f9b34fb", "Audio Output Description");
        mCharacteristics.put("00002b84-0000-1000-8000-00805f9b34fb", "Set Identity Resolving Key");
        mCharacteristics.put("00002b85-0000-1000-8000-00805f9b34fb", "Coordinated Set Size");
        mCharacteristics.put("00002b86-0000-1000-8000-00805f9b34fb", "Set Member Lock");
        mCharacteristics.put("00002b87-0000-1000-8000-00805f9b34fb", "Set Member Rank");
        mCharacteristics.put("00002b88-0000-1000-8000-00805f9b34fb", "Encrypted Data Key Material");
        mCharacteristics.put("00002b89-0000-1000-8000-00805f9b34fb", "Apparent Energy 32");
        mCharacteristics.put("00002b8a-0000-1000-8000-00805f9b34fb", "Apparent Power");
        mCharacteristics.put("00002b8b-0000-1000-8000-00805f9b34fb", "Live Health Observations");
        mCharacteristics.put("00002b8c-0000-1000-8000-00805f9b34fb", "CO2 Concentration");
        mCharacteristics.put("00002b8d-0000-1000-8000-00805f9b34fb", "Cosine of the Angle");
        mCharacteristics.put("00002b8e-0000-1000-8000-00805f9b34fb", "Device Time Feature");
        mCharacteristics.put("00002b8f-0000-1000-8000-00805f9b34fb", "Device Time Parameters");
        mCharacteristics.put("00002b90-0000-1000-8000-00805f9b34fb", "Device Time");
        mCharacteristics.put("00002b91-0000-1000-8000-00805f9b34fb", "Device Time Control Point");
        mCharacteristics.put("00002b92-0000-1000-8000-00805f9b34fb", "Time Change Log Data");
        mCharacteristics.put("00002b93-0000-1000-8000-00805f9b34fb", "Media Player Name");
        mCharacteristics.put("00002b94-0000-1000-8000-00805f9b34fb", "Media Player Icon Object ID");
        mCharacteristics.put("00002b95-0000-1000-8000-00805f9b34fb", "Media Player Icon URL");
        mCharacteristics.put("00002b96-0000-1000-8000-00805f9b34fb", "Track Changed");
        mCharacteristics.put("00002b97-0000-1000-8000-00805f9b34fb", "Track Title");
        mCharacteristics.put("00002b98-0000-1000-8000-00805f9b34fb", "Track Duration");
        mCharacteristics.put("00002b99-0000-1000-8000-00805f9b34fb", "Track Position");
        mCharacteristics.put("00002b9a-0000-1000-8000-00805f9b34fb", "Playback Speed");
        mCharacteristics.put("00002b9b-0000-1000-8000-00805f9b34fb", "Seeking Speed");
        mCharacteristics.put("00002b9c-0000-1000-8000-00805f9b34fb", "Current Track Segments Object ID");
        mCharacteristics.put("00002b9d-0000-1000-8000-00805f9b34fb", "Current Track Object ID");
        mCharacteristics.put("00002b9e-0000-1000-8000-00805f9b34fb", "Next Track Object ID");
        mCharacteristics.put("00002b9f-0000-1000-8000-00805f9b34fb", "Parent Group Object ID");
        mCharacteristics.put("00002ba0-0000-1000-8000-00805f9b34fb", "Current Group Object ID");
        mCharacteristics.put("00002ba1-0000-1000-8000-00805f9b34fb", "Playing Order");
        mCharacteristics.put("00002ba2-0000-1000-8000-00805f9b34fb", "Playing Orders Supported");
        mCharacteristics.put("00002ba3-0000-1000-8000-00805f9b34fb", "Media State");
        mCharacteristics.put("00002ba4-0000-1000-8000-00805f9b34fb", "Media Control Point");
        mCharacteristics.put("00002ba5-0000-1000-8000-00805f9b34fb", "Media Control Point Opcodes Supported");
        mCharacteristics.put("00002ba6-0000-1000-8000-00805f9b34fb", "Search Results Object ID");
        mCharacteristics.put("00002ba7-0000-1000-8000-00805f9b34fb", "Search Control Point");
        mCharacteristics.put("00002ba8-0000-1000-8000-00805f9b34fb", "Energy 32");
        mCharacteristics.put("00002bad-0000-1000-8000-00805f9b34fb", "Constant Tone Extension Enable");
        mCharacteristics.put("00002bae-0000-1000-8000-00805f9b34fb", "Advertising Constant Tone Extension Minimum Length");
        mCharacteristics.put("00002baf-0000-1000-8000-00805f9b34fb", "Advertising Constant Tone Extension Minimum Transmit Count");
        mCharacteristics.put("00002bb0-0000-1000-8000-00805f9b34fb", "Advertising Constant Tone Extension Transmit Duration");
        mCharacteristics.put("00002bb1-0000-1000-8000-00805f9b34fb", "Advertising Constant Tone Extension Interval");
        mCharacteristics.put("00002bb2-0000-1000-8000-00805f9b34fb", "Advertising Constant Tone Extension PHY");
        mCharacteristics.put("00002bb3-0000-1000-8000-00805f9b34fb", "Bearer Provider Name");
        mCharacteristics.put("00002bb4-0000-1000-8000-00805f9b34fb", "Bearer UCI");
        mCharacteristics.put("00002bb5-0000-1000-8000-00805f9b34fb", "Bearer Technology");
        mCharacteristics.put("00002bb6-0000-1000-8000-00805f9b34fb", "Bearer URI Schemes Supported List");
        mCharacteristics.put("00002bb7-0000-1000-8000-00805f9b34fb", "Bearer Signal Strength");
        mCharacteristics.put("00002bb8-0000-1000-8000-00805f9b34fb", "Bearer Signal Strength Reporting Interval");
        mCharacteristics.put("00002bb9-0000-1000-8000-00805f9b34fb", "Bearer List Current Calls");
        mCharacteristics.put("00002bba-0000-1000-8000-00805f9b34fb", "Content Control ID");
        mCharacteristics.put("00002bbb-0000-1000-8000-00805f9b34fb", "Status Flags");
        mCharacteristics.put("00002bbc-0000-1000-8000-00805f9b34fb", "Incoming Call Target Bearer URI");
        mCharacteristics.put("00002bbd-0000-1000-8000-00805f9b34fb", "Call State");
        mCharacteristics.put("00002bbe-0000-1000-8000-00805f9b34fb", "Call Control Point");
        mCharacteristics.put("00002bbf-0000-1000-8000-00805f9b34fb", "Call Control Point Optional Opcodes");
        mCharacteristics.put("00002bc0-0000-1000-8000-00805f9b34fb", "Termination Reason");
        mCharacteristics.put("00002bc1-0000-1000-8000-00805f9b34fb", "Incoming Call");
        mCharacteristics.put("00002bc2-0000-1000-8000-00805f9b34fb", "Call Friendly Name");
        mCharacteristics.put("00002bc3-0000-1000-8000-00805f9b34fb", "Mute");
        mCharacteristics.put("00002bc4-0000-1000-8000-00805f9b34fb", "Sink ASE");
        mCharacteristics.put("00002bc5-0000-1000-8000-00805f9b34fb", "Source ASE");
        mCharacteristics.put("00002bc6-0000-1000-8000-00805f9b34fb", "ASE Control Point");
        mCharacteristics.put("00002bc7-0000-1000-8000-00805f9b34fb", "Broadcast Audio Scan Control Point");
        mCharacteristics.put("00002bc8-0000-1000-8000-00805f9b34fb", "Broadcast Receive State");
        mCharacteristics.put("00002bc9-0000-1000-8000-00805f9b34fb", "Sink PAC");
        mCharacteristics.put("00002bca-0000-1000-8000-00805f9b34fb", "Sink Audio Locations");
        mCharacteristics.put("00002bcb-0000-1000-8000-00805f9b34fb", "Source PAC");
        mCharacteristics.put("00002bcc-0000-1000-8000-00805f9b34fb", "Source Audio Locations");
        mCharacteristics.put("00002bcd-0000-1000-8000-00805f9b34fb", "Available Audio Contexts");
        mCharacteristics.put("00002bce-0000-1000-8000-00805f9b34fb", "Supported Audio Contexts");
        mCharacteristics.put("00002bcf-0000-1000-8000-00805f9b34fb", "Ammonia Concentration");
        mCharacteristics.put("00002bd0-0000-1000-8000-00805f9b34fb", "Carbon Monoxide Concentration");
        mCharacteristics.put("00002bd1-0000-1000-8000-00805f9b34fb", "Methane Concentration");
        mCharacteristics.put("00002bd2-0000-1000-8000-00805f9b34fb", "Nitrogen Dioxide Concentration");
        mCharacteristics.put("00002bd3-0000-1000-8000-00805f9b34fb", "Non-Methane Volatile Organic Compounds Concentration");
        mCharacteristics.put("00002bd4-0000-1000-8000-00805f9b34fb", "Ozone Concentration");
        mCharacteristics.put("00002bd5-0000-1000-8000-00805f9b34fb", "Particulate Matter - PM1 Concentration");
        mCharacteristics.put("00002bd6-0000-1000-8000-00805f9b34fb", "Particulate Matter - PM2.5 Concentration");
        mCharacteristics.put("00002bd7-0000-1000-8000-00805f9b34fb", "Particulate Matter - PM10 Concentration");
        mCharacteristics.put("00002bd8-0000-1000-8000-00805f9b34fb", "Sulfur Dioxide Concentration");
        mCharacteristics.put("00002bd9-0000-1000-8000-00805f9b34fb", "Sulfur Hexafluoride Concentration");
        mCharacteristics.put("00002bda-0000-1000-8000-00805f9b34fb", "Hearing Aid Features");
        mCharacteristics.put("00002bdb-0000-1000-8000-00805f9b34fb", "Hearing Aid Preset Control Point");
        mCharacteristics.put("00002bdc-0000-1000-8000-00805f9b34fb", "Active Preset Index");
        mCharacteristics.put("00002bdd-0000-1000-8000-00805f9b34fb", "Stored Health Observations");
        mCharacteristics.put("00002bde-0000-1000-8000-00805f9b34fb", "Fixed String 64");
        mCharacteristics.put("00002bdf-0000-1000-8000-00805f9b34fb", "High Temperature");
        mCharacteristics.put("00002be0-0000-1000-8000-00805f9b34fb", "High Voltage");
        mCharacteristics.put("00002be1-0000-1000-8000-00805f9b34fb", "Light Distribution");
        mCharacteristics.put("00002be2-0000-1000-8000-00805f9b34fb", "Light Output");
        mCharacteristics.put("00002be3-0000-1000-8000-00805f9b34fb", "Light Source Type");
        mCharacteristics.put("00002be4-0000-1000-8000-00805f9b34fb", "Noise");
        mCharacteristics.put("00002be5-0000-1000-8000-00805f9b34fb", "Relative Runtime in a Correlated Color Temperature Range");
        mCharacteristics.put("00002be6-0000-1000-8000-00805f9b34fb", "Time Second 32");
        mCharacteristics.put("00002be7-0000-1000-8000-00805f9b34fb", "VOC Concentration");
        mCharacteristics.put("00002be8-0000-1000-8000-00805f9b34fb", "Voltage Frequency");
        mCharacteristics.put("00002be9-0000-1000-8000-00805f9b34fb", "Battery Critical Status");
        mCharacteristics.put("00002bea-0000-1000-8000-00805f9b34fb", "Battery Health Status");
        mCharacteristics.put("00002beb-0000-1000-8000-00805f9b34fb", "Battery Health Information");
        mCharacteristics.put("00002bec-0000-1000-8000-00805f9b34fb", "Battery Information");
        mCharacteristics.put("00002bed-0000-1000-8000-00805f9b34fb", "Battery Level Status");
        mCharacteristics.put("00002bee-0000-1000-8000-00805f9b34fb", "Battery Time Status");
        mCharacteristics.put("00002bef-0000-1000-8000-00805f9b34fb", "Estimated Service Date");
        mCharacteristics.put("00002bf0-0000-1000-8000-00805f9b34fb", "Battery Energy Status");
        mCharacteristics.put("00002bf1-0000-1000-8000-00805f9b34fb", "Observation Schedule Changed");
        mCharacteristics.put("00002bf2-0000-1000-8000-00805f9b34fb", "Current Elapsed Time");
        mCharacteristics.put("00002bf3-0000-1000-8000-00805f9b34fb", "Health Sensor Features");
        mCharacteristics.put("00002bf4-0000-1000-8000-00805f9b34fb", "GHS Control Point");
        mCharacteristics.put("00002bf5-0000-1000-8000-00805f9b34fb", "LE GATT Security Levels");
        mCharacteristics.put("00002bf6-0000-1000-8000-00805f9b34fb", "ESL Address");
        mCharacteristics.put("00002bf7-0000-1000-8000-00805f9b34fb", "AP Sync Key Material");
        mCharacteristics.put("00002bf8-0000-1000-8000-00805f9b34fb", "ESL Response Key Material");
        mCharacteristics.put("00002bf9-0000-1000-8000-00805f9b34fb", "ESL Current Absolute Time");
        mCharacteristics.put("00002bfa-0000-1000-8000-00805f9b34fb", "ESL Display Information");
        mCharacteristics.put("00002bfb-0000-1000-8000-00805f9b34fb", "ESL Image Information");
        mCharacteristics.put("00002bfc-0000-1000-8000-00805f9b34fb", "ESL Sensor Information");
        mCharacteristics.put("00002bfd-0000-1000-8000-00805f9b34fb", "ESL LED Information");
        mCharacteristics.put("00002bfe-0000-1000-8000-00805f9b34fb", "ESL Control Point");
        mCharacteristics.put("00002bff-0000-1000-8000-00805f9b34fb", "UDI for Medical Devices");
        mCharacteristics.put("00002c00-0000-1000-8000-00805f9b34fb", "GMAP Role");
        mCharacteristics.put("00002c01-0000-1000-8000-00805f9b34fb", "UGG Features");
        mCharacteristics.put("00002c02-0000-1000-8000-00805f9b34fb", "UGT Features");
        mCharacteristics.put("00002c03-0000-1000-8000-00805f9b34fb", "BGS Features");
        mCharacteristics.put("00002c04-0000-1000-8000-00805f9b34fb", "BGR Features");
        mCharacteristics.put("00002c05-0000-1000-8000-00805f9b34fb", "Percentage 8 Steps");
        mCharacteristics.put("00002c06-0000-1000-8000-00805f9b34fb", "Acceleration");
        mCharacteristics.put("00002c07-0000-1000-8000-00805f9b34fb", "Force");
        mCharacteristics.put("00002c08-0000-1000-8000-00805f9b34fb", "Linear Position");
        mCharacteristics.put("00002c09-0000-1000-8000-00805f9b34fb", "Rotational Speed");
        mCharacteristics.put("00002c0a-0000-1000-8000-00805f9b34fb", "Length");
        mCharacteristics.put("00002c0b-0000-1000-8000-00805f9b34fb", "Torque");
        mCharacteristics.put("00002c0c-0000-1000-8000-00805f9b34fb", "IMD Status");
        mCharacteristics.put("00002c0d-0000-1000-8000-00805f9b34fb", "IMDS Descriptor Value Changed");
        mCharacteristics.put("00002c0e-0000-1000-8000-00805f9b34fb", "First Use Date");
        mCharacteristics.put("00002c0f-0000-1000-8000-00805f9b34fb", "Life Cycle Data");
        mCharacteristics.put("00002c10-0000-1000-8000-00805f9b34fb", "Work Cycle Data");
        mCharacteristics.put("00002c11-0000-1000-8000-00805f9b34fb", "Service Cycle Data");
        mCharacteristics.put("00002c12-0000-1000-8000-00805f9b34fb", "IMD Control");
        mCharacteristics.put("00002c13-0000-1000-8000-00805f9b34fb", "IMD Historical Data");
        mCharacteristics.put("00002c14-0000-1000-8000-00805f9b34fb", "RAS Features");
        mCharacteristics.put("00002c15-0000-1000-8000-00805f9b34fb", "Real-time Ranging Data");
        mCharacteristics.put("00002c16-0000-1000-8000-00805f9b34fb", "On-demand Ranging Data");
        mCharacteristics.put("00002c17-0000-1000-8000-00805f9b34fb", "RAS Control Point");
        mCharacteristics.put("00002c18-0000-1000-8000-00805f9b34fb", "Ranging Data Ready");
        mCharacteristics.put("00002c19-0000-1000-8000-00805f9b34fb", "Ranging Data Overwritten");
        mCharacteristics.put("00002c1b-0000-1000-8000-00805f9b34fb", "Humidity 8");
        mCharacteristics.put("00002c1c-0000-1000-8000-00805f9b34fb", "Illuminance 16");
        mCharacteristics.put("00002c1d-0000-1000-8000-00805f9b34fb", "Acceleration 3D");
        mCharacteristics.put("00002c1e-0000-1000-8000-00805f9b34fb", "Precise Acceleration 3D");
        mCharacteristics.put("00002c1f-0000-1000-8000-00805f9b34fb", "Acceleration Detection Status");
        mCharacteristics.put("00002c20-0000-1000-8000-00805f9b34fb", "Door/Window Status");
        mCharacteristics.put("00002c21-0000-1000-8000-00805f9b34fb", "Pushbutton Status 8");
        mCharacteristics.put("00002c22-0000-1000-8000-00805f9b34fb", "Contact Status 8");
        mCharacteristics.put("00002c23-0000-1000-8000-00805f9b34fb", "HID ISO Properties");
        mCharacteristics.put("00002c24-0000-1000-8000-00805f9b34fb", "LE HID Operation Mode");

        mCharacteristics.put("14702856-620a-3973-7c78-9cfff0876abd", "(Propr: HPLUS Control)");
        mCharacteristics.put("14702853-620a-3973-7c78-9cfff0876abd", "(Propr: HPLUS Measurements)");
        mCharacteristics.put("df334c80-e6a7-d082-274d-78fc66f85e16", "(Propr: Garmin GFDI V0 TX)");
        mCharacteristics.put("4acbcd28-7425-868e-f447-915c8f00d0cb", "(Propr: Garmin GFDI V0 RX)");
        mCharacteristics.put("6a4e4c80-667b-11e3-949a-0800200c9a66", "(Propr: Garmin GFDI V1 TX)");
        mCharacteristics.put("6a4ecd28-667b-11e3-949a-0800200c9a66", "(Propr: Garmin GFDI V1 RX)");
        mCharacteristics.put("6a4e2810-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML 0 RX)");
        mCharacteristics.put("6a4e2820-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML 0 TX)");
        mCharacteristics.put("6a4e2811-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML 1 RX)");
        mCharacteristics.put("6a4e2821-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML 1 TX)");
        mCharacteristics.put("6a4e2812-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML 2 RX)");
        mCharacteristics.put("6a4e2822-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML 2 TX)");
        mCharacteristics.put("6a4e2813-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML 3 RX)");
        mCharacteristics.put("6a4e2823-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML 3 TX)");
        mCharacteristics.put("6a4e2814-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML 4 RX)");
        mCharacteristics.put("6a4e2824-667b-11e3-949a-0800200c9a66", "(Propr: Garmin ML 4 TX)");
        mCharacteristics.put("00000051-0000-1000-8000-00805f9b34fb", "(Propr: Xiaomi V1 Command Read)");
        mCharacteristics.put("00000052-0000-1000-8000-00805f9b34fb", "(Propr: Xiaomi V1 Command Write)");
        mCharacteristics.put("00000053-0000-1000-8000-00805f9b34fb", "(Propr: Xiaomi V1 Activity Data)");
        mCharacteristics.put("00000055-0000-1000-8000-00805f9b34fb", "(Propr: Xiaomi V1 Data Upload)");
        mCharacteristics.put("16186f01-0000-1000-8000-00807f9b34fb", "(Propr: Xiaomi V1 Command Read)");
        mCharacteristics.put("16186f02-0000-1000-8000-00807f9b34fb", "(Propr: Xiaomi V1 Command Write)");
        mCharacteristics.put("16186f03-0000-1000-8000-00807f9b34fb", "(Propr: Xiaomi V1 Activity Data)");
        mCharacteristics.put("16186f04-0000-1000-8000-00807f9b34fb", "(Propr: Xiaomi V1 Data Upload)");
        mCharacteristics.put("16187f02-0000-1000-8000-00807f9b34fb", "(Propr: Xiaomi V1 Command Read)");
        mCharacteristics.put("16187f01-0000-1000-8000-00807f9b34fb", "(Propr: Xiaomi V1 Command Write)");
        mCharacteristics.put("16187f03-0000-1000-8000-00807f9b34fb", "(Propr: Xiaomi V1 Activity Data)");
        mCharacteristics.put("16187f04-0000-1000-8000-00807f9b34fb", "(Propr: Xiaomi V1 Data Upload)");
        mCharacteristics.put("1314f005-1000-9000-7000-301291e21220", "(Propr: Xiaomi V1 Command Read)");
        mCharacteristics.put("1314f001-1000-9000-7000-301291e21220", "(Propr: Xiaomi V1 Command Write)");
        mCharacteristics.put("1314f002-1000-9000-7000-301291e21220", "(Propr: Xiaomi V1 Activity Data)");
        mCharacteristics.put("1314f007-1000-9000-7000-301291e21220", "(Propr: Xiaomi V1 Data Upload)");
        mCharacteristics.put("74950002-a7f3-424b-92dd-4a006a3aef56", "(Propr: Xiaomi V1 Command Read)");
        mCharacteristics.put("74950001-a7f3-424b-92dd-4a006a3aef56", "(Propr: Xiaomi V1 Command Write)");
        mCharacteristics.put("74950003-a7f3-424b-92dd-4a006a3aef56", "(Propr: Xiaomi V1 Activity Data)");
        mCharacteristics.put("0000005e-0000-1000-8000-00805f9b34fb", "(Propr: Xiaomi V2 RX)");
        mCharacteristics.put("0000005f-0000-1000-8000-00805f9b34fb", "(Propr: Xiaomi V2 TX)");
        mCharacteristics.put("0000fff1-0000-1000-8000-00805f9b34fb", "(Propr: Nothing CMF Command Read");
        mCharacteristics.put("0000fff2-0000-1000-8000-00805f9b34fb", "(Propr: Nothing CMF Command Write");
        mCharacteristics.put("02f00000-0000-0000-0000-00000000ffe1", "(Propr: Nothing CMF Data Write");
        mCharacteristics.put("02f00000-0000-0000-0000-00000000ffe2", "(Propr: Nothing CMF Data Read");
        mCharacteristics.put("77d4ff01-2fe2-2334-0d35-9ccd078f529c", "(Propr: Nothing CMF Shell Write");
        mCharacteristics.put("77d4ff02-2fe2-2334-0d35-9ccd078f529c", "(Propr: Nothing CMF Shell Read");
        mCharacteristics.put("02f00000-0000-0000-0000-00000000ff01", "(Propr: Nothing CMF Firmware Write");
        mCharacteristics.put("02f00000-0000-0000-0000-00000000ff02", "(Propr: Nothing CMF Firmware Read");
        mCharacteristics.put("000033f1-0000-1000-8000-00805f9b34fb", "(Propr: GloryFit Command Write");
        mCharacteristics.put("000033f2-0000-1000-8000-00805f9b34fb", "(Propr: GloryFit Command Read");
        mCharacteristics.put("000034f1-0000-1000-8000-00805f9b34fb", "(Propr: GloryFit Data Write");
        mCharacteristics.put("000034f2-0000-1000-8000-00805f9b34fb", "(Propr: GloryFit Data Read");
        mCharacteristics.put("00010203-0405-0607-0809-0a0b0c0d2b12", "(Propr: Telink OTA Write)");
        mCharacteristics.put("ebe0ccb7-7a0a-4b0c-8a1a-6ff2997da3a6", "(Propr: Lywsd TIME)");
        mCharacteristics.put("ebe0ccc4-7a0a-4b0c-8a1a-6ff2997da3a6", "(Propr: Lywsd BATTERY)");
        mCharacteristics.put("ebe0ccbe-7a0a-4b0c-8a1a-6ff2997da3a6", "(Propr: Lywsd TEMPERATURE_UNIT)");
        mCharacteristics.put("ebe0ccd8-7a0a-4b0c-8a1a-6ff2997da3a6", "(Propr: Lywsd CONN_INTERVAL)");
        mCharacteristics.put("ebe0ccbc-7a0a-4b0c-8a1a-6ff2997da3a6", "(Propr: Lywsd HISTORY)");
        mCharacteristics.put("ebe0ccc1-7a0a-4b0c-8a1a-6ff2997da3a6", "(Propr: Lywsd LIVE_DATA)");
        mCharacteristics.put("ebe0ccba-7a0a-4b0c-8a1a-6ff2997da3a6", "(Propr: Lywsd HISTORY_LAST_ID)");
        mCharacteristics.put("00000001-0000-3512-2118-0009af100700", "(Propr: Huami Raw Sensor Control)");
        mCharacteristics.put("00000002-0000-3512-2118-0009af100700", "(Propr: Huami Raw Sensor Data)");
        mCharacteristics.put("00000003-0000-3512-2118-0009af100700", "(Propr: Huami Configuration)");
        mCharacteristics.put("00000004-0000-3512-2118-0009af100700", "(Propr: Huami Activity Control)");
        mCharacteristics.put("00000005-0000-3512-2118-0009af100700", "(Propr: Huami Activity Data)");
        mCharacteristics.put("00000006-0000-3512-2118-0009af100700", "(Propr: Huami Battery Info)");
        mCharacteristics.put("00000007-0000-3512-2118-0009af100700", "(Propr: Huami Realtime Steps)");
        mCharacteristics.put("00000008-0000-3512-2118-0009af100700", "(Propr: Huami User Settings)");
        mCharacteristics.put("00000009-0000-3512-2118-0009af100700", "(Propr: Huami Auth)");
        mCharacteristics.put("0000000f-0000-3512-2118-0009af100700", "(Propr: Huami Workout)");
        mCharacteristics.put("00000010-0000-3512-2118-0009af100700", "(Propr: Huami Device Event)");
        mCharacteristics.put("00000012-0000-3512-2118-0009af100700", "(Propr: Huami Audio Control)");
        mCharacteristics.put("00000013-0000-3512-2118-0009af100700", "(Propr: Huami Audio Data)");
        mCharacteristics.put("00000016-0000-3512-2118-0009af100700", "(Propr: Huami 2021 Chunked Write)");
        mCharacteristics.put("00000017-0000-3512-2118-0009af100700", "(Propr: Huami 2021 Chunked Read)");
        mCharacteristics.put("00000020-0000-3512-2118-0009af100700", "(Propr: Huami Chunked Transfer)");
        mCharacteristics.put("00000023-0000-3512-2118-0009af100700", "(Propr: Zepp OS File Transfer V3 Send)");
        mCharacteristics.put("00000024-0000-3512-2118-0009af100700", "(Propr: Zepp OS File Transfer V3 Receive)");
        mCharacteristics.put("00001531-0000-3512-2118-0009af100700", "(Propr: Huami Firmware Control)");
        mCharacteristics.put("00001532-0000-3512-2118-0009af100700", "(Propr: Huami Firmware Data)");
        mCharacteristics.put("86f61001-f706-58a0-95b2-1fb9261e4dc7", "(Propr: Ultrahuman State)");
        mCharacteristics.put("86f65001-f706-58a0-95b2-1fb9261e4dc7", "(Propr: Ultrahuman Command)");
        mCharacteristics.put("86f65002-f706-58a0-95b2-1fb9261e4dc7", "(Propr: Ultrahuman Response)");
        mCharacteristics.put("86f66001-f706-58a0-95b2-1fb9261e4dc7", "(Propr: Ultrahuman Data)");
        mCharacteristics.put("da2e7828-fbce-4e01-ae9e-261174997c48", "(Propr: SMP - Simple Management Protocol)");
        mCharacteristics.put("6e400002-b5a3-f393-e0a9-e50e24dcca9e", "(Propr: Nordic UART TX)");
        mCharacteristics.put("6e400003-b5a3-f393-e0a9-e50e24dcca9e", "(Propr: Nordic UART RX)");
        mCharacteristics.put("de5bf729-d711-4e47-af26-65e3012a5dc7", "(Propr: Yawell Notify)");
        mCharacteristics.put("de5bf72a-d711-4e47-af26-65e3012a5dc7", "(Propr: Yawell Write)");

        mValueFormats.put(52, "32bit float");
        mValueFormats.put(50, "16bit float");
        mValueFormats.put(34, "16bit signed int");
        mValueFormats.put(36, "32bit signed int");
        mValueFormats.put(33, "8bit signed int");
        mValueFormats.put(18, "16bit unsigned int");
        mValueFormats.put(20, "32bit unsigned int");
        mValueFormats.put(17, "8bit unsigned int");

        // lets add also couple appearance string description
        // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.gap.appearance.xml
        mAppearance.put(833, "Heart Rate Sensor: Belt");
        mAppearance.put(832, "Generic Heart Rate Sensor");
        mAppearance.put(0, "Unknown");
        mAppearance.put(64, "Generic Phone");
        mAppearance.put(1157, "Cycling: Speed and Cadence Sensor");
        mAppearance.put(1152, "General Cycling");
        mAppearance.put(1153, "Cycling Computer");
        mAppearance.put(1154, "Cycling: Speed Sensor");
        mAppearance.put(1155, "Cycling: Cadence Sensor");
        mAppearance.put(1156, "Cycling: Speed and Cadence Sensor");
        mAppearance.put(1157, "Cycling: Power Sensor");

        mHeartRateSensorLocation.put(0, "Other");
        mHeartRateSensorLocation.put(1, "Chest");
        mHeartRateSensorLocation.put(2, "Wrist");
        mHeartRateSensorLocation.put(3, "Finger");
        mHeartRateSensorLocation.put(4, "Hand");
        mHeartRateSensorLocation.put(5, "Ear Lobe");
        mHeartRateSensorLocation.put(6, "Foot");
    }
}
