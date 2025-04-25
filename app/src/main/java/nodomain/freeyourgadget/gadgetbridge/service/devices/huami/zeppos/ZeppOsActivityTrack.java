package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;

public class ZeppOsActivityTrack extends ActivityTrack {
    public final List<StrengthSet> strengthSets = new ArrayList<>();

    public void addStrengthSet(final int reps, final float weightKg) {
        strengthSets.add(new StrengthSet(reps, weightKg));
    }

    public List<StrengthSet> getStrengthSets() {
        return strengthSets;
    }

    public static class StrengthSet {
        private final int reps;
        private final float weightKg;

        public StrengthSet(final int reps, final float weightKg) {
            this.reps = reps;
            this.weightKg = weightKg;
        }

        public int getReps() {
            return reps;
        }

        public float getWeightKg() {
            return weightKg;
        }
    }
}
