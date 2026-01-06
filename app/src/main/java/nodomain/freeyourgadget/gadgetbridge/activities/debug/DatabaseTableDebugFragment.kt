package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.GB
import kotlin.io.use
import kotlin.use

class DatabaseTableDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_database_table, rootKey)

        val tableName = arguments?.getString("tableName")!!

        preferenceScreen?.title = tableName

        try {
            GBApplication.acquireDB().use { db ->
                val cursor = db.database.rawQuery(
                    "SELECT COUNT(*) as \"count\" FROM $tableName;",
                    null
                )

                cursor.use {
                    it.moveToNext()
                    findPreference<Preference>(PREF_DEBUG_DATABASE_COUNT)?.summary = it.getInt(it.getColumnIndexOrThrow("count")).toString()
                }
            }
        } catch (e: Exception) {
            GB.log("Error accessing database", GB.ERROR, e)
        }

        onClick(PREF_DEBUG_DROP_TABLE) { dropTable(tableName) }

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
        pref.title = "SQL"
        pref.summary = ddl
        pref.isPersistent = false
        pref.isIconSpaceReserved = false
        pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(tableName, ddl)
            clipboard.setPrimaryClip(clip)
            true
        }
        preferenceScreen?.addPreference(pref)
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
                            sqlQueries.add(formatSql(it.getString(it.getColumnIndexOrThrow("sql")).trim()) + ";")
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
        private const val PREF_DEBUG_DATABASE_COUNT = "pref_debug_database_count"
        private const val PREF_DEBUG_DROP_TABLE = "pref_debug_drop_table"
    }
}
