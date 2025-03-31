package nodomain.freeyourgadget.gadgetbridge.devices.huawei.stress;

public class HuaweiStressScoreCalculation {

    private static final float[][] NORMALIZATION_PARAMS = {
            {19.381044F, 9.474176F},
            {5.5441837F, 1.9308523F},
            {184.17795F, 54.48858F},
            {33.487267F, 25.893078F},
            {32.97501F, 16.267557F},
            {677.911F, 130.61485F},
            {0.43178082F, 0.1747625F},
            {147.99915F, 170.24158F},
            {208.32425F, 245.64342F},
            {37.63596F, 19.478745F}
    };

    private static final float SCORE_INIT_VALUE = 3.835272F;
    private static final float[] COEFFICIENTS = {0.16605523F, 0.24399279F, 0.0F, 0.0F, -0.07095941F, -0.20609115F, 0.0F, -0.14579488F, -0.09786916F, 0.0F};

    private static final float MIN_SCORE = 0.0F;
    private static final float MAX_SCORE = 7.0F;
    private static final int OUTPUT_MIN = 15;
    private static final int OUTPUT_MAX = 90;

    public static float calculateScoreFactor(float[] features) {
        float[] copyFeatures = features.clone();

        for (int i = 0; i < NORMALIZATION_PARAMS.length; i++) {
            float mean = NORMALIZATION_PARAMS[i][0];
            float stdDev = NORMALIZATION_PARAMS[i][1];
            copyFeatures[i] = (copyFeatures[i] - mean) / stdDev;
        }

        float score = SCORE_INIT_VALUE;
        for (int i = 0; i < COEFFICIENTS.length; i++) {
            score += copyFeatures[i] * COEFFICIENTS[i];
        }

        return Math.max(MIN_SCORE, Math.min(score, MAX_SCORE));
    }

    private static int calculateNormalizedScore(float value) {
        final int score = (int) ((value * 14.0F) + 1.5F);
        return Math.min(Math.max(score, 1), 99);
    }

    public static byte calculateNormalizedFinalScore(float scoreFactor) {
        final float clampedValue = Math.max(MIN_SCORE, Math.min(scoreFactor, MAX_SCORE));
        final int clampedScore = calculateNormalizedScore(clampedValue);
        return (byte) Math.min(Math.max(clampedScore, OUTPUT_MIN), OUTPUT_MAX);
    }

    public static float calibrateScoreFactor(float calibrationScore, float scoreFactor) {
        calibrationScore = Math.max(40, Math.min(calibrationScore, 70));
        float calibrationScoreFactor = (float) (((calibrationScore - 1) * 7.0) / 98.0);
        float clampedCalibrationScoreFactor = Math.max(MIN_SCORE, Math.min(calibrationScoreFactor, MAX_SCORE));
        return (float) ((clampedCalibrationScoreFactor * 0.8) + (scoreFactor * 0.2));
    }

    public static byte calculateLevel(int score) {
        if (score <= 29) return 1;
        if (score <= 59) return 2;
        if (score <= 79) return 3;
        return 4;
    }
}