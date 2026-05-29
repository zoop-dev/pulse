package nodomain.freeyourgadget.gadgetbridge.service.devices.keephealth;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NotificationQueue {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationQueue.class);
    private boolean isRunning = false;
    private final List<NotificationItem> queue = new ArrayList<>();


    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void addToQueue(NotificationItem item) {
        queue.add(item);
    }

    public NotificationItem get() {
        return queue.get(0);
    }

    public void remove() {
        if (!queue.isEmpty()) {
            queue.remove(0);
        }
    }

    public void finish() {
        remove();
        setRunning(false);
        LOG.debug(this.toString());
    }

    public void empty() {
        queue.clear();
        setRunning(false);
        LOG.debug(this.toString());
    }

    @NonNull
    @Override
    public String toString() {
        return "isRunning: " + isRunning + " queueLength: " + queue.toArray().length;
    }
}
