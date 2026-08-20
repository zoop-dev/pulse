package nodomain.freeyourgadget.gadgetbridge.service.usb

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.widget.Toast
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceState
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.Logger
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

abstract class AbstractUsbDeviceSupport(private val logger: Logger) : AbstractDeviceSupport() {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
    private var readThread: Thread? = null
    private var processThread: Thread? = null

    // Decouples the time-sensitive USB read from whatever onUsbRead() does (parsing, dispatch,
    // disk I/O, ...): the read thread's only job is draining the accessory endpoint as fast as
    // possible into this queue, so a slow consumer can never delay the next read. It's limited,
    // so a consumer that falls behind can't grow this into an unbounded memory leak.
    private val readQueue: BlockingQueue<ByteArray> = ArrayBlockingQueue(READ_QUEUE_CAPACITY)

    private lateinit var usbAccessory: UsbAccessory

    @Volatile
    protected var running = false

    /**
     * Called when the USB device has been connected, before the read thread is started. Support classes must ensure
     * the internal state is reset and ready to handle incoming traffic after this function has finished.
     */
    abstract fun onConnected()

    /**
     * Called by the processing thread, for each chunk in [readQueue].
     */
    abstract fun onUsbRead(data: ByteArray)

    override fun useAutoConnect(): Boolean = false

    /// TODO: Maybe we could?
    override fun canReconnect(): Boolean = false

    override fun setContext(gbDevice: GBDevice, btAdapter: BluetoothAdapter, context: Context) {
        throw IllegalStateException("This is a USB support class")
    }

    override fun setContext(gbDevice: GBDevice, usbAccessory: UsbAccessory, context: Context) {
        super.setContext(gbDevice, usbAccessory, context)
        this.usbAccessory = usbAccessory
    }

    /**
     * By the time this is called, DeviceSupportFactory has already ensured that (1) USB permission
     * is granted and (2) the GBDevice and UsbAccessory serial match - it won't build this support
     * class otherwise.
     */
    override fun connect(): Boolean {
        logger.debug("Connecting to {}", usbAccessory)

        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        evaluateGBDeviceEvent(GBDeviceEventUpdateDeviceState(GBDevice.State.CONNECTING))

        val fd = try {
            manager.openAccessory(usbAccessory)
        } catch (e: Exception) {
            logger.error("Failed to open USB accessory", e)
            evaluateGBDeviceEvent(GBDeviceEventUpdateDeviceState(GBDevice.State.NOT_CONNECTED))
            GB.toast(context, "Failed to connect to USB", Toast.LENGTH_SHORT, GB.ERROR)
            return false
        }
        if (fd == null) {
            logger.error("fd is null")
            evaluateGBDeviceEvent(GBDeviceEventUpdateDeviceState(GBDevice.State.NOT_CONNECTED))
            GB.toast(context, "USB fd is null", Toast.LENGTH_SHORT, GB.ERROR)
            return false
        }

        fileDescriptor = fd
        inputStream = FileInputStream(fd.fileDescriptor)
        outputStream = FileOutputStream(fd.fileDescriptor)

        logger.debug("Connected to {}", usbAccessory)
        evaluateGBDeviceEvent(GBDeviceEventUpdateDeviceState(GBDevice.State.CONNECTED))
        running = true

        // onConnected() must run to completion before the read thread starts. Starting the read thread first
        // leads to race conditions with onConnect resetting state while the read thread is already modifying it.
        onConnected()

        logger.debug("Starting read and process threads")
        val process = Thread({ processLoop() }, "UsbDeviceSupport-process")
        processThread = process
        process.start()

        val thread = Thread({ readLoop() }, "UsbDeviceSupport-read")
        readThread = thread
        thread.start()

        return true
    }

    override fun dispose() {
        logger.debug("Disposing")

        running = false
        readThread?.interrupt()
        processThread?.interrupt()

        try {
            inputStream?.close()
        } catch (e: IOException) {
            logger.warn("Error closing USB input stream", e)
        }
        inputStream = null

        try {
            outputStream?.close()
        } catch (e: IOException) {
            logger.warn("Error closing USB output stream", e)
        }
        outputStream = null

        try {
            fileDescriptor?.close()
        } catch (e: IOException) {
            logger.warn("Error closing USB accessory file descriptor", e)
        }
        fileDescriptor = null

        try {
            readThread?.join(2000)
        } catch (e: Exception) {
            logger.warn("Failed to wait for read thread to finish", e)
        }
        readThread = null

        try {
            processThread?.join(2000)
        } catch (e: Exception) {
            logger.warn("Failed to wait for read thread to finish", e)
        }
        processThread = null

        readQueue.clear()
    }

    protected fun write(taskName: String, data: ByteArray) {
        logger.debug("Writing {} to USB: {}", taskName, GB.hexdump(data))
        val out = outputStream
        if (out == null) {
            logger.error("outputStream is null")
            return
        }
        out.write(data)
        out.flush()
    }

    /**
     * Drains the USB accessory endpoint as fast as possible and hands each chunk off to
     * [readQueue]. Does no parsing or dispatch of its own - anything heavier belongs in
     * [processLoop]/[onUsbRead].
     */
    private fun readLoop() {
        logger.debug("Starting read loop")
        try {
            val stream = inputStream
            if (stream == null) {
                logger.error("Input stream is null")
                return
            }
            val buf = ByteArray(READ_BUFFER_SIZE)
            while (running) {
                val n = stream.read(buf)
                if (n < 0) {
                    logger.warn("USB accessory input stream reached EOF")
                    break
                }
                if (n == 0) continue
                val chunk = buf.copyOf(n)
                if (logger.isTraceEnabled) {
                    logger.trace("Read from USB: {}", GB.hexdump(chunk))
                }
                readQueue.offer(chunk)
            }
        } catch (e: IOException) {
            if (running) {
                logger.error("USB accessory read failed", e)
            } else {
                logger.debug("USB accessory read failed", e)
            }
        } finally {
            logger.debug("Read loop finished")
            if (running) {
                running = false
                // processThread is likely blocked in readQueue.take() - nudge it
                processThread?.interrupt()
                evaluateGBDeviceEvent(GBDeviceEventUpdateDeviceState(GBDevice.State.NOT_CONNECTED))
            }
        }
    }

    /**
     * Consumes [readQueue] and calls [onUsbRead] for each chunk.
     */
    private fun processLoop() {
        logger.debug("Starting process loop")
        try {
            while (running) {
                val chunk = try {
                    readQueue.take()
                } catch (_: InterruptedException) {
                    break
                }
                onUsbRead(chunk)
            }
        } finally {
            logger.debug("Process loop stopped")
        }
    }

    companion object {
        private const val READ_BUFFER_SIZE = 1 * 1024 * 1024

        // Limits how much read-ahead the process thread can fall behind by before chunks start getting dropped.
        private const val READ_QUEUE_CAPACITY = 1024
    }
}
