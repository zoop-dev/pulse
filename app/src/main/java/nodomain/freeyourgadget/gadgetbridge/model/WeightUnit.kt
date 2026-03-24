package nodomain.freeyourgadget.gadgetbridge.model

import android.content.Context
import nodomain.freeyourgadget.gadgetbridge.R
import kotlin.math.roundToInt

enum class WeightUnit {
    JIN,
    KILOGRAM,
    POUND,
    STONE,
    ;

    companion object {
        fun convertWeight(kg: Double, target: WeightUnit): Double {
            return when (target) {
                JIN -> kg * 2
                POUND -> kg / 0.45359237
                STONE -> kg / 0.45359237 / 14
                KILOGRAM -> kg
            }
        }

        fun formatWeight(context: Context, kg: Double, target: WeightUnit): String {
            return when (target) {
                JIN -> {
                    val jin: Double = kg * 2
                    context.getString(R.string.weight_scale_jin_format, jin)
                }

                POUND -> {
                    val pound: Double = kg / 0.45359237
                    context.getString(R.string.weight_scale_pound_format, pound)
                }

                STONE -> {
                    val total: Int = (kg / 0.45359237).roundToInt()
                    val stone: Int = total / 14
                    val pound: Int = total % 14
                    context.getString(R.string.weight_scale_stone_format, stone, pound)
                }

                KILOGRAM -> {
                    context.getString(R.string.weight_scale_kilogram_format, kg)
                }
            }
        }
    }
}
