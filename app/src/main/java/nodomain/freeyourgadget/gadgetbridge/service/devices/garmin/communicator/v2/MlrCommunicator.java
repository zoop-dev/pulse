package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v2;

import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * As per <a href="https://gadgetbridge.org/internals/specifics/garmin-protocol/#multi-link-reliable-protocol">the docs.</a>
 */
public class MlrCommunicator {
    private final Logger LOG;

    public static final int MLR_FLAG_MASK = 0x80;
    public static final int HANDLE_MASK = 0x70;
    public static final int HANDLE_SHIFT = 4;
    public static final int REQ_NUM_MASK = 0x0F;
    public static final int SEQ_NUM_MASK = 0x3F;

    private static final int MAX_SEQ_NUM = 0x3F;
    private static final int INITIAL_MAX_UNACKED_SEND = 0x20;
    private static final int MAX_RETRANSMISSION_TIMEOUT = 20000;
    private static final int INITIAL_RETRANSMISSION_TIMEOUT = 1000;
    private static final int ACK_TIMEOUT = 250;
    private static final int ACK_TRIGGER_THRESHOLD = 5;

    private final int handle;
    private int maxPacketSize;

    private int lastSendAck = 0x00;
    private int nextSendSeq = 0x00;
    private int nextRcvSeq = 0x00;
    private int lastRcvAck = 0x00;
    private int maxNumUnackedSend = INITIAL_MAX_UNACKED_SEND;
    private int retransmissionTimeout = INITIAL_RETRANSMISSION_TIMEOUT;

    private final LinkedList<Fragment> fragmentQueue = new LinkedList<>();
    private final Fragment[] sentFragments = new Fragment[MAX_SEQ_NUM + 1];

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable ackRunnable = this::sendAckPacket;
    private final Runnable retransmissionRunnable = this::onRetransmissionTimeout;

    private final MessageSender messageSender;
    private final MessageReceiver messageReceiver;

    public MlrCommunicator(final int handle,
                           final int maxPacketSize,
                           final MessageSender messageSender,
                           final MessageReceiver messageReceiver) {
        this.LOG = LoggerFactory.getLogger(MlrCommunicator.class.getName() + "(" + handle + ")");
        this.handle = handle;
        this.maxPacketSize = maxPacketSize;
        this.messageSender = messageSender;
        this.messageReceiver = messageReceiver;
    }

    public void setMaxPacketSize(final int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public void sendMessage(final String taskName, final byte[] message) {
        if (message == null || message.length == 0) {
            return;
        }

        LOG.debug(
                "Queuing MLR message for '{}' ({} bytes)",
                taskName,
                message.length
        );

        int remainingBytes = message.length;
        int i = 0;
        if (remainingBytes > maxPacketSize - 2) {
            int position = 0;
            while (remainingBytes > 0) {
                final byte[] fragment = Arrays.copyOfRange(message, position, position + Math.min(remainingBytes, maxPacketSize - 2));
                fragmentQueue.add(new Fragment(taskName, i++, fragment));
                position += fragment.length;
                remainingBytes -= fragment.length;
            }
        } else {
            fragmentQueue.add(new Fragment(taskName, 0, message));
        }

        runProtocol();
    }

    public void onPacketReceived(final byte[] packet) {
        if (packet.length < 2) {
            LOG.warn("MLR packet too short: {}", packet.length);
            return;
        }

        // MLR header
        final int byte0 = packet[0] & 0xFF;
        final int byte1 = packet[1] & 0xFF;

        if ((byte0 & MLR_FLAG_MASK) == 0) {
            LOG.error("Received non-MLR packet");
            return;
        }

        final int packetHandle = (byte0 & HANDLE_MASK) >> HANDLE_SHIFT;
        final int reqNum = ((byte0 & REQ_NUM_MASK) << 2) | ((byte1 >> 6) & 0x03);
        final int seqNum = byte1 & SEQ_NUM_MASK;

        if (packetHandle != (handle & 0x07)) {
            LOG.error("MLR packet for wrong handle: expected {}, got {}", handle & 0x07, packetHandle);
            return;
        }

        LOG.debug(
                "MLR packet received: reqNum={}, seqNum={}, dataLen={}",
                reqNum,
                seqNum,
                packet.length - 2
        );

        // Process ACK if request number changed
        if (reqNum != lastRcvAck) {
            processAck(reqNum);
        }

        // Process data if any, and in sequence
        if (packet.length > 2) {
            if (seqNum == nextRcvSeq) {
                // In-sequence packet
                final byte[] data = Arrays.copyOfRange(packet, 2, packet.length);
                try {
                    messageReceiver.onDataReceived(data);
                } catch (final Exception e) {
                    LOG.error("Receiver failed to handle MLR data", e);
                }

                nextRcvSeq = (nextRcvSeq + 1) % (MAX_SEQ_NUM + 1);

                scheduleAckIfNeeded();
            } else {
                LOG.warn("Out-of-sequence packet - expected {}, got {}", nextRcvSeq, seqNum);
                // Correct sequence will be retransmitted by sender
                // Regardless, re-send the expected ack since the sender shouldn't be sending these
                sendAckPacket();
            }
        }

        runProtocol();
    }

    private void processAck(final int reqNum) {
        final int numAcked = (reqNum - lastRcvAck + MAX_SEQ_NUM + 1) % (MAX_SEQ_NUM + 1);
        final int numUnacked = (nextSendSeq - lastRcvAck + MAX_SEQ_NUM + 1) % (MAX_SEQ_NUM + 1);

        LOG.debug(
                "Processing ACK: reqNum={}, numAcked={}, numUnacked={}, will expire fragments [{}, {}]",
                reqNum,
                numAcked,
                numUnacked,
                lastRcvAck,
                reqNum - 1
        );

        // Stop retransmission timer
        timeoutHandler.removeCallbacks(retransmissionRunnable);

        // Remove acked messages from the array
        for (int i = lastRcvAck; i != reqNum; i = (i + 1) % (MAX_SEQ_NUM + 1)) {
            if (sentFragments[i] == null) {
                LOG.error("Attempting to expire null fragment at index {}", i);
            }
            sentFragments[i] = null;
        }

        lastRcvAck = reqNum;

        // Restart retransmission timer if there are still unacked packets
        if (lastRcvAck != nextSendSeq) {
            startRetransmissionTimer();
        }
    }

    private void scheduleAckIfNeeded() {
        timeoutHandler.removeCallbacks(ackRunnable);

        final int numRcvdUnacked = (nextRcvSeq - lastSendAck + MAX_SEQ_NUM + 1) % (MAX_SEQ_NUM + 1);
        if (numRcvdUnacked >= ACK_TRIGGER_THRESHOLD) {
            sendAckPacket();
        } else {
            timeoutHandler.postDelayed(ackRunnable, ACK_TIMEOUT);
            LOG.debug("Started ack timer: {}ms", ACK_TIMEOUT);
        }
    }

    private void sendAckPacket() {
        timeoutHandler.removeCallbacks(ackRunnable);

        // Send ACK-only packet (no data)
        byte[] packet = createPacket(nextRcvSeq, 0, new byte[0]);
        messageSender.sendPacket("ack reqNum=" + nextRcvSeq, packet);
        lastSendAck = nextRcvSeq;
        LOG.debug("Sent ACK packet: reqNum={}", nextRcvSeq);
    }

    private void runProtocol() {
        // Check if we can send more packets
        final int numSentUnacked = (nextSendSeq - lastRcvAck + MAX_SEQ_NUM + 1) % (MAX_SEQ_NUM + 1);

        if (numSentUnacked >= maxNumUnackedSend) {
            LOG.debug("Cannot send more packets: {} unacked, max {}", numSentUnacked, maxNumUnackedSend);
            return;
        }

        // Send next fragment if available
        final Fragment fragment = fragmentQueue.poll();
        if (fragment != null) {
            final byte[] packet = createPacket(nextRcvSeq, nextSendSeq, fragment.data);
            messageSender.sendPacket(fragment.taskName + " (" + fragment.num + ")", packet);
            sentFragments[nextSendSeq] = fragment;

            nextSendSeq = (nextSendSeq + 1) % (MAX_SEQ_NUM + 1);

            // Start retransmission timer if this is the first unacked packet
            if (numSentUnacked == 0) {
                startRetransmissionTimer();
            }

            LOG.debug("Sent MLR packet: seqNum={}, dataLen={}", (nextSendSeq - 1 + MAX_SEQ_NUM + 1) % (MAX_SEQ_NUM + 1), fragment.data.length);
        }
    }

    private byte[] createPacket(final int reqNum, final int seqNum, final byte[] data) {
        byte[] packet = new byte[2 + data.length];

        // First byte: MLR flag (1) + handle (3 bits) + reqNum high bits (4 bits)
        packet[0] = (byte) (MLR_FLAG_MASK | ((handle & 0x07) << HANDLE_SHIFT) | ((reqNum >> 2) & REQ_NUM_MASK));

        // Second byte: reqNum low bits (2 bits) + seqNum (6 bits)
        packet[1] = (byte) (((reqNum & 0x03) << 6) | (seqNum & SEQ_NUM_MASK));

        // Data
        System.arraycopy(data, 0, packet, 2, data.length);

        return packet;
    }

    private void startRetransmissionTimer() {
        timeoutHandler.removeCallbacks(retransmissionRunnable);
        timeoutHandler.postDelayed(retransmissionRunnable, retransmissionTimeout);

        LOG.debug("Started retransmission timer: {}ms", retransmissionTimeout);
    }

    private void onRetransmissionTimeout() {
        LOG.debug("Retransmission timeout expired");

        // Backoff retransmission timeout and reduce the maximum unacked
        retransmissionTimeout = Math.min(retransmissionTimeout * 2, MAX_RETRANSMISSION_TIMEOUT);
        maxNumUnackedSend = Math.max(1, maxNumUnackedSend / 2);

        LOG.debug(
                "Retransmission: timeout={}ms, maxUnacked={}, will re-send fragments [{}, {}]",
                retransmissionTimeout,
                maxNumUnackedSend,
                lastRcvAck,
                nextSendSeq - 1
        );

        for (int i = lastRcvAck; i != nextSendSeq; i = (i + 1) % (MAX_SEQ_NUM + 1)) {
            LOG.debug("Re-sending fragment {}", i);
            final Fragment fragment = sentFragments[i];
            if (fragment == null) {
                LOG.error("Attempting to re-send null fragment at index {}", i);
                continue;
            }
            final byte[] packet = createPacket(nextRcvSeq, i, fragment.data);
            messageSender.sendPacket("retransmission " + fragment.taskName + " (" + fragment.num + ")", packet);
        }

        startRetransmissionTimer();
    }

    public void close() {
        LOG.debug("Closing MLR communicator");

        timeoutHandler.removeCallbacksAndMessages(null);

        fragmentQueue.clear();
    }

    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        if (newState != BluetoothGatt.STATE_CONNECTED) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
    }

    public interface MessageSender {
        void sendPacket(String taskName, byte[] packet);
    }

    public interface MessageReceiver {
        void onDataReceived(final byte[] data);
    }

    private record Fragment(String taskName, int num, byte[] data) {
    }
}
