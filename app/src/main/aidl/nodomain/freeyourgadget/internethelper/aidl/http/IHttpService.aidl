package nodomain.freeyourgadget.internethelper.aidl.http;

import nodomain.freeyourgadget.internethelper.aidl.http.HttpRequest;
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpCallback;

interface IHttpService {
    int version();

    void send(in HttpRequest request, IHttpCallback cb);
}
