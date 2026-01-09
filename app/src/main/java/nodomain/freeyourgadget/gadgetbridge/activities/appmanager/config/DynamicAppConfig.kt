package nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class DynamicAppConfig(open val key: String) : Parcelable {
    class AppConfigBoolean(
        override val key: String,
        val value: Boolean,
    ) : DynamicAppConfig(key)

    class AppConfigString(
        override val key: String,
        val value: String,
    ) : DynamicAppConfig(key)

    class AppConfigInteger(
        override val key: String,
        val value: Int,
    ) : DynamicAppConfig(key)

    class AppConfigFloat(
        override val key: String,
        val value: Float,
    ) : DynamicAppConfig(key)
}
