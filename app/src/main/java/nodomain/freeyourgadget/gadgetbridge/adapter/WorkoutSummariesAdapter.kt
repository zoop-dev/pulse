package nodomain.freeyourgadget.gadgetbridge.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutListViewModel
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.ActivityListItem
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils
import java.util.Date
import java.util.concurrent.TimeUnit

class WorkoutSummariesAdapter(
    context: Context,
    private val device: GBDevice,
    private var activityKindFilter: Int,
    private var dateFromFilter: Long,
    private var dateToFilter: Long,
    private var nameContainsFilter: String?,
    private var deviceFilter: Long,
    private var itemsFilter: List<Long>?
) : AbstractActivityListingAdapter<BaseActivitySummary>(context) {
    var dashboardStats: WorkoutListViewModel.DashboardStats? = null
    var isDashboardLoading: Boolean = false

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AbstractActivityListingViewHolder<BaseActivitySummary> {
        return when (viewType) {
            0 -> // dashboard
                DashboardViewHolder(
                    LayoutInflater.from(context).inflate(R.layout.activity_summary_dashboard_item, parent, false)
                )

            2 -> // item
                ActivityItemViewHolder(
                    device,
                    LayoutInflater.from(context).inflate(R.layout.activity_list_item, parent, false)
                )

            else -> super.onCreateViewHolder(parent, viewType)
        }
    }

    override fun setActivityKindFilter(filter: Int) {
        activityKindFilter = filter
    }

    override fun setDateFromFilter(date: Long) {
        dateFromFilter = date
    }

    override fun setDateToFilter(date: Long) {
        dateToFilter = date
    }

    override fun setNameContainsFilter(name: String?) {
        nameContainsFilter = name
    }

    override fun setItemsFilter(items: List<Long>?) {
        itemsFilter = items
    }

    override fun setDeviceFilter(device: Long) {
        deviceFilter = device
    }

    fun getActivityKindFilter(): Int = activityKindFilter

    class ActivityItemViewHolder(val device: GBDevice, itemView: View) : AbstractActivityListingViewHolder<BaseActivitySummary>(itemView) {
        private val activityListItem = ActivityListItem(itemView)

        override fun fill(position: Int, summary: BaseActivitySummary, selected: Boolean) {
            val parser = device.deviceCoordinator.getActivitySummaryParser(device, itemView.context)
            val workout = parser.parseWorkout(summary, false)

            val hasGps = when {
                workout.summary.gpxTrack != null -> true
                workout.summary.summaryData?.contains(ActivitySummaryEntries.INTERNAL_HAS_GPS) == true -> {
                    workout.data.getBoolean(ActivitySummaryEntries.INTERNAL_HAS_GPS, false)
                }

                else -> false
            }

            activityListItem.update(
                null,
                null,
                ActivityKind.fromCode(summary.activityKind),
                summary.name,
                -1,
                -1f,
                -1,
                -1f,
                summary.endTime.time - summary.startTime.time,
                hasGps,
                summary.startTime,
                position % 2 == 1,
                selected
            )
        }
    }

    inner class DashboardViewHolder(itemView: View) : AbstractActivityListingViewHolder<BaseActivitySummary>(itemView) {
        private val durationSumView: TextView = itemView.findViewById(R.id.summary_dashboard_layout_duration_label)
        private val caloriesBurntSumView: TextView = itemView.findViewById(R.id.summary_dashboard_layout_calories_label)
        private val distanceSumView: TextView = itemView.findViewById(R.id.summary_dashboard_layout_distance_label)
        private val activeSecondsSumView: TextView =
            itemView.findViewById(R.id.summary_dashboard_layout_active_duration_label)
        private val timeStartView: TextView = itemView.findViewById(R.id.summary_dashboard_layout_from_label)
        private val timeEndView: TextView = itemView.findViewById(R.id.summary_dashboard_layout_to_label)
        private val activitiesCountView: TextView = itemView.findViewById(R.id.summary_dashboard_layout_count_label)
        private val activityKindView: TextView = itemView.findViewById(R.id.summary_dashboard_layout_activity_label)
        private val activityIconView: ImageView = itemView.findViewById(R.id.summary_dashboard_layout_activity_icon)
        private val activityIconBigView: ImageView =
            itemView.findViewById(R.id.summary_dashboard_layout_big_activity_icon)
        private val loadingSpinner: ProgressBar = itemView.findViewById(R.id.summary_dashboard_layout_loading)
        private val contentLayout: View = itemView.findViewById(R.id.summary_dashboard_layout_content)

        override fun fill(position: Int, summary: BaseActivitySummary, selected: Boolean) {
            loadingSpinner.visibility = if (isDashboardLoading) View.VISIBLE else View.INVISIBLE
            contentLayout.visibility = if (!isDashboardLoading) View.VISIBLE else View.INVISIBLE

            if (isDashboardLoading || dashboardStats == null) {
                return
            }

            val stats = dashboardStats!!
            val activitiesCount = itemCount - 2 // remove dashboard and end spacer

            durationSumView.text = DateTimeUtils.formatDurationHoursMinutes(stats.durationSum, TimeUnit.MILLISECONDS)
            caloriesBurntSumView.text =
                String.format("%s %s", stats.caloriesBurntSum.toLong(), context.getString(R.string.calories_unit))
            distanceSumView.text = FormatUtils.getFormattedDistanceLabel(stats.distanceSum)
            activeSecondsSumView.text =
                DateTimeUtils.formatDurationHoursMinutes(stats.activeSecondsSum, TimeUnit.SECONDS)
            activitiesCountView.text = activitiesCount.toString()

            activityKindView.text = when {
                activityKindFilter != 0 -> {
                    val activityKind = ActivityKind.fromCode(activityKindFilter)
                    activityIconView.setImageResource(activityKind.icon)
                    activityIconBigView.setImageResource(activityKind.icon)
                    activityKind.getLabel(context)
                }

                stats.activityIcon != 0 -> {
                    val activityKind = ActivityKind.fromCode(stats.activityIcon)
                    activityIconView.setImageResource(activityKind.icon)
                    activityIconBigView.setImageResource(activityKind.icon)
                    context.getString(R.string.activity_summaries_all_activities)
                }

                else -> {
                    activityIconView.setImageResource(R.drawable.ic_activity_unknown_small)
                    activityIconBigView.setImageResource(R.drawable.ic_activity_unknown_small)
                    context.getString(R.string.activity_summaries_all_activities)
                }
            }

            // start and end are inverted when filter not applied, because items are sorted the other way
            timeStartView.text = if (dateFromFilter != 0L) {
                DateTimeUtils.formatDate(Date(dateFromFilter))
            } else {
                DateTimeUtils.formatDate(Date(stats.lastItemDate))
            }

            timeEndView.text = if (dateToFilter != 0L) {
                DateTimeUtils.formatDate(Date(dateToFilter))
            } else {
                DateTimeUtils.formatDate(Date(stats.firstItemDate))
            }
        }
    }
}
