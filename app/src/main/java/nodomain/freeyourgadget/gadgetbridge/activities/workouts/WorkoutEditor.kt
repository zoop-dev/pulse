package nodomain.freeyourgadget.gadgetbridge.activities.workouts

import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.model.workout.Workout
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.Collections
import java.util.Locale

class WorkoutEditor(private val context: Context) {
    var filesGpxList: MutableList<String> = ArrayList()
    var selectedGpxIndex: Int = 0
    var selectedGpxFile: String? = null
    var exportPathRoot: File? = null // original gpx file location, could still contain old gpx files
    var exportPathGpx: File? = null // new gpx file location

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
            .setNegativeButton(R.string.Cancel, null)
            .show()
    }

    private fun buildGpxFileList(): MutableList<String> {
        val files: MutableList<File> = ArrayList()
        val gpxFileFilter = FileFilter { file: File? ->
            file!!.isFile() && file.path.lowercase(Locale.getDefault()).endsWith(".gpx")
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
            list.add(file.getName())
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
            if (File(exportPathGpx, selectedFilename).isFile()) {
                // Note: if selectedFilename exists in both exportPathGpx and exportPathRoot,
                // this code will always choose the one in exportPathGpx. This is acceptable
                // because exportPathGpx is where all new files end up, and gpx files tend to
                // have a unique name anyway because it usually contains some timestamp.
                selectedGpxFile = File(exportPathGpx, selectedFilename).getPath()
            } else {
                selectedGpxFile = File(exportPathRoot, selectedFilename).getPath()
            }
            var message = String.format("%s %s?", context.getString(R.string.set), filesGpxList.get(selectedGpxIndex))
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
                .setNegativeButton(R.string.Cancel) { dialog2, which2 -> }
                .show()
            dialog!!.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    interface Callback {
        fun onWorkoutUpdated()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WorkoutEditor::class.java)
    }
}
