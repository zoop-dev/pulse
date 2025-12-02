package nodomain.freeyourgadget.internethelper.aidl.http;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class HttpRequest implements Parcelable {
    public enum Method {
        GET,
        POST,
        HEAD,
        PUT,
        PATCH,
        DELETE,
        OPTIONS,
    }

    private final String url;
    private final Method method;
    private final String body;
    private final String bodyContentType;
    private final boolean allowInsecure;
    private final HttpHeaders headers;

    protected HttpRequest(final Parcel in) {
        url = in.readString();
        method = Method.values()[in.readInt()];
        body = in.readString();
        bodyContentType = in.readString();
        allowInsecure = in.readByte() != 0;  // readBoolean() requires API level 29
        headers = in.readParcelable(HttpRequest.class.getClassLoader());
    }

    public HttpRequest(String url, Method method, String body, String bodyContentType, boolean allowInsecure, HttpHeaders headers) {
        this.url = url;
        this.method = method;
        this.body = body;
        this.bodyContentType = bodyContentType;
        this.allowInsecure = allowInsecure;
        this.headers = headers;
    }

    public static final Creator<HttpRequest> CREATOR = new Creator<>() {
        @Override
        public HttpRequest createFromParcel(final Parcel in) {
            return new HttpRequest(in);
        }

        @Override
        public HttpRequest[] newArray(final int size) {
            return new HttpRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeString(url);
        dest.writeInt(method.ordinal());
        dest.writeString(body);
        dest.writeString(bodyContentType);
        dest.writeByte((byte) (allowInsecure ? 1 : 0));  // writeBoolean() requires API level 29
        dest.writeParcelable(headers, 0);
    }

    public String getUrl() {
        return url;
    }

    public Method getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public String getBodyContentType() {
        return bodyContentType;
    }

    public boolean getAllowInsecure() {
        return allowInsecure;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }
}
