package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class RequestBuilder<T extends HuaweiPacket> {
    private static final Logger LOG = LoggerFactory.getLogger(RequestBuilder.class);

    public interface OnCallback<U extends HuaweiPacket> {
        void callback(U packet);
    }

    public interface OnTimeout<U extends HuaweiPacket> {
        void timeout(U packet);
    }

    public interface OnException {
        void except(Request.ResponseParseException e);
    }

    public static class GenericTypeException extends Request.ResponseParseException {
        public GenericTypeException(byte serviceId, byte commandId, Exception e) {
            super(String.format("Generic type exception for response for serviceId 0x%x, commandId 0x%x", serviceId, commandId), e);
        }
    }

    private final HuaweiSupportProvider supportProvider;
    private final byte serviceId;
    private final byte commandId;
    private final HuaweiPacket requestPacket;

    private boolean addToResponse = true;

    private OnCallback<T> onCallback = null;
    private OnTimeout<T> onTimeout = null;
    private OnException onException = null;

    public RequestBuilder(
            HuaweiSupportProvider supportProvider,
            byte serviceId,
            byte commandId,
            HuaweiPacket requestPacket
    ) {
        this.supportProvider = supportProvider;
        this.serviceId = serviceId;
        this.commandId = commandId;
        this.requestPacket = requestPacket;
    }

    public RequestBuilder<T> noResponse() {
        this.addToResponse = false;
        return this;
    }

    public RequestBuilder<T> onCallback(OnCallback<T> onCallback) {
        this.onCallback = onCallback;
        return this;
    }

    public RequestBuilder<T> onTimeout(OnTimeout<T> onTimeout) {
        this.onTimeout = onTimeout;
        return this;
    }

    public RequestBuilder<T> onException(OnException onException) {
        this.onException = onException;
        return this;
    }

    public Request build() {
        Request r = new Request(this.supportProvider);
        r.serviceId = serviceId;
        r.commandId = commandId;
        r.sendingPacket = requestPacket;
        r.addToResponse = this.addToResponse;
        r.setFinalizeReq(new Request.RequestCallback() {
            @Override
            public void call(Request request) {
                if (onCallback == null)
                    return;
                try {
                    // Try-catch takes care of the class cast exception, so ignore it
                    //noinspection unchecked
                    onCallback.callback((T) request.receivedPacket);
                } catch (ClassCastException e) {
                    handleException(new GenericTypeException(serviceId, commandId, e));
                }
            }

            @Override
            public void timeout(Request request) {
                if (onTimeout == null)
                    return;
                try {
                    // Try-catch takes care of the class cast exception, so ignore it
                    //noinspection unchecked
                    onTimeout.timeout((T) request.receivedPacket);
                } catch (ClassCastException e) {
                    handleException(new GenericTypeException(serviceId, commandId, e));
                }
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                if (onException == null) {
                    LOG.error("Unhandled exception in a request made by the request builder", e);
                    return;
                }
                onException.except(e);
            }
        });
        return r;
    }
}
