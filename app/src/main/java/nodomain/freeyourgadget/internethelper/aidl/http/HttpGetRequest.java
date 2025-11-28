package nodomain.freeyourgadget.internethelper.aidl.http;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class HttpGetRequest implements Parcelable {
    private final String url;
    private final boolean allowInsecure;
    private final HttpHeaders headers;

    protected HttpGetRequest(final Parcel in) {
        url = in.readString();
        allowInsecure = in.readByte() != 0;  // readBoolean() requires API level 29
        headers = in.readParcelable(HttpGetRequest.class.getClassLoader());
    }

    public HttpGetRequest(String url, boolean allowInsecure, HttpHeaders headers) {
        this.url = url;
        this.allowInsecure = allowInsecure;
        this.headers = headers;
    }

    public static final Creator<HttpGetRequest> CREATOR = new Creator<>() {
        @Override
        public HttpGetRequest createFromParcel(final Parcel in) {
            return new HttpGetRequest(in);
        }

        @Override
        public HttpGetRequest[] newArray(final int size) {
            return new HttpGetRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeString(url);
        dest.writeByte((byte) (allowInsecure ? 1 : 0));  // writeBoolean() requires API level 29
        dest.writeParcelable(headers, 0);
    }

    public String getUrl() {
        return url;
    }

    public boolean getAllowInsecure() {
        return allowInsecure;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }
}
