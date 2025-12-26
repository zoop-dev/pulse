package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import androidx.health.connect.client.records.ExerciseSessionRecord
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import org.junit.Assert.*
import org.junit.Test

class WorkoutSyncerUtilsTest {

    @Test
    fun testMapActivityKind_running() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.RUNNING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.OUTDOOR_RUNNING)
        )
    }

    @Test
    fun testMapActivityKind_walking() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.WALKING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.OUTDOOR_WALKING)
        )
    }

    @Test
    fun testMapActivityKind_cycling() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.CYCLING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.OUTDOOR_CYCLING)
        )
    }

    @Test
    fun testMapActivityKind_indoorCycling() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.INDOOR_CYCLING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.SPINNING)
        )
    }

    @Test
    fun testMapActivityKind_swimming() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.SWIMMING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.POOL_SWIM)
        )
    }

    @Test
    fun testMapActivityKind_openWaterSwimming() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.SWIMMING_OPENWATER)
        )
    }

    @Test
    fun testMapActivityKind_treadmill() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.TREADMILL)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.INDOOR_RUNNING)
        )
    }

    @Test
    fun testMapActivityKind_strengthTraining() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.STRENGTH_TRAINING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.WEIGHTLIFTING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.CROSSFIT)
        )
    }

    @Test
    fun testMapActivityKind_yoga() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.YOGA)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.TAI_CHI)
        )
    }

    @Test
    fun testMapActivityKind_sports() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_SOCCER,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.SOCCER)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.BASKETBALL)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_TENNIS,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.TENNIS)
        )
    }

    @Test
    fun testMapActivityKind_hiking() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.HIKING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.TREKKING)
        )
    }

    @Test
    fun testMapActivityKind_climbing() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.CLIMBING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.ROCK_CLIMBING)
        )
    }

    @Test
    fun testMapActivityKind_skiing() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_SKIING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.SKIING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.SNOWBOARDING)
        )
    }

    @Test
    fun testMapActivityKind_martialArts() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.MARTIAL_ARTS)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.KARATE)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.TAEKWONDO)
        )
    }

    @Test
    fun testMapActivityKind_dancing() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_DANCING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.DANCE)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_DANCING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.ZUMBA)
        )
    }

    @Test
    fun testMapActivityKind_hiit() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.HIIT)
        )
    }

    @Test
    fun testMapActivityKind_pilates() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.PILATES)
        )
    }

    @Test
    fun testMapActivityKind_rowing() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.ROWING)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.ROWING_MACHINE)
        )
    }

    @Test
    fun testMapActivityKind_golf() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_GOLF,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.GOLF)
        )
    }

    @Test
    fun testMapActivityKind_guidedBreathing() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.BREATHWORK)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.MEDITATION)
        )
    }

    @Test
    fun testMapActivityKind_unknownActivity() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.UNKNOWN)
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
            WorkoutSyncerUtils.mapActivityKindToExerciseType(ActivityKind.ARCHERY)
        )
    }

    @Test
    fun testMapActivityKind_allTypesMapToValidExerciseType() {
        // Test that all activity kinds map to a valid exercise type (no exceptions thrown)
        for (activityKind in ActivityKind.entries) {
            val exerciseType = WorkoutSyncerUtils.mapActivityKindToExerciseType(activityKind)
            assertTrue("Exercise type should be a valid int", exerciseType >= 0)
        }
    }
}

