/*  Copyright (C) 2020-2026 José Rebelo, Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.workouts

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.text.InputType
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.workout.Workout
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.Collections
import java.util.Locale

class WorkoutEditor(private val context: Context, resultCaller: ActivityResultCaller) {
    lateinit var gbDevice: GBDevice
    var filesGpxList: MutableList<String> = ArrayList()
    var selectedGpxIndex: Int = 0
    var selectedGpxFile: String? = null
    var exportPathRoot: File? = null // original gpx file location, could still contain old gpx files
    var exportPathGpx: File? = null // new gpx file location

    private val pickHeaderPhotoLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        resultCaller.registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? -> onHeaderPhotoPicked(uri) }
    private var pendingHeaderPhotoWorkout: Workout? = null
    private var pendingHeaderPhotoCallback: Callback? = null

    fun editWorkoutName(workout: Workout, callback: Callback) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(workout.summary.name ?: "")
        }

        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = context.resources.getDimensionPixelSize(R.dimen.dialog_margin)
            rightMargin = context.resources.getDimensionPixelSize(R.dimen.dialog_margin)
        }
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(context)
            .setView(container)
            .setCancelable(true)
            .setTitle(R.string.activity_summary_edit_name_title)
            .setPositiveButton(R.string.ok) { _, _ ->
                val newName = input.text.toString().takeIf { it.isNotEmpty() }
                workout.summary.name = newName
                workout.summary.update()
                callback.onWorkoutUpdated()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun buildGpxFileList(): MutableList<String> {
        val files: MutableList<File> = ArrayList()
        val gpxFileFilter = FileFilter { file: File? ->
            file!!.isFile && file.path.lowercase(Locale.getDefault()).endsWith(".gpx")
        }
        exportPathRoot?.let {
            val rootFiles: Array<File?>? = it.listFiles(gpxFileFilter)
            if (rootFiles != null) Collections.addAll<File?>(files, *rootFiles)
        }
        exportPathGpx?.let {
            val gpxFiles: Array<File?>? = it.listFiles(gpxFileFilter)
            if (gpxFiles != null) Collections.addAll<File?>(files, *gpxFiles)
        }

        files.sortWith { file1: File?, file2: File? ->
            val lastModified1 = file1!!.lastModified()
            val lastModified2 = file2!!.lastModified()
            lastModified2.compareTo(lastModified1) // Descending order
        }

        val list: MutableList<String> = ArrayList()
        list.add(context.getString(R.string.activity_summary_detail_clear_gpx_track))
        for (file in files) {
            list.add(file.name)
        }

        return list
    }

    private fun getPath(): File? {
        var path: File? = null
        try {
            path = FileUtils.getExternalFilesDir()
        } catch (e: IOException) {
            LOG.error("Error getting path", e)
        }
        return path
    }

    private fun getDeviceSpecificPath(): File? {
        var path: File? = null
        try {
            path = gbDevice.deviceCoordinator.getWritableExportDirectory(gbDevice, true)
        } catch (e: IOException) {
            LOG.error("Error getting device specific path", e)
        }
        return path
    }

    fun editGpsTrack(workout: Workout, callback: Callback) {
        exportPathRoot = getPath()
        exportPathGpx = File(getPath(), "gpx")
        filesGpxList = buildGpxFileList()

        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.activity_summary_detail_select_gpx_track)
        val directoryListing = ArrayAdapter(context, android.R.layout.simple_list_item_1, filesGpxList)
        builder.setSingleChoiceItems(directoryListing, 0) { dialog: DialogInterface?, which: Int ->
            selectedGpxIndex = which
            val selectedFilename = filesGpxList[selectedGpxIndex]
            selectedGpxFile = if (File(exportPathGpx, selectedFilename).isFile) {
                // Note: if selectedFilename exists in both exportPathGpx and exportPathRoot,
                // this code will always choose the one in exportPathGpx. This is acceptable
                // because exportPathGpx is where all new files end up, and gpx files tend to
                // have a unique name anyway because it usually contains some timestamp.
                File(exportPathGpx, selectedFilename).path
            } else {
                File(exportPathRoot, selectedFilename).path
            }
            var message = String.format("%s %s?", context.getString(R.string.set), filesGpxList[selectedGpxIndex])
            if (selectedGpxIndex == 0) {
                selectedGpxFile = null
                message = String.format("%s?", context.getString(R.string.activity_summary_detail_clear_gpx_track))
            }

            MaterialAlertDialogBuilder(context)
                .setCancelable(true)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.activity_summary_detail_editing_gpx_track)
                .setMessage(message)
                .setPositiveButton(R.string.ok) { dialog1, which1 ->
                    workout.summary.gpxTrack = selectedGpxFile
                    workout.summary.update()
                    callback.onWorkoutUpdated()
                }
                .setNegativeButton(R.string.cancel) { dialog2, which2 -> }
                .show()
            dialog!!.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun setHeaderPhoto(workout: Workout, callback: Callback) {
        pendingHeaderPhotoWorkout = workout
        pendingHeaderPhotoCallback = callback
        pickHeaderPhotoLauncher.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build()
        )
    }

    fun removeHeaderPhoto(workout: Workout, callback: Callback) {
        if (workout.summary.headerPhoto == null) {
            return
        }
        val photoFile = File(workout.summary.headerPhoto!!)
        if (photoFile.exists())
            photoFile.delete()
        workout.summary.headerPhoto = null
        workout.summary.update()
        callback.onWorkoutUpdated()
    }

    private fun onHeaderPhotoPicked(uri: Uri?) {
        val workout = pendingHeaderPhotoWorkout
        val callback = pendingHeaderPhotoCallback
        pendingHeaderPhotoWorkout = null
        pendingHeaderPhotoCallback = null

        if (uri == null || workout == null || callback == null) {
            // user backed out of the picker, nothing to do
            return
        }

        val savedPath = copyHeaderPhotoToAppStorage(uri)
        if (savedPath != null) {
            workout.summary.headerPhoto = savedPath
            workout.summary.update()
            callback.onWorkoutUpdated()
        } else {
            GB.toast(
                context,
                "Importing photo failed",
                Toast.LENGTH_LONG,
                GB.ERROR
            )
        }
    }

    private fun copyHeaderPhotoToAppStorage(uri: Uri): String? {
        return try {
            val exportPathPhotos = File(getDeviceSpecificPath(), "workout_photos")
            if (!exportPathPhotos.exists() && !exportPathPhotos.mkdirs()) {
                LOG.error("Failed to create photos directory: {}", exportPathPhotos)
                return null
            }

            val mimeType = context.contentResolver.getType(uri)
            val extension = mimeType
                ?.let { mime -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) }
                ?: "jpg"
            val destFile = File(exportPathPhotos, "workout_header_${System.currentTimeMillis()}.$extension")
            LOG.debug("Photo destination path: {}", destFile)

            // Read photo dimensions
            val boundsStream = context.contentResolver.openInputStream(uri) ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            boundsStream.use { input -> BitmapFactory.decodeStream(input, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                LOG.debug("Photo not decodable")
                return null
            }

            // Determine rotation
            val orientation = readExifOrientation(uri)
            val orientationSwapsDimensions = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                orientation == ExifInterface.ORIENTATION_TRANSVERSE
            val needsRotation = orientation != ExifInterface.ORIENTATION_NORMAL &&
                orientation != ExifInterface.ORIENTATION_UNDEFINED
            LOG.debug("Photo needs rotation: {}", needsRotation)

            // Determine whether resize is needed
            val effectiveWidth = if (orientationSwapsDimensions) bounds.outHeight else bounds.outWidth
            val needsResize = effectiveWidth > MAX_HEADER_PHOTO_WIDTH_PX
            LOG.debug("Photo needs resize: {}", needsResize)

            if (!needsRotation && !needsResize) {
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                    true
                } ?: false
                if (!copied) {
                    LOG.debug("Photo could not be copied")
                    return null
                }
            } else {
                val processed = decodeRotateAndResize(uri, bounds.outWidth, orientation, needsResize) ?: return null
                val compressFormat = when {
                    mimeType?.contains("png") == true -> Bitmap.CompressFormat.PNG
                    else -> Bitmap.CompressFormat.JPEG
                }
                val written = destFile.outputStream().use { output ->
                    processed.compress(compressFormat, 90, output)
                }
                processed.recycle()
                if (!written) {
                    LOG.debug("Photo could not be rotated and resized")
                    return null
                }
            }

            destFile.path
        } catch (e: IOException) {
            LOG.error("Error copying header photo", e)
            null
        }
    }

    private fun readExifOrientation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )
            } ?: ExifInterface.ORIENTATION_UNDEFINED
        } catch (_: IOException) {
            // If no EXIF info found
            ExifInterface.ORIENTATION_UNDEFINED
        }
    }

    private fun matrixForExifOrientation(orientation: Int): Matrix {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(-90f)
            else -> {} // ORIENTATION_NORMAL / ORIENTATION_UNDEFINED
        }
        return matrix
    }

    private fun decodeRotateAndResize(
        uri: Uri,
        originalWidth: Int,
        orientation: Int,
        needsResize: Boolean
    ): Bitmap? {
        val sampleSize = if (needsResize) calculateInSampleSize(originalWidth, MAX_HEADER_PHOTO_WIDTH_PX) else 1
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: return null

        val orientationMatrix = matrixForExifOrientation(orientation)
        val rotatedBitmap = if (orientationMatrix.isIdentity) {
            sampledBitmap
        } else {
            Bitmap.createBitmap(
                sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, orientationMatrix, true
            ).also {
                if (it !== sampledBitmap) sampledBitmap.recycle()
            }
        }

        if (rotatedBitmap.width <= MAX_HEADER_PHOTO_WIDTH_PX) {
            return rotatedBitmap
        }

        val scaledHeight =
            (rotatedBitmap.height.toFloat() / rotatedBitmap.width * MAX_HEADER_PHOTO_WIDTH_PX).toInt()
        val resized = rotatedBitmap.scale(MAX_HEADER_PHOTO_WIDTH_PX, scaledHeight)
        if (resized !== rotatedBitmap) {
            rotatedBitmap.recycle()
        }
        return resized
    }

    private fun calculateInSampleSize(originalWidth: Int, targetWidth: Int): Int {
        var sampleSize = 1
        val halfWidth = originalWidth / 2
        while (halfWidth / sampleSize >= targetWidth) {
            sampleSize *= 2
        }
        return sampleSize
    }

    interface Callback {
        fun onWorkoutUpdated()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WorkoutEditor::class.java)
        private const val MAX_HEADER_PHOTO_WIDTH_PX = 1000
    }
}
