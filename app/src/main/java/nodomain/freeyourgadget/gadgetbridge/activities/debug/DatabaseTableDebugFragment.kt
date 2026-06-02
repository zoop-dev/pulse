package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Suppress("unused")
class DatabaseTableDebugFragment : AbstractDebugFragment() {
    private var pendingExportData: Triple<String, Date, Date>? = null
    private var pendingImportTableName: String? = null

    private val saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                pendingExportData?.let { (tableName, startDate, endDate) ->
                    exportTableToUri(tableName, startDate, endDate, uri)
                }
            }
        }
        pendingExportData = null
    }

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val tableName = pendingImportTableName
        pendingImportTableName = null
        if (uri != null && tableName != null) {
            importTableFromUri(tableName, uri)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_database_table, rootKey)

        val tableName = arguments?.getString("tableName")!!

        preferenceScreen?.title = tableName

        loadTableCount(tableName)

        onClick(PREF_DEBUG_EXPORT_TABLE) { startTableExport(tableName) }

        onClick(PREF_DEBUG_IMPORT_TABLE) { startTableImport(tableName) }

        onClick(PREF_DEBUG_CLEAR_TABLE) { clearTable(tableName) }

        if (BuildConfig.DEBUG) {
            findPreference<Preference>(PREF_DEBUG_DROP_TABLE)?.isVisible = true
            onClick(PREF_DEBUG_DROP_TABLE) { dropTable(tableName) }
        }

        val ddl = getTableDdl(tableName)

        val pref = object : Preference(requireContext()) {
            override fun onBindViewHolder(holder: PreferenceViewHolder) {
                super.onBindViewHolder(holder)
                val summary = holder.findViewById(android.R.id.summary) as? TextView
                summary?.let {
                    // HACK: sql can be very long
                    summary.isSingleLine = false
                    summary.maxLines = ddl.lines().size + 1
                }
            }
        }
        pref.key = "database_table_sql"
        pref.summary = ddl
        pref.isPersistent = false
        pref.isIconSpaceReserved = false
        pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(tableName, ddl)
            clipboard.setPrimaryClip(clip)
            true
        }
        findPreference<PreferenceCategory>(PREF_HEADER_SQL)?.addPreference(pref)
    }

    private fun loadTableCount(tableName: String) {
        try {
            GBApplication.acquireDB().use { db ->
                val cursor = db.database.rawQuery(
                    "SELECT COUNT(*) as \"count\" FROM $tableName;",
                    null
                )

                cursor.use {
                    it.moveToNext()
                    findPreference<Preference>(PREF_DEBUG_DATABASE_COUNT)?.summary =
                        it.getInt(it.getColumnIndexOrThrow("count")).toString()
                }
            }
        } catch (e: Exception) {
            LOG.error("Error accessing database", e)
        }
    }

    private fun getTableDdl(tableName: String): String {
        try {
            GBApplication.acquireDB().use { db ->
                val cursor = db.database.rawQuery(
                    """
                    SELECT sql 
                    FROM sqlite_master 
                    WHERE name='${tableName}' OR tbl_name='${tableName}';
                    """.trimIndent(),
                    null
                )

                val sqlQueries = mutableListOf<String>()
                cursor.use {
                    while (it.moveToNext()) {
                        if (it.getColumnIndex("sql") >= 0) {
                            it.getString(it.getColumnIndexOrThrow("sql"))?.let { rawSql ->
                                sqlQueries.add(formatSql(rawSql.trim()) + ";")
                            }
                        }
                    }
                }
                return sqlQueries.joinToString("\n\n")
            }
        } catch (e: Exception) {
            return e.message ?: "Failed to get DDL for table $tableName"
        }
    }

    private fun formatSql(sql: String): String {
        val indentStep = "  "
        val sb = StringBuilder()
        var indentLevel = 0
        var i = 0

        while (i < sql.length) {
            when (sql[i]) {
                '(' -> {
                    indentLevel++
                    sb.append("(\n")
                    sb.append(indentStep.repeat(indentLevel))
                }

                ')' -> {
                    indentLevel--
                    sb.append("\n")
                    sb.append(indentStep.repeat(indentLevel))
                    sb.append(")")
                }

                ',' -> {
                    sb.append(",\n")
                    sb.append(indentStep.repeat(indentLevel))
                }

                else -> sb.append(sql[i])
            }
            i++
        }

        return sb.toString().trim()
    }

    private fun startTableExport(tableName: String) {
        if (getTableTimestampType(tableName) == null) {
            showExportMethodDialog(tableName, Date(), Date())
        } else {
            showExportStartDatePicker(tableName)
        }
    }

    private fun showExportStartDatePicker(tableName: String) {
        // Default to 7 days ago
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -7)

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.export_select_start_date))
            .setSelection(calendar.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = DateTimeUtils.dayStart(Date(selection))
            showExportEndDatePicker(tableName, startDate)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER_START")
    }

    private fun showExportEndDatePicker(tableName: String, startDate: Date) {
        val calendar = Calendar.getInstance()

        val constraintsBuilder = CalendarConstraints.Builder()
            .setStart(startDate.time)
            .setEnd(System.currentTimeMillis())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.export_select_end_date))
            .setSelection(calendar.timeInMillis)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val endDate = DateTimeUtils.dayEnd(Date(selection))
            showExportMethodDialog(tableName, startDate, endDate)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER_END")
    }

    private fun showExportMethodDialog(tableName: String, startDate: Date, endDate: Date) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export $tableName")
            .setMessage("How would you like to export the data?")
            .setPositiveButton(R.string.save) { _, _ ->
                exportTableToFile(tableName, startDate, endDate)
            }
            .setNegativeButton(R.string.share) { _, _ ->
                exportTableAndShare(tableName, startDate, endDate)
            }
            .setNeutralButton(R.string.Cancel, null)
            .show()
    }

    private fun exportTableToFile(tableName: String, startDate: Date, endDate: Date) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val fileName = "${tableName}_${timestamp}.csv"

            pendingExportData = Triple(tableName, startDate, endDate)

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }

            saveFileLauncher.launch(intent)
        } catch (e: Exception) {
            LOG.error("Failed to launch file picker", e)
            GB.toast("Failed to open file picker", Toast.LENGTH_LONG, GB.ERROR)
            pendingExportData = null
        }
    }

    private fun exportTableToUri(tableName: String, startDate: Date, endDate: Date, uri: Uri) {
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                generateCsvData(tableName, startDate, endDate, outputStream)
            }
            GB.toast(getString(R.string.export_success), Toast.LENGTH_LONG, GB.INFO)
        } catch (e: Exception) {
            LOG.error("Failed to save export", e)
            GB.toast("Failed to save export", Toast.LENGTH_LONG, GB.ERROR)
        }
    }

    private fun exportTableAndShare(tableName: String, startDate: Date, endDate: Date) {
        try {
            val file = createExportFile(tableName, startDate, endDate)

            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.screenshot_provider",
                file
            )

            val sharingIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, "Export of $tableName")
                putExtra(Intent.EXTRA_STREAM, contentUri)
            }

            startActivity(Intent.createChooser(sharingIntent, "Share export via"))
        } catch (e: Exception) {
            LOG.error("Failed to share table file", e)
            GB.toast("Share failed", Toast.LENGTH_LONG, GB.ERROR)
        }
    }

    private fun getTableTimestampType(tableName: String): String? {
        GBApplication.acquireDB().use { db ->
            // Check if table has TIMESTAMP column
            val columnsCursor = db.database.rawQuery("PRAGMA table_info($tableName)", null)
            var hasTimestamp = false

            columnsCursor.use {
                while (it.moveToNext()) {
                    val columnName = it.getString(it.getColumnIndexOrThrow("name"))
                    if (columnName.equals("TIMESTAMP", ignoreCase = true)) {
                        hasTimestamp = true
                        break
                    }
                }
            }

            if (!hasTimestamp) {
                return null
            }

            // Sample one row to determine if timestamp is in milliseconds or seconds
            val sampleCursor = db.database.rawQuery(
                "SELECT TIMESTAMP FROM $tableName LIMIT 1",
                null
            )

            sampleCursor.use {
                if (it.moveToFirst()) {
                    val timestamp = it.getLong(0)

                    // Timestamps in milliseconds are typically > 1,000,000,000,000 (Sept 2001)
                    // Timestamps in seconds are typically < 10,000,000,000 (Nov 2286)
                    // Current time in ms is ~1,737,000,000,000 (Jan 2025)
                    // Current time in seconds is ~1,737,000,000
                    return if (timestamp > 100000000000L) {
                        "milliseconds"
                    } else {
                        "seconds"
                    }
                }
            }

            // If no data in table, assume milliseconds, nothing will be exported anyway
            return "milliseconds"
        }
    }

    private fun generateCsvData(tableName: String, startDate: Date, endDate: Date, outputStream: OutputStream) {
        GBApplication.acquireDB().use { db ->
            // Get columns for the table
            val columnsCursor = db.database.rawQuery("PRAGMA table_info($tableName)", null)
            val columns = mutableListOf<String>()

            columnsCursor.use {
                while (it.moveToNext()) {
                    columns.add(it.getString(it.getColumnIndexOrThrow("name")))
                }
            }

            if (columns.isEmpty()) {
                throw Exception("No columns found for table $tableName")
            }

            val timestampType = getTableTimestampType(tableName)
            val query = if (timestampType != null) {
                "SELECT * FROM $tableName WHERE TIMESTAMP BETWEEN ? AND ? ORDER BY TIMESTAMP"
            } else {
                "SELECT * FROM $tableName"
            }

            val args = if (timestampType != null) {
                // Convert to appropriate timestamp format
                val startTimestamp = if (timestampType == "seconds") {
                    (startDate.time / 1000).toString()
                } else {
                    startDate.time.toString()
                }
                val endTimestamp = if (timestampType == "seconds") {
                    (endDate.time / 1000).toString()
                } else {
                    endDate.time.toString()
                }
                arrayOf(startTimestamp, endTimestamp)
            } else {
                null
            }

            val cursor = db.database.rawQuery(query, args)

            val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8), 8192)

            // Write CSV header
            writer.write(columns.joinToString(",") { escapeValueForCsv(it) })
            writer.write("\n")

            // Write rows
            cursor.use {
                var rowCount = 0
                while (it.moveToNext()) {
                    val row = columns.map { column ->
                        val index = it.getColumnIndex(column)
                        if (index >= 0 && !it.isNull(index)) {
                            val value = when (it.getType(index)) {
                                android.database.Cursor.FIELD_TYPE_BLOB ->
                                    "<blob:${it.getBlob(index).joinToString("") { b -> "%02x".format(b) }}>"

                                else -> it.getString(index) ?: NULL_FIELD_CSV_VALUE
                            }
                            escapeValueForCsv(value)
                        } else {
                            ""
                        }
                    }
                    writer.write(row.joinToString(","))
                    writer.write("\n")
                    rowCount++
                }

                if (rowCount == 0) {
                    GB.toast("No data found", Toast.LENGTH_LONG, GB.WARN)
                }
            }

            writer.flush()
        }
    }

    private fun createExportFile(tableName: String, startDate: Date, endDate: Date): File {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        val fileName = "${tableName}_${dateFormat.format(startDate)}_${dateFormat.format(endDate)}.csv"
        val csvDir = File(requireContext().cacheDir, "csv")
        csvDir.mkdir()
        val file = File(csvDir, fileName)

        FileOutputStream(file).use { outputStream ->
            generateCsvData(tableName, startDate, endDate, outputStream)
        }

        return file
    }

    private fun escapeValueForCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun startTableImport(tableName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Import into $tableName")
            .setMessage("Import a CSV file into $tableName? Existing rows with the same primary key will be replaced.")
            .setPositiveButton(R.string.file_import) { _, _ ->
                pendingImportTableName = tableName
                openFileLauncher.launch(arrayOf("text/csv", "text/plain", "*/*"))
            }
            .setNegativeButton(R.string.Cancel, null)
            .show()
    }

    private fun importTableFromUri(tableName: String, uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
                ?: throw Exception("Failed to open file")

            val lines = inputStream.use { stream ->
                stream.bufferedReader(StandardCharsets.UTF_8).readLines()
            }

            if (lines.isEmpty()) {
                GB.toast("File is empty", Toast.LENGTH_LONG, GB.WARN)
                return
            }

            val headers = parseCsvLine(lines[0])
            if (headers.isEmpty()) {
                GB.toast("File has no headers", Toast.LENGTH_LONG, GB.ERROR)
                return
            }

            // Validate that all headers exist as columns in the table
            val tableColumns = getTableColumns(tableName)
            val unknownHeaders = headers.filter { it !in tableColumns }
            if (!unknownHeaders.isEmpty()) {
                GB.toast("File contains unknown columns", Toast.LENGTH_LONG, GB.ERROR)
                return
            }

            var rowsImported = 0
            var rowsFailed = 0

            GBApplication.acquireDB().use { db ->
                db.database.beginTransaction()
                try {
                    linesLoop@ for (i in 1 until lines.size) {
                        val line = lines[i]
                        if (line.isBlank()) continue

                        val values = parseCsvLine(line)
                        if (values.size != headers.size) {
                            rowsFailed++
                            continue
                        }

                        val contentValues = ContentValues()
                        for (j in headers.indices) {
                            if (headers[j] in tableColumns) {
                                val value = values[j]
                                when {
                                    value == NULL_FIELD_CSV_VALUE -> contentValues.putNull(headers[j])
                                    value.startsWith("<blob:") && value.endsWith(">") -> {
                                        val blob = parseBlobHex(value)
                                        if (blob == null) {
                                            rowsFailed++
                                            continue@linesLoop
                                        }
                                        contentValues.put(headers[j], blob)
                                    }

                                    else -> contentValues.put(headers[j], value)
                                }
                            }
                        }

                        val result = db.database.insertWithOnConflict(
                            tableName,
                            null,
                            contentValues,
                            SQLiteDatabase.CONFLICT_REPLACE
                        )
                        if (result == -1L) {
                            rowsFailed++
                        } else {
                            rowsImported++
                        }
                    }
                    db.database.setTransactionSuccessful()
                } finally {
                    db.database.endTransaction()
                }
            }

            loadTableCount(tableName)

            val message = if (rowsFailed == 0) {
                "Imported $rowsImported rows"
            } else {
                "Imported $rowsImported rows, $rowsFailed failed"
            }
            GB.toast(message, Toast.LENGTH_LONG, GB.INFO)
        } catch (e: Exception) {
            LOG.error("Failed to import table", e)
            GB.toast("Import failed: ${e.localizedMessage}", Toast.LENGTH_LONG, GB.ERROR)
        }
    }

    private fun getTableColumns(tableName: String): Set<String> {
        GBApplication.acquireDB().use { db ->
            val cursor = db.database.rawQuery("PRAGMA table_info($tableName)", null)
            val columns = mutableSetOf<String>()
            cursor.use {
                while (it.moveToNext()) {
                    columns.add(it.getString(it.getColumnIndexOrThrow("name")))
                }
            }
            return columns
        }
    }

    private fun parseBlobHex(value: String): ByteArray? {
        val hex = value.removePrefix("<blob:").removeSuffix(">")
        if (hex.length % 2 != 0) return null
        return try {
            ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        } catch (e: NumberFormatException) {
            LOG.error("Failed to parse blob from {}", value, e)
            null
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"')
                    i += 2
                }

                c == '"' -> {
                    inQuotes = !inQuotes
                    i++
                }

                c == ',' && !inQuotes -> {
                    fields.add(sb.toString())
                    sb.clear()
                    i++
                }

                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        fields.add(sb.toString())
        return fields
    }

    private fun clearTable(tableName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setCancelable(true)
            .setIcon(R.drawable.ic_warning)
            .setTitle("Clear $tableName")
            .setMessage("Clear $tableName? All data in this table will be deleted.")
            .setPositiveButton(R.string.Delete) { _, _ ->
                try {
                    GBApplication.acquireDB().use { db ->
                        db.database.execSQL("DELETE FROM $tableName;")
                    }
                    loadTableCount(tableName)
                    GB.toast("Table cleared", Toast.LENGTH_LONG, GB.INFO)
                } catch (e: Exception) {
                    GB.toast("Failed to clear table", Toast.LENGTH_LONG, GB.ERROR, e)
                }
            }
            .setNegativeButton(R.string.Cancel) { _, _ -> }
            .show()
    }

    private fun dropTable(tableName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setCancelable(true)
            .setIcon(R.drawable.ic_warning)
            .setTitle("Drop $tableName")
            .setMessage("Drop $tableName? All data in this table will be lost, and the table must be re-created manually.")
            .setPositiveButton(R.string.Delete) { _, _ ->
                try {
                    GBApplication.acquireDB().use { db ->
                        db.database.execSQL("DROP TABLE IF EXISTS $tableName;")
                    }
                    parentFragmentManager.popBackStack()
                } catch (e: Exception) {
                    GB.toast("Failed to drop table", Toast.LENGTH_LONG, GB.ERROR, e)
                }
            }
            .setNegativeButton(R.string.Cancel) { _, _ -> }
            .show()

    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DatabaseTableDebugFragment::class.java)

        private const val NULL_FIELD_CSV_VALUE = "<null>"

        private const val PREF_DEBUG_DATABASE_COUNT = "pref_debug_database_count"
        private const val PREF_DEBUG_EXPORT_TABLE = "pref_debug_export_table"
        private const val PREF_DEBUG_IMPORT_TABLE = "pref_debug_import_table"
        private const val PREF_HEADER_DANGEROUS_ACTIONS = "pref_header_dangerous_actions"
        private const val PREF_DEBUG_CLEAR_TABLE = "pref_debug_clear_table"
        private const val PREF_DEBUG_DROP_TABLE = "pref_debug_drop_table"
        private const val PREF_HEADER_SQL = "pref_header_sql"
    }
}
