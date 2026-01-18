package nodomain.freeyourgadget.gadgetbridge.util.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class OffsetDateTimeAdapter : TypeAdapter<OffsetDateTime?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: OffsetDateTime?) {
        if (value == null) {
            out.nullValue()
        } else {
            val formatted = value.format(FMT)
            if (formatted.endsWith("Z")) {
                out.value(formatted.dropLast(1) + "+00:00")
            } else {
                out.value(formatted)
            }
        }
    }

    @Throws(IOException::class)
    override fun read(jsonReader: JsonReader): OffsetDateTime? {
        return OffsetDateTime.parse(jsonReader.nextString(), FMT)
    }

    companion object {
        private val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    }
}
