package nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

public abstract class AbstractGreeMessage {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(getTypeAdapterFactory())
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    @NonNull
    @Override
    public String toString() {
        return GSON.toJson(this);
    }

    public static TypeAdapterFactory getTypeAdapterFactory() {
        return RuntimeTypeAdapterFactory
                .of(AbstractGreeMessage.class, "t")
                .registerSubtype(GreePackMessage.class, GreePackMessage.TYPE)
                .registerSubtype(GreeBindMessage.class, GreeBindMessage.TYPE)
                .registerSubtype(GreeBleInfoMessage.class, GreeBleInfoMessage.TYPE)
                .registerSubtype(GreeBleKeyMessage.class, GreeBleKeyMessage.TYPE)
                .registerSubtype(GreeWlanMessage.class, GreeWlanMessage.TYPE)
                .recognizeSubtypes();
    }

    public static AbstractGreeMessage fromJson(final String json) {
        return GSON.fromJson(json, AbstractGreeMessage.class);
    }
}
