package nodomain.freeyourgadget.gadgetbridge.activities.workouts

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary

class WorkoutViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private var workouts: List<BaseActivitySummary> = emptyList()

    override fun getItemCount(): Int = workouts.size

    override fun createFragment(position: Int): Fragment {
        return WorkoutDetailsFragment.newInstance(workouts[position].id)
    }

    override fun getItemId(position: Int): Long {
        return workouts[position].id
    }

    override fun containsItem(itemId: Long): Boolean {
        return workouts.any { it.id == itemId }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateWorkouts(newWorkouts: List<BaseActivitySummary>) {
        workouts = newWorkouts
        notifyDataSetChanged()
    }
}
