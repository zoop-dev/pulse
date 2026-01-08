/*  Copyright (C) 2025 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.adablefs

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.protobuf.LazyStringArrayList.emptyList
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.pinetime.PineTimeJFConstants
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile
import nodomain.freeyourgadget.gadgetbridge.util.GBZipFile
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.roundToInt

class AdaBleFsProfile<T : AbstractBTLESingleDeviceSupport>(support: T) : AbstractBleProfile<T>(support) {

    private val PADDING_BYTE: Byte = 0x00
    private val REQUEST_CONTINUED: Byte = 0x01
    private val REQUEST_WRITE_FILE_START: Byte = 0x20
    private val RESPONSE_WRITE_FILE: Byte = 0x21
    private val REQUEST_WRITE_FILE_DATA: Byte = 0x22
    private val REQUEST_DELETE_FILE: Byte = 0x30
    private val RESPONSE_DELETE_FILE: Byte = 0x31
    private val REQUEST_MAKE_DIRECTORY: Byte = 0x40
    private val RESPONSE_MAKE_DIRECTORY: Byte = 0x41
    private val REQUEST_LIST_DIRECTORY: Byte = 0x50
    private val RESPONSE_LIST_DIRECTORY: Byte = 0x51
    private val REQUEST_MOVE_FILE_DIRECTORY: Byte = 0x60
    private val RESPONSE_MOVE_FILE_DIRECTORY: Byte = 0x61
    private val STATUS_OK: Byte = 0x01

    companion object {
        @JvmField
        val UUID_SERVICE_FS: UUID = UUID.fromString("0000febb-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_FS_VERSION: UUID =
            UUID.fromString("adaf0100-4669-6c65-5472-616e73666572")
        val UUID_CHARACTERISTIC_FS_TRANSFER: UUID =
            UUID.fromString("adaf0200-4669-6c65-5472-616e73666572")

        private val LOG = LoggerFactory.getLogger(AdaBleFsProfile::class.java)
    }

    private var btleQueue: BtLEQueue? = null
    private var currentBuilder: TransactionBuilder? = null
    private var device: GBDevice? = null
    private val adaBleFsQueue: ArrayDeque<AdaBleFsAction> = ArrayDeque()
    private var currentAction: AdaBleFsAction? = null
    private var bytesWritten = 0
    private var bytesProgress = 0
    private var bytesTotal = 0
    private var chunkSize = 235  // Maximum possible within MTU
    private var watchFsContents: MutableList<String> = ArrayList()
    private var listingDirectory: List<String> = emptyList()
    private var allActionsCount = 0
    private var currentActionNr = 0

    fun loadResources(uri: Uri, context: Context, queue: BtLEQueue) {
        btleQueue = queue
        watchFsContents = ArrayList()
        try {
            // Retrieve directory listing for finding and deleting existing files
            adaBleFsQueue.addFirst(
                AdaBleFsAction(
                    AdaBleFsAction.Method.LIST_DIRECTORY,
                    "/"
                )
            )
            allActionsCount++
            val uriHelper = UriHelper.get(uri, context)
            val zipPackage = GBZipFile(uriHelper.openInputStream())
            val resourcesManifest = JSONObject(String(zipPackage.getFileFromZip("resources.json")))
            // Delete files declared obsolete in the manifest
            var resources = resourcesManifest.getJSONArray("obsolete_files")
            for (i in 0 until resources.length()) {
                val fileItem = resources.getJSONObject(i)
                val filePath = fileItem.getString("path")
                LOG.info("Adding to queue: DELETE (obsolete), $filePath")
                adaBleFsQueue.add(
                    AdaBleFsAction(
                        AdaBleFsAction.Method.DELETE,
                        filePath
                    )
                )
                allActionsCount += 1
            }
            // Upload new/updated resource files
            resources = resourcesManifest.getJSONArray("resources")
            for (i in 0 until resources.length()) {
                val fileItem = resources.getJSONObject(i)
                val filePath = fileItem.getString("path")
                val fileName = fileItem.getString("filename")
                LOG.info("Adding to queue: DELETE, $filePath")
                adaBleFsQueue.add(
                    AdaBleFsAction(
                        AdaBleFsAction.Method.DELETE,
                        filePath
                    )
                )
                allActionsCount += 1
                val fileData = zipPackage.getFileFromZip(fileName)
                bytesTotal += fileData.size
                LOG.info("Adding to queue: UPLOAD, $fileName, $filePath, ${fileData.size} bytes")
                adaBleFsQueue.add(
                    AdaBleFsAction(
                        AdaBleFsAction.Method.UPLOAD,
                        filePath,
                        fileData
                    )
                )
                allActionsCount += 1
            }
            // This directory listing is only useful to get some data in the log
            adaBleFsQueue.add(
                AdaBleFsAction(
                    AdaBleFsAction.Method.LIST_DIRECTORY,
                    "/"
                )
            )
            allActionsCount++
        } catch (e: ZipFileException) {
            LOG.error("Unable to read the zip file.", e)
        } catch (e: FileNotFoundException) {
            LOG.error("The update file was not found.", e)
        } catch (e: IOException) {
            LOG.error("General IO error occurred.", e)
        } catch (e: Exception) {
            LOG.error("Unknown error occurred.", e)
        }

        try {
            startNextAdaFsAction()
        } catch (e: IOException) {
            LOG.error("Error while loading resources: ", e)
        }
    }

    @Throws(IOException::class)
    private fun startNextAdaFsAction() {
        try {
            currentAction = adaBleFsQueue.removeFirst()
        } catch (e: NoSuchElementException) {
            notify(createSuccessIntent())
            return
        }
        currentActionNr++
        when (currentAction!!.method) {
            AdaBleFsAction.Method.UPLOAD -> uploadFileStart()
            AdaBleFsAction.Method.DELETE -> deleteFile()
            AdaBleFsAction.Method.LIST_DIRECTORY -> listDirectoryFromQueue()
            else -> {
                LOG.warn("Skipping unimplemented AdaBleFsAction method ${currentAction!!.method}")
            }
        }
    }

    override fun enableNotify(builder: TransactionBuilder, enable: Boolean) {
        builder.notify(UUID_CHARACTERISTIC_FS_TRANSFER, enable)
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ): Boolean {
        if (status == 0x08) {  // BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION
            notify(createErrorIntent(context.getString(R.string.infinitime_filesystem_access_disabled)))
        }
        return super.onCharacteristicWrite(gatt, characteristic, status)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray?
    ): Boolean {
        if (characteristic?.uuid == UUID_CHARACTERISTIC_FS_TRANSFER) {
            try {
                handleNextStatus(gatt, characteristic)
            } catch (e: IOException) {
                LOG.error("Error handling status: ", e)
            }
            return true
        }
        return false
    }

    @Throws(IOException::class)
    private fun handleNextStatus(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
        val returned = characteristic.value

        if (returned == null || returned.isEmpty()) {
            LOG.warn("Received empty BLE characteristic value for ${characteristic.uuid}")
            return
        }

        var actionResult = false
        when (returned[0]) {
            RESPONSE_WRITE_FILE -> actionResult = checkContinueFileUpload(returned)
            RESPONSE_DELETE_FILE -> actionResult = checkDeleteFile(returned)
            RESPONSE_MAKE_DIRECTORY -> actionResult = checkMakeDirectory(returned)
            RESPONSE_LIST_DIRECTORY -> actionResult = checkListDirectory(returned)
            RESPONSE_MOVE_FILE_DIRECTORY -> actionResult = checkMove(returned)
            else -> LOG.warn("Unknown BLE FS response: ${returned[0]}")
        }
        if (actionResult) {
            startNextAdaFsAction()
        }
    }

    private fun checkStatus(status: Byte) {
        if (status != STATUS_OK) throw IOException("Operation failed with status $status")
    }

    private fun checkMove(returned: ByteArray) = true.also { checkStatus(returned[1]) }

    private fun checkListDirectory(returned: ByteArray): Boolean {
        checkStatus(returned[1])

        val pathLength = returned[2].toInt() or (returned[3].toInt() shl 8)
        val entryNum = returned.sliceArray(4..7).foldIndexed(0) { i, acc, b -> acc or ((b.toInt() and 0xFF) shl (8 * i)) }
        val totalEntries = returned.sliceArray(8..11).foldIndexed(0) { i, acc, b -> acc or ((b.toInt() and 0xFF) shl (8 * i)) }
        val flags = returned.sliceArray(12..15).foldIndexed(0) { i, acc, b -> acc or ((b.toInt() and 0xFF) shl (8 * i)) }
        val isDirectory = (flags and 1) == 1
        val timestamp = returned.sliceArray(16..23).foldIndexed(0L) { i, acc, b -> acc or ((b.toLong() and 0xFF) shl (8 * i)) }
        val fileSize = returned.sliceArray(24..27).foldIndexed(0) { i, acc, b -> acc or ((b.toInt() and 0xFF) shl (8 * i)) }
        val path = String(returned.copyOfRange(28, 28 + pathLength), Charsets.UTF_8)

        val absPath = "/" + (listingDirectory + path).filter { it.isNotEmpty() }.joinToString("/")
        LOG.debug("LISTDIR: $absPath, $fileSize bytes")
        if (path.isNotEmpty() && path != "." && path != "..") {
            watchFsContents.add(absPath)
            if (isDirectory) {
                LOG.info("Adding to queue: LISTDIR, $absPath")
                adaBleFsQueue.addFirst(
                    AdaBleFsAction(
                        AdaBleFsAction.Method.LIST_DIRECTORY,
                        absPath
                    )
                )
                allActionsCount++
            }
        }
        return entryNum == totalEntries
    }

    private fun checkMakeDirectory(returned: ByteArray) = true.also { checkStatus(returned[1]) }

    private fun checkDeleteFile(returned: ByteArray) = true.also { checkStatus(returned[1]) }

    @Throws(IOException::class)
    private fun checkContinueFileUpload(returned: ByteArray): Boolean {
        checkStatus(returned[1])
        val offset = ByteBuffer.wrap(returned, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val sizeLeft = ByteBuffer.wrap(returned, 16, 4).order(ByteOrder.LITTLE_ENDIAN).int
        LOG.debug("Status from watch: ${returned[1]}, offset=$offset, sizeLeft=$sizeLeft")
        if (bytesWritten < currentAction!!.data.size) {
            uploadNextFileChunk()
            return false
        }
        return true
    }

    @Throws(IOException::class)
    private fun uploadFileStart() {
        val unixTime = System.currentTimeMillis() / 1000L
        val pathBytes = currentAction!!.filenameorpath.toByteArray()
        val buffer = ByteBuffer.allocate(1 + 1 + 2 + 4 + 8 + 4 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(REQUEST_WRITE_FILE_START)
        buffer.put(PADDING_BYTE)
        buffer.putShort(pathBytes.size.toShort())
        buffer.putInt(0)
        buffer.putLong(unixTime)
        buffer.putInt(currentAction!!.data.size)
        buffer.put(pathBytes)

        bytesWritten = 0
        LOG.info("Sending start packet for ${currentAction!!.filenameorpath} (${currentAction!!.data.size} bytes)")
        notify(createProgressIntent())
        currentBuilder = performInitialized("Upload file start")
        currentBuilder?.write(UUID_CHARACTERISTIC_FS_TRANSFER, *buffer.array())
        currentBuilder?.queue()
    }

    @Throws(IOException::class)
    private fun uploadNextFileChunk() {
        val toSendSize = minOf(chunkSize, currentAction!!.data.size - bytesWritten)
        val buffer = ByteBuffer.allocate(2 + 2 + 4 + 4 + toSendSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(REQUEST_WRITE_FILE_DATA)
        buffer.put(REQUEST_CONTINUED)
        buffer.put(PADDING_BYTE)
        buffer.put(PADDING_BYTE)
        buffer.putInt(bytesWritten)
        buffer.putInt(toSendSize)
        buffer.put(currentAction!!.data, bytesWritten, toSendSize)

        bytesProgress += toSendSize
        val percentage = ((bytesProgress.toFloat() / bytesTotal.toFloat()) * 100).roundToInt()
        LOG.info("Sending chunk of $toSendSize bytes for ${currentAction!!.filenameorpath}, at offset $bytesWritten")
        notify(createProgressIntent())
        currentBuilder = performInitialized("Upload file chunk")
        currentBuilder?.write(UUID_CHARACTERISTIC_FS_TRANSFER, *buffer.array())
        currentBuilder?.setProgress(R.string.uploading_resources, true, percentage)
        currentBuilder?.queue()
        bytesWritten += toSendSize
    }

    @Throws(IOException::class)
    private fun deleteFile() {
        if (!watchFsContents.contains(currentAction!!.filenameorpath)) {
            LOG.info("Not deleting non-existent file: ${currentAction!!.filenameorpath}")
            startNextAdaFsAction()
            return
        }
        val pathBytes = currentAction!!.filenameorpath.toByteArray()
        val buffer = ByteBuffer.allocate(2 + 2 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(REQUEST_DELETE_FILE)
        buffer.put(PADDING_BYTE)
        buffer.putShort(pathBytes.size.toShort())
        buffer.put(pathBytes)

        notify(createProgressIntent())
        currentBuilder = performInitialized("Delete file or directory: ${currentAction!!.filenameorpath}")
        currentBuilder?.write(UUID_CHARACTERISTIC_FS_TRANSFER, *buffer.array())
        currentBuilder?.queue()
    }

    @Throws(IOException::class)
    private fun makeDirectory() {
        val unixTime = System.currentTimeMillis() / 1000L
        val pathBytes = currentAction!!.filenameorpath.toByteArray()
        val buffer = ByteBuffer.allocate(2 + 2 + 4 + 8 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(REQUEST_MAKE_DIRECTORY)
        buffer.put(PADDING_BYTE)
        buffer.putShort(pathBytes.size.toShort())
        buffer.putInt(0)
        buffer.putLong(unixTime)
        buffer.put(pathBytes)

        notify(createProgressIntent())
        currentBuilder = performInitialized("Create directory")
        currentBuilder?.write(UUID_CHARACTERISTIC_FS_TRANSFER, *buffer.array())
        currentBuilder?.queue()
    }

    @Throws(IOException::class)
    private fun moveFileOrDirectory() {
        val firstPathBytes = currentAction!!.filenameorpath.toByteArray()
        val secondPathBytes = currentAction!!.secondFilenameorpath.toByteArray()
        val buffer = ByteBuffer.allocate(2 + 4 + firstPathBytes.size + secondPathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(REQUEST_MOVE_FILE_DIRECTORY)
        buffer.put(PADDING_BYTE)
        buffer.putShort(firstPathBytes.size.toShort())
        buffer.putShort(secondPathBytes.size.toShort())
        buffer.put(firstPathBytes)
        buffer.put(PADDING_BYTE)
        buffer.put(secondPathBytes)

        notify(createProgressIntent())
        currentBuilder = performInitialized("Move file or directory")
        currentBuilder?.write(UUID_CHARACTERISTIC_FS_TRANSFER, *buffer.array())
        currentBuilder?.queue()
    }

    @Throws(IOException::class)
    private fun listDirectoryFromQueue() {
        listingDirectory = currentAction!!.filenameorpath.split("/").filter { it.isNotEmpty() }
        val pathBytes = currentAction!!.filenameorpath.toByteArray()
        val buffer = ByteBuffer.allocate(2 + 2 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(REQUEST_LIST_DIRECTORY)
        buffer.put(PADDING_BYTE)
        buffer.putShort(pathBytes.size.toShort())
        buffer.put(pathBytes)

        notify(createProgressIntent())
        currentBuilder = performInitialized("List directory")
        currentBuilder?.write(UUID_CHARACTERISTIC_FS_TRANSFER, *buffer.array())
        currentBuilder?.queue()
    }

    private fun createProgressIntent(): Intent {
        val intent = Intent(PineTimeJFConstants.ACTION_UPLOAD_PROGRESS)
        intent.putExtra("filename", currentAction!!.filenameorpath)
        intent.putExtra("currentAction", currentAction!!.method.toString())
        intent.putExtra("currentActionNr", currentActionNr)
        intent.putExtra("allActionsCount", allActionsCount)
        return intent
    }

    private fun createSuccessIntent(): Intent {
        val intent = Intent(PineTimeJFConstants.ACTION_UPLOAD_FINISHED)
        return intent
    }

    private fun createErrorIntent(errorMsg: String): Intent {
        val intent = Intent(PineTimeJFConstants.ACTION_UPLOAD_ERROR)
        intent.putExtra("errorMsg", errorMsg)
        return intent
    }
}