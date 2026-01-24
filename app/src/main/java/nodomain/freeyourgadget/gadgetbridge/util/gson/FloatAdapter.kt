package nodomain.freeyourgadget.gadgetbridge.util.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.Locale

object FloatAdapter : TypeAdapter<Float>() {
    override fun write(out: JsonWriter, value: Float?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(String.format(Locale.ROOT, "%.2f", value).toFloat())
        }
    }

    override fun read(input: JsonReader): Float {
        return input.nextDouble().toFloat()
    }
}
