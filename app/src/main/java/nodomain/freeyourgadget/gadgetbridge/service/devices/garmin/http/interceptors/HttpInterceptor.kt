package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors

import android.net.Uri
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpRequest
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpResponse

interface HttpInterceptor {
    fun supports(request: GarminHttpRequest): Boolean
    fun handle(request: GarminHttpRequest): GarminHttpResponse?
}
