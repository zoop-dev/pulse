package nodomain.freeyourgadget.gadgetbridge.util.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.Locale

object DoubleAdapter : TypeAdapter<Double>() {
    override fun write(out: JsonWriter, value: Double?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(String.format(Locale.ROOT, "%.2f", value).toDouble())
        }
    }

    override fun read(input: JsonReader): Double {
        return input.nextDouble()
    }
}
