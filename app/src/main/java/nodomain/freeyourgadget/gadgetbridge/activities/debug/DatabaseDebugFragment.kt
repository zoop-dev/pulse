package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.util.GB

class DatabaseDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_database, rootKey)

        onClick(PREF_DEBUG_EMPTY_DATABASE) { emptyDatabase() }
        onClick(PREF_DEBUG_DELETE_OLD_DATABASE) { deleteOldActivityDbFile() }
        if (!hasOldActivityDatabase()) {
            findPreference<Preference>(PREF_DEBUG_DELETE_OLD_DATABASE)!!.isVisible = false
        }

        addTables()
    }

    override fun onResume() {
        super.onResume()
        addTables()
    }

    private fun addTables() {
        val tablesHeader: PreferenceCategory = findPreference(PREF_HEADER_DATABASE_TABLES)!!
        removeDynamicPrefs(tablesHeader)

        val tableNames = mutableListOf<String>()

        try {
            GBApplication.acquireDB().use { db ->
                findPreference<Preference>(PREF_DEBUG_DATABASE_VERSION)?.summary = db.database.version.toString()

                val cursor = db.database.rawQuery(
                    """
                        SELECT name 
                        FROM sqlite_master 
                        WHERE type='table'
                          AND name NOT LIKE 'android_%'
                          AND name NOT LIKE 'sqlite_%'
                        """.trimIndent(),
                    null
                )

                cursor.use {
                    while (it.moveToNext()) {
                        val name = it.getString(it.getColumnIndexOrThrow("name"))
                        tableNames.add(name)
                    }
                }
                tableNames.sortWith(Comparator { o1, o2 -> o1.compareTo(o2, true) })
            }
        } catch (e: Exception) {
            GB.log("Error accessing database", GB.ERROR, e)
        }

        for (tableName in tableNames) {
            addDynamicPref(tablesHeader, tableName) {
                goTo(
                    DatabaseTableDebugFragment().apply {
                        arguments = Bundle().apply { putString("tableName", tableName) }
                    }
                )
            }
        }
    }

    private fun hasOldActivityDatabase(): Boolean {
        return DBHelper(requireContext()).existsDB("ActivityDatabase")
    }

    private fun emptyDatabase() {
        MaterialAlertDialogBuilder(requireContext())
            .setCancelable(true)
            .setIcon(R.drawable.ic_warning)
            .setTitle(R.string.dbmanagementactivity_delete_activity_data_title)
            .setMessage(R.string.dbmanagementactivity_really_delete_entire_db)
            .setPositiveButton(R.string.Delete) { _, _ ->
                if (GBApplication.deleteActivityDatabase(requireContext())) {
                    GB.toast(
                        requireContext(),
                        getString(R.string.dbmanagementactivity_database_successfully_deleted),
                        Toast.LENGTH_SHORT,
                        GB.INFO
                    )
                    requireActivity().finishAffinity()
                    GBApplication.restart()
                } else {
                    GB.toast(
                        requireContext(),
                        getString(R.string.dbmanagementactivity_db_deletion_failed),
                        Toast.LENGTH_SHORT,
                        GB.INFO
                    )
                }
            }
            .setNegativeButton(R.string.Cancel) { _, _ -> }
            .show()
    }

    private fun deleteOldActivityDbFile() {
        MaterialAlertDialogBuilder(requireContext())
            .setCancelable(true)
            .setTitle(R.string.dbmanagementactivity_delete_old_activity_db)
            .setIcon(R.drawable.ic_warning)
            .setMessage(R.string.dbmanagementactivity_delete_old_activitydb_confirmation)
            .setPositiveButton(R.string.Delete) { _, _ ->
                if (GBApplication.deleteOldActivityDatabase(requireContext())) {
                    GB.toast(
                        requireContext(),
                        getString(R.string.dbmanagementactivity_old_activity_db_successfully_deleted),
                        Toast.LENGTH_SHORT,
                        GB.INFO
                    )
                } else {
                    GB.toast(
                        requireContext(),
                        getString(R.string.dbmanagementactivity_old_activity_db_deletion_failed),
                        Toast.LENGTH_SHORT,
                        GB.INFO
                    )
                }

                if (!hasOldActivityDatabase()) {
                    findPreference<Preference>(PREF_DEBUG_DELETE_OLD_DATABASE)!!.isVisible = false
                }
            }.setNegativeButton(R.string.Cancel) { _, _ -> }.show()
    }

    // TODO ability to run raw sql from the UI
    private fun runSql() {
        try {
            GBApplication.acquireDB().use { db ->
                db.database.execSQL("DROP TABLE IF EXISTS TABLE_NAME_TO_DROP_HERE")
            }
        } catch (e: Exception) {
            GB.log("Error accessing database", GB.ERROR, e)
        }
    }

    companion object {
        private const val PREF_DEBUG_DATABASE_VERSION = "pref_debug_database_version"
        private const val DANGEROUS_ACTIONS = "dangerous_actions"
        private const val PREF_DEBUG_DELETE_OLD_DATABASE = "pref_debug_delete_old_database"
        private const val PREF_DEBUG_EMPTY_DATABASE = "pref_debug_empty_database"
        private const val PREF_HEADER_DATABASE_TABLES = "pref_header_database_tables"
    }
}
