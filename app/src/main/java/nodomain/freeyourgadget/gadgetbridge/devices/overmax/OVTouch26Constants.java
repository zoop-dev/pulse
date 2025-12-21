/*  Copyright (C) 2019-2025 vappster

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
package nodomain.freeyourgadget.gadgetbridge.devices.overmax;

import java.util.UUID;

public final class OVTouch26Constants {

    public static final UUID UUID_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_REPORT = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    //Packet structure (commands)
    //ab 00 (payload length) (payload crc) (packet seq#) (unknown) 00 (cmd) 00 [args]
    //                                                    ^--------- payload --------^
    //payload len, payload crc and packet seq# are uint16, unknown and cmd are uint8
    //checksum is crc16/arc and is calculated starting from the (unknown) variable until the end
    //of the packet
    //packet seq# is a counter that increments itself for every packet sent to the device
    //NOTE:
    //Based on the known possible values, the unknown variable here seems to at least control which
    //ack id to use for the command's associated ACK request (see below). However, there may be more
    //to this variable than just that (for example, why do two completely unrelated commands like
    //CMD_FIND_DEVICE and CMD_NOTIFICATION use the same value?) so I'm not *fully* sure of this yet

    public static final byte[] PACKET_CMD_TEMPLATE = {
            (byte) 0xab,
            (byte) 0x00,
            (byte) 0,       //length
            (byte) 0,
            (byte) 0,       //crc16 arc
            (byte) 0,
            (byte) 0,       //packet seq
            (byte) 0x01,
            (byte) 0,       //unknown (see note above)
            (byte) 0x00,
            (byte) 0,       //cmd
            (byte) 0x00
//           ,arguments
    };

    //Packet structure (ACK reply)
    //ab 10 00 00 00 00 (msg id)
    //
    //msg id is uint16 and is given to the client via an ACK request with the following structure:
    //ab 00 00 (ack id) (unknown) (msg id)
    //ack id is uint8 and it seems to be a value used to differentiate which cmd requested the ack
    //unknown is uint16 (doesn't seem to be crc)
    public static final byte[] PACKET_ACK_TEMPLATE = {
            (byte) 0xab,
            (byte) 0x10,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0,       //msg id
            (byte) 0
    };

    public static final byte[] ARGS_NONE = {
            (byte) 0x00
    };

    public static final byte[] ARGS_FIND_DEVICE = {
            (byte) 0x01,
            (byte) 0x01
    };

    public static final byte[] CMD_FIND_DEVICE = new byte[]{(byte) 0x04, 0x61};
    public static final byte[] CMD_GET_STEPS = new byte[]{(byte) 0x05, 0x41};
    public static final byte[] CMD_GET_HEART_RATE = new byte[]{(byte) 0x05, 0x43};
    public static final byte[] CMD_BATTERY_INFO = new byte[]{(byte) 0x02, 0x08};
    public static final byte[] CMD_NOTIFICATION = new byte[]{(byte) 0x04, 0x52};

    //Known reply values
    public static final int REPLY_HEADER_BATTERY = 0x02000900;
    public static final int REPLY_HEADER_STEPS = 0x05004200;
    public static final int REPLY_HEADER_HEART_RATE = 0x05004400;
    public static final int REPLY_HEADER_FIND_MY_PHONE = 0x04006000;
    public static final int REPLY_HEADER_ERROR = 0xe6000000;
    public static final int REPLY_HEADER_ACK = 0xab0000;

    /* A note about pairing and bonding

    When pairing the watch in its proprietary app, the following command & args sent to the watch:
    ------------------------------------------------------------------------------------------------
    public static final byte[] ARGS_BIND = {
            (byte) 0x20,    //arg length, 1-based (excludes itself from length), always 0x20
            (byte) 0xff     //always 0xff
            //The next 31 bytes appear to all always be 0x00
    };

    public static final byte[] CMD_BIND = new byte[]{(byte) 0x03, 0x01};
    ------------------------------------------------------------------------------------------------
    The command will trigger a screen on the device in which the user can choose whether to pair the
    device to the phone. Once the user makes their decision, their choice will get reported to the
    app, which will then proceed accordingly.

    However, on the watch's side, the command's only purpose is exactly that: display the screen and
    return the resulting boolean choice. No other info about either device is sent at any point nor
    any further action is taken on the watch's side. The watch will otherwise allow anyone to
    connect, bond and send/receive commands regardless of whether this screen has been shown before
    (in fact, all commands work even if the device is connected but not bound!). The pairing and
    bonding process is handled entirely by the app via standard BTLE procedures.

    Therefore, the command is not actually needed for the binding process and it only is an extra
    arbitrary step intended specifically for the proprietary app. Due to this, it can be skipped
    entirely and, as Gadgetbridge can effectively give the user additional choices compared to the
    proprietary app via its BONDING_STYLE_ASK setting (for example whether to bond and to add the
    device as a companion) this command is unused in the current implementation. */
}