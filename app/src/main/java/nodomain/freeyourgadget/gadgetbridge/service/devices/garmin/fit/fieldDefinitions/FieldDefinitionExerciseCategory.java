package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionExerciseCategory extends FieldDefinition {

    public FieldDefinitionExerciseCategory(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            return ExerciseCategory.fromId(raw);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof ExerciseCategory) {
            baseType.encode(byteBuffer, (((ExerciseCategory) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum ExerciseCategory {
        // see https://github.com/dtcooper/python-fitparse/blob/master/fitparse/profile.py
        CATEGORY_BENCH_PRESS(0, R.string.exercise_category_bench_press),
        CATEGORY_CALF_RAISE(1, R.string.exercise_category_calf_raise),
        CATEGORY_CARDIO(2, R.string.activity_type_cardio),
        CATEGORY_CARRY(3, R.string.exercise_category_carry),
        CATEGORY_CHOP(4, R.string.exercise_category_chop),
        CATEGORY_CORE(5, R.string.exercise_category_core),
        CATEGORY_CRUNCH(6, R.string.exercise_category_crunch),
        CATEGORY_CURL(7, R.string.activity_type_curling),
        CATEGORY_DEADLIFT(8, R.string.activity_type_deadlift),
        CATEGORY_FLYE(9, R.string.exercise_category_flye),
        CATEGORY_HIP_RAISE(10, R.string.exercise_category_hip_raise),
        CATEGORY_HIP_STABILITY(11, R.string.exercise_category_hip_stability),
        CATEGORY_HIP_SWING(12, R.string.exercise_category_hip_swing),
        CATEGORY_HYPEREXTENSION(13, R.string.exercise_category_hyperextension),
        CATEGORY_LATERAL_RAISE(14, R.string.exercise_category_lateral_raise),
        CATEGORY_LEG_CURL(15, R.string.exercise_category_leg_curl),
        CATEGORY_LEG_RAISE(16, R.string.exercise_category_leg_raise),
        CATEGORY_LUNGE(17, R.string.exercise_category_lunge),
        CATEGORY_OLYMPIC_LIFT(18, R.string.exercise_category_olympic_lift),
        CATEGORY_PLANK(19, R.string.activity_type_plank),
        CATEGORY_PLYO(20, R.string.exercise_category_plyo),
        CATEGORY_PULL_UP(21, R.string.activity_type_pull_ups),
        CATEGORY_PUSH_UP(22, R.string.activity_type_push_ups),
        CATEGORY_ROW(23, R.string.activity_type_rowing),
        CATEGORY_SHOULDER_PRESS(24, R.string.exercise_category_shoulder_press),
        CATEGORY_SHOULDER_STABILITY(25, R.string.exercise_category_shoulder_stability),
        CATEGORY_SHRUG(26, R.string.exercise_category_shrug),
        CATEGORY_SIT_UP(27, R.string.activity_type_sit_ups),
        CATEGORY_SQUAT(28, R.string.exercise_category_squat),
        CATEGORY_TOTAL_BODY(29, R.string.exercise_category_total_body),
        CATEGORY_TRICEPS_EXTENSION(30, R.string.exercise_category_triceps_extension),
        CATEGORY_WARM_UP(31, R.string.hrZoneWarmUp),
        CATEGORY_RUN(32, R.string.activity_type_running),
        CATEGORY_UNKNOWN(250, R.string.unknown)
        ;

        private final int id;
        private final int nameResId;

        ExerciseCategory(int i, int nameResId) {
            this.id = i;
            this.nameResId = nameResId;
        }

        @Nullable
        public static ExerciseCategory fromId(int id) {
            for (ExerciseCategory language : ExerciseCategory.values()) {
                if (id == language.getId()) {
                    return language;
                }
            }
            return CATEGORY_UNKNOWN;
        }

        public int getId() {
            return id;
        }

        @StringRes
        public int getNameResId() {
            return nameResId;
        }
    }
}
