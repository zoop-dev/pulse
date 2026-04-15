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
            val convertedWeight = convertWeight(kg, target)
            return formatConvertedWeight(context, convertedWeight, target)
        }

        fun formatConvertedWeight(context: Context, convertedWeight: Double, target: WeightUnit): String {
            return when (target) {
                JIN -> {
                    context.getString(R.string.weight_scale_jin_format, convertedWeight)
                }

                POUND -> {
                    context.getString(R.string.weight_scale_pound_format, convertedWeight)
                }

                STONE -> {
                    val totalPounds: Int = (convertedWeight * 14).roundToInt()
                    val stone: Int = totalPounds / 14
                    val pound: Int = totalPounds % 14
                    context.getString(R.string.weight_scale_stone_format, stone, pound)
                }

                KILOGRAM -> {
                    if (convertedWeight != 0.0 && convertedWeight < 1) {
                        context.getString(R.string.weight_scale_gram_format, (convertedWeight * 1000).roundToInt())
                    } else {
                        context.getString(R.string.weight_scale_kilogram_format, convertedWeight)
                    }
                }
            }
        }
    }
}
