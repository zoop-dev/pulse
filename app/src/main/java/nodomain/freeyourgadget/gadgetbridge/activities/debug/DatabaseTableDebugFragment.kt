package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import kotlin.use

class DatabaseTableDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_empty, rootKey)

        val tableName = arguments?.getString("tableName")!!

        preferenceScreen?.title = tableName

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
}
