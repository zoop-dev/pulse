package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;

public class ZeppOsActivityTrack extends ActivityTrack {
    public final List<StrengthSet> strengthSets = new ArrayList<>();
    public final List<Lap> laps = new ArrayList<>();

    public void addStrengthSet(final int reps, final float weightKg) {
        strengthSets.add(new StrengthSet(reps, weightKg));
    }

    public void addLap(final int number,
                       final int hr,
                       final int pace,
                       final int calories,
                       final int distance,
                       final int duration) {
        laps.add(new Lap(number, hr, pace, calories, distance, duration));
    }

    public List<StrengthSet> getStrengthSets() {
        return strengthSets;
    }

    public List<Lap> getLaps() {
        return laps;
    }

    public record StrengthSet(int reps, float weightKg) {
    }

    public record Lap(int number,
                      int hr,
                      int pace,
                      int calories,
                      int distance,
                      int duration) {
    }
}
