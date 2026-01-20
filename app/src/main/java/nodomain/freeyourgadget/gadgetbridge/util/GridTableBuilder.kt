/*  Copyright (C) 2026 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.gridlayout.widget.GridLayout
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryEntry
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummarySimpleEntry

class GridTableBuilder @JvmOverloads constructor(
    private val context: Context,
    private val workoutValueFormatter: WorkoutValueFormatter = WorkoutValueFormatter()
) {
    private var cellNumber = 0
    private val columnSpans = mutableListOf<Int>()
    private val gridLayout = GridLayout(context).apply {
        setBackgroundColor(ContextCompat.getColor(context, R.color.gauge_line_color))
        columnCount = 2
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun addEntry(key: String, entry: ActivitySummaryEntry?) {
        val entry = entry ?: ActivitySummarySimpleEntry.EMPTY

        val columnSpan = entry.columnSpan
        if (columnSpan == 2 && cellNumber % 2 != 0) {
            cellNumber++
        }

        val linearLayout = generateLinearLayout(cellNumber, columnSpan)
        entry.populate(key, linearLayout, workoutValueFormatter)
        gridLayout.addView(linearLayout)
        columnSpans.add(columnSpan)
        cellNumber += columnSpan
    }

    fun build(): GridLayout {
        if (gridLayout.isNotEmpty() && cellNumber % 2 != 0) {
            // When in an odd number of cells, add an empty one to prevent a gray hole from showing up
            val emptyLayout = generateLinearLayout(cellNumber, 1)
            ActivitySummarySimpleEntry("", "string").populate("", emptyLayout, workoutValueFormatter)
            gridLayout.addView(emptyLayout)
            columnSpans.add(1)
        }

        // Then, adjust the bottom margin for the last row
        var adjustedColumns = 0
        if (gridLayout.isNotEmpty()) {
            for (i in gridLayout.childCount - 1 downTo 0) {
                val layoutParams = gridLayout.getChildAt(i).layoutParams
                if (layoutParams is GridLayout.LayoutParams) {
                    layoutParams.bottomMargin = dpToPx(2)
                    adjustedColumns += columnSpans[i]

                    if (adjustedColumns >= gridLayout.columnCount) {
                        break
                    }
                }
            }
        }

        return gridLayout
    }

    private fun generateLinearLayout(i: Int, columnSize: Int): LinearLayout {
        return LinearLayout(context).apply {
            val layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, columnSize, GridLayout.FILL, 1f)
            )
            layoutParams.width = 0
            this.layoutParams = layoutParams
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15))
            setBackgroundColor(GBApplication.getWindowBackgroundColor(context))

            val marginLeft = if (i % 2 == 0) 0 else 1
            val marginRight = if (i % 2 == 0) 1 else 0
            val marginTop = 2
            val marginBottom = 0 // will be changed to 2 for the last row

            layoutParams.setMargins(dpToPx(marginLeft), dpToPx(marginTop), dpToPx(marginRight), dpToPx(marginBottom))
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
