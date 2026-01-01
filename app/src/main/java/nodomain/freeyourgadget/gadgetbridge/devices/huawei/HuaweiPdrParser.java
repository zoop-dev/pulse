package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class HuaweiPdrParser {

    public static final double DEG_TO_RAD = 0.017453292519943d;

    public static class PdrPoint {
        public int timestamp;
        public double x;
        public double y;
        public double distance;
        public double speed;
        public double yaw;
        public byte quality;

        @NonNull
        @Override
        public String toString() {
            return "PdrPoint{" + "timestamp=" + timestamp +
                    ", x=" + x +
                    ", y=" + y +
                    ", distance=" + distance +
                    ", speed=" + speed +
                    ", yaw=" + yaw +
                    ", quality=" + quality +
                    '}';
        }
    }

    public static PdrPoint[] parseHuaweiPdr(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Skip trim
        buffer.position(0x20);

        int timestamp = buffer.getInt();
        buffer.get();
        buffer.get();

        int time = timestamp;

        double dx = 0;
        double dy = 0;

        ArrayList<PdrPoint> retv = new ArrayList<>(buffer.remaining() / 6);
        while (buffer.remaining() > 6) {
            short time_delta = buffer.getShort();
            short rawYaw = buffer.getShort();
            byte rawDistance = buffer.get();
            byte quality = buffer.get(); //not sure

            time += time_delta;

            double yaw = (double) (rawYaw & 0xFFFF) * 0.01 * DEG_TO_RAD;
            double distance = (double) (rawDistance & 0xFF) * 0.1;

            double x = Math.cos(yaw) * distance;
            double y = Math.sin(yaw) * distance;

            dx = x + dx;
            dy = y + dy;

            double speed = distance / time_delta;

            PdrPoint point = new PdrPoint();
            point.timestamp = time;
            point.x = dx;
            point.y = dy;
            point.distance = distance;
            point.speed = speed;
            point.yaw = yaw;
            point.quality = quality;

            retv.add(point);
        }
        return retv.toArray(new PdrPoint[0]);
    }

}
